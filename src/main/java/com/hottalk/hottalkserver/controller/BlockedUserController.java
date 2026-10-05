package com.hottalk.hottalkserver.controller;

import com.hottalk.hottalkserver.dto.BlockedUserDTO;
import com.hottalk.hottalkserver.model.BlockedUser;
import com.hottalk.hottalkserver.model.User;
import com.hottalk.hottalkserver.repository.BlockedUserRepository;
import com.hottalk.hottalkserver.repository.UserRepository;
import com.hottalk.hottalkserver.service.BlockedUserService;
import com.hottalk.hottalkserver.service.UserService;
import com.hottalk.hottalkserver.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/block")
public class BlockedUserController {

    @Autowired
    private BlockedUserService blockedUserService;
    @Autowired
    private UserService userService;
    @Autowired
    private BlockedUserRepository blockedUserRepository;
    @Autowired
    private UserRepository userRepository;

    @PostMapping("/block-user")
    public ResponseEntity<?> blockUser(@RequestBody Map<String, String> requestData, @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);  // "Bearer " 부분 제거
        String blockerExternalUserId = JwtUtil.validateToken(token);  // JWT에서 externalUserId 추출

        String blockedExternalUserId = requestData.get("externalUserId");

        // 차단 등록
        blockedUserService.blockUser(blockerExternalUserId, blockedExternalUserId);

        return ResponseEntity.ok(Map.of("message", "사용자가 성공적으로 차단되었습니다."));
    }

    @PostMapping("/block-user-chat")
    public ResponseEntity<?> blockUserChat(@RequestBody Map<String, String> requestData, @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);  // "Bearer " 부분 제거
        String blockerExternalUserId = JwtUtil.validateToken(token);  // JWT에서 externalUserId 추출
        //System.out.println("차단시도");
        String blockedUserId = requestData.get("blockedUserId");

        // 차단 등록
        blockedUserService.blockUserChat(blockerExternalUserId, blockedUserId);

        return ResponseEntity.ok(Map.of("message", "사용자가 성공적으로 차단되었습니다."));
    }

    @PostMapping("/unblock-user")
    public ResponseEntity<?> unblockUser(@RequestBody Map<String, String> requestData, @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);  // "Bearer " 부분 제거
            String blockerExternalUserId = JwtUtil.validateToken(token);  // JWT에서 externalUserId 추출

            String blockedExternalUserId = requestData.get("externalUserId");

            // 로그 추가
            System.out.println("Unblock request received for blockedExternalUserId: " + blockedExternalUserId);

            if (blockedExternalUserId == null || blockedExternalUserId.isEmpty()) {
                return ResponseEntity.status(400).body("Invalid request: blockedExternalUserId is missing.");
            }

            // 차단 해제
            blockedUserService.unblockUser(blockerExternalUserId, blockedExternalUserId);

            return ResponseEntity.ok(Map.of("message", "사용자의 차단이 성공적으로 해제되었습니다."));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("차단 해제 중 오류가 발생했습니다.");
        }
    }

    @Transactional
    @GetMapping("/me")
    public ResponseEntity<?> getUserProfile(@RequestHeader("Authorization") String authHeader) {
        try {
            String jwtToken = authHeader.replace("Bearer ", "");
            // JWT 토큰을 검증하고 externalUserId 추출
            String externalUserId = JwtUtil.validateToken(jwtToken);

            User user = userService.getUserProfile(externalUserId);
            if (user == null) {
                return ResponseEntity.status(404).body("User not found.");
            }

            List<User> blockedUsersDetails = blockedUserService.getBlockedUsersDetails(user.getExternalUserId());

            // Convert to DTO
            List<BlockedUserDTO> blockedUsersInfo = blockedUsersDetails.stream()
                    .map(u -> {
                        BlockedUserDTO dto = new BlockedUserDTO();
                        dto.setNickname(u.getNickname());
                        dto.setAge(u.getAge());
                        dto.setGender(u.getGender());
                        dto.setExternalUserId(u.getExternalUserId());
                        return dto;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(blockedUsersInfo);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error fetching user profile.");
        }
    }



}