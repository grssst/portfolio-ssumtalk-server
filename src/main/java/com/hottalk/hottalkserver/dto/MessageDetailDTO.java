package com.hottalk.hottalkserver.dto;

public class MessageDetailDTO {
    private long timestamp;
    private String messageContent;

    public MessageDetailDTO(long timestamp, String messageContent) {
        this.timestamp = timestamp;
        this.messageContent = messageContent;
    }


    // Getter, Setter

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    public String getMessageContent() {
        return messageContent;
    }

    public void setMessageContent(String messageContent) {
        this.messageContent = messageContent;
    }
}
