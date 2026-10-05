package com.hottalk.hottalkserver.dto;

import lombok.Getter;
import lombok.Setter;


public class SendMessageDTO {
    private Long recipientId;
    private String messageContent;

    private String roomId;
    private String myNickname;
    private long timestamp;
    private String senderId;

    // Getters and Setters
    public Long getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(Long recipientId) {
        this.recipientId = recipientId;
    }

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
}
