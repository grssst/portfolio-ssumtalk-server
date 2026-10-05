package com.hottalk.hottalkserver.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import com.hottalk.hottalkserver.model.BlockedUser;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

@Entity
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_external_user_id", columnList = "external_user_id"),
                @Index(name = "idx_last_login", columnList = "last_login")
        }
)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // 데이터베이스에서 자동으로 생성되는 고유 ID

    @Column(name = "external_user_id", nullable = false)
    private String externalUserId;  // 외부 시스템의 사용자 ID

    @Column(name = "provider", nullable = true)
    private String provider;  // 로그인 제공자 (예: "apple", "google")

    @Column(name = "email")
    private String email;  // 사용자의 이메일 주소

    @Column(name = "full_name")
    private String fullName;  // 사용자의 전체 이름

    @Column(name = "nickname")
    private String nickname;  // 닉네임

    @Column(name = "phone_number")
    private String phoneNumber;  // 전화번호

    @Column(name = "gender")
    private String gender;  // 성별

    @Column(name = "age")
    private Integer age;  // 나이

    @Column(name = "profile_image_url")
    private String profileImageUrl;  // 프로필 사진 URL

    @Column(name = "profile_image_url2")
    private String profileImageUrl2;  // 프로필 사진 URL

    @Column(name = "profile_image_url3")
    private String profileImageUrl3;  // 프로필 사진 URL

    @Column(name = "status")
    private String status;  // 자기소개

    @Column(name = "myLocation")
    private String myLocation;  // 활동지역

    @Column(nullable = false, columnDefinition = "POINT SRID 4326")
    @JdbcTypeCode(SqlTypes.GEOMETRY)
    private Point location;

    @Column(name = "is_admin", nullable = false)
    private Boolean isAdmin = false;  // 관리자 여부 (기본값 false)

    @Column(name = "interests_key")
    private String interestsKey; // "1,2,3" 형식으로 저장

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;  // 계정 생성 날짜

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;  // 정보 업데이트 날짜

    @Column(name = "last_login")
    private LocalDateTime lastLogin;  // 마지막 로그인 시간

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;  // 탈퇴 여부

    @OneToMany(mappedBy = "blocker", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<BlockedUser> blockedUsers;

    @OneToMany(mappedBy = "blocked", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<BlockedUser> blockedByUsers;

    @Column(name = "candy", nullable = false)
    private Integer candy = 0;  // 초기값 0

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FcmToken> fcmTokens = new ArrayList<>(); // FCM 토큰 리스트

    // 오늘 작성한 게시글 수 (하루마다 초기화 필요)
    @Column(name = "daily_post_count")
    private Integer dailyPostCount = 0;

    // 마지막으로 게시글 작성한 날짜
    @Column(name = "last_post_date")
    private LocalDate lastPostDate;


    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        if (location == null) {
            GeometryFactory geometryFactory = new GeometryFactory();
            this.location = geometryFactory.createPoint(new Coordinate(0, 0));
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    }

    // Default constructor
    public User() {}

    // Constructor for required fields
    public User(String externalUserId, String provider) {
        this.externalUserId = externalUserId;
        this.provider = provider;
    }

    // Constructor for ID only
    public User(Long id) {
        this.id = id;
    }

    // Getters and Setters

    public List<FcmToken> getFcmTokens() {
        return fcmTokens;
    }

    public void setFcmTokens(List<FcmToken> fcmTokens) {
        this.fcmTokens = fcmTokens;
    }
    public List<BlockedUser> getBlockedUsers() {
        return blockedUsers;
    }

    public void setBlockedUsers(List<BlockedUser> blockedUsers) {
        this.blockedUsers = blockedUsers;
    }

    public List<BlockedUser> getBlockedByUsers() {
        return blockedByUsers;
    }

    public void setBlockedByUsers(List<BlockedUser> blockedByUsers) {
        this.blockedByUsers = blockedByUsers;
    }

    public Boolean getIsAdmin() {
        return isAdmin;
    }

    public void setIsAdmin(Boolean isAdmin) {
        this.isAdmin = isAdmin;
    }
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getExternalUserId() {
        return externalUserId;
    }

    public void setExternalUserId(String externalUserId) {
        this.externalUserId = externalUserId;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public String getProfileImageUrl2() {
        return profileImageUrl2;
    }

    public void setProfileImageUrl2(String profileImageUrl2) {
        this.profileImageUrl2 = profileImageUrl2;
    }

    public String getProfileImageUrl3() {
        return profileImageUrl3;
    }

    public void setProfileImageUrl3(String profileImageUrl3) {
        this.profileImageUrl3 = profileImageUrl3;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMyLocation() {
        return myLocation;
    }

    public void setMyLocation(String myLocation) {
        this.myLocation = myLocation;
    }

    public Point getLocation() {
        return location;
    }

    public void setLocation(Point location) {
        this.location = location;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }

    public Boolean getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Boolean isDeleted) {
        this.isDeleted = isDeleted;
    }


    public Integer getCandy() {
        return candy;
    }

    public void setCandy(Integer candy) {
        this.candy = candy;
    }
    public String getInterestsKey() {
        return interestsKey;
    }

    public void setInterestsKey(String interestsKey) {
        this.interestsKey = interestsKey;
    }

    public int getDailyPostCount() {
        return dailyPostCount;
    }
    public void setDailyPostCount(int dailyPostCount) {
        this.dailyPostCount = dailyPostCount;
    }

    public LocalDate getLastPostDate() {
        return lastPostDate;
    }
    public void setLastPostDate(LocalDate lastPostDate) {
        this.lastPostDate = lastPostDate;
    }
}
