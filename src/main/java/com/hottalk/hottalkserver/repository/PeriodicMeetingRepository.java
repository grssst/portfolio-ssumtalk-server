package com.hottalk.hottalkserver.repository;

import com.hottalk.hottalkserver.model.Meeting;
import com.hottalk.hottalkserver.model.PeriodicMeeting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PeriodicMeetingRepository extends JpaRepository<PeriodicMeeting, Long> {
    List<PeriodicMeeting> findByMeeting(Meeting meeting);
    // 추가로 필요한 조회 메서드가 있다면 여기에 정의

    // meetingId와 periodicId로 정기모임을 조회
    Optional<PeriodicMeeting> findByIdAndMeetingId(Long id, Long meetingId);

    List<PeriodicMeeting> findByMeetingId(Long meetingId);
}
