package com.hottalk.hottalkserver.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "periodic_meetings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PeriodicMeeting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 모임 대표가 등록한 정기모임 제목, 설명 등
    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    // 정기모임 날짜 (예: 2025-03-25)
    @Column(nullable = false)
    private LocalDate date;

    // 정기모임 시간 (예: "09:00")
    @Column(nullable = false)
    private String time;

    @Column(nullable = false)
    private String location;

    @Column(nullable = false)
    private Integer cost;

    // 정기모임에 해당하는 최대 인원 수
    @Column(nullable = false)
    private Integer maxParticipants;

    // 해당 PeriodicMeeting이 어느 Meeting에 속하는지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", nullable = false)
    private Meeting meeting;
}
