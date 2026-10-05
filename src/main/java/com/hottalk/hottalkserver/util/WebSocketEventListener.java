package com.hottalk.hottalkserver.util;

import com.hottalk.hottalkserver.model.UserChatRooms;
import com.hottalk.hottalkserver.repository.UserChatRoomsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
public class WebSocketEventListener {
    @Autowired
    private UserChatRoomsRepository userChatRoomsRepository;
    private static final Logger logger = LoggerFactory.getLogger(WebSocketEventListener.class);

    // WebSocket 연결 시 이벤트 처리
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        String userId = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : "Unknown";
        String sessionId = headerAccessor.getSessionId();

        logger.info("유저 연결: 사용자 ID={}, 세션 ID={}", userId, sessionId);

        // 연결 시간을 기록하는 로직 추가
        // 예: 데이터베이스에 저장
        // connectionService.recordConnectionStart(userId, sessionId, LocalDateTime.now(ZoneId.of("Asia/Seoul")));
    }

    // WebSocket 연결 해제 시 이벤트 처리
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        // SessionAttributes에서 userId와 roomId 추출
        String userIdStr = headerAccessor.getSessionAttributes() != null && headerAccessor.getSessionAttributes().get("myUserId") != null
                ? headerAccessor.getSessionAttributes().get("myUserId").toString()
                : null;
        String roomId = headerAccessor.getSessionAttributes() != null && headerAccessor.getSessionAttributes().get("roomId") != null
                ? headerAccessor.getSessionAttributes().get("roomId").toString()
                : null;

        if (userIdStr != null && roomId != null) {
            Long userId = Long.parseLong(userIdStr);
            LocalDateTime disconnectAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));

            logger.info("유저 연결 해제: 사용자 ID={}, 채팅방 ID={}, 해제 시간={}", userId, roomId, disconnectAt);


        } else {
            logger.warn("연결 해제 시 사용자 ID 또는 채팅방 ID가 누락되었습니다.");
        }
    }

    @Transactional
    public void updateLastDisconnectAt(Long userId, String roomId, LocalDateTime disconnectAt) {
        UserChatRooms userChatRoom = userChatRoomsRepository.findByUserIdAndRoomId(userId, roomId)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자와 채팅방의 레코드를 찾을 수 없습니다."));

        userChatRoom.setLastDisconnectAt(disconnectAt);
        userChatRoomsRepository.save(userChatRoom);
    }

    
}

