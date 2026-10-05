package com.hottalk.hottalkserver.repository;

import com.hottalk.hottalkserver.model.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;


@Repository
public interface MeetingRepository extends JpaRepository<Meeting, Long> {

    List<Meeting> findBySubscriptionEndDateBefore(LocalDate date);
    @Query("SELECT m FROM Meeting m LEFT JOIN m.participants p " +
            "WHERE m.isActive = true " +
            "GROUP BY m " +
            "ORDER BY COUNT(p) DESC")
    List<Meeting> findByIsActiveTrueOrderByParticipantCountDesc();

    @Query("SELECT m FROM Meeting m LEFT JOIN m.participants p " +
            "WHERE m.location LIKE %:location% AND m.isActive = true " +
            "GROUP BY m " +
            "ORDER BY COUNT(p) DESC")
    List<Meeting> findByLocationContainingAndIsActiveTrueOrderByParticipantCountDesc(@Param("location") String location);

    @Query("SELECT m FROM Meeting m LEFT JOIN m.participants p " +
            "WHERE m.topic IN :topics AND m.isActive = true " +
            "GROUP BY m " +
            "ORDER BY COUNT(p) DESC")
    List<Meeting> findByTopicInAndIsActiveTrueOrderByParticipantCountDesc(@Param("topics") List<String> topics);

    @Query("SELECT m FROM Meeting m LEFT JOIN m.participants p " +
            "WHERE m.location LIKE %:location% AND m.topic IN :topics AND m.isActive = true " +
            "GROUP BY m " +
            "ORDER BY COUNT(p) DESC")
    List<Meeting> findByLocationContainingAndTopicInAndIsActiveTrueOrderByParticipantCountDesc(
            @Param("location") String location,
            @Param("topics") List<String> topics);

    // 여러 지역에 대해 정확한 일치를 IN절로 조회
    @Query("SELECT m FROM Meeting m LEFT JOIN m.participants p " +
            "WHERE m.location IN :locations AND m.isActive = true " +
            "GROUP BY m " +
            "ORDER BY COUNT(p) DESC")
    List<Meeting> findByLocationInAndIsActiveTrueOrderByParticipantCountDesc(@Param("locations") List<String> locations);

    // 여러 지역과 주제 모두 IN절로 조회
    @Query("SELECT m FROM Meeting m LEFT JOIN m.participants p " +
            "WHERE m.location IN :locations AND m.topic IN :topics AND m.isActive = true " +
            "GROUP BY m " +
            "ORDER BY COUNT(p) DESC")
    List<Meeting> findByLocationInAndTopicInAndIsActiveTrueOrderByParticipantCountDesc(@Param("locations") List<String> locations,
                                                                                       @Param("topics") List<String> topics);


    List<Meeting> findByExternalUserId(String externalUserId);

    // ✅ 외부 ID를 기준으로 writerNickname 업데이트
    @Transactional
    @Modifying
    @Query("UPDATE Meeting m SET m.writerNickname = :nickname WHERE m.externalUserId = :externalUserId")
    void updateWriterNicknameByExternalUserId(@Param("externalUserId") String externalUserId,
                                              @Param("nickname") String nickname);

    @Query("""
        SELECT p.user.id
        FROM Meeting m
        JOIN m.participants p
        WHERE m.id = :meetingId
    """)
    List<Long> findUserIdsByMeetingId(@Param("meetingId") Long meetingId);
}
