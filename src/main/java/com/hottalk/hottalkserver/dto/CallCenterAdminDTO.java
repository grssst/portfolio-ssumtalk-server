package com.hottalk.hottalkserver.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CallCenterAdminDTO {
    private Long id;
    private String callCenterContent;
    private LocalDateTime calledAt;
    private long callerUserId;
    private String externalUserId;
    private String category;
    // deleteAt 등 필요 시...
}
