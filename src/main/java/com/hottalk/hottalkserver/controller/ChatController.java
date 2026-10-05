package com.hottalk.hottalkserver.controller;


import com.amazonaws.services.s3.internal.S3AbortableInputStream;
import com.hottalk.hottalkserver.dto.RealtimeChatDTO;
import com.hottalk.hottalkserver.model.MeetingParticipant;
import com.hottalk.hottalkserver.model.User;
import com.hottalk.hottalkserver.repository.MeetingParticipantsRepository;
import com.hottalk.hottalkserver.repository.MeetingRepository;
import com.hottalk.hottalkserver.repository.UserRepository;
import com.hottalk.hottalkserver.service.AdminService;
import com.hottalk.hottalkserver.service.ChatService;
import com.hottalk.hottalkserver.service.FcmService;
import com.hottalk.hottalkserver.util.S3Uploader;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.catalina.manager.StatusTransformer.formatTime;

@Controller
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;
    @Autowired
    private FcmService fcmService;
    @Autowired
    private S3Uploader s3Uploader;
    @Autowired
    private SimpMessagingTemplate messagingTemplate; // 필드로 주입
    @Autowired
    private UserRepository userRepository;
    @Autowired
    MeetingRepository meetingRepository;
    @Autowired
    MeetingParticipantsRepository meetingParticipantsRepository;

    @PostMapping(value = "/uploadImage", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("roomId") String roomId,
            @RequestParam("senderId") String senderId,
            @RequestParam("recipientId") String recipientId,
            @RequestParam(value = "myNickname", required = false) String myNickname,
            @RequestParam(value = "senderProfileImageUrl", required = false) String senderProfileImageUrl
            // 필요 시 추가 파라미터 (예: senderNickname, senderProfileImageUrl)도 함께 받을 수 있음
    ) {
        try {
            // S3Uploader를 이용하여 채팅 이미지 업로드 (예: roomId별 폴더)
            String imageUrl = s3Uploader.uploadChattingImage(file, roomId);

            // RealtimeChatDTO 객체 생성: 채팅 내역처럼 저장할 이미지 메시지 구성
            RealtimeChatDTO message = new RealtimeChatDTO();
            message.setRoomId(roomId);
            message.setSenderId(senderId);
            message.setRecipientId(recipientId);
            message.setMessageContent(imageUrl); // 업로드된 이미지 URL
            message.setType("image"); // 이미지 타입 메시지로 설정
            message.setTimestamp(System.currentTimeMillis());
            // ✅ 닉네임이 없으면 기본 문구 적용
            if (myNickname == null || myNickname.isBlank()) {
                message.setMyNickname("새 메시지가 도착하였습니다.");
            } else {
                message.setMyNickname(myNickname);
            }
            // 필요 시 추가 정보 (senderNickname, senderProfileImageUrl 등)도 설정
            // sender의 닉네임, 프로필 이미지 추가
            //User senderUser = userRepository.findById(Long.valueOf(message.getSenderId()))
            //        .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

            message.setSenderNickname(myNickname);
            message.setSenderProfileImageUrl(senderProfileImageUrl);
            // 모임 채팅일 경우 effectiveRoomId 계산하여 설정
            if (roomId.startsWith("meeting_")) {
                String[] parts = roomId.split("_");
                if (parts.length >= 2) {
                    String effectiveRoomId = parts[0] + "_" + parts[1]; // 예: "meeting_23"
                    message.setEffectiveRoomId(effectiveRoomId);
                }
            }

            // DynamoDB에 메시지 저장
            chatService.saveMessage(message);
            System.out.println(message.getType());
            System.out.println("저장된 이미지 메시지 roomId: " + roomId);

            // WebSocket 브로드캐스팅
            String destination;
            if (roomId.startsWith("meeting_")) {
                destination = "/topic/chatroom/" + message.getEffectiveRoomId();
            } else {
                destination = "/topic/chatroom/" + roomId;
            }
            messagingTemplate.convertAndSend(destination, message);

            if (roomId.startsWith("meeting_")) {
                String[] parts = roomId.split("_"); // meeting_150_2624
                if (parts.length >= 2) {
                    String meetingId = parts[1];                     // "150"
                    String effectiveRoomId = "meeting_" + meetingId; // "meeting_150"
                    message.setRecipientId(effectiveRoomId);
                } else {
                    // 일반 1:1 채팅이면 클라이언트에서 준 recipientId 그대로 사용
                    message.setRecipientId(recipientId);
                }
                fcmService.sendToSqsMeeting(message);
            } else {
                fcmService.sendToSqs(message);
            }



            // 결과로 업로드된 imageUrl을 클라이언트에 반환 (원하면 메시지 전체 데이터를 반환해도 됨)
            Map<String, String> response = new HashMap<>();
            response.put("imageUrl", imageUrl);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Collections.singletonMap("error", "이미지 업로드 실패"));
        }
    }

    @MessageMapping("/postMessage")
    public void realtimeChat(@Payload RealtimeChatDTO message) throws Exception {
        try {
            System.out.println("메시지: " + message.getMessageContent());

            // 모임 채팅인 경우 effectiveRoomId 계산 후 메시지 객체에 설정
            if (message.getRoomId().startsWith("meeting_")) {
                String[] parts = message.getRoomId().split("_");
                if (parts.length >= 2) {
                    String effectiveRoomId = parts[0] + "_" + parts[1]; // 예: "meeting_23"
                    message.setEffectiveRoomId(effectiveRoomId);  // RealtimeChatDTO에 해당 필드가 있어야 함
                }
            }

            // DynamoDB에 메시지 저장 (추가 필드도 포함해서 저장)
            chatService.saveMessage(message);
            System.out.println("roomId:" + message.getRoomId());

            // 브로드캐스트를 위해 effectiveRoomId 사용
            String destination;
            if (message.getRoomId().startsWith("meeting_")) {
                destination = "/topic/chatroom/" + message.getEffectiveRoomId();
            } else {
                destination = "/topic/chatroom/" + message.getRoomId();
            }
            messagingTemplate.convertAndSend(destination, message);

            // 모임 채팅이 아닌 경우에만 SQS에 푸시 알림 요청 전송
            if (!message.getRoomId().startsWith("meeting")) {
                fcmService.sendToSqs(message);
            }
        } catch (Exception e) {
            System.err.println("DynamoDB 저장 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @MessageMapping("/postMessage/meeting")
    public void realtimeChatMeeting(@Payload RealtimeChatDTO message) throws Exception {
        try {
            System.out.println("🔹 메시지 수신: " + message.getMessageContent());

            if (message.getRoomId().startsWith("meeting_")) {
                String[] parts = message.getRoomId().split("_");
                if (parts.length >= 2) {
                    String effectiveRoomId = parts[0] + "_" + parts[1]; // "meeting_XX"
                    message.setEffectiveRoomId(effectiveRoomId);
                    System.out.println("✅ 설정된 effectiveRoomId: " + effectiveRoomId);
                }
            }

            // DynamoDB에 메시지 저장
            chatService.saveMessage(message);
            System.out.println("roomId: " + message.getRoomId());

            // sender의 닉네임, 프로필 이미지 추가
            //User senderUser = userRepository.findById(Long.valueOf(message.getSenderId()))
            //        .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

            //message.setSenderNickname(senderUser.getNickname());
            //message.setSenderProfileImageUrl(senderUser.getProfileImageUrl());

            // ✅ WebSocket 메시지 전송 (meeting 채팅방 처리 확인)
            String destination = message.getRoomId().startsWith("meeting_") ?
                    "/topic/chatroom/" + message.getEffectiveRoomId() :
                    "/topic/chatroom/" + message.getRoomId();

            System.out.println("🛜 WebSocket 전송 경로: " + destination);
            messagingTemplate.convertAndSend(destination, message);


            if (message.getRoomId().startsWith("meeting_")) {
                String[] parts   = message.getRoomId().split("_");
                Long   meetingId = Long.valueOf(parts[1]);

                // room_?? == "meeting_{meetingId}" 를 그대로 푸시 토픽으로 사용
                RealtimeChatDTO pushDto = new RealtimeChatDTO();
                pushDto.setMyNickname(message.getMyNickname());
                pushDto.setMessageContent(message.getMessageContent());
                pushDto.setRecipientId(String.format("meeting_%d", meetingId)); // == roomId

                fcmService.sendToSqsMeeting(pushDto);
                System.out.println("SQS 알림 전송 to " + pushDto.getRecipientId());
            } else {
                // 1:1 채팅은 기존 로직 유지 (user 토큰 기반이면 별도 메서드 써도 됨)
                fcmService.sendToSqsMeeting(message);
            }


        } catch (Exception e) {
            System.err.println("❌ 메시지 처리 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }


    // 예시: ChatService에 추가









}