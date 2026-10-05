package com.hottalk.hottalkserver.dto;
import lombok.Data;

@Data
public class MeetingUpdateRequestDTO {
    private String title;
    private String location;
    private String description;
    private Integer maxParticipants;
}
