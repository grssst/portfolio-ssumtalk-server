package com.hottalk.hottalkserver.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "purchase_transactions_ios")
public class PurchaseTransactionIos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "product_id", length = 255)
    private String productId;

    // iOS 결제에서 고유 식별자로 사용될 필드 (transaction_id, original_transaction_id)
    @Column(name = "transaction_id", length = 255)
    private String transactionId;

    @Column(name = "original_transaction_id", length = 255)
    private String originalTransactionId;

    // 구매 시간 (Receipt 검증 결과에서 얻은 purchase_date를 LocalDateTime 변환)
    @Column(name = "purchase_date")
    private LocalDateTime purchaseDate;

    // 서버 검증 여부
    @Column(name = "verified")
    private boolean verified = false;

    // 소비 처리 여부
    @Column(name = "consumed")
    private boolean consumed = false;
    @Column(name = "status")
    private int status; // 0: 구매완료, 1: 환불 등

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
    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getProductId() {
        return productId;
    }
    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getTransactionId() {
        return transactionId;
    }
    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getOriginalTransactionId() {
        return originalTransactionId;
    }
    public void setOriginalTransactionId(String originalTransactionId) {
        this.originalTransactionId = originalTransactionId;
    }

    public LocalDateTime getPurchaseDate() {
        return purchaseDate;
    }
    public void setPurchaseDate(LocalDateTime purchaseDate) {
        this.purchaseDate = purchaseDate;
    }
    public boolean isVerified() {
        return verified;
    }
    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public boolean isConsumed() {
        return consumed;
    }
    public void setConsumed(boolean consumed) {
        this.consumed = consumed;
    }

    public LocalDateTime getCreateAt() {
        return createAt;
    }
    public void setCreateAt(LocalDateTime createAt) {
        this.createAt = createAt;
    }

    public LocalDateTime getUpdateAt() {
        return updateAt;
    }
    public void setUpdateAt(LocalDateTime updateAt) {
        this.updateAt = updateAt;
    }
}