package com.hottalk.hottalkserver.service;


import com.hottalk.hottalkserver.dto.*;
import com.hottalk.hottalkserver.model.*;
import com.hottalk.hottalkserver.repository.*;
import com.hottalk.hottalkserver.util.DynamoDBUtils;
import com.hottalk.hottalkserver.util.JwtUtil;
import com.hottalk.hottalkserver.util.S3Uploader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MeetingService {
    private final MeetingRepository meetingRepository;
    private final BlockedUserRepository blockedUserRepository;
    private final BoardRepository boardRepository;
    private final S3Uploader s3Uploader;
    @Value("${dynamodb.tableName}")
    private String tableName;
    private final UserRepository userRepository;
    private final UserChatRoomsRepository userChatRoomsRepository;
    private final GalleryImageRepository galleryImageRepository;
    private final MeetingParticipantsRepository meetingParticipantsRepository;
    private SimpMessagingTemplate messagingTemplate; // 필드로 주입
    private final PeriodicMeetingRepository periodicMeetingRepository;
    private final ReportRepository reportRepository;
    private final DynamoDbClient dynamoDbClient; // 여기에 클래스 필드로 선언
    private final ReportChattingRepository reportChattingRepository;
    private final BalanceHistoryRepository balanceHistoryRepository;

    @Autowired
    public MeetingService(MeetingRepository meetingRepository, S3Uploader s3Uploader, UserRepository userRepository,
                          BlockedUserRepository blockedUserRepository, MeetingParticipantsRepository meetingParticipantsRepository,
                          PeriodicMeetingRepository periodicMeetingRepository, UserChatRoomsRepository userChatRoomsRepository,
                          SimpMessagingTemplate messagingTemplate, ReportRepository reportRepository, @Value("${aws.accessKeyId}") String accessKey,
                          @Value("${aws.secretAccessKey}") String secretKey,
                          @Value("${aws.region}") String region, GalleryImageRepository galleryImageRepository,
                          ReportChattingRepository reportChattingRepository, BalanceHistoryRepository balanceHistoryRepository, BoardRepository boardRepository) {
        this.meetingRepository = meetingRepository;
        this.s3Uploader = s3Uploader;
        this.userRepository = userRepository;
        this.blockedUserRepository = blockedUserRepository;
        this.messagingTemplate = messagingTemplate;
        this.meetingParticipantsRepository = meetingParticipantsRepository;
        this.userChatRoomsRepository = userChatRoomsRepository;
        this.balanceHistoryRepository = balanceHistoryRepository;
        this.reportRepository = reportRepository;
        this.reportChattingRepository = reportChattingRepository;
        this.galleryImageRepository = galleryImageRepository;
        this.periodicMeetingRepository = periodicMeetingRepository;
        this.boardRepository = boardRepository;
        this.dynamoDbClient = DynamoDbClient.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)
                ))
                .build();
    }

    public Long saveMeeting(Meeting meeting, MultipartFile image) throws IOException {
        if (image != null && !image.isEmpty()) {
            String imageUrl = s3Uploader.uploadMeetingProfileImage(image, meeting.getExternalUserId());
            meeting.setImageUrl(imageUrl);
        }

        meetingRepository.save(meeting);
        return meeting.getId();
    }
    /**
     * 모임장인지 확인 후, 특정 멤버를 추방하는 로직
     * @param meetingId 추방을 진행할 모임 ID
     * @param organizerExternalUserId 실제로 요청을 보낸 (추방을 시도하는) 사람
     * @param targetExternalUserId 추방될 대상 externalUserId
     */
    @Transactional
    public void kickMember(Long meetingId, String organizerExternalUserId, String targetExternalUserId) throws Exception {
        // (1) 모임 조회
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 모임입니다."));

        // (2) 요청자가 모임 대표인지 확인
        if (!meeting.getExternalUserId().equals(organizerExternalUserId)) {
            throw new Exception("모임 대표만 멤버를 추방할 수 있습니다.");
            // -> 보통 RuntimeException (ex. UnsupportedOperationException)으로 던집니다.
        }

        // (3) 추방 대상 멤버 조회
        MeetingParticipant member = meetingParticipantsRepository
                .findByMeetingIdAndUser_ExternalUserId(meetingId, targetExternalUserId)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자는 모임 멤버가 아닙니다."));

        // (4) 멤버 DB 삭제
        meetingParticipantsRepository.delete(member);

        // (5) 상대 유저 조회 후 쪽지방 삭제 (선택 로직)
        Optional<User> target = userRepository.findByExternalUserId(targetExternalUserId);
        //System.out.println("추방당한 사람 닉네임:"+ target.get().getNickname());
        userChatRoomsRepository.deleteByRoomId("meeting_"+ meetingId + "_" + target.get().getId());
        //System.out.println("삭제될 채팅방:" + "meeting_"+ meetingId + "_" +target.get().getId());
    }

    @Transactional
    public List<MeetingDTO> getAllMeetings(String externalUserId, String location, String selectedTopics) {
        try {
            List<Meeting> meetings;
            boolean hasLocation = location != null && !location.isEmpty();
            boolean hasTopics = selectedTopics != null && !selectedTopics.isEmpty();

            if (hasLocation) {
                // 쉼표로 구분된 지역들을 리스트로 변환 (정확한 일치 비교)
                List<String> locationList = Arrays.stream(location.split(","))
                        .map(String::trim)
                        .collect(Collectors.toList());
                if (hasTopics) {
                    // 쉼표로 구분된 주제들도 리스트로 변환
                    List<String> topicsList = Arrays.stream(selectedTopics.split(","))
                            .map(String::trim)
                            .collect(Collectors.toList());
                    meetings = meetingRepository.findByLocationInAndTopicInAndIsActiveTrueOrderByParticipantCountDesc(locationList, topicsList);
                } else {
                    meetings = meetingRepository.findByLocationInAndIsActiveTrueOrderByParticipantCountDesc(locationList);
                }
            } else if (hasTopics) {
                List<String> topicsList = Arrays.stream(selectedTopics.split(","))
                        .map(String::trim)
                        .collect(Collectors.toList());
                meetings = meetingRepository.findByTopicInAndIsActiveTrueOrderByParticipantCountDesc(topicsList);
            } else {
                meetings = meetingRepository.findByIsActiveTrueOrderByParticipantCountDesc();
            }

            return meetings.stream()
                    .map(MeetingDTO::fromEntity)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }




    @Transactional
    public void joinMeeting(String externalUserId, Long meetingId, boolean firstCreate) {
        Optional<Meeting> meetingOptional = meetingRepository.findById(meetingId);
        if (!meetingOptional.isPresent()) {
            throw new IllegalArgumentException("해당 모임이 존재하지 않습니다.");
        }
        Meeting meeting = meetingOptional.get();

        // 참가자 User 조회
        Optional<User> participantOptional = userRepository.findByExternalUserId(externalUserId);
        if (!participantOptional.isPresent()) {
            throw new IllegalArgumentException("참가자 정보가 존재하지 않습니다.");
        }
        User participant = participantOptional.get();

        // 모임 대표의 외부 사용자 ID는 meeting.getExternalUserId()로 가져옵니다.
        String organizerExternalUserId = meeting.getExternalUserId();
        if (organizerExternalUserId.equals(externalUserId) && !firstCreate) {
            throw new IllegalArgumentException("모임 대표는 참가할 수 없습니다.");
        }

        // 차단 관계 확인: 모임 대표와 참가자 간 양방향 차단 여부 확인
        if (isBlocked(organizerExternalUserId, externalUserId)) {
            throw new IllegalArgumentException("차단 관계로 인해 참가할 수 없습니다.");
        }

        // 이미 참가한 사용자인지 확인 (중복 참가 방지)
        List<MeetingParticipant> participants = meeting.getParticipants();
        if (participants != null) {
            boolean alreadyJoined = participants.stream()
                    .anyMatch(mp -> mp.getUser().getId().equals(participant.getId()));
            if (alreadyJoined) {
                throw new IllegalArgumentException("이미 참가한 사용자입니다.");
            }
        } else {
            participants = new ArrayList<>();
        }

        // 최대 참가자 수 체크
        // 최대 참가자 수 체크 (참가자 수가 꽉 찼으면 400 에러 발생)
        if (meeting.getMaxParticipants() != null && participants.size() >= meeting.getMaxParticipants()) {
            throw new IllegalArgumentException("참가자 수가 꽉 찼습니다.");
        }

        // MeetingParticipant 객체 생성 (Lombok @Builder 사용)
        MeetingParticipant meetingParticipant = MeetingParticipant.builder()
                .meeting(meeting)
                .user(participant)
                .build();

        participants.add(meetingParticipant);
        meeting.setParticipants(participants);
        meetingRepository.save(meeting);

    }

    /**
     * 두 사용자 간 차단 관계가 존재하는지 확인합니다.
     * 차단 관계는 두 방향 모두 체크합니다.
     */
    private boolean isBlocked(String organizerExternalUserId, String participantExternalUserId) {
        return blockedUserRepository.existsByBlockerExternalUserIdAndBlockedExternalUserId(organizerExternalUserId, participantExternalUserId)
                || blockedUserRepository.existsByBlockerExternalUserIdAndBlockedExternalUserId(participantExternalUserId, organizerExternalUserId);
    }


    @Transactional
    public List<MeetingParticipantUserDto> getMembers(Long meetingId) {
        List<MeetingParticipant> participants = meetingParticipantsRepository.findByMeetingId(meetingId);
        if (participants == null || participants.isEmpty()) {
            return Collections.emptyList(); // 멤버가 없으면 빈 리스트 반환
        }
        return participants.stream().map(participant -> {
                    String externalUserId = participant.getUser().getExternalUserId();
                    Optional<User> userOpt = userRepository.findByExternalUserId(externalUserId);
                    // 외부 사용자 조회 결과가 없으면 해당 항목은 스킵하거나, 빈 DTO를 반환할 수 있음.
                    if (!userOpt.isPresent()) {
                        return null;
                    }
                    User user = userOpt.get();
                    return new MeetingParticipantUserDto(
                            participant.getId(),              // MeetingParticipant의 id
                            user.getNickname(),               // User의 닉네임
                            user.getAge(),                    // User의 나이
                            user.getProfileImageUrl(),        // User의 프로필 이미지 URL
                            user.getExternalUserId(),         // User의 외부 사용자 ID
                            participant.getMeeting().getId()  // 모임 ID
                    );
                })
                .filter(Objects::nonNull) // null 항목 제거
                .collect(Collectors.toList());
    }



    @Transactional
    public MeetingInfoDto getMeetingInfo(String token, Long meetingId) {
        Optional<Meeting> meetingOptional = meetingRepository.findById(meetingId);
        if (!meetingOptional.isPresent()) {
            throw new IllegalArgumentException("해당 모임이 존재하지 않습니다.");
        }
        Meeting meeting = meetingOptional.get();
        String jwtToken = token.replace("Bearer ", "");
        String externalUserId = JwtUtil.validateToken(jwtToken);

        Optional<User> myUser = userRepository.findByExternalUserId(externalUserId);
        String myNickname = myUser.isPresent() ? myUser.get().getNickname() : "";
        Long myUserId = myUser.isPresent() ? myUser.get().getId() : 0;
        // 잔여일 계산: 현재 날짜와 모임의 구독 종료일 사이의 일수
        long remainingDays = ChronoUnit.DAYS.between(LocalDate.now(), meeting.getSubscriptionEndDate());
        // 필요에 따라 음수인 경우 0으로 처리할 수도 있음.
        if (remainingDays < 0) {
            remainingDays = 0;
        }

        // Meeting 엔티티의 필드에 따라 DTO를 구성합니다.
        return new MeetingInfoDto(
                meeting.getId(),
                meeting.getExternalUserId(),
                meeting.getTitle(),
                meeting.getDescription(),
                meeting.getImageUrl(),
                meeting.getLocation(),
                meeting.getWriterNickname(),  // 모임 대표 이름
                meeting.getCreatedAt(),
                myNickname,                   // 내 닉네임 추가
                myUserId,
                remainingDays,               // 잔여일 추가
                meeting.getMaxParticipants()
        );
    }

    @Transactional
    public ResponseEntity<String> extendMeeting(Long meetingId, int months) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new IllegalArgumentException("해당 모임이 존재하지 않습니다."));

        // 연장 개월 수에 따른 캔디 비용 결정
        int cost;
        switch (months) {
            case 1:
                cost = 150;
                break;
            case 3:
                cost = 400;
                break;
            case 6:
                cost = 700;
                break;
            default:
                throw new IllegalArgumentException("유효한 연장 개월 수가 아닙니다.");
        }

        // 모임 대표(소유자) 정보 조회 (meeting의 externalUserId 기준)
        User owner = userRepository.findByExternalUserId(meeting.getExternalUserId())
                .orElseThrow(() -> new IllegalArgumentException("모임 대표 사용자 정보를 찾을 수 없습니다."));

        // 캔디 잔액 확인 및 차감
        if (owner.getCandy() < cost) {
            return ResponseEntity.badRequest().body("캔디가 부족합니다.");
        }
        int balanceBefore = owner.getCandy();
        owner.setCandy(balanceBefore - cost);
        userRepository.save(owner);

        // 캔디 사용 내역 기록 (BalanceHistory 엔티티 이용)
        BalanceHistory balanceHistory = new BalanceHistory();
        balanceHistory.setUserId(owner.getId());
        balanceHistory.setExternalUserId(owner.getExternalUserId());
        balanceHistory.setAmount(-cost);  // 사용하므로 음수 값
        balanceHistory.setBalanceBefore(balanceBefore);
        balanceHistory.setBalanceAfter(owner.getCandy());
        balanceHistory.setChangeType("EXTEND_MEETING");
        balanceHistory.setCreateAt(LocalDateTime.now(ZoneId.of("Asia/Seoul"))); // 현재 시간 설정
        balanceHistoryRepository.save(balanceHistory);

        // 모임 유지 기간 연장 (subscriptionEndDate에 months개월 추가)
        meeting.setSubscriptionEndDate(meeting.getSubscriptionEndDate().plusMonths(months));
        meetingRepository.save(meeting);
        return null;
    }

    @Transactional
    public Meeting updateMeetingInfo(Long meetingId, MeetingUpdateRequestDTO updateRequest, String requesterExternalUserId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Meeting not found"));

        // 요청한 사용자가 모임 생성자(Organizer)와 일치하는지 확인
        if (!meeting.getExternalUserId().equals(requesterExternalUserId)) {
            throw new RuntimeException("Unauthorized: Only organizer can update meeting info");
        }

        // 업데이트할 필드 설정
        meeting.setTitle(updateRequest.getTitle());
        meeting.setLocation(updateRequest.getLocation());
        meeting.setDescription(updateRequest.getDescription());
        meeting.setMaxParticipants(updateRequest.getMaxParticipants());
        return meetingRepository.save(meeting);
    }

    public Meeting updateMeetingImage(Long meetingId, MultipartFile image, String externalUserId) throws Exception {
        // 모임 조회 (존재하지 않으면 예외 발생)
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new Exception("Meeting not found"));

        // 모임 생성자와 요청한 사용자의 externalUserId 비교하여 권한 체크
        if (!meeting.getExternalUserId().equals(externalUserId)) {
            throw new Exception("Unauthorized access: You are not the organizer");
        }

        s3Uploader.deleteGalleryImage(meeting.getImageUrl());
        // 파일 저장 (예: 로컬/클라우드 스토리지에 저장 후 URL 반환)
        String imageUrl = s3Uploader.uploadMeetingProfileImage(image, externalUserId);


        // 모임의 이미지 URL 업데이트 후 DB에 저장
        meeting.setImageUrl(imageUrl);
        meetingRepository.save(meeting);

        return meeting;
    }

    // 정기모임 등록 (모임 대표만 가능)
    @Transactional
    public void registerPeriodicMeeting(String externalUserId, Long meetingId, PeriodicMeetingRegistrationDto request) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new IllegalArgumentException("해당 모임이 존재하지 않습니다."));

        // 모임 대표만 등록할 수 있도록 확인 (Meeting 엔티티의 externalUserId가 대표 정보라고 가정)
        if (!meeting.getExternalUserId().equals(externalUserId)) {
            throw new IllegalArgumentException("모임 대표만 정기모임을 등록할 수 있습니다.");
        }

        // PeriodicMeeting 엔티티 생성 및 요청 DTO의 값 매핑
        PeriodicMeeting periodicMeeting = PeriodicMeeting.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .time(request.getTime())
                .location(request.getLocation())
                .cost(request.getCost())
                .maxParticipants(request.getMaxParticipants())
                // 날짜는 ISO 문자열을 LocalDate로 변환
                .date(LocalDate.parse(request.getDate()))
                .meeting(meeting)
                .build();

        periodicMeetingRepository.save(periodicMeeting);
    }

    @Transactional
    public List<PeriodicMeetingDto> getPeriodicMeetings(Long meetingId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new IllegalArgumentException("해당 모임이 존재하지 않습니다."));

        // meeting에 연결된 정기모임들을 조회 (PeriodicMeetingRepository에 findByMeeting(Meeting meeting) 메서드가 있다고 가정)
        List<PeriodicMeeting> periodicMeetings = periodicMeetingRepository.findByMeeting(meeting);

        return periodicMeetings.stream().map(pm -> new PeriodicMeetingDto(
                pm.getId(),
                pm.getTitle(),
                pm.getDescription(),
                pm.getDate().toString(),  // LocalDate를 문자열로 변환
                pm.getTime(),
                pm.getLocation(),
                pm.getCost(),
                pm.getMaxParticipants()
        )).collect(Collectors.toList());
    }


    @Transactional
    public void leaveMeeting(String externalUserId, String meetingId) {
        // 채팅방에서 해당 meetingId와 관련된 데이터 삭제
        userChatRoomsRepository.deleteByRoomId(meetingId);

        // 외부 사용자 ID로 User 조회
        Optional<User> userOpt = userRepository.findByExternalUserId(externalUserId);
        if (!userOpt.isPresent()) {
            throw new IllegalArgumentException("참가자 정보가 존재하지 않습니다: " + externalUserId);
        }
        Long userId = userOpt.get().getId();

        // meetingId가 "meeting_"으로 시작하면 두 번째 조각을 숫자로 추출
        Long meetingIdLong;
        if (meetingId.startsWith("meeting_")) {
            String[] parts = meetingId.split("_");
            // 예: "meeting_14_21"인 경우 parts[1]가 "14"
            meetingIdLong = Long.valueOf(parts[1]);
        } else {
            meetingIdLong = Long.valueOf(meetingId);
        }

        // 모임 참가자에서 해당 userId와 meetingIdLong을 가진 항목 삭제
        meetingParticipantsRepository.deleteByUserIdAndMeetingId(userId, meetingIdLong);
    }



    @Transactional
    public void deleteMeeting(String externalUserId, Long meetingId, Long myUserId) {
        // 🔹 존재하는 모임인지 확인
        Optional<Meeting> meetingOptional = meetingRepository.findById(meetingId);
        if (meetingOptional.isEmpty()) {
            throw new IllegalArgumentException("해당 meetingId에 대한 모임이 존재하지 않습니다.");
        }

        // 🔹 모임 주최자인지 검증
        Meeting meeting = meetingOptional.get();
        if (!meeting.getExternalUserId().equals(externalUserId)) {
            throw new IllegalArgumentException("모임을 삭제할 권한이 없습니다.");
        }

        // 2. 신고된 채팅방인지 확인
        Optional<Report> latestReport = reportRepository
                .findTopByRoomIdContainingOrderByReportedAtDesc(String.valueOf(meetingId));
        if (latestReport.isPresent()) {
            Report report = latestReport.get();
            System.out.println("신고된 채팅방임");

            // "meeting_124_" 로 시작하는 모든 채팅방 레코드를 가져온다
            String roomPrefix = "meeting_" + meetingId + "_";
            List<UserChatRooms> chatRooms = userChatRoomsRepository
                    .findByRoomIdStartingWith(roomPrefix);

            if (chatRooms.isEmpty()) {
                //System.out.println("[DEBUG] ✖ chatRooms 가 비어 있습니다. prefix=" + roomPrefix);
            } else {
                // 가장 이른 생성 시각(createdAt) 을 구한다
                LocalDateTime earliestCreatedAt = chatRooms.stream()
                        .map(UserChatRooms::getCreatedAt)
                        .min(LocalDateTime::compareTo)
                        .get();
                //System.out.println("[DEBUG] ✔ earliestCreatedAt=" + earliestCreatedAt);

                // 신고 시간이 그 이후인지 비교
                if (report.getReportedAt().isAfter(earliestCreatedAt)) {
                    //System.out.println("[DEBUG] ✔ report.getReportedAt() 이후이므로 백업 실행");
                    getAllMessagesReportMeeting(roomPrefix.substring(0, roomPrefix.length()-1), report.getReporterUserId());
                } else {
                    //System.out.println("[DEBUG] ✖ report 시간이 생성시간 이전이므로 백업 생략");
                }
            }
        }

        ChattingDTO systemMsg = new ChattingDTO();
        systemMsg.setType("system");
        systemMsg.setMessageContent("System: 모임이 삭제되었습니다.");
        systemMsg.setTimestamp(System.currentTimeMillis());

        // meeting_123 이런 식으로 클라이언트의 effectiveRoomId와 맞춰야 함
        String destination = "/topic/chatroom/meeting_" + meetingId;

        // 3. 해당 모임 방의 모든 유저에게 브로드캐스트
        messagingTemplate.convertAndSend(destination, systemMsg);

        // 🔹 참가자 삭제 (meetingId가 아니라 Meeting 객체를 기준으로 삭제)
        meetingParticipantsRepository.deleteByMeeting(meeting);

        // 🔹 채팅방 삭제
        userChatRoomsRepository.deleteByRoomId("meeting_" + meetingId);


        // 🔹 갤러리 사진 삭제
        // 미팅에 속한 모든 갤러리 이미지 가져오기
        List<GalleryImage> galleryImages = galleryImageRepository.findByMeetingId(String.valueOf(meetingId));
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
        List<PeriodicMeeting> periodicMeetings = periodicMeetingRepository.findByMeetingId(meetingId);
        if (!periodicMeetings.isEmpty()) {
            periodicMeetingRepository.deleteAll(periodicMeetings);
        }

        // 🔹 게시판 글 삭제
        // 미팅에 속한 모든 게시글 가져오기
        List<Board> boardPosts = boardRepository.findByMeetingId(meetingId);
        if (!boardPosts.isEmpty()) {
            boardRepository.deleteAll(boardPosts);
        }

        // 🔹 모임 삭제
        meetingRepository.deleteById(meetingId);
        s3Uploader.deleteMeetingImage(String.valueOf(meetingId), myUserId);
        // 🔹 DynamoDB에서 채팅 내역 삭제
        removeChatRoomAndDynamoRecord("meeting_" + meetingId);

        s3Uploader.deletePostImage(meeting.getImageUrl());
    }



    @Transactional
    public void removeChatRoomAndDynamoRecord(String roomId) {
        // ✅ 모임 채팅방인지 확인
        boolean isMeetingRoom = roomId.startsWith("meeting_");

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

    public String getAllMessagesReportMeeting(String roomId, Long reporterUserId) {
        System.out.println("[DEBUG] enter getAllMessagesReportMeeting: roomId=" + roomId + ", reporterUserId=" + reporterUserId);
        try {
            // GSI만 사용
            Map<String, AttributeValue> expressionAttributeValues = Map.of(
                    ":roomId", AttributeValue.builder().s(roomId).build()
            );
            System.out.println("[DEBUG] expressionAttributeValues prepared");

            QueryRequest.Builder queryBuilder = QueryRequest.builder()
                    .tableName(tableName)
                    .indexName("effective_room_id-timestamp-index")
                    .keyConditionExpression("effective_room_id = :roomId")
                    .expressionAttributeValues(expressionAttributeValues)
                    .scanIndexForward(true);
            System.out.println("[DEBUG] QueryRequest builder ready: " + queryBuilder.build());

            QueryResponse response;
            int page = 0;
            do {
                page++;
                System.out.println("[DEBUG] querying page " + page);
                response = dynamoDbClient.query(queryBuilder.build());
                System.out.println("[DEBUG] query returned " + response.count() + " items");

                for (Map<String, AttributeValue> item : response.items()) {
                    System.out.println("[DEBUG] mapping item: " + item);
                    ReportChatting entity = DynamoDBUtils.mapToEntity(item);
                    entity.setReporterUserId(reporterUserId);
                    reportChattingRepository.save(entity);
                    System.out.println("[DEBUG] saved ReportChatting id=" + entity.getId());
                }

                if (response.hasLastEvaluatedKey()) {
                    System.out.println("[DEBUG] hasLastEvaluatedKey, continuing with new start key");
                    queryBuilder = queryBuilder.exclusiveStartKey(response.lastEvaluatedKey());
                } else {
                    System.out.println("[DEBUG] no more pages");
                    break;
                }
            } while (true);

            System.out.println("[DEBUG] exit getAllMessagesReportMeeting successfully");
        } catch (Exception e) {
            System.err.println("[ERROR] exception in getAllMessagesReportMeeting:");
            e.printStackTrace();
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



    /**
     * ✅ 미처리 항목 재시도 로직
     */
    private void retryUnprocessedItems(Map<String, List<WriteRequest>> unprocessedItems) {
        unprocessedItems.forEach((table, writeRequests) -> {
            for (WriteRequest request : writeRequests) {
                dynamoDbClient.batchWriteItem(BatchWriteItemRequest.builder()
                        .requestItems(Map.of(table, List.of(request)))
                        .build());
            }
        });
    }

    /**
     * 전달된 periodicId와 meetingId에 해당하는 정기모임을 삭제합니다.
     * (S3 파일 삭제 로직이 필요한 경우 별도로 추가하면 됩니다.)
     */
    public void deletePeriodicMeeting(Long periodicId, Long meetingId) {
        PeriodicMeeting meeting = periodicMeetingRepository.findByIdAndMeetingId(periodicId, meetingId)
                .orElseThrow(() -> new RuntimeException("정기모임을 찾을 수 없거나 모임 ID가 일치하지 않습니다."));
        periodicMeetingRepository.delete(meeting);
    }
}
