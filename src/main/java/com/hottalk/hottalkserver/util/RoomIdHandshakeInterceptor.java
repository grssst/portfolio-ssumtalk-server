package com.hottalk.hottalkserver.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;

import java.util.Map;

@Component
public class RoomIdHandshakeInterceptor implements HandshakeInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(RoomIdHandshakeInterceptor.class);

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        // WebSocket 요청의 쿼리 파라미터에서 roomId와 myUserId 추출
        String roomId = request.getURI().getQuery() != null && request.getURI().getQuery().contains("roomId")
                ? request.getURI().getQuery().split("roomId=")[1].split("&")[0]
                : null;

        String myUserId = request.getURI().getQuery() != null && request.getURI().getQuery().contains("userId")
                ? request.getURI().getQuery().split("userId=")[1].split("&")[0]
                : null;

        // SessionAttributes에 저장
        if (roomId != null && !roomId.isEmpty()) {
            attributes.put("roomId", roomId);
        }
        if (myUserId != null && !myUserId.isEmpty()) {
            attributes.put("myUserId", myUserId);
        }

        logger.info("Handshake - roomId: {}, myUserId: {}", roomId, myUserId);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception ex) {
        // 필요 시 추가 작업
    }
}
