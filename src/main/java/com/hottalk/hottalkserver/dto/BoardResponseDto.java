package com.hottalk.hottalkserver.dto;



import java.time.LocalDateTime;

public class BoardResponseDto {
    private Long id;
    private Long meetingId;
    private String externalUserId;
    private String title;
    private String content;
    private LocalDateTime createdAt;
    // 추가된 필드
    private String nickname;
    private String profileImageUrl;
    private String gender;

    public BoardResponseDto() {}

    public BoardResponseDto(Long id, Long meetingId, String externalUserId, String title, String content,
                            LocalDateTime createdAt, String nickname, String profileImageUrl, String gender) {
        this.id = id;
        this.meetingId = meetingId;
        this.externalUserId = externalUserId;
        this.title = title;
        this.content = content;
        this.createdAt = createdAt;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.gender = gender;
    }

    public BoardResponseDto(Long id, String meetingId, String externalUserId, String title, String content, LocalDateTime createdAt, String nickname, String profileImageUrl, String gender) {
    }

    // getters & setters

    public Long getId() {
        return id;
    }

    public Long getMeetingId() {
        return meetingId;
    }

    public String getExternalUserId() {
        return externalUserId;
    }

    public String getGender() {
        return gender;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getNickname() {
        return nickname;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setMeetingId(Long meetingId) {
        this.meetingId = meetingId;
    }

    public void setExternalUserId(String externalUserId) {
        this.externalUserId = externalUserId;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }
}
