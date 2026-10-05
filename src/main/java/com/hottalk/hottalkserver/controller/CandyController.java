package com.hottalk.hottalkserver.controller;


import com.hottalk.hottalkserver.dto.PurchaseVerifyRequestAndroidDTO;
import com.hottalk.hottalkserver.dto.PurchaseVerifyResponseAndroidDTO;
import com.hottalk.hottalkserver.dto.PurchaseVerifyResponseIosDTO;
import com.hottalk.hottalkserver.model.User;
import com.hottalk.hottalkserver.service.CandyService;
import com.hottalk.hottalkserver.service.UserService;
import com.hottalk.hottalkserver.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/candy")
public class CandyController {

    private final CandyService candyService;
    private final UserService userService;
    @Autowired
    public CandyController(CandyService candyService, UserService userService) {
        this.candyService = candyService;
        this.userService = userService;
    }

    @PostMapping("/purchase/verify/android")
    public ResponseEntity<PurchaseVerifyResponseAndroidDTO> verifyPurchaseAndroid(@RequestHeader("Authorization") String token, @RequestBody PurchaseVerifyRequestAndroidDTO request) {
        // "Bearer " 부분을 제거하고 토큰만 추출
        String jwtToken = token.replace("Bearer ", "");
        System.out.println(request.getOrderId());
        // JWT 토큰을 검증하고 externalUserId 추출
        String externalUserId = JwtUtil.validateToken(jwtToken);
        // externalUserId로 사용자 조회
        User user = userService.getUserProfile(externalUserId);
        request.setProvider(user.getProvider());
        request.setUserId(user.getId());
        request.setExternalUserId(externalUserId);
        // request에는 orderId, purchaseToken, productId, userId 등이 담겨 있다고 가정
        // 여기서 서버는 영수증 검증을 수행하고 DB에 기록한 뒤, user의 updated candy를 리턴
        PurchaseVerifyResponseAndroidDTO response = candyService.verifyAndCompletePurchaseAndroid(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/purchase/verify/ios")
    public ResponseEntity<PurchaseVerifyResponseIosDTO> verifyPurchaseIos(@RequestHeader("Authorization") String token, @RequestBody String receipt) {
        System.out.println("=== [iOS] /purchase/verify/ios 호출됨 ===");
        System.out.println("전달받은 receipt(body): " + receipt);

        // CandyService에서 영수증 검증 및 결제처리를 진행
        PurchaseVerifyResponseIosDTO response = candyService.verifyAndCompletePurchaseIos(token, receipt);

        System.out.println("=== [iOS] /purchase/verify/ios 응답 완료 ===");
        return ResponseEntity.ok(response);
    }



}