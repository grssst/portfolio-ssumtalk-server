package com.hottalk.hottalkserver.dto;

import jakarta.persistence.criteria.CriteriaBuilder;

public class PurchaseVerifyRequestAndroidDTO {
    private Long userId;
    private String productId;
    private String orderId;
    private String purchaseToken;
    private String provider;
    private String packageName;
    private int purchaseState;
    private String externalUserId;

    // getters and setters
    public String getExternalUserId() {
        return externalUserId;
    }
    public void setExternalUserId(String externalUserId) {
        this.externalUserId = externalUserId;
    }
    public String getPackageName() {
        return packageName;
    }
    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }
    public Long getUserId() {
        return userId;
    }
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public int getPurchaseState() {
        return purchaseState;
    }
    public void setPurchaseState(int purchaseState) {
        this.purchaseState = purchaseState;
    }

    public String getProvider() {
        return provider;
    }
    public void setProvider(String provider) {
        this.provider = provider;
    }
    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getPurchaseToken() {
        return purchaseToken;
    }

    public void setPurchaseToken(String purchaseToken) {
        this.purchaseToken = purchaseToken;
    }

}
