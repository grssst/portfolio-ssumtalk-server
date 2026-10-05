package com.hottalk.hottalkserver.repository;

import com.hottalk.hottalkserver.model.BalanceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface BalanceHistoryRepository extends JpaRepository<BalanceHistory, Long> {
    @Query("SELECT COUNT(b) FROM BalanceHistory b WHERE b.changeType = 'purchase'")
    Long countPurchase();

    @Query("SELECT b.amount FROM BalanceHistory b WHERE b.changeType = 'purchase'")
    List<Long> sumKRW();

    // 특정 날짜 범위 내 'purchase' 타입 레코드 개수 조회
    @Query("SELECT COUNT(b) FROM BalanceHistory b WHERE b.changeType = 'purchase' AND b.createAt BETWEEN :startDate AND :endDate")
    Long countPurchaseInRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // 특정 날짜 범위 내 'purchase' 타입 금액 조회
    @Query("SELECT b.amount FROM BalanceHistory b WHERE b.changeType = 'purchase' AND b.createAt BETWEEN :startDate AND :endDate")
    List<Long> sumKRWInRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    List<BalanceHistory> findByExternalUserId(String finalExtUserId);
}
