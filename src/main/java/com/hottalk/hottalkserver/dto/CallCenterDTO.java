package com.hottalk.hottalkserver.dto;

import jakarta.persistence.Column;

import java.time.LocalDateTime;

public class CallCenterDTO {
    private Long id;
    private String callCenterContent;
    private LocalDateTime calledAt;
    private long callerUserId;
    private String category;




    // Getters and Setters
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

