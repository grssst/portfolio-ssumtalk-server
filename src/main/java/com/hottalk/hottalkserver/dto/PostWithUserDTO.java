// PostWithUserDTO.java
package com.hottalk.hottalkserver.dto;

import com.hottalk.hottalkserver.model.Post;
import com.hottalk.hottalkserver.model.User;
import jakarta.persistence.Column;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;
import java.time.ZoneId;

public class PostWithUserDTO {
    private Long id;
    private String content;
    private String imageUrl;
    private LocalDateTime createdAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    //비정규화 된 게시글 user 테이블의 컬럼
    private Long userId;
    private String externalUserId;
    private Double postLatitude;
    private Double postLongitude;

    private String nickname;  // 닉네임
    private String gender;  // 성별
    private Integer age;  // 나이
    private String profileImageUrl;  // 프로필 사진 URL
    private String status;  // 주제
    private String myLocation;

    private Double userLatitude;
    private Double userLongitude;
    private Integer dailyPostCount;


    // 기본 생성자
    public PostWithUserDTO() {
    }

    // ID만 받는 생성자
    public PostWithUserDTO(Long id) {
        this.id = id;
    }

    // 모든 필드를 받는 생성자
    public PostWithUserDTO(Long id, String content, String imageUrl, LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime deletedAt,
                           Long userId, String externalUserId, Double postLatitude, Double postLongitude,
                           String nickname, String gender, Integer age, String profileImageUrl, String status, String myLocation,
                           Double userLatitude, Double userLongitude, Integer dailyPostCount) {
        this.id = id;
        this.content = content;
        this.imageUrl = imageUrl;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
        this.userId = userId;
        this.externalUserId = externalUserId;
        this.postLatitude = postLatitude;
        this.postLongitude = postLongitude;
        this.nickname = nickname;
        this.gender = gender;
        this.age = age;
        this.profileImageUrl = profileImageUrl;
        this.status = status;
        this.myLocation = myLocation;
        this.userLatitude = userLatitude;
        this.userLongitude = userLongitude;
        this.dailyPostCount = dailyPostCount;
    }

    // Getters and Setters
    public String getMyLocation() {
        return myLocation;
    }

    public void setMyLocation(String myLocation) {
        this.myLocation = myLocation;
    }
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getExternalUserId() {
        return externalUserId;
    }

    public void setExternalUserId(String externalUserId) {
        this.externalUserId = externalUserId;
    }

    public Double getPostLatitude() {
        return postLatitude;
    }

    public void setPostLatitude(Double postLatitude) {
        this.postLatitude = postLatitude;
    }

    public Double getPostLongitude() {
        return postLongitude;
    }

    public void setPostLongitude(Double postLongitude) {
        this.postLongitude = postLongitude;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }
    public Integer getDailyPostCount() {
        return dailyPostCount;
    }

    public void setDailyPostCount(Integer dailyPostCount) {
        this.dailyPostCount = dailyPostCount;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getUserLatitude() {
        return userLatitude;
    }

    public void setUserLatitude(Double userLatitude) {
        this.userLatitude = userLatitude;
    }

    public Double getUserLongitude() {
        return userLongitude;
    }

    public void setUserLongitude(Double userLongitude) {
        this.userLongitude = userLongitude;
    }
}