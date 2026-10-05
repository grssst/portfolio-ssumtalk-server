package com.hottalk.hottalkserver.repository;

import com.hottalk.hottalkserver.model.BlockedUser;
import com.hottalk.hottalkserver.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BlockedUserRepository extends JpaRepository<BlockedUser, Long> {

    // 차단자와 차단당한자에 해당하는 모든 기록 삭제
    void deleteByBlockerAndBlocked(User blocker, User blocked);
    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId); // 차단 관계 확인
    boolean existsByBlockedIdAndBlockerId(Long blockedId, Long blockerId); // 역방향 차단 확인
    void deleteByBlockerExternalUserIdOrBlockedExternalUserId(String blockerExternalUserId, String blockedExternalUserId);

    boolean existsByBlockerExternalUserIdAndBlockedExternalUserId(String blockerExternalUserId, String blockedExternalUserId);

    List<BlockedUser> findAllByBlockerExternalUserIdOrBlockedExternalUserId(String finalExtUserId, String finalExtUserId1);
}
