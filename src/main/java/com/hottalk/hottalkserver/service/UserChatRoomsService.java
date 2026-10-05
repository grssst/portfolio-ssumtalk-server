package com.hottalk.hottalkserver.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hottalk.hottalkserver.dto.*;
import com.hottalk.hottalkserver.model.*;
import com.hottalk.hottalkserver.repository.*;
import com.hottalk.hottalkserver.util.DynamoDBUtils;
import com.hottalk.hottalkserver.util.JwtUtil;
import com.hottalk.hottalkserver.util.S3Uploader;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cglib.core.Local;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.messaging.simp.SimpMessagingTemplate;
@Service
public class UserChatRoomsService {

    @Value("${dynamodb.tableName}")
    private String tableName;
    private final UserChatRoomsRepository userChatRoomsRepository;
    private final ReportChattingRepository reportChattingRepository;
    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final MeetingRepository meetingRepository;
    private final S3Uploader s3Uploader;

    private final DynamoDbClient dynamoDbClient; // 여기에 클래스 필드로 선언
    private SimpMessagingTemplate messagingTemplate; // 필드로 주입
    private final FcmService fcmService;
    private final MeetingParticipantsRepository meetingParticipantsRepository;

    @Autowired
    public UserChatRoomsService(UserChatRoomsRepository userChatRoomsRepository, FcmService fcmService,
                                UserRepository userRepository, ReportChattingRepository reportChattingRepository, ReportRepository reportRepository,
                                @Value("${aws.accessKeyId}") String accessKey,
                                @Value("${aws.secretAccessKey}") String secretKey,
                                @Value("${aws.region}") String region,
                                MeetingRepository meetingRepository, SimpMessagingTemplate messagingTemplate, S3Uploader s3Uploader,
                                MeetingParticipantsRepository meetingParticipantsRepository) {
        this.userChatRoomsRepository = userChatRoomsRepository;
        this.fcmService = fcmService;
        this.userRepository = userRepository;
        this.reportRepository = reportRepository;
        this.reportChattingRepository = reportChattingRepository;
        this.meetingRepository = meetingRepository;
        this.s3Uploader = s3Uploader;
        this.meetingParticipantsRepository = meetingParticipantsRepository;
        this.messagingTemplate = messagingTemplate;
        // DynamoDB Client 초기화
        this.dynamoDbClient = DynamoDbClient.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)
                ))
                .build();
    }

    public User getUserProfile(String externalUserId) {
        // 외부 사용자 ID로 사용자 정보를 조회
        Optional<User> userOptional = userRepository.findByExternalUserId(externalUserId);

        // 사용자 정보를 찾으면 반환, 그렇지 않으면 예외 발생
        return userOptional.orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Transactional
    public void createChatRooms(SendMessageDTO sendMessageDTO, String token) {
        // JWT 토큰에서 externalUserId 추출
        String externalUserId = JwtUtil.validateToken(token.replace("Bearer ", ""));
        Long myId = getUserProfile(externalUserId).getId();
        String messageContent = sendMessageDTO.getMessageContent();
        Long recipientId = sendMessageDTO.getRecipientId();

        // roomId 생성
        String roomId = generateRoomId(myId, recipientId);

        System.out.println("메시지 내용: " + messageContent);
        System.out.println("나의 ID: " + myId);
        System.out.println("상대방 ID: " + recipientId);
        System.out.println("채팅방 ID: " + roomId);

        if (userChatRoomsRepository.existsByUserIdAndRoomId(myId, roomId)) {
            System.out.println("채팅방이 이미 존재합니다.");
            //메시지를 해당 다이나모DB 테이블에 메시지 보내듯 전송하는 로직 필요
            saveMessageToDynamoDB(roomId, myId, recipientId, messageContent);
            //SQS 푸시 전송 로직
            RealtimeChatDTO dto = new RealtimeChatDTO();
            dto.setRoomId(roomId);
            dto.setMessageContent(messageContent);
            dto.setRecipientId(String.valueOf(recipientId));
            dto.setSenderId(String.valueOf(myId));
            dto.setMyNickname("새로운 메시지 도착");
            fcmService.sendToSqs(dto);
            return;
        }
        else{
            // 나의 채팅방 정보 저장
            UserChatRooms myChatRoom = new UserChatRooms(new User(myId), roomId);
            userChatRoomsRepository.save(myChatRoom);


            // 상대방의 채팅방 정보 저장
            UserChatRooms recipientChatRoom = new UserChatRooms(new User(recipientId), roomId);
            userChatRoomsRepository.save(recipientChatRoom);

            System.out.println("채팅방이 생성되었습니다.");

            // 다이나모DB에 첫 번째 메시지 저장
            saveMessageToDynamoDB(roomId, myId, recipientId, messageContent);
        }
        RealtimeChatDTO dto = new RealtimeChatDTO();
        dto.setRoomId(roomId);
        dto.setMessageContent(messageContent);
        dto.setRecipientId(String.valueOf(recipientId));
        dto.setSenderId(String.valueOf(myId));
        dto.setMyNickname("새로운 메시지 도착");



        //SQS 푸시 전송 로직
        fcmService.sendToSqs(dto);

    }

    @Transactional
    public void createChatRoomsMeeting(SendMessageDTO sendMessageDTO, String token) {
        System.out.println("작동함");
        // JWT 토큰에서 externalUserId 추출
        String externalUserId = JwtUtil.validateToken(token.replace("Bearer ", ""));
        Long myId = getUserProfile(externalUserId).getId();
        String messageContent = sendMessageDTO.getMessageContent();
        Long recipientId = sendMessageDTO.getRecipientId();

        Optional<User> myUser = userRepository.findById(myId);

        User user = myUser.get();

        // roomId 생성
        String roomId = generateRoomIdMeeting(recipientId, myId);

        System.out.println("메시지 내용: " + messageContent);
        System.out.println("나의 ID: " + myId);
        System.out.println("상대방 ID: " + recipientId);
        System.out.println("채팅방 ID: " + roomId);

        boolean isExistingChatRoom = userChatRoomsRepository.existsByUserIdAndRoomId(myId, roomId);

        if (!isExistingChatRoom) {
            // 새로운 채팅방이면, DB에 저장
            UserChatRooms myChatRoom = new UserChatRooms(new User(myId), roomId);
            userChatRoomsRepository.save(myChatRoom);
            System.out.println("새로운 채팅방이 생성되었습니다.");
        } else {
            System.out.println("채팅방이 이미 존재합니다.");
        }

        // 다이나모DB에 메시지 저장
        saveMessageToDynamoDB(roomId, myId, recipientId, messageContent);
        // sender info
        User sender = userRepository.findById(myId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        // SQS 푸시 전송 로직

        // roomId가 "meeting_"으로 시작하면 "meeting_" 이후의 두 번째 부분만 추출 (예: "meeting_49_21" -> "49")
        // 2) SQS 푸시 – 모임 채팅방이면 참가자 전원에게, 아니면 1:1
        if (roomId.startsWith("meeting_")) {
            Long meetingId = Long.valueOf(roomId.split("_")[1]);
            List<MeetingParticipant> participants =
                    meetingParticipantsRepository.findByMeetingId(meetingId);

            for (MeetingParticipant mp : participants) {
                Long userId = mp.getUser().getId();
                if (userId.equals(myId)) continue;

                RealtimeChatDTO pushDto = new RealtimeChatDTO();
                pushDto.setRoomId(roomId);
                pushDto.setSenderId(String.valueOf(myId));
                pushDto.setMyNickname(sender.getNickname());
                pushDto.setMessageContent(messageContent);
                pushDto.setRecipientId(
                        String.format("meeting_%d_%d", meetingId, userId)
                );
                fcmService.sendToSqsMeeting(pushDto);
            }
        } else {
            RealtimeChatDTO pushDto = new RealtimeChatDTO();
            pushDto.setRoomId(roomId);
            pushDto.setSenderId(String.valueOf(myId));
            pushDto.setMyNickname(sender.getNickname());
            pushDto.setMessageContent(messageContent);
            pushDto.setRecipientId(String.valueOf(recipientId));
            fcmService.sendToSqsMeeting(pushDto);
        }



        // ✅ 올바른 숫자형 타임스탬프 (epoch milliseconds)
        Long kstTimestamp = Instant.now().toEpochMilli();

        // WebSocket 메시지 브로드캐스트 (첫 메시지 포함)
        RealtimeChatDTO message = new RealtimeChatDTO();
        message.setRoomId(roomId);
        message.setMessageContent(messageContent);
        //.setRecipientId(String.valueOf(recipientId));
        message.setTimestamp(Long.parseLong(String.valueOf(kstTimestamp)));
        message.setSenderNickname(user.getNickname());
        message.setSenderProfileImageUrl(user.getProfileImageUrl());
        // meeting_ 채팅방의 경우, roomId 정리
        if (message.getRoomId().startsWith("meeting_")) {
            String[] parts = message.getRoomId().split("_");
            if (parts.length >= 2) {
                String effectiveRoomId = parts[0] + "_" + parts[1];
                message.setEffectiveRoomId(effectiveRoomId);
            }
        }

        // WebSocket 브로드캐스팅 (첫 메시지도 포함)
        String destination = message.getRoomId().startsWith("meeting_") ?
                "/topic/chatroom/" + message.getEffectiveRoomId() :
                "/topic/chatroom/" + message.getRoomId();

        messagingTemplate.convertAndSend(destination, message);
    }

    public String generateRoomId(Long userId1, Long userId2) {
        Long smallerId = Math.min(userId1, userId2);
        Long largerId = Math.max(userId1, userId2);
        return "room_" + smallerId + "_" + largerId;
    }

    public String generateRoomIdMeeting(Long meetingId, Long userId) {
        return "meeting_" + meetingId + "_" + userId;
    }

    public void saveMessageToDynamoDB(String roomId, Long senderId, Long recipientId, String messageContent) {
        try {
            // 한국 시간(KST) 기준 ISO 8601 형식 타임스탬프 생성
            String kstTimestamp = ZonedDateTime.now(ZoneId.of("Asia/Seoul"))
                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

            Map<String, AttributeValue> item = new HashMap<>();
            item.put("room_id", AttributeValue.builder().s(roomId).build());
            item.put("timestamp", AttributeValue.builder().s(kstTimestamp).build());
            item.put("senderId", AttributeValue.builder().n(String.valueOf(senderId)).build());
            item.put("recipientId", AttributeValue.builder().n(String.valueOf(recipientId)).build());
            item.put("messageContent", AttributeValue.builder().s(messageContent).build());
            item.put("isRead", AttributeValue.builder().bool(false).build());
            //item.put("myNickname", AttributeValue.builder().s("0").build());

            // meeting 채팅의 경우 effective_room_id 필드 추가
            if (roomId.startsWith("meeting_")) {
                String[] parts = roomId.split("_");
                String effectiveRoomId = roomId;
                if (parts.length >= 2) {
                    effectiveRoomId = parts[0] + "_" + parts[1]; // 예: "meeting_23"
                }
                item.put("effective_room_id", AttributeValue.builder().s(effectiveRoomId).build());
            }

            PutItemRequest putItemRequest = PutItemRequest.builder()
                    .tableName(tableName)
                    .item(item)
                    .build();

            dynamoDbClient.putItem(putItemRequest);

            System.out.println("DynamoDB에 메시지가 저장되었습니다: " + messageContent);
        } catch (Exception e) {
            System.err.println("DynamoDB 저장 중 오류 발생: " + e.getMessage());
        }
    }



    public List<Map<String, AttributeValue>> getMessagesForRoom(String roomId) {
        try {
            Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
            String keyConditionExpression;
            QueryRequest.Builder queryRequestBuilder = QueryRequest.builder()
                    .tableName(tableName)
                    .scanIndexForward(false)
                    .limit(11);
            //System.out.println("roomId: " + roomId);
            //System.out.println("startsWith meeting_: " + roomId.startsWith("meeting_"));

            if (roomId.startsWith("meeting_")) {
                String[] parts = roomId.split("_");
                String effectiveRoomId = (parts.length >= 2) ? parts[0] + "_" + parts[1] : roomId;

                keyConditionExpression = "effective_room_id = :effectiveRoomId";
                expressionAttributeValues.put(":effectiveRoomId", AttributeValue.builder().s(effectiveRoomId).build());

                queryRequestBuilder.indexName("effective_room_id-timestamp-index");
            } else {
                // 반드시 일반 room_id 조회 시 이곳으로 명확히 떨어져야 함
                keyConditionExpression = "room_id = :roomId";
                expressionAttributeValues.put(":roomId", AttributeValue.builder().s(roomId).build());
            }

            queryRequestBuilder.keyConditionExpression(keyConditionExpression)
                    .expressionAttributeValues(expressionAttributeValues);

            QueryResponse queryResponse = dynamoDbClient.query(queryRequestBuilder.build());

            return queryResponse.items();
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }




    // 사용자 프로필 조회
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
    }

    public List<ChatRoomDTO> getChatRoomsForUser(String token) {
        String externalUserId = JwtUtil.validateToken(token.replace("Bearer ", ""));
        Long myId = getUserProfile(externalUserId).getId();

        // MySQL에서 사용자 참여 중인 채팅방 검색
        List<UserChatRooms> userChatRooms = userChatRoomsRepository.findByUserId(myId);

        // 채팅방 데이터 생성
        List<ChatRoomDTO> chatRoomDTOs = new ArrayList<>();
        for (UserChatRooms chatRoom : userChatRooms) {
            String roomId = chatRoom.getRoomId();
            LocalDateTime lastDisconnectAt = chatRoom.getLastDisconnectAt();
            // DynamoDB에서 메시지 가져오기
            List<Map<String, AttributeValue>> messages = getMessagesForRoom(roomId);

            if (!messages.isEmpty()) {
                Map<String, AttributeValue> latestMessage = messages.get(0);

                // 타임스탬프 파싱
                long timestamp = 0L;
                String timestampStr = latestMessage.get("timestamp") != null && latestMessage.get("timestamp").s() != null
                        ? latestMessage.get("timestamp").s()
                        : null;
                if (timestampStr != null) {
                    try {
                        ZonedDateTime zdt = ZonedDateTime.parse(timestampStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                        timestamp = zdt.toInstant().toEpochMilli();
                    } catch (Exception e) {
                        System.out.println("Invalid timestamp format: " + timestampStr);
                    }
                }

                // roomId가 "room"으로 시작하는 경우 (개인 채팅)
                if (roomId.startsWith("room")) {
                    Long otherUserId = extractOtherUserId(myId, roomId);
                    User otherUser = getUserById(otherUserId);

                    String nickname = otherUser.getNickname() != null ? otherUser.getNickname() : "Unknown";
                    int age = otherUser.getAge() != null ? otherUser.getAge() : 0;
                    String profileImageUrl = otherUser.getProfileImageUrl() != null ? otherUser.getProfileImageUrl() : "";
                    Double latitude = otherUser.getLocation() != null ? otherUser.getLocation().getY() : 0.0;
                    Double longitude = otherUser.getLocation() != null ? otherUser.getLocation().getX() : 0.0;
                    String gender = otherUser.getGender() != null ? otherUser.getGender() : "unknown";
                    String latestMessageContent = (latestMessage.get("messageContent") != null)
                            ? latestMessage.get("messageContent").s()
                            : "No messages yet";

                    ChatRoomDTO chatRoomDTO = new ChatRoomDTO(
                            roomId,
                            nickname,
                            age,
                            profileImageUrl,
                            latitude,
                            longitude,
                            latestMessageContent,
                            timestamp,
                            countUnreadMessages(messages, myId, lastDisconnectAt),
                            gender,
                            lastDisconnectAt,
                            myId
                    );
                    chatRoomDTOs.add(chatRoomDTO);
                }
                // roomId가 "meeting"으로 시작하는 경우 (모임 채팅)
                else if (roomId.startsWith("meeting")) {
                    String latestMessageContent = (latestMessage.get("messageContent") != null)
                            ? latestMessage.get("messageContent").s()
                            : "No messages yet";

                    // roomId 형식이 "meeting_14_21"이라면, 두 번째 부분이 모임 ID가 됩니다.
                    String[] parts = roomId.split("_");
                    Long meetingIdFromRoom = null;
                    try {
                        meetingIdFromRoom = Long.valueOf(parts[1]);
                    } catch (Exception e) {
                        System.out.println("Invalid meeting room format: " + roomId);
                        continue;
                    }
                    User senderUser = userRepository.findById(myId)
                            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
                    //System.out.println(senderUser.getProfileImageUrl());
                    // meetingRepository에서 모임 정보 조회 후 imageUrl 추출
                    Meeting meeting = meetingRepository.findById(meetingIdFromRoom)
                            .orElse(null);
                    String imageUrl = (meeting != null && meeting.getImageUrl() != null) ? meeting.getImageUrl() : "";
                    String roomName = (meeting != null && meeting.getTitle() != null) ? meeting.getTitle() : "";
                    String region = (meeting != null && meeting.getLocation() != null) ? meeting.getLocation() : "";
                    ChatRoomDTO chatRoomDTO = new ChatRoomDTO(
                            roomId,
                            roomName,         // nickname: 모임 채팅은 상대 정보 없이 처리
                            0,          // age
                            imageUrl,   // 모임의 imageUrl 사용
                            senderUser.getProfileImageUrl(),
                            0.0,        // latitude
                            0.0,        // longitude
                            latestMessageContent,
                            timestamp,
                            countUnreadMessages(messages, myId, lastDisconnectAt),
                            region,         // gender
                            lastDisconnectAt,
                            myId
                    );
                    chatRoomDTOs.add(chatRoomDTO);
                }
                else {
                    System.out.println("Invalid data for roomId or otherUser, skipping entry.");
                }
            }
        }
        return chatRoomDTOs;
    }


    public class DefaultLocation {
        public static Point createDefaultPoint() {
            GeometryFactory geometryFactory = new GeometryFactory();
            return geometryFactory.createPoint(new Coordinate(128.000000, 37.000000)); // (longitude=0, latitude=0)
        }
    }


    // roomId에서 상대방의 userId를 추출
    private Long extractOtherUserId(Long myId, String roomId) {
        String[] parts = roomId.split("_");
        Long id1 = Long.parseLong(parts[1]);
        Long id2 = Long.parseLong(parts[2]);
        return (id1.equals(myId)) ? id2 : id1;
    }

    // 읽지 않은 메시지 개수 계산
    private int countUnreadMessages(List<Map<String, AttributeValue>> messages, Long myId, LocalDateTime lastDisconnectAt) {
        int unreadCount = 0;

        for (Map<String, AttributeValue> message : messages) {
            Long senderId = Long.parseLong(message.get("senderId").n());
            Long recipientId = Long.parseLong(message.get("recipientId").n());


            // 메시지가 내가 받은 메시지인지 확인
            if (recipientId.equals(myId)) {

                String timestampStr = message.get("timestamp").s();

                try {
                    // 파싱 시 소수점 자릿수를 유연하게 처리
                    DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                            .append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                            .optionalStart()
                            .appendOffset("+HH:mm", "Z")
                            .optionalEnd()
                            .toFormatter();

                    ZonedDateTime zdt = ZonedDateTime.parse(timestampStr, formatter);
                    LocalDateTime messageTime = zdt.toLocalDateTime();


                    // 내 마지막 연결 시간 이후의 메시지인 경우
                    // 마지막 접속 해제 시간이 null인 경우 모든 메시지를 읽지 않은 메시지로 처리
                    if (lastDisconnectAt == null) {
                        unreadCount++;
                    }
                    else if(messageTime.isAfter(lastDisconnectAt)){
                        unreadCount++;
                    }

                } catch (Exception e) {
                    System.out.println("Invalid timestamp format: " + timestampStr);
                }
            }
        }

        return unreadCount;
    }


    public List<ChattingDTO> getChatting(String token, String roomId, String lastTimestamp, boolean refresh) {
        String externalUserId = JwtUtil.validateToken(token.replace("Bearer ", ""));
        User myProfile = getUserProfile(externalUserId);
        Long myId = myProfile.getId();
        String myNickname = myProfile.getNickname();

        // 만약 roomId가 "meeting_"으로 시작하면 앞의 두 부분만 effectiveRoomId로 사용
        String effectiveRoomId = roomId;
        if (roomId.startsWith("meeting_")) {
            String[] parts = roomId.split("_");
            if (parts.length >= 2) {
                effectiveRoomId = parts[0] + "_" + parts[1];
            }
        }

        System.out.println(roomId);
        // DynamoDB에서 메시지 가져오기 - effectiveRoomId를 사용
        Map<String, Object> result = getAllMessages(effectiveRoomId, lastTimestamp, refresh);
        List<Map<String, AttributeValue>> messages = (List<Map<String, AttributeValue>>) result.get("messages");

        // 메시지가 없으면 nextTimestamp는 null로 처리
        String nextTimestamp = (messages == null || messages.isEmpty()) ? null : (String) result.get("nextTimestamp");

        // 메시지를 ChattingDTO 리스트로 변환
        List<ChattingDTO> chattingDTOs = new ArrayList<>();
        if (messages != null) {
            for (Map<String, AttributeValue> message : messages) {
                String senderId = message.get("senderId") != null ? message.get("senderId").n() : null;
                String recipientId = message.get("recipientId") != null ? message.get("recipientId").n() : null;
                String messageContent = message.get("messageContent") != null ? message.get("messageContent").s() : null;
                String timestampStr = message.get("timestamp") != null ? message.get("timestamp").s() : null;
                String type = message.get("type") != null ? message.get("type").s() : "text";
                // 필수 필드가 모두 존재하는지 확인
                if (senderId != null && recipientId != null && messageContent != null && timestampStr != null) {
                    long timestamp = 0L;
                    try {
                        ZonedDateTime zdt = ZonedDateTime.parse(timestampStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                        timestamp = zdt.toInstant().toEpochMilli();
                    } catch (Exception e) {
                        System.err.println("Invalid timestamp format: " + timestampStr + " | Error: " + e.getMessage());
                        continue; // 잘못된 데이터는 스킵
                    }

                    ChattingDTO chattingDTO = new ChattingDTO(senderId, recipientId, messageContent, timestamp, myId, nextTimestamp, myNickname, type);
                    chattingDTOs.add(chattingDTO);
                } else {
                    System.out.println("불완전한 메시지 데이터: " + message);
                }
            }
        }

        return chattingDTOs;
    }

    public Map<String, Object> getMeetingChatting(String token, String roomId, String lastTimestamp, boolean refresh) {
        String externalUserId = JwtUtil.validateToken(token.replace("Bearer ", ""));
        User myProfile = getUserProfile(externalUserId);
        Long myId = myProfile.getId();
        String myNickname = myProfile.getNickname();

        String effectiveRoomId = roomId.startsWith("meeting_") ?
                String.join("_", Arrays.copyOf(roomId.split("_"), 2)) : roomId;

        Long meetingOrganizerId = null;
        if (roomId.startsWith("meeting_")) {
            String[] parts = roomId.split("_");
            if (parts.length >= 2) {
                Long meetingId = Long.valueOf(parts[1]); // meeting ID 추출
                meetingOrganizerId = getMeetingOrganizerId(meetingId); // 모임장 ID 가져오기
            }
        }
        if (lastTimestamp != null) {
                        // 스프링이 URL 디코딩 과정에서 '+'를 ' '로 바꿔 버린 경우가 있습니다.
                                // 원래 타임스탬프 문자열이랑 정확히 일치하려면 ' '를 '+'로 복원해야 DynamoDB 키가 유효해집니다.
                                        lastTimestamp = lastTimestamp.replace(' ', '+');
                    }
        // DynamoDB에서 메시지 가져오기
        Map<String, Object> result = getAllMessages(effectiveRoomId, lastTimestamp, refresh);
        List<Map<String, AttributeValue>> messages = (List<Map<String, AttributeValue>>) result.get("messages");
        String nextTimestamp = (messages == null || messages.isEmpty()) ? null : (String) result.get("nextTimestamp");

        // 캐싱으로 유저정보 중복 조회 방지
        Map<Long, User> userCache = new HashMap<>();
        List<ChattingDTO> chattingDTOs = new ArrayList<>();



        if (messages != null) {
            for (Map<String, AttributeValue> message : messages) {
                String senderIdStr = message.get("senderId").n();
                String recipientIdStr = message.get("recipientId").n();
                String messageContent = message.get("messageContent").s();
                String timestampStr = message.get("timestamp").s();
                String type = message.get("type") != null ? message.get("type").s() : "text";
                long timestamp;
                try {
                    timestamp = ZonedDateTime.parse(timestampStr).toInstant().toEpochMilli();
                } catch (Exception e) {
                    System.err.println("Invalid timestamp format: " + timestampStr);
                    continue; // 잘못된 데이터 스킵
                }


                Long senderId = Long.valueOf(senderIdStr);

                // senderId로 유저 정보 조회 및 캐싱
                User senderUser = userCache.computeIfAbsent(senderId, id -> userRepository.findById(id).orElse(null));

                if (senderUser == null) continue;

                ChattingDTO chattingDTO = new ChattingDTO(
                        senderIdStr,
                        recipientIdStr,
                        messageContent,
                        timestamp,
                        myId,
                        nextTimestamp,
                        myNickname,
                        senderUser.getNickname(),
                        senderUser.getProfileImageUrl(),
                        meetingOrganizerId,
                        type
                );

                chattingDTOs.add(chattingDTO);
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("chatting", chattingDTOs);
        response.put("nextTimestamp", nextTimestamp);

        return response;
    }

    public Long getMeetingOrganizerId(Long meetingId) {
        String oldestRoomId = userChatRoomsRepository.findOldestRoomIdByMeetingId(meetingId);

        if (oldestRoomId != null && oldestRoomId.split("_").length == 3) {
            String organizerIdStr = oldestRoomId.split("_")[2];
            return Long.valueOf(organizerIdStr);
        } else {
            throw new IllegalStateException("모임장을 찾을 수 없습니다.");
        }
    }

    public Map<String, Object> getAllMessages(String roomId, String lastTimestamp, boolean refresh) {
        try {
            System.out.println("▶ getAllMessages 호출: roomId=" + roomId +
                    ", lastTimestamp=" + lastTimestamp + ", refresh=" + refresh);

            // 1) 파티션 키 셋업
            String keyConditionExpression;
            Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
            if (roomId.startsWith("meeting_")) {
                String[] parts = roomId.split("_");
                String effectiveRoomId = parts.length >= 2 ? parts[0] + "_" + parts[1] : roomId;
                keyConditionExpression = "effective_room_id = :effectiveRoomId";
                expressionAttributeValues.put(
                        ":effectiveRoomId",
                        AttributeValue.builder().s(effectiveRoomId).build()
                );
            } else {
                keyConditionExpression = "room_id = :roomId";
                expressionAttributeValues.put(
                        ":roomId",
                        AttributeValue.builder().s(roomId).build()
                );
            }
            System.out.println("1️⃣ keyConditionExpression=" + keyConditionExpression);

            // 2) 페이징용 sort-key 별칭은 lastTimestamp가 있을 때만 등록
            String fullKeyCondition = keyConditionExpression;
            Map<String, String> expressionAttributeNames = new HashMap<>();
            if (lastTimestamp != null && !lastTimestamp.trim().isEmpty() && !"null".equalsIgnoreCase(lastTimestamp.trim())) {
                String fixedTs = lastTimestamp.replace(' ', '+');
                expressionAttributeNames.put("#ts", "timestamp");
                if (refresh) {
                    fullKeyCondition += " AND #ts > :lastTimestamp";
                } else {
                    fullKeyCondition += " AND #ts < :lastTimestamp";
                }
                expressionAttributeValues.put(
                        ":lastTimestamp",
                        AttributeValue.builder().s(fixedTs).build()
                );
                System.out.println("2️⃣ fullKeyCondition=" + fullKeyCondition + ", lastTimestamp=" + fixedTs);
            }

            // 3) QueryRequest 빌드
            QueryRequest.Builder qb = QueryRequest.builder()
                    .tableName(tableName)
                    .keyConditionExpression(fullKeyCondition)
                    .expressionAttributeValues(expressionAttributeValues)
                    .scanIndexForward(refresh)
                    .limit(60);

            // alias가 등록된 경우에만 넣어 줌
            if (!expressionAttributeNames.isEmpty()) {
                qb.expressionAttributeNames(expressionAttributeNames);
                System.out.println("3️⃣ ExpressionAttributeNames 설정: " + expressionAttributeNames);
            }

            // GSI 사용
            if (roomId.startsWith("meeting_")) {
                qb.indexName("effective_room_id-timestamp-index");
                System.out.println("4️⃣ GSI 인덱스 사용");
            }

            QueryRequest req = qb.build();
            System.out.println("5️⃣ QueryRequest: " + req);

            // 4) 실행
            QueryResponse resp = dynamoDbClient.query(req);
            System.out.println("6️⃣ QueryResponse 받음, items=" + resp.items().size());

            // 5) nextTimestamp 계산
            Map<String, AttributeValue> lek = resp.lastEvaluatedKey();
            String nextTimestamp = (lek != null && lek.containsKey("timestamp"))
                    ? lek.get("timestamp").s()
                    : null;
            System.out.println("7️⃣ nextTimestamp=" + nextTimestamp);

            // 6) 리턴
            Map<String, Object> result = new HashMap<>();
            result.put("messages", resp.items());
            result.put("nextTimestamp", nextTimestamp);
            return result;

        } catch (Exception e) {
            System.err.println("❌ getAllMessages 예외 발생!");
            e.printStackTrace();
            return Collections.emptyMap();
        }
    }







    public String getAllMessagesReport(String roomId, Long reporterUserId) {
        try {
            System.out.println("채팅내역 백업");
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



    public void updateLastDisconnectAt(Long userId, String roomId, LocalDateTime disconnectAt) {
        try {
            UserChatRooms userChatRoom = userChatRoomsRepository.findByUserIdAndRoomId(userId, roomId)
                    .orElseThrow(() -> new IllegalArgumentException("해당 사용자와 채팅방의 레코드를 찾을 수 없습니다."));

            userChatRoom.setLastDisconnectAt(disconnectAt);
            userChatRoomsRepository.save(userChatRoom);
        } catch (Exception e){
            System.out.println("채팅방이 존재하지 않습니다");
        }
    }
    @Transactional
    public void removeChatRoomAndDynamoRecord(String roomId) {
        // 신고된 채팅방인지 확인
        Optional<Report> latestReport = reportRepository.findTopByRoomIdOrderByReportedAtDesc(roomId);
        if (latestReport.isPresent()) {
            Report report = latestReport.get();

            // 해당 roomId에 대한 채팅방 생성 시간 중 하나를 선택
            List<UserChatRooms> chatRooms = userChatRoomsRepository.findByRoomId(roomId);

            if (!chatRooms.isEmpty()) {
                // 리스트의 첫 번째 항목 선택
                UserChatRooms selectedChatRoom = chatRooms.get(0);

                // 신고 시간이 채팅방 생성 시간 이후라면 채팅 내역 기록
                if (report.getReportedAt().isAfter(selectedChatRoom.getCreatedAt())) {
                    getAllMessagesReport(roomId, report.getReporterUserId());
                }
            }
        }

        //채팅방 삭제
        userChatRoomsRepository.deleteByRoomId(roomId);

        //채팅 내역 삭제
        // DynamoDB에서 해당 roomId의 모든 레코드 삭제
        try {
            Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
            expressionAttributeValues.put(":roomId", AttributeValue.builder().s(roomId).build());

            QueryRequest queryRequest = QueryRequest.builder()
                    .tableName(tableName)
                    .keyConditionExpression("room_id = :roomId")
                    .expressionAttributeValues(expressionAttributeValues)
                    .build();

            QueryResponse queryResponse;

            do {
                queryResponse = dynamoDbClient.query(queryRequest);

                // Batch 삭제 요청 생성
                List<WriteRequest> deleteRequests = new ArrayList<>();
                for (Map<String, AttributeValue> item : queryResponse.items()) {
                    Map<String, AttributeValue> key = Map.of(
                            "room_id", item.get("room_id"),
                            "timestamp", item.get("timestamp")
                    );
                    deleteRequests.add(WriteRequest.builder()
                            .deleteRequest(DeleteRequest.builder().key(key).build())
                            .build());
                }

                // BatchWriteItem 처리
                if (!deleteRequests.isEmpty()) {
                    for (int i = 0; i < deleteRequests.size(); i += 25) {
                        List<WriteRequest> batch = deleteRequests.subList(i, Math.min(i + 25, deleteRequests.size()));

                        BatchWriteItemRequest batchWriteRequest = BatchWriteItemRequest.builder()
                                .requestItems(Map.of(tableName, batch))
                                .build();

                        // BatchWriteItem 요청
                        BatchWriteItemResponse response = dynamoDbClient.batchWriteItem(batchWriteRequest);

                        // 실패 항목 재처리
                        if (!response.unprocessedItems().isEmpty()) {
                            retryUnprocessedItems(response.unprocessedItems());
                        }
                    }
                }

                // 다음 페이지로 이동
                if (queryResponse.hasLastEvaluatedKey()) {
                    queryRequest = queryRequest.toBuilder()
                            .exclusiveStartKey(queryResponse.lastEvaluatedKey())
                            .build();
                } else {
                    break;
                }
            } while (true);
            s3Uploader.deleteChattingImage(roomId);
            System.out.println("DynamoDB에서 roomId " + roomId + "의 모든 레코드가 삭제되었습니다.");
        } catch (Exception e) {
            System.err.println("DynamoDB 삭제 중 오류 발생: " + e.getMessage());
        }
    }

    @Transactional
    public void removeMeetingRoomAndDynamoRecord(String roomId) {
        // 신고된 채팅방인지 확인
        System.out.println("신고된지 확인할 룸아이디" + roomId);
        Optional<Report> latestReport = reportRepository.findTopByRoomIdOrderByReportedAtDesc(roomId);
        if (latestReport.isPresent()) {
            Report report = latestReport.get();

            // 해당 roomId에 대한 채팅방 생성 시간 중 하나를 선택
            List<UserChatRooms> chatRooms = userChatRoomsRepository.findByRoomId(roomId);

            if (!chatRooms.isEmpty()) {
                // 리스트의 첫 번째 항목 선택
                UserChatRooms selectedChatRoom = chatRooms.get(0);

                // 신고 시간이 채팅방 생성 시간 이후라면 채팅 내역 기록
                if (report.getReportedAt().isAfter(selectedChatRoom.getCreatedAt())) {
                    System.out.println("신고된 채팅방입니다");
                    getAllMessagesReport(roomId, report.getReporterUserId());
                }
            }
        }

        //채팅방 삭제
        userChatRoomsRepository.deleteByRoomId(roomId);

        //채팅 내역 삭제
        // DynamoDB에서 해당 roomId의 모든 레코드 삭제
        try {
            Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
            expressionAttributeValues.put(":roomId", AttributeValue.builder().s(roomId).build());

            QueryRequest queryRequest = QueryRequest.builder()
                    .tableName(tableName)
                    .keyConditionExpression("room_id = :roomId")
                    .expressionAttributeValues(expressionAttributeValues)
                    .build();

            QueryResponse queryResponse;

            do {
                queryResponse = dynamoDbClient.query(queryRequest);

                // Batch 삭제 요청 생성
                List<WriteRequest> deleteRequests = new ArrayList<>();
                for (Map<String, AttributeValue> item : queryResponse.items()) {
                    Map<String, AttributeValue> key = Map.of(
                            "room_id", item.get("room_id"),
                            "timestamp", item.get("timestamp")
                    );
                    deleteRequests.add(WriteRequest.builder()
                            .deleteRequest(DeleteRequest.builder().key(key).build())
                            .build());
                }

                // BatchWriteItem 처리
                if (!deleteRequests.isEmpty()) {
                    for (int i = 0; i < deleteRequests.size(); i += 25) {
                        List<WriteRequest> batch = deleteRequests.subList(i, Math.min(i + 25, deleteRequests.size()));

                        BatchWriteItemRequest batchWriteRequest = BatchWriteItemRequest.builder()
                                .requestItems(Map.of(tableName, batch))
                                .build();

                        // BatchWriteItem 요청
                        BatchWriteItemResponse response = dynamoDbClient.batchWriteItem(batchWriteRequest);

                        // 실패 항목 재처리
                        if (!response.unprocessedItems().isEmpty()) {
                            retryUnprocessedItems(response.unprocessedItems());
                        }
                    }
                }

                // 다음 페이지로 이동
                if (queryResponse.hasLastEvaluatedKey()) {
                    queryRequest = queryRequest.toBuilder()
                            .exclusiveStartKey(queryResponse.lastEvaluatedKey())
                            .build();
                } else {
                    break;
                }
            } while (true);
            s3Uploader.deleteChattingImage(roomId);
            System.out.println("DynamoDB에서 roomId " + roomId + "의 모든 레코드가 삭제되었습니다.");
        } catch (Exception e) {
            System.err.println("DynamoDB 삭제 중 오류 발생: " + e.getMessage());
        }
    }

    // 실패 항목 재처리 메서드
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
