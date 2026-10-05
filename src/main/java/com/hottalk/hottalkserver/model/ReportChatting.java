package com.hottalk.hottalkserver.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "report_chatting")
public class ReportChatting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false)
    private String roomId;

    @Column(name = "sender_id", nullable = false)
    private String senderId;

    @Column(name = "recipient_id", nullable = false)
    private String recipientId;

    @Column(name = "is_read", nullable = false)
    private boolean isRead;

    @Column(name = "message_content", nullable = false)
    private String messageContent;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "delete_at")
    private LocalDateTime deleteAt;
    @Column(name = "reporter_user_id") // 🚀 신고한 사람 ID 추가
    private Long reporterUserId;


    // 기본 생성자 (JPA 요구 사항)
    public ReportChatting() {
    }

    // 모든 필드를 사용하는 생성자
    public ReportChatting(String roomId, String senderId, String recipientId, boolean isRead, String messageContent, LocalDateTime timestamp) {
        this.roomId = roomId;
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.isRead = isRead;
        this.messageContent = messageContent;
        this.timestamp = timestamp;
    }

    // Getter and Setter
    public LocalDateTime getDeleteAt() { return deleteAt; }
    public void setDeleteAt(LocalDateTime deleteAt) { this.deleteAt = deleteAt; }
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }


    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }
    public Long getReporterUserId() {
        return reporterUserId;
    }

    public void setReporterUserId(Long reporterUserId) {
        this.reporterUserId = reporterUserId;
    }

    public String getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(String recipientId) {
        this.recipientId = recipientId;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean isRead) {
        this.isRead = isRead;
    }

    public String getMessageContent() {
        return messageContent;
    }

    public void setMessageContent(String messageContent) {
        this.messageContent = messageContent;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
