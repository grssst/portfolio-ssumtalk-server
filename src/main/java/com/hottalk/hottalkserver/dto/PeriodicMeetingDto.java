package com.hottalk.hottalkserver.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PeriodicMeetingDto {
    private Long id;
    private String title;
    private String description;
    private String date;       // "YYYY-MM-DD" 형식 문자열
    private String time;
    private String location;
    private Integer cost;
    private Integer maxParticipants;
}
