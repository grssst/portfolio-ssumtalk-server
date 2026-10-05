package com.hottalk.hottalkserver.repository;

import com.hottalk.hottalkserver.model.Sanction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SanctionRepository extends JpaRepository<Sanction, Long> {
    Optional<Sanction> findByUserId(Long userId);
    void deleteByUserId(Long userId);  // 제재 해제를 위해 사용자 ID로 삭제
    Optional<Sanction> findTopByUserIdOrderByIdDesc(Long userId);

    List<Sanction> findByUserIdOrderByIdDesc(Long userId);
}
