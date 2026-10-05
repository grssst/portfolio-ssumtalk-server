package com.hottalk.hottalkserver.dto;

import lombok.Data;

@Data
public class BlockedUserAdminDTO {
    private Long id;
    private String blockerExternalUserId;
    private String blockedExternalUserId;
    // 기타 필요 시...
}
