package com.hottalk.hottalkserver.repository;

import com.hottalk.hottalkserver.model.UserChatRooms;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserChatRoomsRepository extends JpaRepository<UserChatRooms, Long> {
    List<UserChatRooms> findByUserId(Long userId);  // 특정 사용자가 참여 중인 채팅방 조회
    List<UserChatRooms> findByRoomId(String roomId);  // 특정 채팅방의 사용자 목록 조회
    void deleteByUserIdAndRoomId(Long userId, String roomId);  // 특정 사용자의 채팅방 참여 정보 삭제
    boolean existsByUserIdAndRoomId(Long userId, String roomId);  // 특정 사용자가 채팅방에 있는지 확인
    List<UserChatRooms> findAllByUserId(Long userId);

    Optional<UserChatRooms> findByUserIdAndRoomId(Long userId, String roomId);

    @Query(value = "SELECT room_id FROM user_chat_rooms " +
            "WHERE room_id LIKE CONCAT('meeting_', :meetingId, '_%') " +
            "ORDER BY created_at ASC LIMIT 1", nativeQuery = true)
    String findOldestRoomIdByMeetingId(@Param("meetingId") Long meetingId);

    void deleteByRoomIdContains(String roomPrefix); // ✅ meeting_31이 포함된 모든 row 삭제
    void deleteByRoomId(String roomId);

    List<UserChatRooms> findByRoomIdStartingWith(String prefix);

    @Transactional
    @Modifying
    void deleteByRoomIdStartingWith(String prefix);
}
