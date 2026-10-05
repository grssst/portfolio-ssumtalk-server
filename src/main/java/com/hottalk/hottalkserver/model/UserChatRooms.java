package com.hottalk.hottalkserver.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "user_chat_rooms")
public class UserChatRooms {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // 데이터베이스에서 자동으로 생성되는 고유 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT))
    private User user;


    @Column(name = "room_id", nullable = false)
    private String roomId;  // 채팅방 ID

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;  // 생성 시간

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;  // 마지막 업데이트 시간

    // 마지막 웹소켓 연결 해제 시간
    @Column(name = "last_disconnect_at")
    private LocalDateTime lastDisconnectAt;


    // Default constructor
    public UserChatRooms() {}

    // Constructor
    public UserChatRooms(User user, String roomId) {
        this.user = user;
        this.roomId = roomId;
    }

    // Getters and Setters
    public LocalDateTime getLastDisconnectAt() {
        return lastDisconnectAt;
    }

    public void setLastDisconnectAt(LocalDateTime lastDisconnectAt) {
        this.lastDisconnectAt = lastDisconnectAt;
    }
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
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

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    }
}