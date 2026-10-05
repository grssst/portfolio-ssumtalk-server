package com.hottalk.hottalkserver.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hottalk.hottalkserver.dto.ChatRoomDTO;
import com.hottalk.hottalkserver.dto.ChattingDTO;
import com.hottalk.hottalkserver.dto.SendMessageDTO;
import com.hottalk.hottalkserver.model.User;
import com.hottalk.hottalkserver.model.UserChatRooms;
import com.hottalk.hottalkserver.repository.UserChatRoomsRepository;
import com.hottalk.hottalkserver.repository.UserRepository;
import com.hottalk.hottalkserver.service.BlockedUserService;
import com.hottalk.hottalkserver.service.UserChatRoomsService;
import com.hottalk.hottalkserver.service.UserService;
import com.hottalk.hottalkserver.util.JwtUtil;
import com.hottalk.hottalkserver.util.S3Uploader;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@RestController
@RequestMapping("/api/userChatRooms")
public class UserChatRoomsController {

    @Autowired
    private UserChatRoomsService userChatRoomsService;
    @Autowired
    private BlockedUserService blockedUserService;
    @Autowired
    private S3Uploader s3Uploader;
    @Autowired
    private UserService userService;
    @Autowired
    private UserChatRoomsRepository userChatRoomsRepository;
    @Autowired
    private UserRepository userRepository;
    @PostMapping("/first-send-message")
    public ResponseEntity<?> firstSendMessage(
            @RequestHeader("Authorization") String token,
            @RequestBody SendMessageDTO sendMessageDTO  ) {
        try {
            String externalUserId = JwtUtil.validateToken(token.replace("Bearer ", ""));
            User user = userService.getUserProfile(externalUserId);
            Long myId = user.getId();
            Integer candy = user.getCandy(); // candy가 int 타입이라고 가정

            long recipientId = sendMessageDTO.getRecipientId();

            if(userRepository.existsById(recipientId)){
                System.out.println("존재하는 상대");
            } else{
                return ResponseEntity.badRequest().body("탈퇴한 사용자입니다.");
            }

            //차단 여부를 먼저 확인
            if (blockedUserService.isBlocked(myId, recipientId)) {
                return ResponseEntity.badRequest().body("쪽지를 보낼 수 없는 상대입니다.");
            }

            if (candy < 3) {
                return ResponseEntity.badRequest().body("캔디가 부족합니다.");
            }


            userService.decreaseUserCandy(myId, 3);




            // 채팅방 개설 로직
            userChatRoomsService.createChatRooms(sendMessageDTO, token);
            return ResponseEntity.ok("채팅방이 개설 되었습니다.");



        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("채팅방 개설 중 오류가 발생했습니다.");
        }
    }

    @PostMapping("/first-send-message-meeting")
    public ResponseEntity<?> firstSendMessageMeeting(
            @RequestHeader("Authorization") String token,
            @RequestBody SendMessageDTO sendMessageDTO  ) {
        try {
            System.out.println("작동함3");
            String externalUserId = JwtUtil.validateToken(token.replace("Bearer ", ""));
            User user = userService.getUserProfile(externalUserId);
            Long myId = user.getId();
            Integer candy = user.getCandy(); // candy가 int 타입이라고 가정

            long recipientId = sendMessageDTO.getRecipientId();

            //차단 여부를 먼저 확인
            if (blockedUserService.isBlocked(myId, recipientId)) {
                return ResponseEntity.badRequest().body("차단된 상대입니다.");
            }

            if (candy < 3) {
                return ResponseEntity.badRequest().body("캔디가 부족합니다.");
            }


            userService.decreaseUserCandy(myId, 3);




            // 채팅방 개설 로직
            userChatRoomsService.createChatRoomsMeeting(sendMessageDTO, token);
            return ResponseEntity.ok("채팅방이 개설 되었습니다.");



        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("채팅방 개설 중 오류가 발생했습니다.");
        }
    }

    @PostMapping("/first-send-message-meeting-create")
    public ResponseEntity<?> firstSendMessageMeetingCreate(
            @RequestHeader("Authorization") String token,
            @RequestBody SendMessageDTO sendMessageDTO  ) {
        try {
            System.out.println("작동함1");

            // 채팅방 개설 로직
            userChatRoomsService.createChatRoomsMeeting(sendMessageDTO, token);
            return ResponseEntity.ok("채팅방이 개설 되었습니다.");



        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("채팅방 개설 중 오류가 발생했습니다.");
        }
    }

    @GetMapping("/fetchChatRooms")
    public ResponseEntity<?> fetchChatRooms(@RequestHeader("Authorization") String token) {
        try {
            List<ChatRoomDTO> chatRooms = userChatRoomsService.getChatRoomsForUser(token);

            // 데이터를 JSON으로 출력 (가독성 향상)
            //ObjectMapper mapper = new ObjectMapper();
            //String chatRoomsJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(chatRooms);
            //System.out.println(chatRoomsJson);
            System.out.println("전송함");
            return ResponseEntity.ok(chatRooms);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("채팅방 데이터를 가져오는 중 오류가 발생했습니다.");
        }
    }

    @GetMapping("/fetchChatting")
    public ResponseEntity<?> fetchChatting(@RequestHeader("Authorization") String token, @RequestParam String roomId, @RequestParam(required = false) String lastTimestamp, @RequestParam String isRefresh) {
        try {
            // isRefresh 값을 boolean으로 변환하여 처리
            boolean refresh = Objects.equals(isRefresh, "true");

            List<ChattingDTO> chatting = userChatRoomsService.getChatting(token, roomId, lastTimestamp, refresh);


            System.out.println("해당 채팅방 정보: " + roomId);

            // 채팅과 함께 lastTimestamp를 반환
            //String nextTimestamp =                      //반환된 nextTimestamp를 여기에 매핑
            //System.out.println("라스트타임스탬프:" + lastTimestamp);

            Map<String, Object> response = new HashMap<>();
            response.put("chatting", chatting);

            return ResponseEntity.ok(response); // chatting과 lastTimestamp를 함께 반환
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("채팅 데이터를 가져오는 중 오류가 발생했습니다.");
        }
    }

    @GetMapping("/fetchChattingMeeting")
    public ResponseEntity<?> fetchChattingMeeting(
            @RequestHeader("Authorization") String token,
            @RequestParam String roomId,
            @RequestParam(required = false) String lastTimestamp,
            @RequestParam String isRefresh) {
        try {
            boolean refresh = Boolean.parseBoolean(isRefresh);

            Map<String, Object> response = userChatRoomsService.getMeetingChatting(token, roomId, lastTimestamp, refresh);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("채팅 데이터를 가져오는 중 오류가 발생했습니다.");
        }
    }


    @GetMapping("/saveDisconnectTime")
    public ResponseEntity<?> saveDisconnectTime(@RequestHeader("Authorization") String token, @RequestParam String roomId) {
        try {
            String externalUserId = JwtUtil.validateToken(token.replace("Bearer ", ""));
            Long myId = userService.getUserProfile(externalUserId).getId();
            LocalDateTime disconnectAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));


            // 연결 해제 시간을 데이터베이스에 업데이트
            userChatRoomsService.updateLastDisconnectAt(myId, roomId, disconnectAt);

            return ResponseEntity.ok("접속 종료 시간 기록 됨");
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return ResponseEntity.status(500).body("채팅방 데이터를 가져오는 중 오류가 발생했습니다.");
        }
    }


    @GetMapping("/removeChatRoom")
    public ResponseEntity<?> removeChatRoom(@RequestHeader("Authorization") String token, @RequestParam String roomId) {
        try {


            // roomId로 채팅방이랑 채팅내역 삭제함, 채팅 내역 기록함
            userChatRoomsService.removeChatRoomAndDynamoRecord(roomId);

            return ResponseEntity.ok("채팅방 종료됨");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(404).body("존재하지 않는 채팅방입니다.");
        }
    }

    @GetMapping("/removeMeetingRoom")
    public ResponseEntity<?> removeMeetingRoom(@RequestHeader("Authorization") String token, @RequestParam String roomId) {
        try {


            // roomId로 채팅방이랑 채팅내역 삭제함, 채팅 내역 기록함
            userChatRoomsService.removeMeetingRoomAndDynamoRecord(roomId);

            return ResponseEntity.ok("채팅방 종료됨");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(404).body("존재하지 않는 채팅방입니다.");
        }
    }


}

