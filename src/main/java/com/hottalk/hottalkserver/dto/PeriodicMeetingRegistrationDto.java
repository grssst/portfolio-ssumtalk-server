package com.hottalk.hottalkserver.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PeriodicMeetingRegistrationDto {
    private String title;
    private String description;
    private String time;        // 예: "09:00"
    private String location;
    private Integer cost;
    private Integer maxParticipants; // 정기모임에 해당하는 최대 인원 수
    private String date; // ISO 형식 날짜 문자열 (예: "2025-03-25")
}
