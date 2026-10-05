package com.hottalk.hottalkserver.repository;


import com.hottalk.hottalkserver.model.GalleryImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GalleryImageRepository extends JpaRepository<GalleryImage, Long> {
    List<GalleryImage> findByMeetingIdOrderByCreatedAtDesc(String meetingId);

    List<GalleryImage> findByMeetingId(String meetingId);
    // 기본 CRUD 메서드 사용
}
