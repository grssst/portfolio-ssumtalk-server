package com.hottalk.hottalkserver.dto;

public class PurchaseVerifyResponseIosDTO {
    private boolean success;
    private int updatedCandy;
    private long status;


    // getters and setters
    public long getStatus() {
        return status;
    }
    public void setStatus(long status) {
        this.status = status;
    }
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getUpdatedCandy() {
        return updatedCandy;
    }

    public void setUpdatedCandy(int updatedCandy) {
        this.updatedCandy = updatedCandy;
    }
}
