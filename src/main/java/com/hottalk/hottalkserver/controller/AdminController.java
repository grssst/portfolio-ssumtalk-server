package com.hottalk.hottalkserver.controller;

import com.hottalk.hottalkserver.dto.NoticeAdminDTO;
import com.hottalk.hottalkserver.dto.PostAdminDTO;
import com.hottalk.hottalkserver.dto.SimpleUserDTO;
import com.hottalk.hottalkserver.dto.UserDetailAdminDTO;
import com.hottalk.hottalkserver.model.*;
import com.hottalk.hottalkserver.repository.*;
import com.hottalk.hottalkserver.service.AdminService;
import com.hottalk.hottalkserver.service.SanctionService;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.hottalk.hottalkserver.util.JwtUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;
    @Autowired
    private BalanceHistoryRepository balanceHistoryRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private PurchaseTransactionAndroidRepository purchaseTransactionAndroidRepository;
    @Autowired
    private PurchaseTransactionIosRepository purchaseTransactionIosRepository;
    @Autowired
    private SanctionService sanctionService;
    @Value("${OTP_SECRET_KEY}")  // 환경변수에서 Secret Key 가져오기
    private String secretKey;
    @Autowired
    private ReportChattingRepository reportChattingRepository;
    @Autowired
    private ReportRepository reportRepository;
    private final GoogleAuthenticator gAuth = new GoogleAuthenticator();

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginData) {
        String username = loginData.get("username");
        String password = loginData.get("password");

        if (adminService.authenticate(username, password)) {
            adminService.updateLastLogin(username);
            return ResponseEntity.ok(Map.of("success", true, "message", "Login successful"));
        } else {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Invalid credentials"));
        }
    }

    /**
     * 문의 목록 조회
     *
     * @param category (optional) 카테고리 (예: '결제', '기능 요청', '기타', 'all' 등)
     * @param sort     (optional) 정렬 방식 ('asc' or 'desc'), 기본값 desc
     * @param token    Authorization 헤더 (Bearer ...)
     */
    @GetMapping("/inquiries")
    public ResponseEntity<?> getInquiries(
            @RequestParam(required = false, defaultValue = "all") String category,
            @RequestParam(required = false, defaultValue = "desc") String sort,
            @RequestHeader("Authorization") String token
    ) {
        try {
            List<CallCenter> inquiries = adminService.getInquiries(category, sort);
            return ResponseEntity.ok(inquiries);

        } catch (Exception e) {
            return ResponseEntity.status(401).body("유효하지 않은 토큰 또는 인증 실패");
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestParam int otpCode) {
        // 현재 secretKey와 시간 기반으로 예상되는 OTP 값을 계산
        //int expectedOtp = gAuth.getTotpPassword(secretKey);
        //System.out.println("입력받은 OTP: " + otpCode);
        //System.out.println("예상 OTP: " + expectedOtp);

        boolean result = gAuth.authorize(secretKey, otpCode);
        System.out.println("인증 결과: " + result);

        if (result) {
            // 로그인 과정에서 관리자의 고유 ID를 사용하여 토큰 생성 (예: "admin"으로 고정하거나, 실제 admin ID를 사용)
            String adminId = "admin";  // 실제 관리자의 ID를 여기에 할당
            String jwtToken = JwtUtil.generateAdminToken(adminId);

            // 예시: JWT 생성 시 role 클레임을 "admin"으로 설정하도록 구현되어 있다고 가정
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "token", jwtToken
            ));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "success", false,
                            "message", "Invalid OTP"
                    ));
        }

        //return result;
    }

    @GetMapping("/reports")
    public List<Report> getAllReports() {
        return reportRepository.findAll(); // 필터링, 정렬은 필요 시 추가
    }

    @GetMapping("/reported-chats")
    public List<ReportChatting> getReportedChats() {
        return reportChattingRepository.findAll(); // 마찬가지로 필터링 가능
    }

    /**
     * 유저 상세 조회
     * @param userId (Optional) 유저 DB PK
     * @param externalUserId (Optional) 유저 external ID
     * @param token 관리자 토큰
     * @return user + posts + meetings + blockedUsers + chatRooms + sanctions + callCenters + reports + balanceHistories
     */
    @GetMapping("/user-detail")
    public ResponseEntity<?> getUserDetail(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String externalUserId,
            @RequestHeader("Authorization") String token
    ) {

        // 2) 실제 Service 호출
        UserDetailAdminDTO detailDTO = adminService.getUserDetail(userId, externalUserId);
        if (detailDTO == null) {
            return ResponseEntity.badRequest().body("해당 유저를 찾을 수 없습니다.");
        }

        return ResponseEntity.ok(detailDTO);
    }

    @GetMapping("/notices")
    public ResponseEntity<?> getNotices(@RequestHeader("Authorization") String token) {
        try {
            // 관리자 인증 확인 (예: JwtUtil.validateToken(...) 등)
            String adminExternalId = JwtUtil.validateToken(token.substring(7));
            // 실제 관리자 권한 체크 로직이 필요하면 추가

            List<Notice> list = adminService.getAllNotices();
            return ResponseEntity.ok(list);
        } catch (Exception e) {
            return ResponseEntity.status(401).body("인증 오류. 다시 로그인해주세요.");
        }
    }

    /**
     * 전체 유저 목록 (ID, 닉네임만)
     */
    @GetMapping("/all-users")
    public ResponseEntity<?> getAllUsers(@RequestHeader("Authorization") String token) {
        try {
            String adminId = JwtUtil.validateToken(token.substring(7));
            // 관리자 권한 체크 로직 필요하다면 추가
        } catch (Exception e) {
            return ResponseEntity.status(401).body("다시 로그인 해주세요.");
        }

        List<SimpleUserDTO> userList = adminService.getAllUsersSimple();
        return ResponseEntity.ok(userList);
    }

    /**
     * 공지 등록
     */
    @PostMapping("/notices")
    public ResponseEntity<?> createNotice(@RequestHeader("Authorization") String token,
                                          @RequestBody NoticeAdminDTO dto) {
        try {
            // 관리자 인증 확인
            String adminExternalId = JwtUtil.validateToken(token.substring(7));
            // 관리자 권한 체크 로직 필요하면 추가

            Notice saved = adminService.createNotice(dto.getTitle(), dto.getContent());
            return ResponseEntity.ok("공지 등록 완료 (ID: " + saved.getId() + ")");
        } catch (Exception e) {
            return ResponseEntity.status(401).body("인증 오류. 다시 로그인해주세요.");
        }
    }

    // 제재 적용 API
    @PostMapping("/sanction")
    public ResponseEntity<?> applySanction(@RequestBody Map<String, Object> requestData, @RequestHeader("Authorization") String token) {
        Long userId = Long.valueOf(requestData.get("userId").toString());
        String reason = requestData.get("reason").toString();
        String duration = requestData.get("sanctionDuration").toString();
        LocalDateTime startTime = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        LocalDateTime endTime = calculateEndTime(startTime, duration);

        sanctionService.applySanction(userId, reason, startTime, endTime);
        return ResponseEntity.ok(Map.of("message", "제재가 성공적으로 적용되었습니다."));

    }

    // 제재 해제 API
    @PostMapping("/release-sanction")
    public ResponseEntity<?> releaseSanction(@RequestBody Map<String, Object> requestData, @RequestHeader("Authorization") String token) {
        Long userId = Long.valueOf(requestData.get("userId").toString());
        sanctionService.releaseSanction(userId);
        return ResponseEntity.ok(Map.of("message", "제재해제가 성공적으로 적용되었습니다."));
    }

    // 제재 기간 계산
    private LocalDateTime calculateEndTime(LocalDateTime startTime, String duration) {
        switch (duration) {
            case "10분":
                return startTime.plusMinutes(10);
            case "30분":
                return startTime.plusMinutes(30);
            case "1시간":
                return startTime.plusHours(1);
            case "3시간":
                return startTime.plusHours(3);
            case "12시간":
                return startTime.plusHours(12);
            case "1일":
                return startTime.plusDays(1);
            case "3일":
                return startTime.plusDays(3);
            case "7일":
                return startTime.plusDays(7);
            case "30일":
                return startTime.plusDays(30);
            case "1년":
                return startTime.plusYears(1);
            case "permanent":
                return null; // 영구 제재인 경우 종료 시간이 없음
            default:
                throw new IllegalArgumentException("Invalid sanction duration: " + duration);
        }
    }




    @GetMapping("/count-all-users")
    public ResponseEntity<?> getAllUserCount(@RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(userRepository.countUsers());
    }

    @GetMapping("/count-daily-users")
    public ResponseEntity<?> getDailyUserCount(@RequestHeader("Authorization") String token) {

        LocalDateTime startOfDay = LocalDateTime.of(LocalDateTime.now(ZoneId.of("Asia/Seoul")).toLocalDate(), LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.of(LocalDateTime.now(ZoneId.of("Asia/Seoul")).toLocalDate(), LocalTime.MAX);
        return ResponseEntity.ok(userRepository.countLoginsBetween(startOfDay, endOfDay));
    }

    @GetMapping("/count-month-users")
    public ResponseEntity<?> getMonthUserCount(@RequestHeader("Authorization") String token) {

        LocalDate today = LocalDate.now();
        // 이번 달의 시작 (예: 2025-03-01 00:00:00)
        LocalDateTime startOfMonth = today.withDayOfMonth(1).atStartOfDay();
        // 이번 달의 마지막 (예: 2025-03-31 23:59:59.999)
        LocalDateTime endOfMonth = today.withDayOfMonth(today.lengthOfMonth()).atTime(LocalTime.MAX);
        return ResponseEntity.ok(userRepository.countLoginsBetween(startOfMonth, endOfMonth));
    }

    @GetMapping("/count-total-posts")
    public ResponseEntity<?> getTotalPostCount(@RequestHeader("Authorization") String token) {

        return ResponseEntity.ok(postRepository.countPosts());
    }

    @GetMapping("/count-daily-posts")
    public ResponseEntity<?> getDailyPostCount(@RequestHeader("Authorization") String token) {

        LocalDateTime startOfDay = LocalDateTime.of(LocalDateTime.now(ZoneId.of("Asia/Seoul")).toLocalDate(), LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.of(LocalDateTime.now(ZoneId.of("Asia/Seoul")).toLocalDate(), LocalTime.MAX);
        return ResponseEntity.ok(postRepository.countLoginsBetween(startOfDay, endOfDay));
    }

    @GetMapping("/count-month-posts")
    public ResponseEntity<?> getMonthPostCount(@RequestHeader("Authorization") String token) {

        LocalDate today = LocalDate.now();
        // 이번 달의 시작 (예: 2025-03-01 00:00:00)
        LocalDateTime startOfMonth = today.withDayOfMonth(1).atStartOfDay();
        // 이번 달의 마지막 (예: 2025-03-31 23:59:59.999)
        LocalDateTime endOfMonth = today.withDayOfMonth(today.lengthOfMonth()).atTime(LocalTime.MAX);
        return ResponseEntity.ok(postRepository.countLoginsBetween(startOfMonth, endOfMonth));
    }

   @GetMapping("/total-cash-and-count")
    public ResponseEntity<?> getTotalCashAndCount(@RequestHeader("Authorization") String token) {
       Long purchaseCount = balanceHistoryRepository.countPurchase();
       List<Long> amounts = balanceHistoryRepository.sumKRW();  // 수정: 개별 금액 가져오기

       Long krw = 0L;

       for (Long b : amounts) {
           if (b == 30) {
               krw += 1100;
           } else if (b == 100) {
               krw += 4400;
           } else if (b == 300) {
               krw += 9900;
           } else if (b == 500) {
               krw += 15000;
           } else if (b == 1000) {
               krw += 33000;
           } else if (b == 3000) {
               krw += 99000;
           }
       }

       Map<String, Object> response = new HashMap<>();
       response.put("totalCash", krw);
       response.put("purchaseCount", purchaseCount);

       return ResponseEntity.ok(response);
    }

    @GetMapping("/daily-cash-and-count")
    public ResponseEntity<?> getDailyCashAndCount(@RequestHeader("Authorization") String token) {
        LocalDateTime startOfDay = LocalDateTime.of(LocalDateTime.now(ZoneId.of("Asia/Seoul")).toLocalDate(), LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.of(LocalDateTime.now(ZoneId.of("Asia/Seoul")).toLocalDate(), LocalTime.MAX);

        Long purchaseCount = balanceHistoryRepository.countPurchaseInRange(startOfDay, endOfDay);
        List<Long> amounts = balanceHistoryRepository.sumKRWInRange(startOfDay, endOfDay);  // 수정: 개별 금액 가져오기

        Long krw = 0L;

        for (Long b : amounts) {
            if (b == 30) {
                krw += 1100;
            } else if (b == 100) {
                krw += 4400;
            } else if (b == 300) {
                krw += 9900;
            } else if (b == 500) {
                krw += 15000;
            } else if (b == 1000) {
                krw += 33000;
            } else if (b == 3000) {
                krw += 99000;
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("dailyCash", krw);
        response.put("purchaseCount", purchaseCount);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/month-cash-and-count")
    public ResponseEntity<?> getMonthCashAndCount(@RequestHeader("Authorization") String token) {
        LocalDate today = LocalDate.now();
        // 이번 달의 시작 (예: 2025-03-01 00:00:00)
        LocalDateTime startOfMonth = today.withDayOfMonth(1).atStartOfDay();
        // 이번 달의 마지막 (예: 2025-03-31 23:59:59.999)
        LocalDateTime endOfMonth = today.withDayOfMonth(today.lengthOfMonth()).atTime(LocalTime.MAX);

        Long purchaseCount = balanceHistoryRepository.countPurchaseInRange(startOfMonth, endOfMonth);
        List<Long> amounts = balanceHistoryRepository.sumKRWInRange(startOfMonth, endOfMonth);  // 수정: 개별 금액 가져오기

        Long krw = 0L;

        for (Long b : amounts) {
            if (b == 30) {
                krw += 1100;
            } else if (b == 100) {
                krw += 4400;
            } else if (b == 300) {
                krw += 9900;
            } else if (b == 500) {
                krw += 15000;
            } else if (b == 1000) {
                krw += 33000;
            } else if (b == 3000) {
                krw += 99000;
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("monthCash", krw);
        response.put("purchaseCount", purchaseCount);

        return ResponseEntity.ok(response);
    }

    @Transactional
    @PostMapping("/add-candy")
    public ResponseEntity<?> addCandy(@RequestHeader("Authorization") String token, @RequestBody Map<String, Object> payload) {
        try {
            Long userId;
            Integer amount;

            // userId 변환 (Object → Long)
            Object userIdObj = payload.get("userId");
            if (userIdObj instanceof Integer) {
                userId = ((Integer) userIdObj).longValue();
            } else if (userIdObj instanceof Long) {
                userId = (Long) userIdObj;
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Collections.singletonMap("message", "유효하지 않은 userId 형식입니다."));
            }

            // amount 변환 (Object → Integer)
            Object amountObj = payload.get("amount");
            if (amountObj instanceof Integer) {
                amount = (Integer) amountObj;
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Collections.singletonMap("message", "유효하지 않은 amount 형식입니다."));
            }


            Optional<User> userOptional = userRepository.findById(userId);
            if (userOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Collections.singletonMap("message", "해당 유저를 찾을 수 없습니다."));
            }

            User userEntity = userOptional.get();

            // 캔디 값이 null이면 0으로 초기화 후 추가
            int currentCandy = userEntity.getCandy() != null ? userEntity.getCandy() : 0;
            userEntity.setCandy(currentCandy + amount);

            userRepository.save(userEntity);

            // BalanceHistoryAndroid 기록
            BalanceHistory history = new BalanceHistory();
            history.setUserId(userEntity.getId());
            history.setChangeType("adminAdd");
            history.setAmount(amount);
            history.setBalanceBefore(currentCandy);
            history.setBalanceAfter(currentCandy + amount);
            history.setExternalUserId(userEntity.getExternalUserId());
            balanceHistoryRepository.save(history);

            return ResponseEntity.ok(Collections.singletonMap("message", "캔디가 정상적으로 추가되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("message", "서버 오류: " + e.getMessage()));
        }
    }

    @Transactional
    @PostMapping("/remove-candy")
    public ResponseEntity<?> removeCandy(@RequestHeader("Authorization") String token, @RequestBody Map<String, Object> payload) {
        try {
            Long userId;
            Integer amount;

            // userId 변환 (Object → Long)
            Object userIdObj = payload.get("userId");
            if (userIdObj instanceof Integer) {
                userId = ((Integer) userIdObj).longValue();
            } else if (userIdObj instanceof Long) {
                userId = (Long) userIdObj;
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Collections.singletonMap("message", "유효하지 않은 userId 형식입니다."));
            }

            // amount 변환 (Object → Integer)
            Object amountObj = payload.get("amount");
            if (amountObj instanceof Integer) {
                amount = (Integer) amountObj;
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Collections.singletonMap("message", "유효하지 않은 amount 형식입니다."));
            }

            Optional<User> userOptional = userRepository.findById(userId);
            if (userOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Collections.singletonMap("message", "해당 유저를 찾을 수 없습니다."));
            }

            User userEntity = userOptional.get();

            // 현재 캔디 값 (null이면 0으로 설정)
            int currentCandy = userEntity.getCandy() != null ? userEntity.getCandy() : 0;

            // 차감 후 음수가 될 경우 0으로 설정
            int newCandy = Math.max(0, currentCandy - amount);

            // 캔디 업데이트
            userEntity.setCandy(newCandy);
            userRepository.save(userEntity);

            // BalanceHistoryAndroid 기록
            BalanceHistory history = new BalanceHistory();
            history.setUserId(userEntity.getId());
            history.setChangeType("AdminRemove"); // 차감이므로 "deduction" 사용
            history.setAmount(-amount); // 차감된 금액을 음수로 저장
            history.setBalanceBefore(currentCandy);
            history.setBalanceAfter(newCandy);
            history.setExternalUserId(userEntity.getExternalUserId());
            balanceHistoryRepository.save(history);

            return ResponseEntity.ok(Collections.singletonMap("message", "캔디가 정상적으로 차감되었습니다. 현재 캔디: " + newCandy));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("message", "서버 오류: " + e.getMessage()));
        }
    }

    @Transactional
    @PostMapping("/set-candy")
    public ResponseEntity<?> setCandy(@RequestHeader("Authorization") String token, @RequestBody Map<String, Object> payload) {
        try {
            Long userId;
            Integer amount;

            // userId 변환 (Object → Long)
            Object userIdObj = payload.get("userId");
            if (userIdObj instanceof Integer) {
                userId = ((Integer) userIdObj).longValue();
            } else if (userIdObj instanceof Long) {
                userId = (Long) userIdObj;
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Collections.singletonMap("message", "유효하지 않은 userId 형식입니다."));
            }

            // amount 변환 (Object → Integer)
            Object amountObj = payload.get("amount");
            if (amountObj instanceof Integer) {
                amount = (Integer) amountObj;
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Collections.singletonMap("message", "유효하지 않은 amount 형식입니다."));
            }

            Optional<User> userOptional = userRepository.findById(userId);
            if (userOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Collections.singletonMap("message", "해당 유저를 찾을 수 없습니다."));
            }

            User userEntity = userOptional.get();
            int currentCandy = userEntity.getCandy() != null ? userEntity.getCandy() : 0;
            // 캔디 업데이트
            userEntity.setCandy(amount);
            userRepository.save(userEntity);

            // 현재 캔디 값 (null이면 0으로 설정)
            int changedCandy=0;

            if(currentCandy > amount) {
                changedCandy = -(currentCandy-amount);
            } else if(currentCandy < amount) {
                changedCandy = amount-currentCandy;
            } else if (currentCandy == amount) {
                changedCandy = currentCandy;
            }

            // BalanceHistoryAndroid 기록
            BalanceHistory history = new BalanceHistory();
            history.setUserId(userEntity.getId());
            history.setChangeType("AdminSet"); // 차감이므로 "deduction" 사용

            history.setAmount(changedCandy); // 차감된 금액을 음수로 저장

            history.setBalanceBefore(currentCandy);
            history.setBalanceAfter(amount);
            history.setExternalUserId(userEntity.getExternalUserId());
            balanceHistoryRepository.save(history);

            return ResponseEntity.ok(Collections.singletonMap("message", "캔디가 정상적으로 변경되었습니다. 현재 캔디: " + amount));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("message", "서버 오류: " + e.getMessage()));
        }
    }

    @GetMapping("/users-by-nickname")
    public ResponseEntity<?> getUsersByNickname(
            @RequestParam("keyword") String keyword,
            @RequestHeader("Authorization") String token
    ) {
        try {
            JwtUtil.validateToken(token.substring(7)); // 기존과 동일한 관리자 토큰검증
        } catch (Exception e) {
            return ResponseEntity.status(401).body("다시 로그인 해주세요.");
        }

        if (keyword == null || keyword.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("keyword를 입력해주세요.");
        }

        List<SimpleUserDTO> list = adminService.getUsersSimpleByNickname(keyword.trim());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/user-detail-by-nickname")
    public ResponseEntity<?> getUserDetailByNickname(
            @RequestParam("nickname") String nickname,
            @RequestHeader("Authorization") String token
    ) {
        try {
            JwtUtil.validateToken(token.substring(7));
        } catch (Exception e) {
            return ResponseEntity.status(401).body("다시 로그인 해주세요.");
        }

        if (nickname == null || nickname.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("nickname을 입력해주세요.");
        }

        UserDetailAdminDTO detail = adminService.getUserDetailByNickname(nickname.trim());
        if (detail == null) {
            return ResponseEntity.badRequest().body("해당 닉네임의 유저를 찾을 수 없습니다.");
        }
        return ResponseEntity.ok(detail);
    }
    @GetMapping("/search-posts")
    public ResponseEntity<?> searchPosts(
            @RequestParam("keyword") String keyword,
            @RequestHeader("Authorization") String token
    ) {
        try {
            JwtUtil.validateToken(token.substring(7));
        } catch (Exception e) {
            return ResponseEntity.status(401).body("다시 로그인 해주세요.");
        }
        if (keyword == null || keyword.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("keyword를 입력해주세요.");
        }
        List<PostAdminDTO> list = adminService.searchPostsByContent(keyword.trim());
        return ResponseEntity.ok(list);
    }

    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<?> deletePost(
            @PathVariable Long postId,
            @RequestHeader("Authorization") String token
    ) {
        try {
            JwtUtil.validateToken(token.substring(7));
        } catch (Exception e) {
            return ResponseEntity.status(401).body("다시 로그인 해주세요.");
        }
        boolean ok = adminService.deletePostById(postId);
        if (!ok) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("해당 게시글이 없습니다.");
        }
        return ResponseEntity.ok("삭제 완료");
    }

    @DeleteMapping("/users/{userId}/profile-images")
    public ResponseEntity<?> deleteUserProfileImages(
            @PathVariable Long userId,
            @RequestHeader("Authorization") String token
    ) {
        try {
            // 관리자 토큰 검증
            String adminId = JwtUtil.validateToken(token.substring(7));
            // 필요시 role 체크

            boolean ok = adminService.deleteAllProfileImagesByUserId(userId);
            if (!ok) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false, "message", "유저를 찾을 수 없거나 외부ID가 없습니다."));
            }
            return ResponseEntity.ok(Map.of("success", true, "message", "프로필 이미지(1/2/3) 전체 삭제 및 DB 초기화 완료"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "인증 오류. 다시 로그인해주세요."));
        }
    }
}
