package com.hottalk.hottalkserver.dto;

import lombok.Data;
import java.util.Date;

@Data
public class MeetingAdminDTO {
    private Long id;
    private String externalUserId;
    private String title;
    private String description;
    private String topic;
    private String location;
    private String writerNickname;
    private Integer maxParticipants;
    private Date createdAt;
    // 기타 필요한 필드(정기모임 여부, cost 등)...
}
