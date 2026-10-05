package com.hottalk.hottalkserver.repository;

import com.hottalk.hottalkserver.model.Post;
import com.hottalk.hottalkserver.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {
    @Modifying
    @Transactional
    @Query(value = "UPDATE posts SET nickname = :nickname, gender = :gender, age = :age, profile_image_url = :profileImageUrl, status = :status, my_location = :myLocation, location = :location WHERE external_user_id = :externalUserId", nativeQuery = true)
    int updatePostUserInfoByExternalUserId(String externalUserId, String nickname, String gender, Integer age, String profileImageUrl, String status, String myLocation, Point location);




    @Query(value = "SELECT p.*, ST_Distance_Sphere(:myLocation, p.location) AS distance " +
            "FROM posts p " +
            "WHERE MBRContains(ST_GeomFromText(:boundingBox, 4326), p.location) " +
            "AND p.age BETWEEN :ageRange1 AND :ageRange2 " +
            "AND NOT EXISTS (" +
            "    SELECT 1 FROM blocked_users bu " +
            "    WHERE (bu.blocker_external_user_id = :externalUserId AND bu.blocked_external_user_id = p.external_user_id) " +
            "       OR (bu.blocked_external_user_id = :externalUserId AND bu.blocker_external_user_id = p.external_user_id)) " +
            "ORDER BY distance ASC " +
            "LIMIT 100",
            nativeQuery = true)
    List<Post> findPostsWithinBoundingBox(
            @Param("boundingBox") String boundingBox,
            @Param("myLocation") Point myLocation,
            @Param("externalUserId") String externalUserId,
            @Param("ageRange1") Integer ageRange1,
            @Param("ageRange2") Integer ageRange2
    );



    @Query(value = "SELECT p.*, ST_Distance_Sphere(:myLocation, p.location) AS distance " +
            "FROM posts p " +
            "WHERE MBRContains(ST_GeomFromText(:boundingBox, 4326), p.location) " +
            "AND p.age BETWEEN :ageRange1 AND :ageRange2 " +
            "AND NOT EXISTS (" +
            "    SELECT 1 FROM blocked_users bu " +
            "    WHERE (bu.blocker_external_user_id = :externalUserId AND bu.blocked_external_user_id = p.external_user_id) " +
            "       OR (bu.blocked_external_user_id = :externalUserId AND bu.blocker_external_user_id = p.external_user_id)) " +
            "AND p.gender = :gender " +
            "ORDER BY distance ASC " +
            "LIMIT 70",
            nativeQuery = true)
    List<Post> findPostsWithinBoundingBoxAndGender(@Param("boundingBox") String boundingBox, @Param("gender") String gender,
                                                   @Param("myLocation") Point myLocation, @Param("externalUserId") String externalUserId,
                                                   @Param("ageRange1") Integer ageRange1,
                                                   @Param("ageRange2") Integer ageRange2
                                                   );



    @Query(value = "SELECT * FROM posts p " +
            "WHERE p.age BETWEEN :ageRange1 AND :ageRange2 " +
            "AND NOT EXISTS (" +
            "    SELECT 1 FROM blocked_users bu " +
            "    WHERE (bu.blocker_external_user_id = :externalUserId AND bu.blocked_external_user_id = p.external_user_id) " +
            "       OR (bu.blocked_external_user_id = :externalUserId AND bu.blocker_external_user_id = p.external_user_id)) " +
            "ORDER BY p.id DESC LIMIT 70",
            nativeQuery = true)
    List<Post> findTop70ByAgeRangeAndOrderByIdDesc(@Param("externalUserId") String externalUserId, @Param("ageRange1") Integer ageRange1, @Param("ageRange2") Integer ageRange2);

    @Query(value = "SELECT * FROM posts p " +
            "WHERE p.gender = :gender " +
            "AND p.age BETWEEN :ageRange1 AND :ageRange2 " +
            "AND NOT EXISTS (" +
            "    SELECT 1 FROM blocked_users bu " +
            "    WHERE (bu.blocker_external_user_id = :externalUserId AND bu.blocked_external_user_id = p.external_user_id) " +
            "       OR (bu.blocked_external_user_id = :externalUserId AND bu.blocker_external_user_id = p.external_user_id)) " +
            "ORDER BY p.id DESC LIMIT 70",
            nativeQuery = true)
    List<Post> findTop70ByGenderAndAgeRangeAndOrderByIdDesc(@Param("gender") String gender, @Param("externalUserId") String externalUserId, @Param("ageRange1") Integer ageRange1, @Param("ageRange2") Integer ageRange2);

    @Query(value = "SELECT * FROM posts p " +
            "WHERE p.id < :lastPostId " +
            "AND p.age BETWEEN :ageRange1 AND :ageRange2 " +
            "AND NOT EXISTS (" +
            "    SELECT 1 FROM blocked_users bu " +
            "    WHERE (bu.blocker_external_user_id = :externalUserId AND bu.blocked_external_user_id = p.external_user_id) " +
            "       OR (bu.blocked_external_user_id = :externalUserId AND bu.blocker_external_user_id = p.external_user_id)) " +
            "ORDER BY p.id DESC " +
            "LIMIT 70",
            nativeQuery = true)
    List<Post> findTop70ByCreatedAtAndIdBeforeOrderByIdDesc(
                                                                   @Param("externalUserId") String externalUserId,
                                                                   @Param("lastPostId") long lastPostId,
                                                                   @Param("ageRange1") Integer ageRange1,
                                                                   @Param("ageRange2") Integer ageRange2);


    @Query(value = "SELECT * FROM posts p " +
            "WHERE p.gender = :gender " +
            "AND p.age BETWEEN :ageRange1 AND :ageRange2 " +
            "AND p.id < :lastPostId " +
            "AND NOT EXISTS (" +
            "    SELECT 1 FROM blocked_users bu " +
            "    WHERE (bu.blocker_external_user_id = :externalUserId AND bu.blocked_external_user_id = p.external_user_id) " +
            "       OR (bu.blocked_external_user_id = :externalUserId AND bu.blocker_external_user_id = p.external_user_id)) " +
            "ORDER BY p.id DESC " +
            "LIMIT 70",
            nativeQuery = true)
    List<Post> findTop70ByGenderAndIdBeforeOrderByIdDesc(
            @Param("gender") String gender,
            @Param("externalUserId") String externalUserId,
            @Param("lastPostId") long lastPostId,
            @Param("ageRange1") Integer ageRange1,
            @Param("ageRange2") Integer ageRange2
    );


    @Query(value = "SELECT p.*, ST_Distance_Sphere(:myLocation, p.location) / 1000 AS distance " + // 미터 → 킬로미터 변환
            "FROM posts p " +
            "WHERE MBRContains(ST_GeomFromText(:boundingBox, 4326), p.location) " +
            "AND p.age BETWEEN :ageRange1 AND :ageRange2 " +
            "AND NOT EXISTS (" +
            "    SELECT 1 FROM blocked_users bu " +
            "    WHERE (bu.blocker_external_user_id = :externalUserId AND bu.blocked_external_user_id = p.external_user_id) " +
            "       OR (bu.blocked_external_user_id = :externalUserId AND bu.blocker_external_user_id = p.external_user_id)) " +
            "AND ST_Distance_Sphere(:myLocation, p.location) / 1000 > :lastDistance " + // 킬로미터 단위 비교
            "ORDER BY distance ASC " +
            "LIMIT 70",
            nativeQuery = true)
    List<Post> findPostsWithinBoundingBoxAndMinDistance(
            @Param("boundingBox") String boundingBox,
            @Param("myLocation") Point myLocation,
            @Param("lastDistance") double lastDistance, // 킬로미터 단위
            @Param("externalUserId") String externalUserId,
            @Param("ageRange1") Integer ageRange1,
            @Param("ageRange2") Integer ageRange2
    );

    @Query(value = "SELECT p.*, ST_Distance_Sphere(:myLocation, p.location) / 1000 AS distance " + // 미터 → 킬로미터 변환
            "FROM posts p " +
            "WHERE MBRContains(ST_GeomFromText(:boundingBox, 4326), p.location) " +
            "AND p.age BETWEEN :ageRange1 AND :ageRange2 " +
            "AND NOT EXISTS (" +
            "    SELECT 1 FROM blocked_users bu " +
            "    WHERE (bu.blocker_external_user_id = :externalUserId AND bu.blocked_external_user_id = p.external_user_id) " +
            "       OR (bu.blocked_external_user_id = :externalUserId AND bu.blocker_external_user_id = p.external_user_id)) " +
            "AND p.gender = :gender " +
            "AND ST_Distance_Sphere(:myLocation, p.location) / 1000 > :lastDistance " + // 킬로미터 단위 비교
            "ORDER BY distance ASC " +
            "LIMIT 70",
            nativeQuery = true)
    List<Post> findPostsWithinBoundingBoxAndGenderAndMinDistance(
            @Param("boundingBox") String boundingBox,
            @Param("gender") String gender,
            @Param("myLocation") Point myLocation,
            @Param("lastDistance") double lastDistance, // 킬로미터 단위
            @Param("externalUserId") String externalUserId,
            @Param("ageRange1") Integer ageRange1,
            @Param("ageRange2") Integer ageRange2
    );

    void deleteByExternalUserId(String externalUserId);

    List<Post> findByUserId(Long userId);  // 특정 유저의 게시글 조회

    @Query("SELECT COUNT(p) FROM Post p")
    Long countPosts();

    @Query("SELECT COUNT(p) FROM Post p WHERE p.createdAt BETWEEN :start AND :end")
    long countLoginsBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    List<Post> findByExternalUserId(String finalExtUserId);

    // 내용 부분 일치 검색 (최대 200개, 최신순)
    List<Post> findTop200ByContentContainingIgnoreCaseOrderByCreatedAtDesc(String keyword);
}
