package com.hottalk.hottalkserver.dto;

import java.util.List;

public class DeleteUserResultDTO {
    private Long userId;
    private List<Long> joinedMeetingIds;   // 내가 "가입한" 미팅
    private List<Long> createdMeetingIds;  // 내가 "개설한" 미팅 (원하면 제거해도 됨)

    public DeleteUserResultDTO(Long userId,
                               List<Long> joinedMeetingIds,
                               List<Long> createdMeetingIds) {
        this.userId = userId;
        this.joinedMeetingIds = joinedMeetingIds;
        this.createdMeetingIds = createdMeetingIds;
    }

    // getter / setter
}
