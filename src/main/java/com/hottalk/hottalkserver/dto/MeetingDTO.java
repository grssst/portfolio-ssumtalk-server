package com.hottalk.hottalkserver.dto;


import com.hottalk.hottalkserver.model.Meeting;
import lombok.*;

import java.time.LocalDate;
import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeetingDTO {
    private Long id;
    private String title;
    private String description;
    private String topic;
    private String location;
    private Integer maxParticipants;
    private String imageUrl;
    private Date createdAt;
    private Boolean isActive;
    private Boolean isSubscriptionRequired;
    private LocalDate subscriptionStartDate;
    private String writerExternalUserId;
    private String writerNickname;
    private LocalDate subscriptionEndDate;
    private Boolean isSubscriptionExpired; // 구독 기간이 끝났는지 여부
    private int subscriptionCost; // 고정 구독 비용 (150 캔디)
    // 현재 참가자 수 필드 추가
    private int participantCount;

    // 엔티티에서 DTO로 변환하는 메서드
    public static MeetingDTO fromEntity(Meeting meeting) {
        return MeetingDTO.builder()
                .id(meeting.getId())
                .title(meeting.getTitle())
                .description(meeting.getDescription())
                .topic(meeting.getTopic())
                .writerNickname(meeting.getWriterNickname())
                .location(meeting.getLocation())
                .maxParticipants(meeting.getMaxParticipants())
                .writerExternalUserId(meeting.getExternalUserId())
                .imageUrl(meeting.getImageUrl())
                .createdAt(meeting.getCreatedAt())
                .isActive(meeting.getIsActive())
                .isSubscriptionRequired(meeting.getIsSubscriptionRequired())
                .subscriptionStartDate(meeting.getSubscriptionStartDate())
                .subscriptionEndDate(meeting.getSubscriptionEndDate())
                // isSubscriptionActive() 메서드의 반환값이 구독 만료 여부라면 그대로 사용
                .isSubscriptionExpired(meeting.isSubscriptionActive())
                .subscriptionCost(meeting.getSubscriptionCost())
                // 참가자 목록이 null이 아니면 size()로 현재 참가자 수를 계산, null이면 0
                .participantCount(meeting.getParticipants() != null ? meeting.getParticipants().size() : 0)
                .build();
    }

}
