package com.hottalk.hottalkserver.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RealtimeChatDTO {
    private String messageContent;
    private long timestamp;
    private String senderId;
    private String recipientId;
    private String roomId;
    private String myNickname;
    // New field for effective room id (e.g., "meeting_23")
    private String effectiveRoomId;
    private String senderNickname; // 추가
    private String senderProfileImageUrl; // 추가
    private String type; // "text" 또는 "image" 등 메시지 유형
    private String formattedTime;

    public RealtimeChatDTO(String messageContent, long timestamp, String senderId, String recipientId, String roomId, String myNickname, String effectiveRoomId) {
        this.messageContent = messageContent;
        this.timestamp = timestamp;
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.roomId = roomId;
        this.myNickname = myNickname;
        this.effectiveRoomId = effectiveRoomId;
    }

    public RealtimeChatDTO() {
    }

    // Getters and Setters

    public String getMessageContent() {
        return messageContent;
    }
    public void setMessageContent(String messageContent) {
        this.messageContent = messageContent;
    }
    public long getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    public String getRecipientId() {
        return recipientId;
    }
    public void setRecipientId(String recipientId) {
        this.recipientId = recipientId;
    }
    public String getSenderId() {
        return senderId;
    }
    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }
    public String getRoomId() {
        return roomId;
    }
    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }
    public String getMyNickname() {
        return myNickname;
    }
    public void setMyNickname(String myNickname) {
        this.myNickname = myNickname;
    }
    public String getEffectiveRoomId() {
        return effectiveRoomId;
    }
    public void setEffectiveRoomId(String effectiveRoomId) {
        this.effectiveRoomId = effectiveRoomId;
    }
}
