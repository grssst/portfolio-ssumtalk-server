package com.hottalk.hottalkserver.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "purchase_transactions_android")
public class PurchaseTransactionAndroid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "product_id", length = 255)
    private String productId;

    @Column(name = "order_id", length = 255, unique = true)
    private String orderId;

    @Column(name = "purchase_token", length = 255)
    private String purchaseToken;

    @Column(name = "purchase_time")
    private LocalDateTime purchaseTime;

    @Column(name = "purchase_state")
    private int purchaseState; // 0: 구매완료, 1: 환불 등

    @Column(name = "verified")
    private boolean verified = false; // 서버 검증 여부

    @Column(name = "consumed")
    private boolean consumed = false; // 소비 처리 여부

    @Column(name = "create_at")
    private LocalDateTime createAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));

    @Column(name = "update_at")
    private LocalDateTime updateAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));

    @Column(name = "delete_at")
    private LocalDateTime deleteAt;

    @Column(name = "external_user_id")
    private String externalUserId;  // 외부 시스템의 사용자 ID
    @PreUpdate
    public void preUpdate() {
        this.updateAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    }

    // getters and setters
    public String getExternalUserId() {
        return externalUserId;
    }

    public void setExternalUserId(String externalUserId) {
        this.externalUserId = externalUserId;
    }
    public LocalDateTime getDeleteAt() { return deleteAt; }
    public void setDeleteAt(LocalDateTime deleteAt) { this.deleteAt = deleteAt; }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getPurchaseToken() { return purchaseToken; }
    public void setPurchaseToken(String purchaseToken) { this.purchaseToken = purchaseToken; }

    public LocalDateTime getPurchaseTime() { return purchaseTime; }
    public void setPurchaseTime(LocalDateTime purchaseTime) { this.purchaseTime = purchaseTime; }

    public int getPurchaseState() { return purchaseState; }
    public void setPurchaseState(int purchaseState) { this.purchaseState = purchaseState; }

    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }

    public boolean isConsumed() { return consumed; }
    public void setConsumed(boolean consumed) { this.consumed = consumed; }

    public LocalDateTime getCreateAt() { return createAt; }
    public void setCreateAt(LocalDateTime createAt) { this.createAt = createAt; }

    public LocalDateTime getUpdateAt() { return updateAt; }
    public void setUpdateAt(LocalDateTime updateAt) { this.updateAt = updateAt; }
}
