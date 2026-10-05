package com.hottalk.hottalkserver.dto;

import java.time.LocalDateTime;
import java.util.List;

public class UserProfileDTO {
    private String nickname;  // 닉네임
    private String gender;  // 성별
    private Integer age;  // 나이
    private String profileImageUrl;  // 프로필 사진 URL
    private String profileImageUrl2;  // 프로필 사진 URL
    private String profileImageUrl3;  // 프로필 사진 URL
    private String status;  // 주제
    private Double latitude;
    private Double longitude;
    private Long id;
    private String externalUserId;
    private Double userListLatitude;
    private Double userListLongitude;
    private LocalDateTime lastLogin;  // 마지막 로그인 시간
    private String myLocation;
    private String interestsKey;

    public UserProfileDTO() {
    }

    public UserProfileDTO(String nickname, String gender, Integer age, String profileImageUrl, String profileImageUrl2, String profileImageUrl3, String status, String myLocation) {
        this.nickname = nickname;
        this.gender = gender;
        this.age = age;
        this.profileImageUrl = profileImageUrl;
        this.profileImageUrl2 = profileImageUrl2;
        this.profileImageUrl3 = profileImageUrl3;
        this.status = status;
        this.myLocation = myLocation;
    }

    public UserProfileDTO(long id, String externalUserId, String nickname, String gender, Integer age,
                          String profileImageUrl, String profileImageUrl2, String profileImageUrl3, String status, Double latitude, Double longitude, Double userListLatitude, Double userListLongitude,
                          LocalDateTime lastLogin, String myLocation, String interestsKey) {
        this.id = id;
        this.externalUserId = externalUserId;
        this.nickname = nickname;
        this.gender = gender;
        this.age = age;
        this.profileImageUrl = profileImageUrl;
        this.profileImageUrl2 = profileImageUrl2;
        this.profileImageUrl3 = profileImageUrl3;
        this.status = status;
        this.latitude = latitude;
        this.longitude = longitude;
        this.userListLatitude = userListLatitude;
        this.userListLongitude = userListLongitude;
        this.lastLogin = lastLogin;
        this.myLocation = myLocation;
        this.interestsKey = interestsKey;
    }



    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }
    public String getExternalUserId() {
        return externalUserId;
    }

    public void setExternalUserId(String externalUserId) {
        this.externalUserId = externalUserId;
    }

    public Double getUserListLatitude() { return userListLatitude; }
    public void setUserListLatitude(Double userListLatitude) {
        this.userListLatitude = userListLatitude;
    }

    public Double getUserListLongitude() {
        return userListLongitude;
    }
    public void setUserListLongitude(Double userListLongitude) {
        this.userListLongitude = userListLongitude;
    }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
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

    public String getMyLocation() {
        return myLocation;
    }

    public void setMyLocation(String myLocation) {
        this.myLocation = myLocation;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }
    public String getProfileImageUrl2() {
        return profileImageUrl2;
    }

    public void setProfileImageUrl2(String profileImageUrl2) {
        this.profileImageUrl2 = profileImageUrl2;
    }
    public String getProfileImageUrl3() {
        return profileImageUrl3;
    }

    public void setProfileImageUrl3(String profileImageUrl3) {
        this.profileImageUrl3 = profileImageUrl3;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
    public String getInterestsKey() {
        return interestsKey;
    }

    public void setInterestsKey(String interestsKey) {
        this.interestsKey = interestsKey;
    }
}
