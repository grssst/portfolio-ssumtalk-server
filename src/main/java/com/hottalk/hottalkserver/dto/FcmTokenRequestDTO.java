package com.hottalk.hottalkserver.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FcmTokenRequestDTO {
    private String fcmToken;
    private String token;
    @JsonProperty("UUID") // JSON의 "UUID" 필드를 Java 필드에 매핑
    private String UUID;

    // Getter & Setter
    public String getUUID() { return UUID; }
    public void setUUID(String UUID) { this.UUID = UUID; }

    public String getFcmToken() { return fcmToken; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}
