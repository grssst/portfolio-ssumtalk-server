package com.hottalk.hottalkserver.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.database.core.Repo;
import com.hottalk.hottalkserver.dto.DeleteUserResultDTO;
import com.hottalk.hottalkserver.dto.UserProfileDTO;
import com.hottalk.hottalkserver.model.*;
import com.hottalk.hottalkserver.repository.*;
import com.hottalk.hottalkserver.util.DynamoDBUtils;
import com.hottalk.hottalkserver.util.GeoUtils;
import com.hottalk.hottalkserver.util.S3Uploader;
import org.geolatte.geom.M;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final UserChatRoomsRepository userChatRoomsRepository;
    private final UserChatRoomsService userChatRoomsService;
    @Value("${dynamodb.tableName}")
    private String tableName;
    private final S3Uploader s3Uploader;
    private final BalanceHistoryRepository balanceHistoryRepository;
    private final DynamoDbClient logDynamoDbClient;
    private final ReportRepository reportRepository;
    private final ReportChattingRepository reportChattingRepository;
    private final PaymentDeletionScheduler paymentDeletionScheduler;
    private final DeviceUuidRepository deviceUuidRepository;
    private final BlockedUserRepository blockedUserRepository;
    private final FcmTokenRepository fcmTokenRepository;
    private final GalleryImageRepository galleryImageRepository;
    private final DynamoDbClient dynamoDbClient; // 여기에 클래스 필드로 선언
    private final UserTermsRepository userTermsRepository;
    private final MeetingRepository meetingRepository;
    private final PeriodicMeetingRepository periodicMeetingRepository;
    private final BoardRepository boardRepository;
    private final MeetingParticipantsRepository meetingParticipantsRepository;

    @Value("${LOG_DYNAMO_DB_TABLE_NAME}")
    private String logTableName;

    @Autowired
    public UserService(
            UserRepository userRepository,
            GalleryImageRepository galleryImageRepository,
            PostRepository postRepository,
            PeriodicMeetingRepository periodicMeetingRepository,
            BoardRepository boardRepository,
            UserChatRoomsRepository userChatRoomsRepository,
            UserChatRoomsService userChatRoomsService,
            ReportChattingRepository reportChattingRepository,
            S3Uploader s3Uploader,
            MeetingRepository meetingRepository,
            BalanceHistoryRepository balanceHistoryRepository,
            ReportRepository reportRepository,
            DynamoDbClient logDynamoDbClient,
            PaymentDeletionScheduler paymentDeletionScheduler,
            DeviceUuidRepository deviceUuidRepository,
            BlockedUserRepository blockedUserRepository,
            MeetingParticipantsRepository meetingParticipantsRepository,
            FcmTokenRepository fcmTokenRepository,
            UserTermsRepository userTermsRepository,
            @Value("${aws.accessKeyId}") String accessKey,
            @Value("${aws.secretAccessKey}") String secretKey,
            @Value("${aws.region}") String region) {

        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.userChatRoomsRepository = userChatRoomsRepository;
        this.userChatRoomsService = userChatRoomsService;
        this.s3Uploader = s3Uploader;
        this.meetingParticipantsRepository = meetingParticipantsRepository;
        this.balanceHistoryRepository = balanceHistoryRepository;
        this.meetingRepository = meetingRepository;
        this.logDynamoDbClient = logDynamoDbClient;
        this.reportRepository = reportRepository;
        this.reportChattingRepository = reportChattingRepository;
        this.periodicMeetingRepository = periodicMeetingRepository;
        this.boardRepository = boardRepository;
        this.paymentDeletionScheduler = paymentDeletionScheduler;
        this.deviceUuidRepository = deviceUuidRepository;
        this.galleryImageRepository = galleryImageRepository;
        this.blockedUserRepository = blockedUserRepository;
        this.fcmTokenRepository = fcmTokenRepository;
        this.userTermsRepository = userTermsRepository;
        this.dynamoDbClient = DynamoDbClient.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)
                ))
                .build();
    }


    public boolean saveOrUpdateUser(String provider, String externalUserId, String email, String fullName) { //로그인시에 사용됨!!
        Optional<User> optionalUser = userRepository.findByExternalUserId(externalUserId);
        Point myLocation = DefaultLocation.createDefaultPoint(); // 기본값 설정
        User user;
        if (optionalUser.isPresent()) {
            user = optionalUser.get();
        } else {
            user = new User();
            user.setExternalUserId(externalUserId);
        }

        user.setProvider(provider);
        user.setEmail(email);
        user.setFullName(fullName);
        user.setLastLogin(LocalDateTime.now(ZoneId.of("Asia/Seoul"))); // last_login 업데이트
        user.setCreatedAt(LocalDateTime.now(ZoneId.of("Asia/Seoul")));
        user.setLocation(myLocation);  //이건 그냥 기본 위치임 point 타입
        System.out.println("저장될 user:" + user);


        // ✅ 기존 유저라도 UserTerms가 없는 경우 새로 생성 (외래 키만 사용)
        UserTerms userTerms = userTermsRepository.findByExternalUserId(externalUserId).orElse(null);

        if (userTerms == null) { // userTerms가 없으면 새로 생성
            userTerms = new UserTerms();
            userTerms.setExternalUserId(externalUserId); // ✅ 관계 대신 userId 직접 설정
            userTerms.setTermsOfService(true);
            userTerms.setLocationService(true);
            userTerms.setPrivacyPolicy(true);
            userTerms.setPrivacyUse(true);
            userTermsRepository.save(userTerms); // ✅ 명시적으로 저장
        }

        userRepository.save(user);
        // 프로필이 있는지 확인
        return user.getNickname() != null && user.getAge() != null && user.getStatus() != null;
    }

    public class DefaultLocation {
        public static Point createDefaultPoint() {
            GeometryFactory geometryFactory = new GeometryFactory();
            Point point = geometryFactory.createPoint(new Coordinate(128.000000, 37.000000)); // 기본 좌표
            point.setSRID(4326); // SRID 설정
            return point;
        }
    }

    @Transactional
    public void saveUserProfile(User userProfile) { //프로필 저장 or 업데이트 하고 post도 수정
        Optional<User> optionalUser = userRepository.findByExternalUserId(userProfile.getExternalUserId());
        List<Meeting> optionalMeetings = meetingRepository.findByExternalUserId(userProfile.getExternalUserId());



        System.out.println("관심사:" + userProfile.getInterestsKey());
        if (optionalUser.isPresent()) {
            User existingUser = optionalUser.get();
            existingUser.setNickname(userProfile.getNickname());
            existingUser.setGender(userProfile.getGender());
            existingUser.setAge(userProfile.getAge());
            existingUser.setLastLogin(LocalDateTime.now(ZoneId.of("Asia/Seoul")));
            existingUser.setStatus(userProfile.getStatus());
            if(userProfile.getProfileImageUrl() != null){
                existingUser.setProfileImageUrl(userProfile.getProfileImageUrl());
            }
            if(userProfile.getProfileImageUrl2() != null){
                existingUser.setProfileImageUrl2(userProfile.getProfileImageUrl2());
            }
            if(userProfile.getProfileImageUrl3() != null){
                existingUser.setProfileImageUrl3(userProfile.getProfileImageUrl3());
            }
            //existingUser.setProfileImageUrl2(userProfile.getProfileImageUrl2());
            //existingUser.setProfileImageUrl3(userProfile.getProfileImageUrl3());

            existingUser.setMyLocation(userProfile.getMyLocation());
            existingUser.setInterestsKey(userProfile.getInterestsKey());
            userRepository.save(existingUser);
            // ✅ Meeting 테이블의 writerNickname 업데이트
            meetingRepository.updateWriterNicknameByExternalUserId(existingUser.getExternalUserId(), existingUser.getNickname());
            postRepository.updatePostUserInfoByExternalUserId(existingUser.getExternalUserId(), existingUser.getNickname(), existingUser.getGender(),
                    existingUser.getAge(), existingUser.getProfileImageUrl(), existingUser.getStatus(), existingUser.getMyLocation(), existingUser.getLocation());
        } else {
            userRepository.save(userProfile);
        }
    }

    public void updateUserLocation(String externalUserId, double latitude, double longitude) {
        // Optional을 사용하여 User를 찾음
        User user = userRepository.findByExternalUserId(externalUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // GeometryFactory를 사용해 Point 생성
        GeometryFactory geometryFactory = new GeometryFactory();
        Point location = geometryFactory.createPoint(new Coordinate(longitude, latitude));
        //System.out.println(location);
        //System.out.println("latitude:" + latitude);
        location.setSRID(4326); // SRID=4326 설정


        // User 객체를 업데이트
        user.setLastLogin(LocalDateTime.now(ZoneId.of("Asia/Seoul")));
        user.setLocation(location);
        //System.out.println(user.getLocation().getY());
        userRepository.save(user);
    }

    public User getUserProfile(String externalUserId) {
        // 외부 사용자 ID로 사용자 정보를 조회
        Optional<User> userOptional = userRepository.findByExternalUserId(externalUserId);

        // 사용자 정보를 찾으면 반환, 그렇지 않으면 HTTP 에러 코드 404 반환
        return userOptional.orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        );
    }

    // externalUserId로 userId를 조회하는 메서드 추가
    public Long findUserIdByExternalUserId(String externalUserId) {
        return userRepository.findByExternalUserId(externalUserId)
                .map(User::getId)
                .orElseThrow(() -> new RuntimeException("User not found with externalUserId: " + externalUserId));
    }

    @Transactional
    public List<UserProfileDTO> getAllUsers(String externalUserId, String gender, String sort, Integer ageRange1, Integer ageRange2) {
        try {
            Optional<User> currentUser = userRepository.findByExternalUserId(externalUserId);  //유저목록을 호출한 나
            Point myLocation = PostService.DefaultLocation.createDefaultPoint(); // 기본값 설정
            if (currentUser.isPresent() && currentUser.get().getLocation() != null) {
                myLocation = currentUser.get().getLocation(); // 유효한 location이 있으면 덮어씀. point 타입
            }
            //System.out.println(myLocation);
            double latitude = myLocation.getY();  // 나의 위도
            double longitude = myLocation.getX(); // 나의 경도
            //System.out.println(latitude);
            double distanceKm = 30; // 20km 반경
            double maxDistanceKm = 540; // 최대 반경 500km


            //paymentDeletionScheduler.schedulePaymentDeletion(8L, "apple");
            List<User> users = null;
            if (Objects.equals(sort, "distance")) { // 거리순 정렬 초기요청 개발 완료
                while (distanceKm <= maxDistanceKm) {
                    Envelope boundingBox = GeoUtils.calculateBoundingBox(latitude, longitude, distanceKm);
                    String wktBoundingBox = GeoUtils.envelopeToWKT(boundingBox);
                    //System.out.println("Bounding Box: " + wktBoundingBox);

                    if (Objects.equals(gender, "all")) { // 모든 성별
                        users = userRepository.findUsersWithinBoundingBox(wktBoundingBox, myLocation, externalUserId, ageRange1, ageRange2);
                    } else { // 특정 성별
                        users = userRepository.findUsersWithinBoundingBoxAndGender(wktBoundingBox, gender, myLocation, externalUserId, ageRange1, ageRange2);
                    }

                    if (users.size() >= 100) {
                        System.out.println("데이터 100개 찾음");
                        break; // 100개 이상의 데이터가 쿼리되면 중단
                    }
                    System.out.println("쿼리된 데이터 개수:" + users.size());
                    distanceKm += 80; // 범위를 80km씩 증가
                    System.out.println("범위를 80km 확장합니다. 확장된 범위:" + distanceKm);
                }
            } else if (Objects.equals(sort, "time")) {      //시간순 정렬 개발완료
                // 시간 기반 쿼리 수행
                if (Objects.equals(gender, "all")) {   //모든 성별 + 시간순
                    users = userRepository.findTop100ByOrderByLastLoginDesc(externalUserId, ageRange1, ageRange2);
                } else {    //특정 성별 + 시간순
                    users = userRepository.findTop100ByGenderOrderByLastLoginDesc(gender, externalUserId, ageRange1, ageRange2);
                }
            }



            return users.stream().map(user -> {
                Point userLocation = currentUser.isPresent() ? currentUser.get().getLocation() : null;

                Double userListLatitude = null;
                Double userListLongitude = null;
                if (user.getLocation() != null) {
                    userListLatitude = user.getLocation().getY();
                    userListLongitude = user.getLocation().getX();
                }

                Double userLatitude = null;
                Double userLongitude = null;
                if (userLocation != null) {
                    userLatitude = userLocation.getY();
                    userLongitude = userLocation.getX();
                }

                return new UserProfileDTO(
                        user.getId(),
                        user.getExternalUserId(),
                        user.getNickname(),
                        user.getGender(),
                        user.getAge(),
                        user.getProfileImageUrl(),
                        user.getProfileImageUrl2(),
                        user.getProfileImageUrl3(),
                        user.getStatus(),
                        userLatitude,
                        userLongitude,
                        userListLatitude,
                        userListLongitude,
                        user.getLastLogin(),
                        user.getMyLocation(),
                        user.getInterestsKey()
                );
            }).collect(Collectors.toList());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }



    public List<UserProfileDTO> getMoreUsers(String externalUserId, String gender, String sort, Double lastDistance, LocalDateTime lastLogin, Integer ageRange1, Integer ageRange2) {
        try {
            Optional<User> currentUser = userRepository.findByExternalUserId(externalUserId);  //유저목록을 호출한 나
            Point myLocation = PostService.DefaultLocation.createDefaultPoint(); // 기본값 설정
            if (currentUser.isPresent() && currentUser.get().getLocation() != null) {
                myLocation = currentUser.get().getLocation(); // 유효한 location이 있으면 덮어씀
            }

            double latitude = myLocation.getY();  // 나의 위도
            double longitude = myLocation.getX(); // 나의 경도

            double distanceKm = 30; // 20km 반경
            double maxDistanceKm = 540; // 최대 반경 500km


            List<User> users = null;
            if (Objects.equals(sort, "distance")) { // 거리순 정렬
                while (distanceKm <= maxDistanceKm) {
                    Envelope boundingBox = GeoUtils.calculateBoundingBox(latitude, longitude, distanceKm);
                    String wktBoundingBox = GeoUtils.envelopeToWKT(boundingBox);
                    //System.out.println("Bounding Box: " + wktBoundingBox);

                    if (Objects.equals(gender, "all")) { // 모든 성별
                        users = userRepository.findUsersWithinBoundingBoxAndMinDistance(wktBoundingBox, myLocation, lastDistance, externalUserId, ageRange1, ageRange2);
                    } else { // 특정 성별
                        users = userRepository.findUsersWithinBoundingBoxAndGenderAndMinDistance(wktBoundingBox, gender, myLocation, lastDistance, externalUserId, ageRange1, ageRange2);
                    }

                    if (users.size() >= 100) {
                        System.out.println("데이터 100개 찾음");
                        break; // 100개 이상의 데이터가 쿼리되면 중단
                    }
                    System.out.println("쿼리된 데이터 개수:" + users.size());
                    distanceKm += 80; // 범위를 80km씩 증가
                    System.out.println("범위를 80km 확장합니다. 확장된 범위:" + distanceKm);
                }
            }
            else if (Objects.equals(sort, "time")) {   //시간순
                // 시간 기반 쿼리 수행
                if (Objects.equals(gender, "all")) {  //모든 성별
                    users = userRepository.findTop100ByLastLoginBeforeOrderByLastLoginDesc(lastLogin, externalUserId, ageRange1, ageRange2);
                } else {        //특정성별
                    users = userRepository.findTop100ByGenderAndLastLoginBeforeOrderByLastLoginDesc(gender, lastLogin, externalUserId, ageRange1, ageRange2);
                }
            }


            return users.stream().map(user -> {
                Point userLocation = currentUser.isPresent() ? currentUser.get().getLocation() : null;

                Double userListLatitude = null;
                Double userListLongitude = null;
                if (user.getLocation() != null) {
                    userListLatitude = user.getLocation().getY();
                    userListLongitude = user.getLocation().getX();
                }

                Double userLatitude = null;
                Double userLongitude = null;
                if (userLocation != null) {
                    userLatitude = userLocation.getY();
                    userLongitude = userLocation.getX();
                }

                return new UserProfileDTO(
                        user.getId(),
                        user.getExternalUserId(),
                        user.getNickname(),
                        user.getGender(),
                        user.getAge(),
                        user.getProfileImageUrl(),
                        user.getProfileImageUrl2(),
                        user.getProfileImageUrl3(),
                        user.getStatus(),
                        userLatitude,
                        userLongitude,
                        userListLatitude,
                        userListLongitude,
                        user.getLastLogin(),
                        user.getMyLocation(),
                        user.getInterestsKey()
                );
            }).collect(Collectors.toList());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Transactional
    public boolean decreaseUserCandy(Long userId, int amount) {
        // 1. 유저 조회 및 현재 캔디 잔액 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        int balanceBefore = user.getCandy();

        // 캔디가 부족한 경우 추가 처리 (예외 발생, false 리턴 등)
        if (balanceBefore < amount) {
            return false;
        }

        // 2. 캔디 차감 처리 (update 쿼리 실행)
        int updatedRows = userRepository.decreaseCandy(userId, amount);
        if (updatedRows > 0) {
            int balanceAfter = balanceBefore - amount;

            // 3. balance_history 테이블에 사용 내역 기록 생성
            BalanceHistory history = new BalanceHistory();
            history.setUserId(userId);
            // changeType은 'send_message' 혹은 '쪽지 전송' 등으로 구분할 수 있습니다.
            history.setChangeType("send_message");
            // 사용 내역은 음수로 기록 (예: -3)
            history.setAmount(-amount);
            history.setBalanceBefore(balanceBefore);
            history.setBalanceAfter(balanceAfter);
            // 관련 거래가 없으므로 relatedTransactionId는 null로 둡니다.
            balanceHistoryRepository.save(history);

            return true;
        }
        return false;
    }

    @Transactional
    public DeleteUserResultDTO deleteUser(String externalUserId, String UUID) {
        String uuid = null;

        try {
            // JSON 파싱
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(UUID);

            // UUID 값 추출
            uuid = jsonNode.get("UUID").asText();
        } catch (Exception e) {
            System.err.println("❌ JSON 파싱 오류: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Invalid JSON format for UUID"); // 예외 발생 시 처리
        }


        // 1. 유저 조회
        User user = userRepository.findByExternalUserId(externalUserId)
                .orElseThrow(() -> new RuntimeException("User not found with externalUserId: " + externalUserId));
        Long userId = user.getId(); // userId 가져오기


        // 2-1. 신고된 게시글 조회 (`post_id`가 NULL이 아니고, `reported_user_id`가 해당 유저인 경우)
        //List<Post> reportedPosts = reportRepository.findReportedPostsByUserId(userId);
        // 2-2. 유저의 모든 게시글 조회
        List<Post> userPosts = postRepository.findByUserId(userId);

        if (userPosts.isEmpty()) {
            System.out.println("✅ 삭제할 게시글 없음: " + externalUserId);
        } else {
            // 3. 게시글 이미지 S3에서 삭제
            for (Post post : userPosts) {
                String imageUrl = post.getImageUrl(); // Post 엔티티에 imageUrl 필드가 있다고 가정
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    try {
                        s3Uploader.deletePostImage(imageUrl);
                    } catch (Exception e) {
                        System.err.println("❌ S3 이미지 삭제 실패: " + imageUrl);
                        e.printStackTrace();
                    }
                }
            }
        }
        //게시글 삭제
        postRepository.deleteByExternalUserId(externalUserId);
        System.out.println("✅ 게시글 삭제 완료: " + externalUserId);


        //채팅방, 채팅내역 삭제(즉시)
        //유저 채팅방과 채팅 내역 삭제
        List<String> roomIds = userChatRoomsRepository.findAllByUserId(userId)
                .stream()
                .map(UserChatRooms::getRoomId) // roomId 리스트 추출
                .collect(Collectors.toList());

        for (String roomId : roomIds) {
            userChatRoomsService.removeChatRoomAndDynamoRecord(roomId);
            s3Uploader.deleteChattingImage(roomId);
        }

        //로그 파일 삭제 예약(3개월)
        scheduleLogDeletion(externalUserId);



        //결제 내역(balance_history, purchase_transaction_**) 테이블 레코드 삭제 예약 5년       #clear
        if(Objects.equals(user.getProvider(), "google")){
            paymentDeletionScheduler.schedulePaymentDeletion(userId, "google");
        } else {
            paymentDeletionScheduler.schedulePaymentDeletion(userId, "apple");
        }


        //분쟁 내역(Report, ReportChatting) 테이블 레코드 삭제 예약 3년
        paymentDeletionScheduler.scheduleReportDeletion(userId);

        //불만 내역(CallCenter) 레코드 삭제 예약 3년
        paymentDeletionScheduler.scheduleCallCenterDeletion(userId);

        //프로필 이미지 s3에서 삭제
        s3Uploader.deleteProfileImage(externalUserId, 1);
        s3Uploader.deleteProfileImage(externalUserId, 2);
        s3Uploader.deleteProfileImage(externalUserId, 3);

        //차단 내역 삭제
        blockedUserRepository.deleteByBlockerExternalUserIdOrBlockedExternalUserId(externalUserId, externalUserId);
        System.out.println("1");

        // ✅ 내가 가입한 미팅 id들 먼저 모으기 (참가자 기준)
        List<Long> joinedMeetingIds = meetingParticipantsRepository.findByUser_Id(userId)
                .stream()
                .map(mp -> mp.getMeeting().getId())
                .distinct()
                .collect(Collectors.toList());

        // ✅ 내가 만든 미팅 id들도 같이 보내고 싶으면
        List<Meeting> meetings = meetingRepository.findByExternalUserId(externalUserId);
        List<Long> createdMeetingIds = meetings.stream()
                .map(Meeting::getId)
                .collect(Collectors.toList());

        //가입한 미팅 나가기
        meetingParticipantsRepository.deleteByUserId(userId);
        System.out.println("2");

        // 미팅 삭제 (내가 개설한 미팅들을 삭제)
        if (meetings != null && !meetings.isEmpty()) {
            for (Meeting meeting : meetings) {
                try {
                    // deleteMeeting 메서드가 외부 사용자 ID와 미팅 ID를 인자로 받는다고 가정합니다.
                    deleteMeeting(externalUserId, meeting.getId());
                    s3Uploader.deleteMeetingImage(String.valueOf(meeting.getId()), user.getId());
                    System.out.println("✅ 미팅 삭제 완료: " + meeting.getId());
                } catch (Exception e) {
                    System.err.println("❌ 미팅 삭제 실패: " + meeting.getId() + " - " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } else {
            System.out.println("✅ 삭제할 미팅 없음: " + externalUserId);
        }
        System.out.println("3");

        //fcm토큰 삭제
        fcmTokenRepository.deleteByUserId(userId);

        //제재 기록 삭제 (6개월)
        paymentDeletionScheduler.scheduleSanctionDeletion(userId);

        //약관 동의 내역 삭제(1년)
        //userTermsRepository.deleteByExternalUserId(externalUserId);
        paymentDeletionScheduler.scheduleTerms(externalUserId);

        //UUID로 하루 재가입 불가능하게 제한 걸기
        saveOrUpdateDeviceUuid(uuid);
        //UUID제한 내역 하루 뒤 삭제
        paymentDeletionScheduler.scheduleUuidDeletion(uuid, userId);


        //user 테이블 레코드 삭제, 삭제 예정 테이블로 옮기기??
        userRepository.deleteById(userId);


        // ✅ 여기서 DTO로 묶어서 리턴
        return new DeleteUserResultDTO(userId, joinedMeetingIds, createdMeetingIds);
    }

    @Transactional
    public void deleteMeeting(String externalUserId, Long meetingId) {
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

        // 🔹 DynamoDB에서 채팅 내역 삭제
        removeChatRoomAndDynamoRecord("meeting_" + meetingId);

        s3Uploader.deletePostImage(meeting.getImageUrl());
    }

    public void saveOrUpdateDeviceUuid(String UUID) {
        Optional<DeviceUuid> existingUuid = deviceUuidRepository.findByDeviceUuid(UUID);

        if (existingUuid.isPresent()) {
            // 기존 UUID 존재 → deleted_at을 현재 시간으로 업데이트
            DeviceUuid d = existingUuid.get();
            d.setDeletedAt(LocalDateTime.now(ZoneId.of("Asia/Seoul")));
            deviceUuidRepository.save(d);
            System.out.println("✅ 기존 UUID 업데이트 완료: " + UUID);
        } else {
            // 새로운 UUID 저장
            DeviceUuid d = new DeviceUuid();
            d.setDeviceUuid(UUID);
            d.setDeletedAt(LocalDateTime.now(ZoneId.of("Asia/Seoul")));
            deviceUuidRepository.save(d);
            System.out.println("✅ 새로운 UUID 저장 완료: " + UUID);
        }
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

    private void scheduleLogDeletion(String externalUserId) {
        long currentTime = Instant.now().getEpochSecond(); // 현재 시간 (초 단위)
        long expireAt = currentTime + (180L * 24 * 60 * 60); // 6개월(약 180일) 후, currentTime이 epoch seconds 기준일 때


        // 1️⃣ 회원의 기존 로그 가져오기
        QueryRequest queryRequest = QueryRequest.builder()
                .tableName(logTableName)
                .keyConditionExpression("externalUserId = :userId")
                .expressionAttributeValues(Map.of(":userId", AttributeValue.builder().s(externalUserId).build()))
                .build();

        QueryResponse queryResponse = logDynamoDbClient.query(queryRequest);

        // 2️⃣ 각 로그에 TTL (expireAt) 추가
        for (Map<String, AttributeValue> item : queryResponse.items()) {
            Map<String, AttributeValue> key = new HashMap<>();
            key.put("externalUserId", item.get("externalUserId"));
            key.put("dateTime", item.get("dateTime")); // ✅ 복합 키 사용 (필요한 경우)

            Map<String, AttributeValue> updateValues = new HashMap<>();
            updateValues.put(":expireAt", AttributeValue.builder().n(String.valueOf(expireAt)).build());

            UpdateItemRequest updateRequest = UpdateItemRequest.builder()
                    .tableName(logTableName)
                    .key(key)
                    .updateExpression("SET expireAt = :expireAt")
                    .expressionAttributeValues(updateValues)
                    .build();

            logDynamoDbClient.updateItem(updateRequest);
        }

        System.out.println("✅ 로그 삭제 예약 완료 (현재 시간 기준 6개월 후) for user: " + externalUserId);
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

}