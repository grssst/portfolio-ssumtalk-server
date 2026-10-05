package com.hottalk.hottalkserver.repository;


import com.hottalk.hottalkserver.model.Board;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {
    // 기본 CRUD 메서드가 제공됩니다.
    List<Board> findByMeetingIdOrderByCreatedAtDesc(Long meetingId);
    // postId와 meetingId로 게시글을 조회
    Optional<Board> findByIdAndMeetingId(Long id, Long meetingId);

    List<Board> findByMeetingId(Long meetingId);
}
