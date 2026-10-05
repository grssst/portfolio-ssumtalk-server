package com.hottalk.hottalkserver.repository;

import com.hottalk.hottalkserver.model.Post;
import com.hottalk.hottalkserver.model.User;
import org.locationtech.jts.geom.Point;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.core.parameters.P;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByExternalUserId(String externalUserId); // Optional로 변경
    @Query("SELECT profileImageUrl, profileImageUrl2, profileImageUrl3 FROM User WHERE externalUserId = :externalUserId")
    List<Object[]> findAllProfileImageUrlsByExternalUserId(@Param("externalUserId") String externalUserId);



    @Query(value = "SELECT * FROM users WHERE external_user_id IN (?1, ?2)", nativeQuery = true)
    List<User> findBothUsers(String externalUserId, String myExternalUserId);


    @Query(value = "SELECT * FROM users u " +
            "WHERE u.age BETWEEN :ageRange1 AND :ageRange2 " +
            "AND NOT EXISTS (" +
            "    SELECT 1 FROM blocked_users bu " +
            "    WHERE (bu.blocker_external_user_id = :externalUserId AND bu.blocked_external_user_id = u.external_user_id) " +
            "       OR (bu.blocked_external_user_id = :externalUserId AND bu.blocker_external_user_id = u.external_user_id)) " +
            "ORDER BY u.last_login DESC " +
            "LIMIT 100",
            nativeQuery = true)
    List<User> findTop100ByOrderByLastLoginDesc(@Param("externalUserId") String externalUserId,
                                                @Param("ageRange1") Integer ageRange1,
                                                @Param("ageRange2") Integer ageRange2);


    @Query(value = "SELECT * FROM users u " +
            "WHERE u.age BETWEEN :ageRange1 AND :ageRange2 " +
            "AND u.gender = :gender AND NOT EXISTS (" +
            "    SELECT 1 FROM blocked_users bu " +
            "    WHERE (bu.blocker_external_user_id = :externalUserId AND bu.blocked_external_user_id = u.external_user_id) " +
            "       OR (bu.blocked_external_user_id = :externalUserId AND bu.blocker_external_user_id = u.external_user_id)) " +
            "ORDER BY u.last_login DESC " +
            "LIMIT 100",
            nativeQuery = true)
    List<User> findTop100ByGenderOrderByLastLoginDesc(@Param("gender") String gender, @Param("externalUserId") String externalUserId,
                                                      @Param("ageRange1") Integer ageRange1,
                                                      @Param("ageRange2") Integer ageRange2);


    @Query(value = "SELECT * FROM users u " +
            "WHERE u.age BETWEEN :ageRange1 AND :ageRange2 " +
            "AND u.last_login < :lastLogin AND NOT EXISTS (" +
            "    SELECT 1 FROM blocked_users bu " +
            "    WHERE (bu.blocker_external_user_id = :externalUserId AND bu.blocked_external_user_id = u.external_user_id) " +
            "       OR (bu.blocked_external_user_id = :externalUserId AND bu.blocker_external_user_id = u.external_user_id)) " +
            "ORDER BY u.last_login DESC " +
            "LIMIT 100",
            nativeQuery = true)
    List<User> findTop100ByLastLoginBeforeOrderByLastLoginDesc(@Param("lastLogin") LocalDateTime lastLogin, @Param("externalUserId") String externalUserId,
                                                               @Param("ageRange1") Integer ageRange1,
                                                               @Param("ageRange2") Integer ageRange2);


    @Query(value = "SELECT * FROM users u " +
            "WHERE u.last_login < :lastLogin " +
            "AND u.age BETWEEN :ageRange1 AND :ageRange2 " +
            "AND u.gender = :gender " +
            "AND NOT EXISTS (" +
            "    SELECT 1 FROM blocked_users bu " +
            "    WHERE (bu.blocker_external_user_id = :externalUserId AND bu.blocked_external_user_id = u.external_user_id) " +
            "       OR (bu.blocked_external_user_id = :externalUserId AND bu.blocker_external_user_id = u.external_user_id)) " +
            "ORDER BY u.last_login DESC " +
            "LIMIT 100",
            nativeQuery = true)
    List<User> findTop100ByGenderAndLastLoginBeforeOrderByLastLoginDesc(@Param("gender") String gender, @Param("lastLogin") LocalDateTime lastLogin,
                                                                        @Param("externalUserId") String externalUserId,
                                                                        @Param("ageRange1") Integer ageRange1,
                                                                        @Param("ageRange2") Integer ageRange2);






    @Query(value = "SELECT u.*, ST_Distance_Sphere(:myLocation, u.location) AS distance " +
            "FROM users u " +
            "WHERE MBRContains(ST_GeomFromText(:boundingBox, 4326), u.location) " +
            "AND u.age BETWEEN :ageRange1 AND :ageRange2 " +
            "AND NOT EXISTS (" +
            "    SELECT 1 FROM blocked_users bu " +
            "    WHERE (bu.blocker_external_user_id = :externalUserId AND bu.blocked_external_user_id = u.external_user_id) " +
            "       OR (bu.blocked_external_user_id = :externalUserId AND bu.blocker_external_user_id = u.external_user_id)) " +
            "ORDER BY distance ASC " +
            "LIMIT 100",
            nativeQuery = true)
    List<User> findUsersWithinBoundingBox(
            @Param("boundingBox") String boundingBox,
            @Param("myLocation") Point myLocation,
            @Param("externalUserId") String externalUserId,
            @Param("ageRange1") Integer ageRange1,
            @Param("ageRange2") Integer ageRange2
    );



    @Query(value = "SELECT u.*, ST_Distance_Sphere(:myLocation, u.location) AS distance " +
            "FROM users u " +
            "WHERE MBRContains(ST_GeomFromText(:boundingBox, 4326), u.location) " +
            "AND u.age BETWEEN :ageRange1 AND :ageRange2 " +
            "AND NOT EXISTS (" +
            "    SELECT 1 FROM blocked_users bu " +
            "    WHERE (bu.blocker_external_user_id = :externalUserId AND bu.blocked_external_user_id = u.external_user_id) " +
            "       OR (bu.blocked_external_user_id = :externalUserId AND bu.blocker_external_user_id = u.external_user_id)) " +
            "AND u.gender = :gender " +
            "ORDER BY distance ASC " +
            "LIMIT 100",
            nativeQuery = true)
    List<User> findUsersWithinBoundingBoxAndGender(@Param("boundingBox") String boundingBox, @Param("gender") String gender,
                                                   @Param("myLocation") Point myLocation, @Param("externalUserId") String externalUserId,
                                                   @Param("ageRange1") Integer ageRange1,
                                                   @Param("ageRange2") Integer ageRange2);


    @Query(value = "SELECT u.*, ST_Distance_Sphere(:myLocation, u.location) / 1000 AS distance " + // 미터 → 킬로미터 변환
            "FROM users u " +
            "WHERE MBRContains(ST_GeomFromText(:boundingBox, 4326), u.location) " +
            "AND u.age BETWEEN :ageRange1 AND :ageRange2 " +
            "AND NOT EXISTS (" +
            "    SELECT 1 FROM blocked_users bu " +
            "    WHERE (bu.blocker_external_user_id = :externalUserId AND bu.blocked_external_user_id = u.external_user_id) " +
            "       OR (bu.blocked_external_user_id = :externalUserId AND bu.blocker_external_user_id = u.external_user_id)) " +
            "AND ST_Distance_Sphere(:myLocation, u.location) / 1000 > :lastDistance " + // 킬로미터 단위 비교
            "ORDER BY distance ASC " +
            "LIMIT 100",
            nativeQuery = true)
    List<User> findUsersWithinBoundingBoxAndMinDistance(
            @Param("boundingBox") String boundingBox,
            @Param("myLocation") Point myLocation,
            @Param("lastDistance") double lastDistance, // 킬로미터 단위
            @Param("externalUserId") String externalUserId,
            @Param("ageRange1") Integer ageRange1,
            @Param("ageRange2") Integer ageRange2
    );

    @Query(value = "SELECT u.*, ST_Distance_Sphere(:myLocation, u.location) / 1000 AS distance " + // 미터 → 킬로미터 변환
            "FROM users u " +
            "WHERE MBRContains(ST_GeomFromText(:boundingBox, 4326), u.location) " +
            "AND u.age BETWEEN :ageRange1 AND :ageRange2 " +
            "AND NOT EXISTS (" +
            "    SELECT 1 FROM blocked_users bu " +
            "    WHERE (bu.blocker_external_user_id = :externalUserId AND bu.blocked_external_user_id = u.external_user_id) " +
            "       OR (bu.blocked_external_user_id = :externalUserId AND bu.blocker_external_user_id = u.external_user_id)) " +
            "AND u.gender = :gender " +
            "AND ST_Distance_Sphere(:myLocation, u.location) / 1000 > :lastDistance " + // 킬로미터 단위 비교
            "ORDER BY distance ASC " +
            "LIMIT 100",
            nativeQuery = true)
    List<User> findUsersWithinBoundingBoxAndGenderAndMinDistance(
            @Param("boundingBox") String boundingBox,
            @Param("gender") String gender,
            @Param("myLocation") Point myLocation,
            @Param("lastDistance") double lastDistance, // 킬로미터 단위
            @Param("externalUserId") String externalUserId,
            @Param("ageRange1") Integer ageRange1,
            @Param("ageRange2") Integer ageRange2
    );

    @Query("SELECT u.profileImageUrl FROM User u WHERE u.externalUserId = :externalUserId")
    String findProfileImageUrlByExternalUserId(@Param("externalUserId") String externalUserId);

    @Modifying
    @Query("UPDATE User u SET u.candy = u.candy - :amount WHERE u.id = :userId AND u.candy >= :amount")
    int decreaseCandy(@Param("userId") Long userId, @Param("amount") int amount);

    @Query("SELECT COUNT(u) FROM User u")
    Long countUsers();

    // 주어진 기간 동안의 로그인 수를 조회하는 재사용 가능한 쿼리
    @Query("SELECT COUNT(u) FROM User u WHERE u.lastLogin BETWEEN :start AND :end")
    long countLoginsBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    Long findIdByExternalUserId(String externalUserId);

    // 부분 일치로 최대 100명 (목록용)
    List<User> findTop100ByNicknameContainingIgnoreCase(String keyword);

    // 정확 일치 1명 (상세용 - 필요시)
    Optional<User> findFirstByNickname(String nickname);
}
