package com.hottalk.hottalkserver.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SanctionAdminDTO {
    private Long id;
    private Long userId;
    private String reason;
    private LocalDateTime startTime;
    private LocalDateTime endTime;   // null이면 영구정지
    private LocalDateTime createdAt;
}
