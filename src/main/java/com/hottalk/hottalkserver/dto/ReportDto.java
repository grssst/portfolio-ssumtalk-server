package com.hottalk.hottalkserver.dto;

public class ReportDto {
    private Long reportedUserId;
    private String reportContent;
    private Long postId;
    private String roomId;
    private String postText;
    private String chatSnapshot;
    private String status;
    private String reportedExternalUserId;
    private boolean isChatRoom = false;

    // Getters and Setters
    public boolean getIsChatRoom() {
        return isChatRoom;
    }
    public void setIsChatRoom(boolean isChatRoom) {
        this.isChatRoom = isChatRoom;
    }
    public String getReportedExternalUserId() {
        return reportedExternalUserId;
    }
    public void setReportedExternalUserId(String reportedExternalUserId) {
        this.reportedExternalUserId = reportedExternalUserId;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    public String getChatSnapshot() {
        return chatSnapshot;
    }
    public void setChatSnapshot(String chatSnapshot) {
        this.chatSnapshot = chatSnapshot;
    }
    public String getPostText() {
        return postText;
    }
    public void setPostText(String postText) {
        this.postText = postText;
    }
    public String getRoomId() {
        return roomId;
    }
    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public Long getReportedUserId() {
        return reportedUserId;
    }

    public void setReportedUserId(Long reportedUserId) {
        this.reportedUserId = reportedUserId;
    }

    public String getReportContent() {
        return reportContent;
    }

    public void setReportContent(String reportContent) {
        this.reportContent = reportContent;
    }

    public Long getPostId() {
        return postId;
    }

    public void setPostId(Long postId) {
        this.postId = postId;
    }
}
