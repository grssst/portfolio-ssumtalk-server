package com.hottalk.hottalkserver.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserChatRoomAdminDTO {
    private Long id;
    private String roomId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastDisconnectAt;
    // 기타 필요 시...
}
