package com.hottalk.hottalkserver.repository;

import com.hottalk.hottalkserver.model.ReportChatting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportChattingRepository extends JpaRepository<ReportChatting, Long> {
    // 특정 채팅방(roomId)에서 모든 메시지 조회
    List<ReportChatting> findByRoomId(String roomId);

    // 특정 채팅방과 발신자(senderId)로 메시지 조회
    List<ReportChatting> findByRoomIdAndSenderId(String roomId, String senderId);

    // 특정 채팅방에서 읽지 않은 메시지 조회
    List<ReportChatting> findByRoomIdAndIsReadFalse(String roomId);

    // 특정 수신자(recipientId)의 메시지 조회
    List<ReportChatting> findByRecipientId(String recipientId);
}
