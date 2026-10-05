package com.hottalk.hottalkserver.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "sanctions")
public class Sanction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // 제재의 고유 ID

    @Column(name = "user_id", nullable = false)
    private Long userId;  // 제재된 사용자의 ID

    @Column(name = "reason", nullable = false)
    private String reason;  // 제재 사유

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;  // 제재 시작 시간

    @Column(name = "end_time")
    private LocalDateTime endTime;  // 제재 종료 시간 (null이면 영구정지)

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;  // 제재 생성 시간

    public Sanction() {
    }

    public Sanction(Long userId, String reason, LocalDateTime startTime, LocalDateTime endTime) {
        this.userId = userId;
        this.reason = reason;
        this.startTime = startTime;
        this.endTime = endTime;
        this.createdAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
