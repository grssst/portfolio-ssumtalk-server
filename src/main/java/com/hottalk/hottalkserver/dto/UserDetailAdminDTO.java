package com.hottalk.hottalkserver.dto;

import lombok.Data;
import java.util.List;

/**
 * 유저 기본 정보 + 게시글 + 모임 + 차단 정보 + 채팅방 + 제재 + 문의 + 신고 + 결제(밸런스) 정보를 모두 담는 DTO
 */
@Data
public class UserDetailAdminDTO {

    private UserAdminDTO user;                           // 유저 정보
    private List<PostAdminDTO> posts;                    // 게시글 목록
    private List<MeetingAdminDTO> meetings;              // 모임 참여
    private List<BlockedUserAdminDTO> blockedUsers;       // 차단 정보
    private List<UserChatRoomAdminDTO> chatRooms;         // 채팅방
    private List<SanctionAdminDTO> sanctions;            // 제재 내역
    private List<CallCenterAdminDTO> callCenters;         // 문의 내역
    private List<ReportAdminDTO> reports;                // 신고 정보
    private List<BalanceHistoryAdminDTO> balanceHistories; // 결제/밸런스 이력
}
