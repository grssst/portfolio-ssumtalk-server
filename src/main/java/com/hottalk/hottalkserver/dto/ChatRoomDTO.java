package com.hottalk.hottalkserver.dto;

import jakarta.persistence.Column;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;
import java.util.List;

public class ChatRoomDTO {
    private String roomId;
    private String nickname;
    private int age;
    private String profileImageUrl;
    private String myProfileImageUrl;   // ✅ 내 프로필 이미지 추가
    private String lastMessage;
    private long lastMessageTimestamp;
    private int unreadCount;
    private Double latitude;
    private Double longitude;
    private String gender;
    private LocalDateTime lastDisconnectAt;
    private long myId;

    // 생성자
    public ChatRoomDTO(String roomId, String nickname, int age, String profileImageUrl, String myProfileImageUrl,
                       Double latitude, Double longitude, String lastMessage,
                       long lastMessageTimestamp, int unreadCount, String gender, LocalDateTime lastDisconnectAt, Long myId
                       ) {
        this.roomId = roomId;
        this.nickname = nickname;
        this.age = age;
        this.profileImageUrl = profileImageUrl;
        this.myProfileImageUrl = myProfileImageUrl;
        this.latitude = latitude;
        this.longitude = longitude;
        this.lastMessage = lastMessage;
        this.lastMessageTimestamp = lastMessageTimestamp;
        this.unreadCount = unreadCount;
        this.gender = gender;
        this.lastDisconnectAt = lastDisconnectAt;
        this.myId = myId;
    }

    public ChatRoomDTO(String roomId, String nickname, int age, String profileImageUrl,
                       Double latitude, Double longitude, String lastMessage,
                       long lastMessageTimestamp, int unreadCount, String gender, LocalDateTime lastDisconnectAt, Long myId
    ) {
        this.roomId = roomId;
        this.nickname = nickname;
        this.age = age;
        this.profileImageUrl = profileImageUrl;
        this.latitude = latitude;
        this.longitude = longitude;
        this.lastMessage = lastMessage;
        this.lastMessageTimestamp = lastMessageTimestamp;
        this.unreadCount = unreadCount;
        this.gender = gender;
        this.lastDisconnectAt = lastDisconnectAt;
        this.myId = myId;
    }

    public ChatRoomDTO(String roomId, String lastMessage, long lastMessageTimestamp, int unreadCount,
                       LocalDateTime lastDisconnectAt, long myId) {
        this.roomId = roomId;
        this.lastMessage = lastMessage;
        this.lastMessageTimestamp = lastMessageTimestamp;
        this.unreadCount = unreadCount;
        this.lastDisconnectAt = lastDisconnectAt;
        this.myId = myId;
        // 나머지 필드는 기본값으로 초기화 (예: null 또는 0)
        this.nickname = null;
        this.age = 0;
        this.profileImageUrl = null;
        this.latitude = 0.0;
        this.longitude = 0.0;
        this.gender = null;
    }


    // Getter와 Setter 추가 (필요한 경우 Lombok으로 대체 가능)
    public long getMyId() {
        return myId;
    }
    public void setMyId(long myId) {
        this.myId = myId;
    }
    public Double getLatitude() {
        return latitude;
    }
    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }
    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public LocalDateTime getLastDisconnectAt() {
        return lastDisconnectAt;
    }

    public void setLastDisconnectAt(LocalDateTime lastDisconnectAt) {
        this.lastDisconnectAt = lastDisconnectAt;
    }
    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }
    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }
    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public String getMyProfileImageUrl() {
        return myProfileImageUrl;
    }

    public void setMyProfileImageUrl(String myProfileImageUrl) {
        this.myProfileImageUrl = myProfileImageUrl;
    }



    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public long getLastMessageTimestamp() {
        return lastMessageTimestamp;
    }

    public void setLastMessageTimestamp(long lastMessageTimestamp) {
        this.lastMessageTimestamp = lastMessageTimestamp;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }
}
