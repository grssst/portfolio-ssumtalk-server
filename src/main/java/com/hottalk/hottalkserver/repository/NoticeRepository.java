package com.hottalk.hottalkserver.repository;

import com.hottalk.hottalkserver.model.Notice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoticeRepository extends JpaRepository<Notice, Long> {
    // 최신순으로 전체 불러오기 예시 (createdAt desc)
    List<Notice> findAllByOrderByCreatedAtDesc();
}
