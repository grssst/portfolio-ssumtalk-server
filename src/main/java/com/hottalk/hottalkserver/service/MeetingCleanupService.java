package com.hottalk.hottalkserver.service;


import com.hottalk.hottalkserver.model.*;
import com.hottalk.hottalkserver.repository.*;
import com.hottalk.hottalkserver.util.DynamoDBUtils;
import com.hottalk.hottalkserver.util.S3Uploader;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.LocalDate;
import java.util.*;

@Service
public class MeetingCleanupService {

    private final MeetingRepository meetingRepository;
    private final MeetingParticipantsRepository meetingParticipantsRepository;
    private final UserChatRoomsRepository userChatRoomsRepository;
    private final GalleryImageRepository galleryImageRepository;
    private final S3Uploader s3Uploader;
    @Value("${dynamodb.tableName}")
    private String tableName;
    private final PeriodicMeetingRepository periodicMeetingRepository;
    private final ReportChattingRepository reportChattingRepository;
    private final BoardRepository boardRepository;
    private final ReportRepository reportRepository;
    private final DynamoDbClient dynamoDbClient; // 여기에 클래스 필드로 선언
    @Autowired
    public MeetingCleanupService(MeetingRepository meetingRepository,
                                 MeetingParticipantsRepository meetingParticipantsRepository, S3Uploader s3Uploader,
                                 UserChatRoomsRepository userChatRoomsRepository,@Value("${aws.accessKeyId}") String accessKey,
                                 @Value("${aws.secretAccessKey}") String secretKey,
                                 @Value("${aws.region}") String region,
                                 GalleryImageRepository galleryImageRepository, PeriodicMeetingRepository periodicMeetingRepository,
                                 BoardRepository boardRepository, ReportRepository reportRepository, ReportChattingRepository reportChattingRepository
                                ) {
        this.meetingRepository = meetingRepository;
        this.meetingParticipantsRepository = meetingParticipantsRepository;
        this.s3Uploader = s3Uploader;
        this.galleryImageRepository = galleryImageRepository;
        this.userChatRoomsRepository = userChatRoomsRepository;
        this.boardRepository = boardRepository;
        this.reportRepository = reportRepository;
        this.periodicMeetingRepository = periodicMeetingRepository;
        this.dynamoDbClient = DynamoDbClient.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)
                ))
                .build();
        this.reportChattingRepository = reportChattingRepository;
    }

    /**
     * 매일 자정에 실행하여 만료된 모임을 자동 삭제합니다.
     * (subscriptionEndDate가 현재 날짜보다 이전인 모임)
     */
    @Scheduled(cron = "0 0 0 * * *") // 매일 9시 0분 0초에 실행
    @Transactional
    public void cleanupExpiredMeetings() {
        LocalDate today = LocalDate.now();
        List<Meeting> expiredMeetings = meetingRepository.findBySubscriptionEndDateBefore(today);
        if (!expiredMeetings.isEmpty()) {




            for (Meeting meeting : expiredMeetings) {

                // 🔹 참가자 삭제 (meetingId가 아니라 Meeting 객체를 기준으로 삭제)
                meetingParticipantsRepository.deleteByMeeting(meeting);

                // 🔹 채팅방 삭제
                String prefix = "meeting_" + meeting.getId() + "_";
                userChatRoomsRepository.deleteByRoomIdStartingWith(prefix);

                // 🔹 갤러리 사진 삭제
                // 미팅에 속한 모든 갤러리 이미지 가져오기
                List<GalleryImage> galleryImages = galleryImageRepository.findByMeetingId(String.valueOf(meeting.getId()));
                if (!galleryImages.isEmpty()) {
                    // 각 이미지에 대해 S3에서 파일 삭제
                    for (GalleryImage image : galleryImages) {
                        s3Uploader.deletePostImage(image.getImageUrl());
                    }
                    // DB에서 이미지 레코드 삭제
                    galleryImageRepository.deleteAll(galleryImages);
                }

                // 🔹 정기모임 삭제
                // 미팅에 속한 모든 정기모임 가져오기
                List<PeriodicMeeting> periodicMeetings = periodicMeetingRepository.findByMeetingId(meeting.getId());
                if (!periodicMeetings.isEmpty()) {
                    periodicMeetingRepository.deleteAll(periodicMeetings);
                }

                // 🔹 게시판 글 삭제
                // 미팅에 속한 모든 게시글 가져오기
                List<Board> boardPosts = boardRepository.findByMeetingId(meeting.getId());
                if (!boardPosts.isEmpty()) {
                    boardRepository.deleteAll(boardPosts);
                }
                // 🔹 모임 삭제
                meetingRepository.deleteById(meeting.getId());

                // 🔹 DynamoDB에서 채팅 내역 삭제
                removeChatRoomAndDynamoRecord("meeting_" + meeting.getId());

                s3Uploader.deletePostImage(meeting.getImageUrl());


            }

            System.out.println("Deleted " + expiredMeetings.size() + " expired meetings.");
        }



    }


    @Transactional
    public void removeChatRoomAndDynamoRecord(String roomId) {
        // ✅ 모임 채팅방인지 확인
        boolean isMeetingRoom = roomId.startsWith("meeting_");
        System.out.println("룸아이디:" + roomId);
        // ✅ 신고된 채팅방인지 확인 후, 필요하면 백업
        Optional<Report> latestReport = reportRepository.findTopByRoomIdOrderByReportedAtDesc(roomId);
        if (latestReport.isPresent()) {
            Report report = latestReport.get();

            // 채팅방 생성 시간 확인
            List<UserChatRooms> chatRooms = userChatRoomsRepository.findByRoomId(roomId);
            if (!chatRooms.isEmpty()) {
                UserChatRooms selectedChatRoom = chatRooms.get(0);
                if (report.getReportedAt().isAfter(selectedChatRoom.getCreatedAt())) {
                    getAllMessagesReport(roomId, report.getReporterUserId());
                }
            }
        }

        // ✅ 채팅방 삭제
        userChatRoomsRepository.deleteByRoomIdContains(roomId);

        // ✅ 모임이면 참가자 목록과 모임도 삭제
        if (isMeetingRoom) {
            Long meetingId = Long.parseLong(roomId.replace("meeting_", ""));
            Optional<Meeting> meetingOptional = meetingRepository.findById(meetingId);

            if (meetingOptional.isPresent()) {
                Meeting meeting = meetingOptional.get();
                meetingParticipantsRepository.deleteByMeeting(meeting);
                meetingRepository.deleteById(meetingId);
            } else {
                System.out.println("Meeting not found, skipping deletion.");
            }
        }


        // ✅ DynamoDB에서 채팅 내역 삭제
        deleteDynamoDBRecords(roomId);
    }

    public String getAllMessagesReport(String roomId, Long reporterUserId) {
        try {
            Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
            expressionAttributeValues.put(":roomId", AttributeValue.builder().s(roomId).build());

            QueryRequest.Builder queryRequestBuilder = QueryRequest.builder()
                    .tableName(tableName)
                    .keyConditionExpression("room_id = :roomId")
                    .expressionAttributeValues(expressionAttributeValues)
                    .scanIndexForward(true); // 오름차순 정렬

            QueryResponse queryResponse;

            // 모든 메시지를 가져오기
            do {
                queryResponse = dynamoDbClient.query(queryRequestBuilder.build());

                // 각 항목을 엔티티로 변환 후 저장
                for (Map<String, AttributeValue> item : queryResponse.items()) {
                    ReportChatting entity = DynamoDBUtils.mapToEntity(item);
                    entity.setReporterUserId(reporterUserId);

                    // 만약 메시지 타입이 image이거나, messageContent가 특정 접두어(예: S3 CloudFront URL)로 시작하면 백업 처리
                    // 예: "https://d2rhx7q10awn3j.cloudfront.net/"
                    if (entity.getMessageContent() != null
                            && entity.getMessageContent().startsWith("https://d2rhx7q10awn3j.cloudfront.net/")) {
                        // 백업 S3 버킷의 이름 또는 백업 경로를 지정합니다.
                        s3Uploader.backupReportChattingImage(entity.getMessageContent(), roomId);
                    }

                    reportChattingRepository.save(entity);
                }

                if (queryResponse.hasLastEvaluatedKey()) {
                    queryRequestBuilder.exclusiveStartKey(queryResponse.lastEvaluatedKey());
                } else {
                    break;
                }
            } while (true);

            System.out.println("Messages successfully saved to the database.");

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Failed to save messages.");
        }
        return "save success";
    }

    private void deleteDynamoDBRecords(String roomId) {
        try {
            String keyConditionExpression;
            Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
            String effectiveRoomId = roomId;

            // ✅ 모임 채팅방인지 확인 (effective_room_id 사용)
            boolean isMeetingRoom = roomId.startsWith("meeting_");
            if (isMeetingRoom) {
                String[] parts = roomId.split("_");
                if (parts.length >= 2) {
                    effectiveRoomId = parts[0] + "_" + parts[1]; // 예: "meeting_31"
                }
                keyConditionExpression = "effective_room_id = :effectiveRoomId";
                expressionAttributeValues.put(":effectiveRoomId", AttributeValue.builder().s(effectiveRoomId).build());
            } else {
                keyConditionExpression = "room_id = :roomId";
                expressionAttributeValues.put(":roomId", AttributeValue.builder().s(roomId).build());
            }

            QueryRequest.Builder queryRequestBuilder = QueryRequest.builder()
                    .tableName(tableName)
                    .keyConditionExpression(keyConditionExpression)
                    .expressionAttributeValues(expressionAttributeValues)
                    .limit(50) // ✅ 한 번에 50개씩 조회하여 삭제

                    // ✅ GSI 사용 (모임 채팅방인 경우 `effective_room_id-timestamp-index` 명시적 지정)
                    .indexName(isMeetingRoom ? "effective_room_id-timestamp-index" : null);

            QueryResponse queryResponse;

            do {
                queryResponse = dynamoDbClient.query(queryRequestBuilder.build());

                // ✅ 조회한 데이터에서 room_id와 timestamp를 포함하여 삭제 요청 생성
                List<WriteRequest> deleteRequests = new ArrayList<>();
                for (Map<String, AttributeValue> item : queryResponse.items()) {
                    // ✅ room_id가 존재하는지 확인 후 삭제 요청 추가
                    if (item.containsKey("room_id") && item.containsKey("timestamp")) {
                        Map<String, AttributeValue> key = Map.of(
                                "room_id", item.get("room_id"),
                                "timestamp", item.get("timestamp"));

                        deleteRequests.add(WriteRequest.builder()
                                .deleteRequest(DeleteRequest.builder().key(key).build())
                                .build());
                    }
                }

                // ✅ BatchWriteItem 처리 (25개씩)
                if (!deleteRequests.isEmpty()) {
                    for (int i = 0; i < deleteRequests.size(); i += 25) {
                        List<WriteRequest> batch = deleteRequests.subList(i, Math.min(i + 25, deleteRequests.size()));

                        BatchWriteItemRequest batchWriteRequest = BatchWriteItemRequest.builder()
                                .requestItems(Map.of(tableName, batch))
                                .build();

                        // ✅ BatchWriteItem 요청
                        BatchWriteItemResponse response = dynamoDbClient.batchWriteItem(batchWriteRequest);

                        // ✅ 실패 항목 재시도
                        if (!response.unprocessedItems().isEmpty()) {
                            retryUnprocessedItems(response.unprocessedItems());
                        }
                    }
                }

                // ✅ 다음 페이지 처리 (lastEvaluatedKey 존재 시 계속 진행)
                if (queryResponse.hasLastEvaluatedKey()) {
                    queryRequestBuilder = queryRequestBuilder.exclusiveStartKey(queryResponse.lastEvaluatedKey());
                } else {
                    break;
                }
            } while (true);

            System.out.println("✅ DynamoDB에서 roomId " + roomId + "의 모든 레코드가 삭제되었습니다.");
        } catch (Exception e) {
            System.err.println("❌ DynamoDB 삭제 중 오류 발생: " + e.getMessage());
        }
    }

    private void retryUnprocessedItems(Map<String, List<WriteRequest>> unprocessedItems) {
        unprocessedItems.forEach((table, writeRequests) -> {
            for (WriteRequest request : writeRequests) {
                dynamoDbClient.batchWriteItem(BatchWriteItemRequest.builder()
                        .requestItems(Map.of(table, List.of(request)))
                        .build());
            }
        });
    }
}
