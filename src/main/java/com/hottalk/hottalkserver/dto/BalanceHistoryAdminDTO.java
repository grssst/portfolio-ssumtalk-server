package com.hottalk.hottalkserver.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BalanceHistoryAdminDTO {
    private Long id;
    private Long userId;
    private String externalUserId;
    private String changeType;
    private int amount;
    private int balanceBefore;
    private int balanceAfter;
    private Long relatedTransactionId;
    private LocalDateTime createAt;
    // etc...
}
