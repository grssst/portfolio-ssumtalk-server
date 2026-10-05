package com.hottalk.hottalkserver.dto;

import lombok.Getter;
import lombok.Setter;
import software.amazon.awssdk.services.sqs.endpoints.internal.Value;

@Getter
@Setter
public class ChattingDTO {
    private String senderId;
    private String recipientId;
    private String messageContent;
    private long timestamp;
    private long myId;
    private String nextTimestamp;
    private String myNickname;
    private String type;

    // 추가 필드 (미팅방에서 각자 다른 프로필과 닉네임 지원)
    private String senderNickname;
    private String senderProfileImageUrl;
    private Long meetingOrganizerId;
    // 생성자
    public ChattingDTO(String senderId, String recipientId, String messageContent, long timestamp, long myId, String nextTimestamp, String myNickname, String type) {
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.messageContent = messageContent;
        this.timestamp = timestamp;
        this.myId = myId;
        this.nextTimestamp = nextTimestamp;
        this.myNickname = myNickname;
        this.type = type;
    }

    public ChattingDTO() {
    }

    // Constructor
    public ChattingDTO(String senderId, String recipientId, String messageContent, long timestamp, Long myId, String nextTimestamp, String myNickname, String senderNickname, String senderProfileImageUrl,
                       Long meetingOrganizerId, String type) {
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.messageContent = messageContent;
        this.timestamp = timestamp;
        this.myId = myId;
        this.nextTimestamp = nextTimestamp;
        this.myNickname = myNickname;
        this.senderNickname = senderNickname;
        this.senderProfileImageUrl = senderProfileImageUrl;
        this.meetingOrganizerId = meetingOrganizerId;
        this.type = type;
    }

    // Getter와 Setter 추가 (필요한 경우 Lombok으로 대체 가능)
    public long getMyId() {
        return myId;
    }

    public void setMyId(long myId) {
        this.myId = myId;
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

    public String getNextTimestamp() {
        return nextTimestamp;
    }
    public void setNextTimestamp(String nextTimestamp) {
        this.nextTimestamp = nextTimestamp;
    }
    public String getRecipientId() {
        return recipientId;
    }
    public void setRecipientId(String recipientId) {
        this.recipientId = recipientId;
    }
    public String getMessageContent() {
        return messageContent;
    }
    public void setMessageContent(String messageContent) {
        this.messageContent = messageContent;
    }
    public String getMyNickname() {
        return myNickname;
    }
    public void setMyNickname(String myNickname) {
        this.myNickname = myNickname;
    }
}

