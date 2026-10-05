package com.hottalk.hottalkserver.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReportAdminDTO {
    private Long id;
    private Long reportedUserId;
    private String reportedExternalUserId;
    private String reportContent;
    private Long postId;
    private String roomId;
    private LocalDateTime reportedAt;
    private String status;
}
