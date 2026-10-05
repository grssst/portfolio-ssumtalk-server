package com.hottalk.hottalkserver.dto;

public class ImageUpdateResponseDTO {
    private String imageUrl;

    public ImageUpdateResponseDTO(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
