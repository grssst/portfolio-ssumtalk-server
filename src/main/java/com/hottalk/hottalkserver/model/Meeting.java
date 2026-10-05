package com.hottalk.hottalkserver.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

import java.time.LocalDate;
import java.util.Date;
import java.util.Calendar;

@Entity
@Table(name = "meetings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Meeting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 자동 증가 ID

    // MeetingParticipant와의 관계 설정 (모임에 참여한 사용자 목록)
    @OneToMany(mappedBy = "meeting", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MeetingParticipant> participants = new ArrayList<>();

    @Column(name = "external_user_id", nullable = false)
    private String externalUserId;

    @Column(nullable = false)
    private Boolean recurring = false; // 정기모임 여부

    @Column(length = 255)
    private String recurringDescription; // 정기모임 설명

    @Column(length = 50)
    private String recurringDay; // 정기모임 요일

    @Column(length = 20)
    private String recurringTime; // 정기모임 시간

    @Column(nullable = true)
    private Integer cost; // 정기모임 비용


    @Column(nullable = false, length = 100)
    private String title; // 모임 제목

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description; // 모임 설명

    @Column(nullable = false, length = 50)
    private String topic; // 모임 주제

    @Column(nullable = false, length = 100)
    private String location; // 모임 지역 (ex: "서울 강남구 역삼동")

    @Column(nullable = false)
    private String writerNickname; // 작성자 닉네임

    @Column(nullable = false)
    private Integer maxParticipants; // 최대 참가자 수

    @Column(nullable = false)
    private Integer minParticipants = 30; // 최소 참가자 수 (기본값: 30명)

    @Column(length = 255)
    private String imageUrl; // 이미지 URL (선택사항)

    @Column(nullable = false)
    private Boolean isActive = true; // 활성 상태 (기본값: true)

    @Column(nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt = new Date(); // 생성일

    /** 🔹 구독 관련 필드 */
    @Column(nullable = false)
    private Boolean isSubscriptionRequired = true; // 구독 필요 여부 (기본값: true)

    @Column(nullable = false)
    private static final int SUBSCRIPTION_COST = 150; // 구독 비용 (고정값: 150 캔디)

    @Column(nullable = false)
    private LocalDate subscriptionStartDate; // 구독 시작일

    @Column(nullable = false)
    private LocalDate subscriptionEndDate; // 구독 종료일 (30일 후 자동 설정)

    /** 🔹 30일 무료 적용 */
    @PrePersist
    protected void onCreate() {
        this.subscriptionStartDate = LocalDate.now(); // 현재 날짜 설정
        this.subscriptionEndDate = this.subscriptionStartDate.plusDays(30); // 30일 추가
    }

    /** 🔹 구독 비용 반환 */
    public int getSubscriptionCost() {
        return SUBSCRIPTION_COST;
    }

    /** 🔹 구독이 활성 상태인지 확인 (무료 기간 이후) */
    public boolean isSubscriptionActive() {
        return LocalDate.now().isAfter(subscriptionEndDate);
    }


}
