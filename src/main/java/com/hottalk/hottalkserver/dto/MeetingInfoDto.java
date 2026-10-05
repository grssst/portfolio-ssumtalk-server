package com.hottalk.hottalkserver.dto;



import lombok.*;

import java.util.Date;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MeetingInfoDto {
    private Long id;
    private String externalUserId;
    private String title;
    private String description;
    private String imageUrl;
    private String location;
    private String organizer; // 작성자 또는 대표 이름 (예: writerNickname)
    private Date createdAt;   // 필요 시 생성일도 포함
    private String nickname;
    private Long myUserId;
    private long remainingDays; // 잔여일 추가
    private Integer maxParticipants;
}
