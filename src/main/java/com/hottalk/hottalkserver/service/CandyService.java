package com.hottalk.hottalkserver.service;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hottalk.hottalkserver.dto.PurchaseVerifyRequestAndroidDTO;
import com.hottalk.hottalkserver.dto.PurchaseVerifyResponseAndroidDTO;
import com.hottalk.hottalkserver.dto.PurchaseVerifyResponseIosDTO;
import com.hottalk.hottalkserver.dto.RealtimeChatDTO;
import com.hottalk.hottalkserver.model.BalanceHistory;
import com.hottalk.hottalkserver.model.PurchaseTransactionAndroid;
import com.hottalk.hottalkserver.model.PurchaseTransactionIos;
import com.hottalk.hottalkserver.model.User;
import com.hottalk.hottalkserver.repository.BalanceHistoryRepository;
import com.hottalk.hottalkserver.repository.PurchaseTransactionAndroidRepository;
import com.hottalk.hottalkserver.repository.PurchaseTransactionIosRepository;
import com.hottalk.hottalkserver.repository.UserRepository;
import com.hottalk.hottalkserver.util.GooglePlayAuthService;
import com.hottalk.hottalkserver.util.JwtUtil;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class CandyService {
    private final PurchaseTransactionAndroidRepository transactionAndroidRepository;
    private final PurchaseTransactionIosRepository transactionIosRepository;
    private final UserRepository userRepository;
    private final BalanceHistoryRepository balanceHistoryRepository;
    private final GooglePlayAuthService googlePlayAuthService;
    private final FcmService fcmService;
    private final String SANDBOX_URL = "https://sandbox.itunes.apple.com/verifyReceipt";
    private final String PRODUCTION_URL = "https://buy.itunes.apple.com/verifyReceipt";

    @Value("${ios.shared-secret}")
    private String IosSharedSecret;
    private static String SHARED_SECRET; // `static` 변수
    @PostConstruct
    public void init() {
        SHARED_SECRET = IosSharedSecret; // `static` 변수에 값 할당
    }
    public CandyService(PurchaseTransactionAndroidRepository transactionAndroidRepository,
                        UserRepository userRepository,
                        BalanceHistoryRepository balanceHistoryRepository,
                        GooglePlayAuthService googlePlayAuthService,
                        PurchaseTransactionIosRepository transactionIos,
                        FcmService fcmService
    ) {
        this.transactionAndroidRepository = transactionAndroidRepository;
        this.userRepository = userRepository;
        this.balanceHistoryRepository = balanceHistoryRepository;
        this.googlePlayAuthService = googlePlayAuthService;
        this.transactionIosRepository = transactionIos;
        this.fcmService = fcmService;
    }

    @Transactional
    public PurchaseVerifyResponseAndroidDTO verifyAndCompletePurchaseAndroid(PurchaseVerifyRequestAndroidDTO request) {


        // 실제 영수증 검증 로직 구현
        // 1. Access Token 획득
        String accessToken = googlePlayAuthService.getAccessToken();

        // 2. Google Play Developer API 호출
        String url = String.format(
                "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/%s/purchases/products/%s/tokens/%s",
                request.getPackageName(), // request DTO에 packageName 필드가 있다고 가정
                request.getProductId(),
                request.getPurchaseToken()
        );

        // WebClient 사용 예제 (Spring 5 이상)
        WebClient webClient = WebClient.builder().build();

        String responseBody = webClient.get()
                .uri(url)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(String.class)
                .block();


        int purchaseState;
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(responseBody);
            purchaseState = root.get("purchaseState").asInt();
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse purchase response JSON", e);
        }

        // purchaseState == 0 이면 정상 구매
        boolean isValid = (purchaseState == 0);


        // DB 기록
        PurchaseTransactionAndroid tx = new PurchaseTransactionAndroid();
        tx.setUserId(request.getUserId());
        tx.setProductId(request.getProductId());
        tx.setOrderId(request.getOrderId());
        tx.setPurchaseToken(request.getPurchaseToken());
        tx.setPurchaseTime(LocalDateTime.now(ZoneId.of("Asia/Seoul")));
        tx.setPurchaseState(purchaseState);
        tx.setVerified(isValid);
        tx.setConsumed(isValid); // 유효하면 소비 처리
        tx.setExternalUserId(request.getExternalUserId());
        transactionAndroidRepository.save(tx);

        int updatedCandy = 0;

        if (isValid) {
            // 상품 ID에 따른 캔디 수량 계산
            int amount = getCandyAmount(request.getProductId());

            // 유저 찾아서 캔디 업데이트
            User user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            int before = user.getCandy();
            user.setCandy(before + amount);
            userRepository.save(user);
            updatedCandy = user.getCandy();

            // BalanceHistoryAndroid 기록
            BalanceHistory history = new BalanceHistory();
            history.setUserId(request.getUserId());
            history.setChangeType("purchase");
            history.setAmount(amount);
            history.setBalanceBefore(before);
            history.setBalanceAfter(before + amount);
            history.setRelatedTransactionId(tx.getId());
            history.setExternalUserId(request.getExternalUserId());
            balanceHistoryRepository.save(history);
        }

        if(!isValid){
            RealtimeChatDTO dto = new RealtimeChatDTO();
            dto.setRoomId("asd");
            dto.setMessageContent("결제 실패 발생 id:" + request.getUserId());
            dto.setRecipientId("admin");
            dto.setSenderId("server");
            dto.setMyNickname("결제 실패 발생");
            //SQS에 푸시 알림 요청 전송
            fcmService.sendToSqsAdmin(dto);
        }

        PurchaseVerifyResponseAndroidDTO response = new PurchaseVerifyResponseAndroidDTO();
        response.setSuccess(isValid);
        response.setUpdatedCandy(updatedCandy);
        response.setStatus(purchaseState);
        return response;
    }

    @Transactional
    public PurchaseVerifyResponseIosDTO verifyAndCompletePurchaseIos(String token, String receipt) {
        System.out.println("### Step 1: 시작 - iOS Purchase Verification");

        // 1. Bearer 토큰 제거
        String jwtToken = token.replace("Bearer ", "");

        // 2. JWT 토큰 검증 및 externalUserId 파싱
        String externalUserId = JwtUtil.validateToken(jwtToken);

        // 3. 유저 조회
        //    - 실제로는 externalUserId를 User 테이블과 매핑하는 로직이 필요
        User user = userRepository.findByExternalUserId(externalUserId)
                .orElseThrow(() -> new RuntimeException("User not found by externalUserId: " + externalUserId));

        // 4. 영수증 검증 (Production 서버 먼저)
        //System.out.println("### Step 1: Production 서버로 영수증 검증 시작");
        JsonNode verifyResult = callIosReceiptValidationApi(PRODUCTION_URL, receipt);

        // 5. 21007(=Sandbox receipt)인 경우, Sandbox 서버로 재검증
        if (verifyResult != null) {
            String status = verifyResult.get("status").asText();
            System.out.println("### Step 1-1: Production 검증 결과 status=" + status);

            if ("21007".equals(status)) {
                System.out.println("### Step 1-2: Sandbox 영수증이므로 Sandbox 서버로 재검증");
                verifyResult = callIosReceiptValidationApi(SANDBOX_URL, receipt);
            }
        }

        // 6. 최종 검증 결과 확인
        PurchaseVerifyResponseIosDTO response = new PurchaseVerifyResponseIosDTO();
        System.out.println("verifyResult: " + verifyResult);

        if (verifyResult == null) {
            System.out.println("### Step 2: 검증 결과가 null입니다. Apple 서버 응답 없음.");
            throw new RuntimeException("iOS 영수증 검증 실패: 응답이 null");
        }

        String finalStatus = verifyResult.get("status").asText();
        System.out.println("### Step 2: 최종 status = " + finalStatus);

        // purchaseState == 0 이면 정상 구매
        boolean isValid = ("0".equals(finalStatus));

        if(!isValid){
            RealtimeChatDTO dto = new RealtimeChatDTO();
            dto.setRoomId("asd");
            dto.setMessageContent("결제 실패 발생 id:" + user.getId());
            dto.setRecipientId("admin");
            dto.setSenderId("server");
            dto.setMyNickname("결제 실패 발생");
            //SQS에 푸시 알림 요청 전송
            fcmService.sendToSqsAdmin(dto);
        }

        LocalDateTime localDateTime = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        try{
            // 1) purchase_date 문자열 추출
            String rawDateTime = verifyResult.get("receipt")
                    .get("in_app")
                    .get(0)
                    .get("purchase_date")
                    .asText();
            // 예: "2024-12-25 14:09:05 Etc/GMT"

            // 1) " Etc/GMT" 제거 (또는 " Etc/GMT" → "" 치환)
            if (rawDateTime.contains(" Etc/GMT")) {
                rawDateTime = rawDateTime.replace(" Etc/GMT", "");
                // 이제 "2024-12-25 14:09:05"
            }

            // 2) "yyyy-MM-dd HH:mm:ss" 형식으로 파싱
            DateTimeFormatter baseFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.US);
            LocalDateTime naiveDateTime = LocalDateTime.parse(rawDateTime, baseFormatter);

            // 3) 이 시각을 'UTC'로 간주
            ZonedDateTime utcZdt = naiveDateTime.atZone(ZoneId.of("UTC"));

            // 4) (옵션) 서버 로컬 타임존으로 변환
            localDateTime = utcZdt.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();

        } catch (Exception e) {
            localDateTime = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        }


        // DB 기록
        PurchaseTransactionIos tx = new PurchaseTransactionIos();
        tx.setUserId(user.getId());
        tx.setProductId(verifyResult.get("receipt")
                .get("in_app")
                .get(0)
                .get("product_id")
                .asText());
        tx.setTransactionId(verifyResult.get("receipt")
                .get("in_app")
                .get(0)
                .get("transaction_id")
                .asText());
        tx.setOriginalTransactionId(verifyResult.get("receipt")
                .get("in_app")
                .get(0)
                .get("original_transaction_id")
                .asText());
        tx.setPurchaseDate(localDateTime);
        tx.setStatus(Integer.parseInt(finalStatus));
        tx.setVerified(isValid);
        tx.setConsumed(isValid); // 유효하면 소비 처리
        tx.setExternalUserId(externalUserId);
        transactionIosRepository.save(tx);




        // status == 0이면 검증 성공
        if ("0".equals(finalStatus)) {
            System.out.println("### Step 3: iOS 영수증 검증 성공. in_app 배열 파싱 시작");

            JsonNode receiptInfo = verifyResult.get("receipt");
            if (receiptInfo != null && receiptInfo.has("in_app")) {
                JsonNode inAppArray = receiptInfo.get("in_app");
                for (JsonNode item : inAppArray) {
                    // 각 in_app 항목에 대한 정보 파싱
                    String productId = item.get("product_id").asText();

                    // 1) 중복 결제 방지 (예: DB에 transactionId 기록 확인)
                    // if (transactionIosRepository.existsByTransactionId(transactionId)) {
                    //     System.out.println("이미 처리된 transactionId입니다. 중복 처리 방지");
                    //     continue;
                    // }

                    // 2) productId에 따른 캔디 개수 계산 & 유저 캔디 추가
                    int amount = getCandyAmount(productId);
                    if (amount <= 0) {
                        System.out.println("잘못된 productId: " + productId + ", 지급할 캔디 없음.");
                        continue;
                    }

                    int beforeCandy = user.getCandy();
                    user.setCandy(beforeCandy + amount);
                    userRepository.save(user);
                    System.out.println("### Step 3-1: 유저 캔디 업데이트 완료: " + beforeCandy + " -> " + user.getCandy());



                    // BalanceHistoryAndroid 기록
                    BalanceHistory history = new BalanceHistory();
                    history.setUserId(user.getId());
                    history.setChangeType("purchase");
                    history.setAmount(amount);
                    history.setBalanceBefore(beforeCandy);
                    history.setBalanceAfter(beforeCandy + amount);
                    history.setRelatedTransactionId(tx.getId());
                    history.setExternalUserId(externalUserId);
                    balanceHistoryRepository.save(history);
                }
            }

        } else {
            // status != 0 → 검증 실패
            System.out.println("### Step 6: iOS 영수증 검증 실패. status=" + finalStatus);
        }

        // 최종 응답 리턴
        // 응답 DTO 구성
        response.setSuccess(isValid);
        response.setUpdatedCandy(user.getCandy());
        response.setStatus(Long.parseLong(finalStatus));
        return response;
    }





    private JsonNode callIosReceiptValidationApi(String url, String receipt) {
        System.out.println("=== callIosReceiptValidationApi() 시작: " + url);
        try {
            // 요청 Body 구성
            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode requestJson = objectMapper.createObjectNode();
            requestJson.put("receipt-data", receipt);
            requestJson.put("password", SHARED_SECRET);

            // (옵션) 구 버전 트랜잭션 제외
            // requestJson.put("exclude-old-transactions", true);

            // RequestEntity 생성
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(requestJson.toString(), headers);

            // RestTemplate 이용
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, entity, String.class);

            if (responseEntity.getStatusCode() == HttpStatus.OK) {
                System.out.println("=== callIosReceiptValidationApi() HTTP 200 OK");
                String body = responseEntity.getBody();
                System.out.println("=== Apple 서버 응답 body: " + body);

                if (body != null) {
                    return objectMapper.readTree(body);
                }
            } else {
                System.out.println("=== Apple 서버 응답이 정상적이지 않습니다: " + responseEntity.getStatusCode());
            }
            return null;

        } catch (Exception e) {
            System.out.println("=== 예외 발생: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }









    private int getCandyAmount(String productId) {
        switch (productId) {
            case "com.grsst.myhottalk.candy_30": return 30;
            case "com.grsst.myhottalk.candy_100": return 100;
            case "com.grsst.myhottalk.candy_300": return 300;
            case "com.grsst.myhottalk.candy_500": return 500;
            case "com.grsst.myhottalk.candy_1000": return 1000;
            case "com.grsst.myhottalk.candy_3000": return 3000;
            default: return 0;
        }
    }





}
