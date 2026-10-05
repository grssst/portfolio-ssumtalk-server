package com.hottalk.hottalkserver.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MeetingParticipantUserDto {
    private Long id;              // MeetingParticipant의 id
    private String nickname;      // User의 닉네임
    private Integer age;          // User의 나이
    private String profileImageUrl; // User의 프로필 이미지 URL
    private String externalUserId; // User의 외부 사용자 ID
    private Long meetingId;       // 해당 모임의 id
}