package com.hottalk.hottalkserver.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hottalk.hottalkserver.dto.*;
import com.hottalk.hottalkserver.model.User;
import com.hottalk.hottalkserver.repository.UserRepository;
import com.hottalk.hottalkserver.service.UserService;
import com.hottalk.hottalkserver.util.JwtUtil;
import com.hottalk.hottalkserver.util.S3Uploader;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.time.Instant;
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final S3Uploader s3Uploader;
    private final UserRepository userRepository;
    private final DynamoDbClient logDynamoDbClient; // 수정: 생성자로 주입받음


    @Value("${LOG_DYNAMO_DB_TABLE_NAME}")
    private String logTableName;

    @Value("${GOOGLE_WEB_CLIENT_ID}")
    private String GOOGLE_WEB_CLIENT_ID;

    @Value("${GOOGLE_CLIENT_SECRET}")
    private String GOOGLE_CLIENT_SECRET;

    @Value("${GOOGLE_REDIRECT_URI}")
    private String GOOGLE_REDIRECT_URI;

    @Autowired
    public UserController(
            UserService userService,
            S3Uploader s3Uploader,
            UserRepository userRepository,
            DynamoDbClient dynamoDbClient) { // 생성자로 주입
        this.userService = userService;
        this.s3Uploader = s3Uploader;
        this.userRepository = userRepository;
        this.logDynamoDbClient = dynamoDbClient; // 초기화
    }

    // 관리자 여부를 확인하는 API
    @GetMapping("/check-admin")
    public ResponseEntity<?> checkAdminStatus(@RequestHeader("Authorization") String token) {
        try {
            // JWT 토큰에서 externalUserId 추출
            String externalUserId = JwtUtil.validateToken(token.replace("Bearer ", ""));

            // 사용자 정보 조회
            User user = userService.getUserProfile(externalUserId);

            // 관리자 여부 확인 및 반환
            boolean isAdmin = user.getIsAdmin();
            return ResponseEntity.ok(Map.of("isAdmin", isAdmin));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error checking admin status.");
        }
    }

    @PostMapping("/social-login")
    public ResponseEntity<?> socialLogin(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        try {
            String provider = (String) payload.get("provider");
            String externalUserId = (String) payload.get("user");
            String token = (String) payload.get("token");
            String email = (String) payload.get("email");

            Map<String, String> fullNameMap = (Map<String, String>) payload.get("fullName");
            String fullName = fullNameMap != null ? fullNameMap.get("givenName") + " " + fullNameMap.get("familyName") : null;

            boolean hasProfile = userService.saveOrUpdateUser(provider, externalUserId, email, fullName);

            Optional<User> userData = userRepository.findByExternalUserId(externalUserId);

            // 사용자 존재 여부 확인
            if (userData.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Collections.singletonMap("message", "해당 유저를 찾을 수 없습니다."));
            }

            User userProfile = userData.get();

            // 클라이언트 IP 가져오기
            String clientIp = getClientIp(request);


            // DynamoDB에 액세스 로그 저장
            saveAccessLogToDynamoDB(externalUserId, clientIp, userProfile.getNickname(), userProfile.getGender(), userProfile.getMyLocation(),
                    userProfile.getStatus(), String.valueOf(userProfile.getAge()), String.valueOf(userProfile.getLocation()), userProfile.getEmail(),
                    userProfile.getFullName(),String.valueOf(userProfile.getId()));

            if (hasProfile) {
                String jwtToken = JwtUtil.generateToken(externalUserId);  // JWT 토큰 생성
                return ResponseEntity.ok(Map.of("token", jwtToken, "hasProfile", true, "userId", userProfile.getId()));
            } else {
                String jwtToken = JwtUtil.generateToken(externalUserId);  // JWT 토큰 생성
                return ResponseEntity.ok(Map.of("token", jwtToken, "hasProfile", false, "userId", userProfile.getId()));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Server error");
        }
    }

    @GetMapping("/update-login-time")
    public ResponseEntity<?> updateLoginTime(@RequestHeader("Authorization") String token, HttpServletRequest request) {
        try {
            // "Bearer " 부분을 제거하고 토큰만 추출
            String jwtToken = token.replace("Bearer ", "");

            // JWT 토큰을 검증하고 externalUserId 추출
            String externalUserId = JwtUtil.validateToken(jwtToken);

            // externalUserId로 사용자 조회 (여기서 사용자 정보가 없으면 예외를 던질 수 있습니다)
            Optional<User> optionalUser = userRepository.findByExternalUserId(externalUserId);
            if (optionalUser.isEmpty()) {
                System.out.println("⚠️ 로그인 시간 업데이트 실패: 유저가 존재하지 않음 (" + externalUserId + ")");
                return ResponseEntity.ok("없는 유저이므로 로그인 시간 업데이트 하지 않음"); // 예외 던지지 않고, 그냥 리턴
            }
            User user = optionalUser.get();
            // 마지막 로그인 시간 업데이트
            user.setLastLogin(LocalDateTime.now(ZoneId.of("Asia/Seoul")));

            // 사용자 정보를 저장
            userRepository.save(user);

            // 클라이언트 IP 가져오기
            String clientIp = getClientIp(request);

            // DynamoDB에 액세스 로그 저장
            saveAccessLogToDynamoDB(externalUserId, clientIp, user.getNickname(), user.getGender(), user.getMyLocation(),
                    user.getStatus(), String.valueOf(user.getAge()), String.valueOf(user.getLocation()), user.getEmail(),
                    user.getFullName(),String.valueOf(user.getId()));
            //System.out.println("zzzzzzzzzzzzz" + user.getNickname());
            if(user.getNickname() == null) {
                return ResponseEntity.ok("goToProfileScreen");
            }

            return ResponseEntity.ok(user.getId());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Server error");
        }
    }

    /**
     * DynamoDB에 액세스 로그 저장
     */
    private void saveAccessLogToDynamoDB(String externalUserId, String ip, String nickname, String gender, String myLocation,
                                         String status, String age, String location, String email, String fullName, String id) {
        try {
            System.out.println("접속로그 저장");
            // 현재 날짜 및 시간 (YYYY-MM-DD HH:mm:ss) 가져오기
            String dateTime = ZonedDateTime.now(ZoneId.of("Asia/Seoul"))
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            Map<String, AttributeValue> item = new HashMap<>();
            item.put("externalUserId", AttributeValue.builder().s(externalUserId).build()); // ✅ Primary Key 변경
            item.put("dateTime", AttributeValue.builder().s(dateTime).build()); // ✅ 날짜+시간 통합
            item.put("ip", AttributeValue.builder().s(ip).build());
            if (nickname == null || nickname.trim().isEmpty()) {
                // ✅ null이거나 빈 값이면 문자열 "null"로 넣기
                item.put("nickname", AttributeValue.builder().s("null").build());
            } else {
                item.put("nickname", AttributeValue.builder().s(nickname).build());
            }
            if (gender == null || gender.trim().isEmpty()) {
                // ✅ null이거나 빈 값이면 문자열 "null"로 넣기
                item.put("gender", AttributeValue.builder().s("null").build());
            } else {
                item.put("gender", AttributeValue.builder().s(gender).build());
            }
            if (myLocation == null || myLocation.trim().isEmpty()) {
                // ✅ null이거나 빈 값이면 문자열 "null"로 넣기
                item.put("myLocation", AttributeValue.builder().s("null").build());
            } else {
                item.put("myLocation", AttributeValue.builder().s(myLocation).build());
            }
            if (status == null || status.trim().isEmpty()) {
                // ✅ null이거나 빈 값이면 문자열 "null"로 넣기
                item.put("status", AttributeValue.builder().s("null").build());
            } else {
                item.put("status", AttributeValue.builder().s(status).build());
            }
            if (age == null || age.trim().isEmpty()) {
                // ✅ null이거나 빈 값이면 문자열 "null"로 넣기
                item.put("age", AttributeValue.builder().s("null").build());
            } else {
                item.put("age", AttributeValue.builder().s(age).build());
            }
            if (location == null || location.trim().isEmpty()) {
                // ✅ null이거나 빈 값이면 문자열 "null"로 넣기
                item.put("location", AttributeValue.builder().s("null").build());
            } else {
                item.put("location", AttributeValue.builder().s(location).build());
            }
            if (email == null || email.trim().isEmpty()) {
                // ✅ null이거나 빈 값이면 문자열 "null"로 넣기
                item.put("email", AttributeValue.builder().s("null").build());
            } else {
                item.put("email", AttributeValue.builder().s(email).build());
            }
            if (fullName == null || fullName.trim().isEmpty()) {
                // ✅ null이거나 빈 값이면 문자열 "null"로 넣기
                item.put("fullName", AttributeValue.builder().s("null").build());
            } else {
                item.put("fullName", AttributeValue.builder().s(fullName).build());
            }
            if (id == null || id.trim().isEmpty()) {
                // ✅ null이거나 빈 값이면 문자열 "null"로 넣기
                item.put("id", AttributeValue.builder().s("null").build());
            } else {
                item.put("id", AttributeValue.builder().s(id).build());
            }
            //item.put("nickname", AttributeValue.builder().s(nickname).build());
            //item.put("gender", AttributeValue.builder().s(gender).build());
            //item.put("myLocation", AttributeValue.builder().s(myLocation).build());
            //item.put("status", AttributeValue.builder().s(status).build());
            //item.put("age", AttributeValue.builder().s(age).build());
            //item.put("location", AttributeValue.builder().s(location).build());
            //item.put("email", AttributeValue.builder().s(email).build());
            //item.put("fullName", AttributeValue.builder().s(fullName).build());
            //item.put("id", AttributeValue.builder().s(id).build());

            PutItemRequest putItemRequest = PutItemRequest.builder()
                    .tableName(logTableName) // 로그 테이블 이름 사용
                    .item(item)
                    .build();

            logDynamoDbClient.putItem(putItemRequest);

            System.out.println("DynamoDB에 로그인 로그 저장 완료: " + externalUserId + " - " + ip + " - " + dateTime + " - " + dateTime);
        } catch (Exception e) {
            System.err.println("DynamoDB 저장 중 오류 발생: " + e.getMessage());
        }
    }


    /**
     * 클라이언트 IP 주소 가져오기
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    @PostMapping("/profile")
    public ResponseEntity<?> saveUserProfile(@RequestPart("profileData") String profileData, @RequestPart(value = "image", required = false) MultipartFile image, @RequestPart(value = "image2", required = false) MultipartFile image2, @RequestPart(value = "image3", required = false) MultipartFile image3,
                                             @RequestParam(value = "profileImageUrl", required = false) String profileImageUrl,
                                             @RequestParam(value = "profileImageUrl2", required = false) String profileImageUrl2,
                                             @RequestParam(value = "profileImageUrl3", required = false) String profileImageUrl3,
                                             @RequestParam(value = "isProfileChanged1", required = false) boolean isProfileChanged1,
                                             @RequestParam(value = "isProfileChanged2", required = false) boolean isProfileChanged2,
                                             @RequestParam(value = "isProfileChanged3", required = false) boolean isProfileChanged3) {
        try {
            System.out.println("Received profileImageUrl: " + profileImageUrl); // 디버깅 로그
            System.out.println("Received profileImageUrl2: " + profileImageUrl2); // 디버깅 로그
            System.out.println("Received profileImageUrl3: " + profileImageUrl3); // 디버깅 로그

            System.out.println("이미지1 변경여부:"+isProfileChanged1+", 이미지2 변경여부:"+isProfileChanged2+", 이미지3 변경여부:"+isProfileChanged3);
            // 프로필 데이터 파싱
            ObjectMapper mapper = new ObjectMapper();
            User userProfile = mapper.readValue(profileData, User.class);

            if(image != null && !image.isEmpty() && isProfileChanged1){
                System.out.println("이미지 업로드 시도");
                // 이미지 업로드
                s3Uploader.deleteProfileImage(userProfile.getExternalUserId(), 1);
                String imageUrl = s3Uploader.uploadProfileImage(image, userProfile.getExternalUserId());
                userProfile.setProfileImageUrl(imageUrl);
            }
            else if(isProfileChanged1 || profileImageUrl != null){ //기본 이미지로 변경시
                System.out.println("기본 이미지로 변경 시도");
                s3Uploader.deleteProfileImage(userProfile.getExternalUserId(), 1);
                userProfile.setProfileImageUrl(profileImageUrl);
            }

            if(image2 != null && !image2.isEmpty() && isProfileChanged2){
                s3Uploader.deleteProfileImage(userProfile.getExternalUserId(), 2);
                String imageUrl2 = s3Uploader.uploadProfileImage2(image2, userProfile.getExternalUserId());
                userProfile.setProfileImageUrl2(imageUrl2);
            }
            else if(isProfileChanged2 || profileImageUrl2 != null){ //기본 이미지로 변경시
                userProfile.setProfileImageUrl2(profileImageUrl2);
                s3Uploader.deleteProfileImage(userProfile.getExternalUserId(), 2);
            }
            if(image3 != null && !image3.isEmpty() && isProfileChanged3){
                s3Uploader.deleteProfileImage(userProfile.getExternalUserId(), 3);
                String imageUrl3 = s3Uploader.uploadProfileImage3(image3, userProfile.getExternalUserId());
                userProfile.setProfileImageUrl3(imageUrl3);
            }
            else if(isProfileChanged3 || profileImageUrl3 != null){ //기본 이미지로 변경시

                userProfile.setProfileImageUrl3(profileImageUrl3);
                s3Uploader.deleteProfileImage(userProfile.getExternalUserId(), 3);
            }


            userService.saveUserProfile(userProfile);


            return ResponseEntity.ok("Profile saved successfully.");
        } catch (IOException e) {
            e.printStackTrace();  // 서버 로그에 오류 메시지 출력
            return ResponseEntity.status(500).body("Failed to save profile.");
        }
    }

    // 위치 정보 업데이트 API
    @PostMapping("/update-location")
    public ResponseEntity<?> updateLocation(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Double> locationData) {
        try {
            String externalUserId = JwtUtil.validateToken(token.substring(7)); // "Bearer " 제거
            double latitude = locationData.get("latitude");
            double longitude = locationData.get("longitude");
            System.out.println(latitude);
            System.out.println(longitude);
            userService.updateUserLocation(externalUserId, latitude, longitude);
            return ResponseEntity.ok("Location updated successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Failed to update location.");
        }
    }

    @GetMapping("/get-candy")
    public ResponseEntity<?> getUserCandy(@RequestHeader("Authorization") String token) {
        try {
            // "Bearer " 부분을 제거하고 토큰만 추출
            String jwtToken = token.replace("Bearer ", "");

            // JWT 토큰을 검증하고 externalUserId 추출
            String externalUserId = JwtUtil.validateToken(jwtToken);

            // externalUserId로 사용자 조회
            User user = userService.getUserProfile(externalUserId);

            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Collections.singletonMap("message", "사용자를 찾을 수 없습니다."));
            }

            // 잔여 포인트 반환
            return ResponseEntity.ok(Collections.singletonMap("candy", user.getCandy()));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("message", "잔여 포인트를 불러오는 중 오류가 발생했습니다."));
        }
    }
    @GetMapping("/google-login")
    public ResponseEntity<?> handleGoogleLogin(@RequestParam Map<String, String> queryParams, HttpServletRequest request) {
        try {
            // Step 1: 인가 코드 확인
            String authorizationCode = queryParams.get("code");
            if (authorizationCode == null) {
                return ResponseEntity.badRequest().body("Authorization code is missing.");
            }

            // Step 2: 액세스 토큰 요청
            String accessToken = getAccessToken(authorizationCode);

            // Step 3: 유저 정보 요청
            Map<String, Object> userInfo = getUserInfo(accessToken);

            if (userInfo == null || !userInfo.containsKey("id") || !userInfo.containsKey("email")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid user info from Google.");
            }
            System.out.println("유저 정보:" + userInfo);
            // Step 4: 필요한 정보 추출
            String provider = "google";
            String externalUserId = (String) userInfo.get("id");
            String email = (String) userInfo.get("email");
            String fullName = (String) userInfo.get("name");

            // Step 5: 사용자 정보 저장
            boolean hasProfile = userService.saveOrUpdateUser(provider, externalUserId, email, fullName);
            System.out.println("hasProfile:" + hasProfile);
            // Step 6: JWT 토큰 생성
            String jwtToken = JwtUtil.generateToken(externalUserId);


            Optional<User> userData = userRepository.findByExternalUserId(externalUserId);

            // 사용자 존재 여부 확인
            if (userData.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Collections.singletonMap("message", "해당 유저를 찾을 수 없습니다."));
            }

            User userProfile = userData.get();

            // 클라이언트 IP 가져오기
            String clientIp = getClientIp(request);


            // DynamoDB에 액세스 로그 저장
            saveAccessLogToDynamoDB(externalUserId, clientIp, userProfile.getNickname(), userProfile.getGender(), userProfile.getMyLocation(),
                    userProfile.getStatus(), String.valueOf(userProfile.getAge()), String.valueOf(userProfile.getLocation()), userProfile.getEmail(),
                    userProfile.getFullName(),String.valueOf(userProfile.getId()));

            // Step 7: 앱의 딥 링크로 리다이렉트하며 토큰 전달
            String appRedirectUri = String.format(
                    "hottalkscheme://LoginScreen?success=true&token=%s&externalUserId=%s&hasProfile=%s&userId=%s",
                    jwtToken, externalUserId, hasProfile, userProfile.getId()
            );
            System.out.println("딥링크 완료");
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", appRedirectUri)
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }



    private String getAccessToken(String authorizationCode) throws Exception {
        String tokenEndpoint = "https://oauth2.googleapis.com/token";
        RestTemplate restTemplate = new RestTemplate();

        // 요청 바디 구성
        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("code", authorizationCode);
        requestBody.add("client_id", GOOGLE_WEB_CLIENT_ID);
        requestBody.add("client_secret", GOOGLE_CLIENT_SECRET);
        requestBody.add("redirect_uri", GOOGLE_REDIRECT_URI);
        requestBody.add("grant_type", "authorization_code");

        // HTTP 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

        // POST 요청 실행
        ResponseEntity<Map> response = restTemplate.postForEntity(tokenEndpoint, requestEntity, Map.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("access_token")) {
                return (String) responseBody.get("access_token");
            }
        }
        throw new Exception("Failed to retrieve access token.");
    }

    private Map<String, Object> getUserInfo(String accessToken) throws Exception {
        String userInfoEndpoint = "https://www.googleapis.com/oauth2/v2/userinfo";
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<?> entity = new HttpEntity<>(headers);

        // GET 요청 실행
        ResponseEntity<Map> response = restTemplate.exchange(userInfoEndpoint, HttpMethod.GET, entity, Map.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            return response.getBody();
        }
        throw new Exception("Failed to retrieve user info.");
    }

    @GetMapping("/myLocation")
    public ResponseEntity<?> getMyLocation(@RequestHeader("Authorization") String token) {
        try {
            // "Bearer " 부분을 제거하고 토큰만 추출
            String jwtToken = token.replace("Bearer ", "");

            // JWT 토큰을 검증하고 externalUserId 추출
            String externalUserId = JwtUtil.validateToken(jwtToken);

            // externalUserId로 사용자 조회
            User user = userService.getUserProfile(externalUserId);

            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Collections.singletonMap("message", "사용자를 찾을 수 없습니다."));
            }
            Double latitude = user.getLocation().getY();
            Double longitude = user.getLocation().getX();
            //System.out.println(latitude);
            // getLocationDTO 인스턴스를 만들어 ResponseEntity로 반환
            getLocationDTO dto = new getLocationDTO();
            dto.setLatitude(latitude);
            dto.setLongitude(longitude);
            return ResponseEntity.ok(dto);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("message", "위치 불러오던 중 에러"));
        }
    }


    @GetMapping("/get-user-profile")
    public ResponseEntity<?> getUserProfile(@RequestHeader("Authorization") String token) {
        try {
            // "Bearer " 부분을 제거하고 토큰만 추출
            String jwtToken = token.replace("Bearer ", "");

            // JWT 토큰을 검증하고 externalUserId 추출
            String externalUserId = JwtUtil.validateToken(jwtToken);

            Optional<User> userProfileOptional = userRepository.findByExternalUserId(externalUserId);

            // 사용자 존재 여부 확인
            if (userProfileOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Collections.singletonMap("message", "해당 유저를 찾을 수 없습니다."));
            }

            User userProfile = userProfileOptional.get();

            UserProfileDTO dto = new UserProfileDTO(); // 기본 생성자로 객체 생성
            // Setter 메서드 사용
            dto.setNickname(userProfile.getNickname());
            dto.setGender(userProfile.getGender());
            dto.setAge(userProfile.getAge());
            dto.setProfileImageUrl(userProfile.getProfileImageUrl());
            dto.setProfileImageUrl2(userProfile.getProfileImageUrl2());
            dto.setProfileImageUrl3(userProfile.getProfileImageUrl3());
            dto.setStatus(userProfile.getStatus());
            dto.setMyLocation(userProfile.getMyLocation());
            dto.setInterestsKey(userProfile.getInterestsKey());

            //System.out.println(dto);


            // 잔여 포인트 반환
            return ResponseEntity.ok(dto);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("message", "유저 프로필 불러오기 오류"));
        }
    }

    @GetMapping("/get-user")
    public ResponseEntity<List<UserProfileDTO>> getUsers(@RequestHeader("Authorization") String token,
                                                         @RequestParam String gender, @RequestParam String sort,
                                                         @RequestParam Integer ageRange1, @RequestParam Integer ageRange2) {
        try {
            // "Bearer " 부분을 제거하고 토큰만 추출
            String jwtToken = token.replace("Bearer ", "");
            // JWT 토큰을 검증하고 externalUserId 추출
            String externalUserId = JwtUtil.validateToken(jwtToken);


            System.out.println("성별 필터:" + gender);
            System.out.println("정렬:" + sort);


            List<UserProfileDTO> users = userService.getAllUsers(externalUserId, gender, sort, ageRange1, ageRange2);


            return ResponseEntity.ok(users);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }


    @GetMapping("/get-more-user")
    public ResponseEntity<List<UserProfileDTO>> getMoreUsers(
            @RequestHeader("Authorization") String token,
            @RequestParam String gender,
            @RequestParam String sort,
            @RequestParam(required = false) String lastDistance,   // String으로 받음
            @RequestParam(required = false) String lastLogin,        // String으로 받음
            @RequestParam Integer ageRange1,
            @RequestParam Integer ageRange2) {
        try {
            // "Bearer " 부분 제거 및 JWT 검증
            String jwtToken = token.replace("Bearer ", "");
            String externalUserId = JwtUtil.validateToken(jwtToken);

            // 파라미터 검증 및 기본값 할당
            Double parsedLastDistance = 0.0;
            if (lastDistance != null && !lastDistance.trim().isEmpty() && !"null".equalsIgnoreCase(lastDistance.trim())) {
                try {
                    parsedLastDistance = Double.valueOf(lastDistance.trim());
                } catch (NumberFormatException e) {
                    System.out.println("lastDistance 값이 유효하지 않습니다: " + lastDistance);
                    parsedLastDistance = 0.0;
                }
            }

            LocalDateTime parsedLastLogin = LocalDateTime.parse("2025-04-10T00:00:00"); // 기본값
            if (lastLogin != null && !lastLogin.trim().isEmpty() && !"null".equalsIgnoreCase(lastLogin.trim())) {
                try {
                    parsedLastLogin = LocalDateTime.parse(lastLogin.trim());
                } catch (Exception e) {
                    System.out.println("lastLogin 값을 LocalDateTime으로 변환하는 중 오류 발생: " + lastLogin);
                    // 기본값 유지
                }
            }

            System.out.println("성별 필터:" + gender);
            System.out.println("정렬:" + sort);
            System.out.println("받아온 마지막 거리:" + parsedLastDistance);
            System.out.println("마지막 로그인 시간:" + parsedLastLogin);

            List<UserProfileDTO> users = userService.getMoreUsers(
                    externalUserId,
                    gender,
                    sort,
                    parsedLastDistance,
                    parsedLastLogin,
                    ageRange1,
                    ageRange2);

            return ResponseEntity.ok(users);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }

    @PostMapping("/get-user-profile-view")
    public ResponseEntity<?> getUserProfileView(@RequestHeader("Authorization") String token, @RequestPart("externalUserId") String externalUserId) {
        try {
            //Instant start = Instant.now(); // 시작 시간
            // "Bearer " 부분을 제거하고 토큰만 추출
            String jwtToken = token.replace("Bearer ", "");
            //System.out.println("익스터널유저아이디:" + externalUserId);
            // JWT 토큰을 검증하고 externalUserId 추출
            String myExternalUserId = JwtUtil.validateToken(jwtToken);
            System.out.println("Before extraction: " + externalUserId);

            if (externalUserId != null && externalUserId.startsWith("userId-")) {
                String extractedId = externalUserId.replaceFirst("userId-", ""); // "21" 추출
                //System.out.println("Extracted ID: " + extractedId);

                // 해당 유저 조회
                Optional<User> userOptional = userRepository.findById(Long.parseLong(extractedId));

                if (userOptional.isPresent()) {
                    externalUserId = userOptional.get().getExternalUserId(); // 유저의 externalUserId 가져오기
                } else {
                    //System.out.println("User not found for extractedId: " + extractedId);
                    externalUserId = "User Not Found"; // 기본값 설정
                }

                //System.out.println("After extraction: " + externalUserId);
            }





            List<User> users = userRepository.findBothUsers(externalUserId, myExternalUserId);

            //System.out.println("쿼리 실행 시간(ms): " + java.time.Duration.between(start, end).toMillis());
            // 사용자 데이터 매핑
            UserProfileDTO myUserProfile = null;
            UserProfileDTO otherUserProfile = null;

            for (User user : users) { //내 프로필일때
                if(!(user.getExternalUserId().equals(myExternalUserId) && user.getExternalUserId().equals(externalUserId)) && user.getExternalUserId().equals(myExternalUserId)){
                    UserProfileDTO dto = new UserProfileDTO();
                    dto.setLatitude(user.getLocation().getY());
                    dto.setLongitude(user.getLocation().getX());

                    myUserProfile = dto;
                    continue;
                }
                UserProfileDTO dto = new UserProfileDTO(); //상대 프로필일때
                dto.setId(user.getId());
                dto.setNickname(user.getNickname());
                dto.setGender(user.getGender());
                dto.setAge(user.getAge());
                dto.setProfileImageUrl(user.getProfileImageUrl());
                dto.setProfileImageUrl2(user.getProfileImageUrl2());
                dto.setProfileImageUrl3(user.getProfileImageUrl3());
                dto.setStatus(user.getStatus());
                dto.setExternalUserId(user.getExternalUserId());
                dto.setMyLocation(user.getMyLocation());
                dto.setInterestsKey(user.getInterestsKey());
                dto.setLastLogin(user.getLastLogin());
                dto.setLatitude(user.getLocation().getY());
                dto.setLongitude(user.getLocation().getX());

                if(user.getExternalUserId().equals(myExternalUserId) && user.getExternalUserId().equals(externalUserId)){
                    otherUserProfile = dto;  //내 프로필일때
                    UserProfileDTO dto2 = new UserProfileDTO();
                    dto2.setLatitude(user.getLocation().getY());
                    dto2.setLongitude(user.getLocation().getX());
                    myUserProfile = dto2;
                    break;
                } else if (user.getExternalUserId().equals(externalUserId)) {
                    otherUserProfile = dto;
                }
            }

            // 상대방 정보가 없으면 에러 반환
            if (otherUserProfile == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Collections.singletonMap("message", "해당 유저를 찾을 수 없습니다."));
            }

            // 상대방 정보가 없으면 에러 반환
            if (myUserProfile == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Collections.singletonMap("message", "해당 유저를 찾을 수 없습니다."));
            }

            // 응답 DTO 생성
            UserProfileResponseDTO responseDTO = new UserProfileResponseDTO();
            responseDTO.setMyUser(myUserProfile);
            responseDTO.setOtherUser(otherUserProfile);
            //System.out.println("작업완료");

            //Instant end = Instant.now(); // 시작 시간
            //System.out.println("쿼리 실행 시간(ms): " + java.time.Duration.between(start, end).toMillis());
            // 응답 반환
            return ResponseEntity.ok(responseDTO);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("message", "유저 프로필 불러오기 오류"));
        }
    }

    @PostMapping("/delete-user")
    public ResponseEntity<?> deleteUser(@RequestHeader("Authorization") String token, @RequestBody String UUID) {
        try {
            // "Bearer " 부분을 제거하고 토큰만 추출
            String jwtToken = token.replace("Bearer ", "");

            // JWT 토큰을 검증하고 externalUserId 추출
            String externalUserId = JwtUtil.validateToken(jwtToken);

            //
            //Long returnedUserId = userService.deleteUser(externalUserId, UUID);

            DeleteUserResultDTO result = userService.deleteUser(externalUserId, UUID);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("message", "회원탈퇴 중 에러"));
        }
    }

    @GetMapping("/my-id")
    public ResponseEntity<?> getMyUserId(@RequestHeader("Authorization") String token) {
        // "Bearer " 부분을 제거하고 토큰만 추출
        String jwtToken = token.replace("Bearer ", "");

        // JWT 토큰을 검증하고 externalUserId 추출
        String externalUserId = JwtUtil.validateToken(jwtToken);

        try {
            Optional<User> user = userRepository.findByExternalUserId(externalUserId);
            Long userId = user.get().getId();
            return ResponseEntity.ok(Collections.singletonMap("userId", userId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("토큰이 유효하지 않습니다.");
        }
    }
}
