package com.hottalk.hottalkserver.model;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Getter
@Setter
@Table(name = "gallery_images")
public class GalleryImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 이미지 접근 URL 또는 경로
    private String imageUrl;

    private LocalDateTime createdAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    // meetingId를 String으로 변경
    @Column(nullable = false)
    private String meetingId;

    // 기본 생성자
    public GalleryImage() {
    }


}
