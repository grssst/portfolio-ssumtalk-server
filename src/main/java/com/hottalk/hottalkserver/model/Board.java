package com.hottalk.hottalkserver.model;




import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


import java.time.LocalDateTime;
import java.time.ZoneId;

@Getter
@Setter
@Entity
@Table(name = "board_posts")
public class Board {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // meetingId를 String으로 변경
    @Column(nullable = false)
    private Long meetingId;

    // 게시글 작성자의 식별자 (JWT에서 추출한 외부 사용자 ID)
    private String externalUserId;

    private String title;
    private String content;
    private LocalDateTime createdAt;

    // 추가 필드: 작성자의 닉네임과 프로필 이미지 URL
    private String nickname;
    private String profileImageUrl;

    private String gender;

    public Board() {
        this.createdAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    }

    // Getters & Setters

    public Long getId() {
        return id;
    }


    public String getExternalUserId() {
        return externalUserId;
    }

    public void setExternalUserId(String externalUserId) {
        this.externalUserId = externalUserId;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }


    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }
}
