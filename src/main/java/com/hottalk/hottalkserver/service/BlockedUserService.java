package com.hottalk.hottalkserver.service;

import com.hottalk.hottalkserver.model.BlockedUser;
import com.hottalk.hottalkserver.model.User;
import com.hottalk.hottalkserver.repository.BlockedUserRepository;
import com.hottalk.hottalkserver.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BlockedUserService {

    @Autowired
    private BlockedUserRepository blockedUserRepository;
    @Autowired
    private UserRepository userRepository;

    // 차단 등록
    public BlockedUser blockUser(String blockerExternalUserId, String blockedExternalUserId) {
        Optional<User> blockerOptional = userRepository.findByExternalUserId(blockerExternalUserId);
        Optional<User> blockedOptional = userRepository.findByExternalUserId(blockedExternalUserId);

        // Optional에서 User 객체를 추출하여 존재하지 않으면 예외를 발생시킴
        User blocker = blockerOptional.orElseThrow(() -> new IllegalArgumentException("Blocker user not found"));
        User blocked = blockedOptional.orElseThrow(() -> new IllegalArgumentException("Blocked user not found"));

        BlockedUser blockedUser = new BlockedUser(blocker, blocked, blockerExternalUserId, blockedExternalUserId);
        return blockedUserRepository.save(blockedUser);
    }

    @Transactional
    public BlockedUser blockUserChat(String blockerExternalUserId, String blockedUserId) {
        // 1) User 엔티티 조회
        User blocker = userRepository.findByExternalUserId(blockerExternalUserId)
                .orElseThrow(() -> new IllegalArgumentException("Blocker user not found"));
        User blocked = userRepository.findById(Long.valueOf(blockedUserId))
                .orElseThrow(() -> new IllegalArgumentException("Blocked user not found"));

        // 2) BlockedUser 엔티티 생성 (빈 생성자 + setter 사용)
        BlockedUser bu = new BlockedUser();
        bu.setBlocker(blocker);
        bu.setBlocked(blocked);

        // 3) **문자열 필드도 반드시 채워주기**
        bu.setBlockerExternalUserId(blocker.getExternalUserId());
        bu.setBlockedExternalUserId(blocked.getExternalUserId());

        // 4) 저장
        return blockedUserRepository.save(bu);
    }


    // 특정 사용자가 차단한 사용자 목록 반환 (차단한 사용자의 상세 정보 포함)
    @Transactional
    public List<User> getBlockedUsersDetails(String blockerExternalUserId) {
        Optional<User> blockerOptional = userRepository.findByExternalUserId(blockerExternalUserId);

        // Optional에서 User 객체를 추출하여 존재하지 않으면 예외를 발생시킴
        User blocker = blockerOptional.orElseThrow(() -> new IllegalArgumentException("Blocker user not found"));

        return blocker.getBlockedUsers().stream()
                .map(BlockedUser::getBlocked)
                .collect(Collectors.toList());
    }

    // 차단 해제 (특정 차단자와 차단당한자 간의 모든 차단 기록 삭제)
    @Transactional
    public void unblockUser(String blockerExternalUserId, String blockedExternalUserId) {
        Optional<User> blockerOptional = userRepository.findByExternalUserId(blockerExternalUserId);
        Optional<User> blockedOptional = userRepository.findByExternalUserId(blockedExternalUserId);

        // Optional에서 User 객체를 추출하여 존재하지 않으면 예외를 발생시킴
        User blocker = blockerOptional.orElseThrow(() -> new IllegalArgumentException("Blocker user not found with ID: " + blockerExternalUserId));
        User blocked = blockedOptional.orElseThrow(() -> new IllegalArgumentException("Blocked user not found with ID: " + blockedExternalUserId));

        // 차단자와 차단당한자 간의 모든 기록 삭제
        blockedUserRepository.deleteByBlockerAndBlocked(blocker, blocked);
    }

    public boolean isBlocked(Long userId1, Long userId2) {
        // userId1이 userId2를 차단했거나 userId2가 userId1을 차단했는지 확인
        return blockedUserRepository.existsByBlockerIdAndBlockedId(userId1, userId2)
                || blockedUserRepository.existsByBlockedIdAndBlockerId(userId1, userId2);
    }

}