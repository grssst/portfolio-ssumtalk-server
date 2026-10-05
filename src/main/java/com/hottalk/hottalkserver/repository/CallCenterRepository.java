package com.hottalk.hottalkserver.repository;

import com.hottalk.hottalkserver.model.CallCenter;
import com.hottalk.hottalkserver.model.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CallCenterRepository extends JpaRepository<CallCenter, Long> {

    // 1) 전체 (calledAt 기준 오름차순, 내림차순)
    List<CallCenter> findAllByOrderByCalledAtAsc();
    List<CallCenter> findAllByOrderByCalledAtDesc();

    // 2) 특정 카테고리 (calledAt 기준 오름차순, 내림차순)
    List<CallCenter> findByCategoryOrderByCalledAtAsc(String category);
    List<CallCenter> findByCategoryOrderByCalledAtDesc(String category);

    List<CallCenter> findByExternalUserId(String finalExtUserId);
}
