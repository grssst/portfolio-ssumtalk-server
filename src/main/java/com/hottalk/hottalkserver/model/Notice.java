package com.hottalk.hottalkserver.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "notices")
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // 공지사항 ID (자동 증가)

    @Column(nullable = false)
    private String title;  // 공지 제목

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;  // 공지 내용

    @Column(nullable = false)
    private LocalDateTime createdAt;  // 작성일

}
