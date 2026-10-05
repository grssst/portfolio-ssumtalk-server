package com.hottalk.hottalkserver.repository;


import com.hottalk.hottalkserver.model.User;
import com.hottalk.hottalkserver.model.UserTerms;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserTermsRepository extends JpaRepository<UserTerms, Long>{
    void deleteByExternalUserId(String externalUserId);

    // ✅ userId를 기준으로 UserTerms를 조회하는 쿼리 메서드
    Optional<UserTerms> findByExternalUserId(String externalUserId);
}
