package com.hottalk.hottalkserver.repository;

import com.hottalk.hottalkserver.model.Admin;
import com.hottalk.hottalkserver.model.Meeting;
import com.hottalk.hottalkserver.model.MeetingParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface MeetingParticipantsRepository extends JpaRepository<MeetingParticipant, Long>  {


    Optional<MeetingParticipant> findAllById(Long meetingId);

    List<MeetingParticipant> findByMeetingId(Long meetingId);

    void deleteByUserIdAndMeetingId(Long userId, Long meetingId);


    // ✅ 기존 방식은 meetingId(Long)를 받지만, Meeting 객체를 받도록 수정해야 함
    // ✅ meetingId가 아니라 Meeting 객체를 기반으로 삭제
    void deleteByMeeting(Meeting meeting);

    void deleteByUserId(Long userId);

    // 이렇게 리턴 타입을 Optional<MeetingParticipant>로 해야 함
    // "user"가 @ManyToOne으로 되어 있으므로 user.xxx와 이어짐
    Optional<MeetingParticipant> findByMeetingIdAndUser_ExternalUserId(Long meetingId, String externalUserId);

    @Query("""
    SELECT mp
    FROM MeetingParticipant mp
    JOIN FETCH mp.meeting m
    WHERE mp.user.id = :userId
""")
    List<MeetingParticipant> findByUserIdWithMeeting(@Param("userId") Long userId);

    List<MeetingParticipant> findByUser_Id(Long userId);
}
