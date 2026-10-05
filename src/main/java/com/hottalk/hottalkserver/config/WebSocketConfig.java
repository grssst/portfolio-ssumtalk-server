package com.hottalk.hottalkserver.config;

import com.hottalk.hottalkserver.util.RoomIdHandshakeInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Autowired
    private RoomIdHandshakeInterceptor handshakeInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 메시지 브로커를 활성화하고 클라이언트가 구독할 수 있는 프리픽스 설정
        config.enableSimpleBroker("/topic");
        // 클라이언트가 메시지를 보낼 때 사용할 프리픽스 설정
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 웹소켓 엔드포인트 등록
        registry.addEndpoint("/ws")
                .addInterceptors(handshakeInterceptor) // HandshakeInterceptor 등록
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
