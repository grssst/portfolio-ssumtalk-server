package com.hottalk.hottalkserver.repository;

import com.hottalk.hottalkserver.model.FcmToken;
import com.hottalk.hottalkserver.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {
    Optional<FcmToken> findByFcmToken(String fcmToken);

    Optional<FcmToken> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}

