package com.hottalk.hottalkserver.controller;

import com.hottalk.hottalkserver.model.Sanction;
import com.hottalk.hottalkserver.service.SanctionService;
import com.hottalk.hottalkserver.service.UserService;
import com.hottalk.hottalkserver.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/sanction")
public class SanctionController {

    @Autowired
    private SanctionService sanctionService;
    @Autowired
    private UserService userService;



    // 사용자의 제재 상태를 확인하는 API
    @GetMapping("/check-sanction")
    public ResponseEntity<?> checkSanctionStatus(HttpServletRequest request) {
        String token = request.getHeader("Authorization").replace("Bearer ", "");
        String externalUserId = JwtUtil.validateToken(token);  // 토큰에서 externalUserId를 추출
        Long userId = userService.findUserIdByExternalUserId(externalUserId);  // externalUserId로 userId 조회

        // userId로 사용자의 최신 제재 상태를 확인
        Optional<Sanction> sanctionOpt = sanctionService.getRecentSanction(userId);

        if (sanctionOpt.isPresent()) {
            Sanction sanction = sanctionOpt.get();
            // 최신 제재 데이터가 유효한지 확인
            if (sanction.getEndTime() == null || sanction.getEndTime().isAfter(LocalDateTime.now(ZoneId.of("Asia/Seoul")))) {
                return ResponseEntity.ok(Map.of(
                        "isSanctioned", true,
                        "reason", sanction.getReason(),
                        "endTime", sanction.getEndTime()
                ));
            } else {
                // 제재 기간이 끝난 경우 제재 상태를 false로 반환
                return ResponseEntity.ok(Map.of("isSanctioned", false));
            }
        } else {
            return ResponseEntity.ok(Map.of("isSanctioned", false));
        }
    }

}
