package com.hottalk.hottalkserver.controller;

import com.hottalk.hottalkserver.dto.FcmTokenRequestDTO;
import com.hottalk.hottalkserver.model.User;
import com.hottalk.hottalkserver.service.FcmService;
import com.hottalk.hottalkserver.service.UserService;
import com.hottalk.hottalkserver.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/fcm")
public class FcmController {

    @Autowired
    private FcmService fcmService;

    // FCM 토큰 저장 API
    @PostMapping("/save-fcm-token")
    public ResponseEntity<Map<String, Object>> saveFcmToken(
            @RequestHeader("Authorization") String token,
            @RequestBody FcmTokenRequestDTO request) {
        System.out.println("요청 도착");
        return fcmService.saveFcmToken(request.getFcmToken(), token, request.getUUID());
    }
}
