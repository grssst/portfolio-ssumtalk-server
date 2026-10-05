package com.hottalk.hottalkserver.service;

import com.hottalk.hottalkserver.dto.*;
import com.hottalk.hottalkserver.model.*;
import com.hottalk.hottalkserver.repository.*;
import com.hottalk.hottalkserver.util.S3Uploader;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminService {

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CallCenterRepository callCenterRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private NoticeRepository noticeRepository;
    @Autowired
    private UserChatRoomsRepository userChatRoomsRepository;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private ReportRepository reportRepository;
    @Autowired
    private MeetingParticipantsRepository meetingParticipantsRepository;
    @Autowired
    private BalanceHistoryRepository balanceHistoryRepository;
    @Autowired
    private BlockedUserRepository blockedUserRepository;
    @Autowired
    private SanctionRepository sanctionRepository;
    @Autowired
    private S3Uploader s3Uploader;

    // 로그인 인증
    public boolean authenticate(String username, String password) {
        return adminRepository.findByUsername(username)
                .map(admin -> passwordEncoder.matches(password, admin.getPassword()))
                .orElse(false);
    }

    // 로그인 성공 시 마지막 로그인 시간 업데이트
    public void updateLastLogin(String username) {
        adminRepository.findByUsername(username).ifPresent(admin -> {
            admin.setLastLogin(LocalDateTime.now(ZoneId.of("Asia/Seoul")));
            adminRepository.save(admin);
        });
    }

    /**
     * DB에서 문의 목록을 조회.
     *
     * @param category '결제', '기능 요청' 등. 'all'인 경우 전체
     * @param sort     'asc' or 'desc'
     * @return List<CallCenter>
     */
    @Transactional(readOnly = true)
    public List<CallCenter> getInquiries(String category, String sort) {
        // (1) 카테고리가 'all'이 아닐 때 필터링
        boolean isAllCategory = "all".equalsIgnoreCase(category);

        // (2) 정렬 방향
        boolean isAsc = "asc".equalsIgnoreCase(sort);

        // (3) 간단히 분기 처리 (JPA 쿼리메서드를 여러개 써도 되고, JPQL 써도 됨)
        if (isAllCategory && isAsc) {
            return callCenterRepository.findAllByOrderByCalledAtAsc();
        } else if (isAllCategory && !isAsc) {
            return callCenterRepository.findAllByOrderByCalledAtDesc();
        } else if (!isAllCategory && isAsc) {
            return callCenterRepository.findByCategoryOrderByCalledAtAsc(category);
        } else {
            return callCenterRepository.findByCategoryOrderByCalledAtDesc(category);
        }
    }


    /**
     * 유저의 모든 관련 정보를 모아서 반환
     * @param userId DB PK
     * @param externalUserId external ID
     * @return Map<String, Object> 구조에 user, posts, meetings, blockedUsers, chatRooms, sanctions, callCenters, reports, balanceHistories
     */
    public UserDetailAdminDTO getUserDetail(Long userId, String externalUserId) {
        // 1) user 찾기
        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId).orElse(null);
        }
        if (user == null && externalUserId != null && !externalUserId.isEmpty()) {
            user = userRepository.findByExternalUserId(externalUserId).orElse(null);
        }
        if (user == null) {
            return null; // 유저 없음
        }

        // 최종 userId / externalUserId
        Long finalUserId = user.getId();
        String finalExtUserId = user.getExternalUserId();

        // 2) 엔티티 조회
        List<Post> postEntities = postRepository.findByExternalUserId(finalExtUserId);

        // 모임
        List<Meeting> meetingEntities = new ArrayList<>();
        meetingParticipantsRepository.findByUserIdWithMeeting(finalUserId).forEach(mp -> {
            if (mp.getMeeting() != null) {
                meetingEntities.add(mp.getMeeting());
            }
        });

        List<BlockedUser> blockedUserEntities =
                blockedUserRepository.findAllByBlockerExternalUserIdOrBlockedExternalUserId(finalExtUserId, finalExtUserId);

        List<UserChatRooms> chatRoomEntities = userChatRoomsRepository.findByUserId(finalUserId);

        List<Sanction> sanctionEntities = sanctionRepository.findByUserIdOrderByIdDesc(finalUserId);

        List<CallCenter> callCenterEntities = callCenterRepository.findByExternalUserId(finalExtUserId);

        List<Report> reportEntities = reportRepository.findByReportedUserIdOrReportedExternalUserId(finalUserId, finalExtUserId);

        List<BalanceHistory> balanceHistoryEntities = balanceHistoryRepository.findByExternalUserId(finalExtUserId);

        // 3) DTO 변환
        UserAdminDTO userDTO = toAdminUserDTO(user);

        List<PostAdminDTO> postDTOs = postEntities.stream()
                .map(this::toAdminPostDTO)
                .toList();

        List<MeetingAdminDTO> meetingDTOs = meetingEntities.stream()
                .map(this::toAdminMeetingDTO)
                .toList();

        List<BlockedUserAdminDTO> blockedUserDTOs = blockedUserEntities.stream()
                .map(this::toAdminBlockedUserDTO)
                .toList();

        List<UserChatRoomAdminDTO> chatRoomDTOs = chatRoomEntities.stream()
                .map(this::toAdminUserChatRoomDTO)
                .toList();

        List<SanctionAdminDTO> sanctionDTOs = sanctionEntities.stream()
                .map(this::toAdminSanctionDTO)
                .toList();

        List<CallCenterAdminDTO> callCenterDTOs = callCenterEntities.stream()
                .map(this::toAdminCallCenterDTO)
                .toList();

        List<ReportAdminDTO> reportDTOs = reportEntities.stream()
                .map(this::toAdminReportDTO)
                .toList();

        List<BalanceHistoryAdminDTO> balanceHistoryDTOs = balanceHistoryEntities.stream()
                .map(this::toAdminBalanceHistoryDTO)
                .toList();

        // 4) 최종 UserDetailAdminDTO 구성
        UserDetailAdminDTO detailDTO = new UserDetailAdminDTO();
        detailDTO.setUser(userDTO);
        detailDTO.setPosts(postDTOs);
        detailDTO.setMeetings(meetingDTOs);
        detailDTO.setBlockedUsers(blockedUserDTOs);
        detailDTO.setChatRooms(chatRoomDTOs);
        detailDTO.setSanctions(sanctionDTOs);
        detailDTO.setCallCenters(callCenterDTOs);
        detailDTO.setReports(reportDTOs);
        detailDTO.setBalanceHistories(balanceHistoryDTOs);

        return detailDTO;
    }

    // 아래부터 엔티티→DTO 변환 함수 (간단 예시)
    private UserAdminDTO toAdminUserDTO(User user) {
        UserAdminDTO dto = new UserAdminDTO();
        dto.setId(user.getId());
        dto.setExternalUserId(user.getExternalUserId());
        dto.setNickname(user.getNickname());
        dto.setGender(user.getGender());
        dto.setCandy(user.getCandy());
        dto.setAge(user.getAge());
        dto.setProfileImageUrl(user.getProfileImageUrl());
        return dto;
    }

    private PostAdminDTO toAdminPostDTO(Post post) {
        PostAdminDTO dto = new PostAdminDTO();
        dto.setId(post.getId());
        dto.setContent(post.getContent());
        dto.setImageUrl(post.getImageUrl());
        dto.setCreatedAt(post.getCreatedAt());
        return dto;
    }

    private MeetingAdminDTO toAdminMeetingDTO(Meeting meeting) {
        MeetingAdminDTO dto = new MeetingAdminDTO();
        dto.setId(meeting.getId());
        dto.setExternalUserId(meeting.getExternalUserId());
        dto.setTitle(meeting.getTitle());
        dto.setDescription(meeting.getDescription());
        dto.setTopic(meeting.getTopic());
        dto.setLocation(meeting.getLocation());
        dto.setWriterNickname(meeting.getWriterNickname());
        dto.setMaxParticipants(meeting.getMaxParticipants());
        dto.setCreatedAt(meeting.getCreatedAt());
        return dto;
    }

    private BlockedUserAdminDTO toAdminBlockedUserDTO(BlockedUser blockedUser) {
        BlockedUserAdminDTO dto = new BlockedUserAdminDTO();
        dto.setId(blockedUser.getId());
        dto.setBlockerExternalUserId(blockedUser.getBlockerExternalUserId());
        dto.setBlockedExternalUserId(blockedUser.getBlockedExternalUserId());
        return dto;
    }

    private UserChatRoomAdminDTO toAdminUserChatRoomDTO(UserChatRooms room) {
        UserChatRoomAdminDTO dto = new UserChatRoomAdminDTO();
        dto.setId(room.getId());
        dto.setRoomId(room.getRoomId());
        dto.setCreatedAt(room.getCreatedAt());
        dto.setUpdatedAt(room.getUpdatedAt());
        dto.setLastDisconnectAt(room.getLastDisconnectAt());
        return dto;
    }

    private SanctionAdminDTO toAdminSanctionDTO(Sanction sanc) {
        SanctionAdminDTO dto = new SanctionAdminDTO();
        dto.setId(sanc.getId());
        dto.setUserId(sanc.getUserId());
        dto.setReason(sanc.getReason());
        dto.setStartTime(sanc.getStartTime());
        dto.setEndTime(sanc.getEndTime());
        dto.setCreatedAt(sanc.getCreatedAt());
        return dto;
    }

    private CallCenterAdminDTO toAdminCallCenterDTO(CallCenter cc) {
        CallCenterAdminDTO dto = new CallCenterAdminDTO();
        dto.setId(cc.getId());
        dto.setCallCenterContent(cc.getCallCenterContent());
        dto.setCalledAt(cc.getCalledAt());
        dto.setCallerUserId(cc.getCallerUserId());
        dto.setExternalUserId(cc.getExternalUserId());
        dto.setCategory(cc.getCategory());
        return dto;
    }

    private ReportAdminDTO toAdminReportDTO(Report rpt) {
        ReportAdminDTO dto = new ReportAdminDTO();
        dto.setId(rpt.getId());
        dto.setReportedUserId(rpt.getReportedUserId());
        dto.setReportedExternalUserId(rpt.getReportedExternalUserId());
        dto.setReportContent(rpt.getReportContent());
        dto.setPostId(rpt.getPostId());
        dto.setRoomId(rpt.getRoomId());
        dto.setReportedAt(rpt.getReportedAt());
        dto.setStatus(rpt.getStatus());
        return dto;
    }

    private BalanceHistoryAdminDTO toAdminBalanceHistoryDTO(BalanceHistory bh) {
        BalanceHistoryAdminDTO dto = new BalanceHistoryAdminDTO();
        dto.setId(bh.getId());
        dto.setUserId(bh.getUserId());
        dto.setExternalUserId(bh.getExternalUserId());
        dto.setChangeType(bh.getChangeType());
        dto.setAmount(bh.getAmount());
        dto.setBalanceBefore(bh.getBalanceBefore());
        dto.setBalanceAfter(bh.getBalanceAfter());
        dto.setRelatedTransactionId(bh.getRelatedTransactionId());
        dto.setCreateAt(bh.getCreateAt());
        return dto;
    }

    public List<SimpleUserDTO> getAllUsersSimple() {
        // User 엔티티 전체 조회 -> SimpleUserDTO로 변환
        return userRepository.findAll().stream()
                .map(u -> new SimpleUserDTO(u.getId(), u.getNickname(), u.getProfileImageUrl(), u.getProfileImageUrl2(), u.getProfileImageUrl3(), u.getCandy()))
                .collect(Collectors.toList());
    }
    /**
     * 공지 전체 조회 (최신 순)
     */
    public List<Notice> getAllNotices() {
        return noticeRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * 공지 등록
     */
    public Notice createNotice(String title, String content) {
        Notice notice = Notice.builder()
                .title(title)
                .content(content)
                .createdAt(LocalDateTime.now(ZoneId.of("Asia/Seoul"))) // 서버에서 작성일 설정
                .build();

        return noticeRepository.save(notice);
    }

    @Transactional(readOnly = true)
    public List<SimpleUserDTO> getUsersSimpleByNickname(String keyword) {
        return userRepository.findTop100ByNicknameContainingIgnoreCase(keyword).stream()
                .map(u -> new SimpleUserDTO(
                        u.getId(),
                        u.getNickname(),
                        u.getProfileImageUrl(),
                        u.getProfileImageUrl2(),
                        u.getProfileImageUrl3(),
                        u.getCandy()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public UserDetailAdminDTO getUserDetailByNickname(String nickname) {
        User user = userRepository.findFirstByNickname(nickname).orElse(null);
        if (user == null) return null;
        // 이미 잘 동작하는 기존 상세조회를 그대로 재사용
        return getUserDetail(user.getId(), null);
    }

    @Transactional(readOnly = true)
    public List<PostAdminDTO> searchPostsByContent(String keyword) {
        return postRepository
                .findTop200ByContentContainingIgnoreCaseOrderByCreatedAtDesc(keyword)
                .stream()
                .map(this::toAdminPostDTO) // 기존 변환 재사용
                .toList();
    }

    @Transactional
    public boolean deletePostById(Long postId) {
        // 1) 엔티티 조회
        Post post = postRepository.findById(postId).orElse(null);
        if (post == null) {
            return false;
        }

        // 2) S3 이미지 삭제 (있으면)
        safelyDeletePostImages(post);

        // ⚠ 필요 시 연관 데이터(댓글/신고/좋아요 등) 선삭제 or DB에서 ON DELETE CASCADE 보장
        // ex) commentRepository.deleteByPostId(postId); reportRepository.deleteByPostId(postId); ...

        // 3) 게시글 삭제
        postRepository.delete(post);
        return true;
    }

    /**
     * 게시글에 연결된 이미지들을 S3에서 안전하게 삭제한다.
     * 이미지 필드가 하나(imageUrl)만 있으면 그 하나만 처리하고,
     * 여러 개(imageUrl2, imageUrl3...)가 있으면 아래처럼 확장해도 된다.
     */
    private void safelyDeletePostImages(Post post) {
        // 필드가 하나만 있다면:
        if (post.getImageUrl() != null && !post.getImageUrl().isBlank()) {
            try {
                s3Uploader.deletePostImage(post.getImageUrl());
            } catch (Exception e) {
                // S3 삭제 실패시 로깅만 하고 계속 진행
                System.err.println("❌ S3 이미지 삭제 실패: " + post.getImageUrl());
                e.printStackTrace();
            }
        }
    }

    @Transactional
    public boolean deleteAllProfileImagesByUserId(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return false;

        String extId = user.getExternalUserId();
        if (extId == null || extId.isEmpty()) return false;

        // S3에서 1/2/3 모두 삭제
        try {
            // 헬퍼 사용
            s3Uploader.deleteAllProfileImagesByExternalUserId(extId);
        } catch (Exception e) {
            System.err.println("❌ S3 전체 프로필 삭제 중 오류: " + e.getMessage());
        }

        // DB 필드 초기화 (기본 이미지로 되돌리거나 null 처리)
        // 기본 이미지 정책에 맞춰 선택:
        // 1) 남/여 기본 이미지로 초기화
        String defaultMale = "../assets/images/men.png";
        String defaultFemale = "../assets/images/women.png";
        String fallback = (user.getGender()!=null && user.getGender().equalsIgnoreCase("female"))
                ? defaultFemale : defaultMale;

        user.setProfileImageUrl(fallback);
        user.setProfileImageUrl2(fallback);
        user.setProfileImageUrl3(fallback);

        userRepository.save(user);
        return true;
    }
}
