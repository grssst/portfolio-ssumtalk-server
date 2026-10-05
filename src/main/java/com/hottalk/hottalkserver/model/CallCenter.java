package com.hottalk.hottalkserver.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class CallCenter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 2000)
    private String callCenterContent;

    private LocalDateTime calledAt;

    private long callerUserId;
    @Column(name = "external_user_id")
    private String externalUserId;  // 외부 시스템의 사용자 ID

    private String category;
    @Column(name = "delete_at")
    private LocalDateTime deleteAt;



    // Getters and Setters
    public LocalDateTime getDeleteAt() { return deleteAt; }
    public void setDeleteAt(LocalDateTime deleteAt) { this.deleteAt = deleteAt; }
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public Long getCallerUserId() {
        return callerUserId;
    }

    public void setCallerUserId(Long callerUserId) {
        this.callerUserId = callerUserId;
    }
    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getExternalUserId() {
        return externalUserId;
    }

    public void setExternalUserId(String externalUserId) {
        this.externalUserId = externalUserId;
    }

    public LocalDateTime getCalledAt() {
        return calledAt;
    }

    public void setCalledAt(LocalDateTime calledAt) {
        this.calledAt = calledAt;
    }

    public String getCallCenterContent() {
        return callCenterContent;
    }

    public void setCallCenterContent(String callCenterContent) {
        this.callCenterContent = callCenterContent;
    }
}
