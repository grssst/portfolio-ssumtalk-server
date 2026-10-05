package com.hottalk.hottalkserver.dto;


public class BoardRequestDto {

    private String title;
    private String content;

    // 기본 생성자
    public BoardRequestDto() {
    }

    // getters and setters

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
