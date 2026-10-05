package com.hottalk.hottalkserver.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PostAdminDTO {
    private Long id;
    private String content;
    private String imageUrl;
    private LocalDateTime createdAt;
    // 기타 필요한 필드...
}
