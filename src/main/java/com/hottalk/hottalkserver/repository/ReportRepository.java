package com.hottalk.hottalkserver.repository;

import com.hottalk.hottalkserver.model.Post;
import com.hottalk.hottalkserver.model.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    boolean existsByRoomId(String roomId);
    @Query(value = "SELECT * FROM post WHERE id IN (SELECT post_id FROM report WHERE post_id IS NOT NULL AND reported_user_id = :userId)", nativeQuery = true)
    List<Post> findReportedPostsByUserId(@Param("userId") Long userId);
    Optional<Report> findTopByRoomIdOrderByReportedAtDesc(String roomId);

    List<Report> findByReportedUserIdOrReportedExternalUserId(Long reportedUserId, String reportedExternalUserId);

    Optional<Report> findByRoomId(String roomId);

    Optional<Report> findTopByRoomIdContainingOrderByReportedAtDesc(String roomIdPart);
}
