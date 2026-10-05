package com.hottalk.hottalkserver.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "meeting_participants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeetingParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 모임과의 다대일 관계 설정
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", nullable = false)
    private Meeting meeting;

    // 사용자와의 다대일 관계 설정 (사용자 엔티티가 있다고 가정)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 추가적인 필드가 필요하다면 (예: 참여 시간, 역할 등) 추가할 수 있습니다.
}
