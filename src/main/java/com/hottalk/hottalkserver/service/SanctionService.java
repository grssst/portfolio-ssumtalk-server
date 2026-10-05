package com.hottalk.hottalkserver.service;

import com.hottalk.hottalkserver.model.Sanction;
import com.hottalk.hottalkserver.repository.SanctionRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

@Service
public class SanctionService {

    @Autowired
    private SanctionRepository sanctionRepository;

    // 제재 적용
    public Sanction applySanction(Long userId, String reason, LocalDateTime startTime, LocalDateTime endTime) {
        Sanction sanction = new Sanction(userId, reason, startTime, endTime);
        return sanctionRepository.save(sanction);
    }

    // 제재 해제
    @Transactional
    public void releaseSanction(Long userId) {
        sanctionRepository.deleteByUserId(userId);
    }

    // 특정 사용자가 제재 중인지 확인
    public Optional<Sanction> getSanction(Long userId) {
        return sanctionRepository.findByUserId(userId);
    }

    // 제재 기간 연장/변경
    public Sanction extendSanction(Long userId, LocalDateTime newEndTime) {
        Optional<Sanction> optionalSanction = sanctionRepository.findByUserId(userId);
        if (optionalSanction.isPresent()) {
            Sanction sanction = optionalSanction.get();
            sanction.setEndTime(newEndTime);
            return sanctionRepository.save(sanction);
        }
        throw new RuntimeException("Sanction not found for user ID: " + userId);
    }
    // 제재 상태 확인
    public Optional<Sanction> checkSanctionStatusByUserId(Long userId) {
        return sanctionRepository.findByUserId(userId)
                .filter(s -> s.getEndTime() == null || s.getEndTime().isAfter(LocalDateTime.now(ZoneId.of("Asia/Seoul"))));
    }
    // 특정 사용자의 가장 최근 제재 조회
    public Optional<Sanction> getRecentSanction(Long userId) {
        return sanctionRepository.findTopByUserIdOrderByIdDesc(userId);
    }
}
