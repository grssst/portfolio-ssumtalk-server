package com.hottalk.hottalkserver.service;

import com.hottalk.hottalkserver.dto.RealtimeChatDTO;
import com.hottalk.hottalkserver.model.DeviceUuid;
import com.hottalk.hottalkserver.model.FcmToken;
import com.hottalk.hottalkserver.model.User;
import com.hottalk.hottalkserver.repository.DeviceUuidRepository;
import com.hottalk.hottalkserver.repository.FcmTokenRepository;
import com.hottalk.hottalkserver.repository.UserRepository;
import com.hottalk.hottalkserver.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class FcmService {

    private final FcmTokenRepository fcmTokenRepository;
    private final UserRepository userRepository;
    private final SqsClient sqsClient;
    private final DeviceUuidRepository deviceUuidRepository;

    @Value("${aws.sqs.url}") // SQS URL
    private String sqsUrl;

    @Value("${aws.sqs.url.admin}") // SQS URL
    private String sqsUrlAdmin;

    @Autowired
    public FcmService(SqsClient sqsClient, UserRepository userRepository, FcmTokenRepository fcmTokenRepository, DeviceUuidRepository deviceUuidRepository) {
        this.sqsClient = sqsClient;
        this.userRepository = userRepository;
        this.fcmTokenRepository = fcmTokenRepository;
        this.deviceUuidRepository = deviceUuidRepository;
    }



    public ResponseEntity<Map<String, Object>> saveFcmToken(String fcmToken, String token, String UUID) {
        Map<String, Object> response = new HashMap<>();
        try{
            // JWT 토큰에서 externalUserId 추출
            String externalUserId = JwtUtil.validateToken(token.replace("Bearer ", ""));

            // 사용자 정보 조회
            User user = getUserProfile(externalUserId);

            Optional<DeviceUuid> deviceUuid = deviceUuidRepository.findByDeviceUuid(UUID);
            if (deviceUuid.isPresent() && deviceUuid.get().getDeletedAt() != null) {
                LocalDateTime deletedAtUtc = deviceUuid.get().getDeletedAt();
                LocalDateTime restrictedUntil = deletedAtUtc.plusHours(24);  // ⏳ 탈퇴 후 24시간 제한

                System.out.println("DB에서 가져온 deleted_at (UTC 기준): " + deletedAtUtc);
                System.out.println("UUID 제한 해제 시간: " + restrictedUntil);
                System.out.println("현재 UTC 시간과 비교: " + LocalDateTime.now(ZoneOffset.UTC));

                if (restrictedUntil.isAfter(LocalDateTime.now(ZoneOffset.UTC))) {
                    System.out.println("🚨 UUID 제한 조건 충족: 로그인을 차단해야 함");
                    response.put("success", false);
                    response.put("message", "⛔ 재가입 제한: 탈퇴 후 24시간 이후 다시 시도하세요.");
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
                } else {
                    System.out.println("✅ 제한 시간이 지나서 로그인 허용됨");
                }

            } else {
                System.out.println("🚨 UUID가 DB에 없음! 제한이 안 걸림.");
            }


            // ✅ FCM 토큰 확인 및 저장
            Optional<FcmToken> existingToken = fcmTokenRepository.findByUserId(user.getId());
            if (existingToken.isPresent()) {
                // 기존 토큰이 존재하면 업데이트
                FcmToken tokenEntity = existingToken.get();
                tokenEntity.setUser(user);  // 기존 레코드의 계정을 최신 로그인 계정으로 업데이트
                tokenEntity.setIsActive(true);
                tokenEntity.setUpdatedAt(LocalDateTime.now(ZoneId.of("Asia/Seoul")));
                fcmTokenRepository.save(tokenEntity);
            } else {
                // 기존 토큰이 없으면 새로 저장
                FcmToken newToken = new FcmToken(user, fcmToken);
                fcmTokenRepository.save(newToken);
            }

            response.put("success", true);
            response.put("message", "✅ FCM 토큰 저장 완료");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "❌ 서버 오류: FCM 토큰 저장 실패");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }



    public User getUserProfile(String externalUserId) {
        // 외부 사용자 ID로 사용자 정보를 조회
        Optional<User> userOptional = userRepository.findByExternalUserId(externalUserId);

        // 사용자 정보를 찾으면 반환, 그렇지 않으면 HTTP 에러 코드 404 반환
        return userOptional.orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        );
    }

    public void sendToSqs(RealtimeChatDTO message) {
        try {
            // 메시지 본문 구성
            String sqsMessage = String.format(
                    "{\"myNickname\":\"%s\", \"recipientId\":\"%s\", \"content\":\"%s\"}",
                    message.getMyNickname(), message.getRecipientId(), message.getMessageContent()
            );

            // SQS 메시지 전송
            SendMessageRequest request = SendMessageRequest.builder()
                    .queueUrl(sqsUrl)
                    .messageBody(sqsMessage)
                    .build();
            sqsClient.sendMessage(request);

            System.out.println("SQS 일반 채팅방 메시지 전송 완료: " + sqsMessage);
        } catch (Exception e) {
            System.err.println("SQS 메시지 전송 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void sendToSqsAdmin(RealtimeChatDTO message) {
        try {
            // 메시지 본문 구성
            String sqsMessage = String.format(
                    "{\"myNickname\":\"%s\", \"recipientId\":\"%s\", \"content\":\"%s\"}",
                    message.getMyNickname(), message.getRecipientId(), message.getMessageContent()
            );
            System.out.println(sqsUrlAdmin);
            // SQS 메시지 전송
            SendMessageRequest request = SendMessageRequest.builder()
                    .queueUrl(sqsUrlAdmin)
                    .messageBody(sqsMessage)
                    .build();
            sqsClient.sendMessage(request);

            System.out.println("SQS 어드민 메시지 전송 완료: " + sqsMessage);
        } catch (Exception e) {
            System.err.println("SQS 메시지 전송 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void sendToSqsMeeting(RealtimeChatDTO message) {
        try {
            // 🔹 이제 recipientId 자체가 "meeting_{meetingId}" / "room_{...}" 형식이라고 가정
            String topic = message.getRecipientId();

            System.out.println(message);

            String sqsMessage = String.format(
                    "{\"myNickname\":\"%s\", \"recipientId\":\"%s\", \"content\":\"%s\"}",
                    message.getMyNickname(), topic, message.getMessageContent()
            );
            System.out.println(sqsMessage);

            SendMessageRequest request = SendMessageRequest.builder()
                    .queueUrl(sqsUrl)
                    .messageBody(sqsMessage)
                    .build();
            sqsClient.sendMessage(request);

            System.out.println("SQS 미팅 채팅방 메시지 전송 완료: " + sqsMessage);
        } catch (Exception e) {
            System.err.println("SQS 메시지 전송 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }



}
