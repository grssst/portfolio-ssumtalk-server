package com.hottalk.hottalkserver.model;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hottalk.hottalkserver.model.User;
import jakarta.persistence.*;

@Entity
@Table(
        name = "blocked_users",
        indexes = {
                @Index(name = "idx_blocker_external_user_id", columnList = "blocker_external_user_id"),
                @Index(name = "idx_blocked_external_user_id", columnList = "blocked_external_user_id"),
                @Index(name = "idx_blocker_blocked_external_user_id", columnList = "blocker_external_user_id, blocked_external_user_id")
        }
)
public class BlockedUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "blocker_id", referencedColumnName = "id")
    @JsonIgnore  // JSON 직렬화 시 이 필드를 무시
    private User blocker;

    @ManyToOne
    @JoinColumn(name = "blocked_id", referencedColumnName = "id")
    @JsonIgnore  // JSON 직렬화 시 이 필드를 무시
    private User blocked;

    @Column(name = "blocker_external_user_id", nullable = false)
    private String blockerExternalUserId;

    @Column(name = "blocked_external_user_id", nullable = false)
    private String blockedExternalUserId;

    public BlockedUser() {}

    public BlockedUser(User blocker, User blocked) {
        this.blocker = blocker;
        this.blocked = blocked;
    }
    public BlockedUser(User blocker, User blocked, String blockerExternalUserId, String blockedExternalUserId) {
        this.blocker = blocker;
        this.blocked = blocked;
        this.blockerExternalUserId = blockerExternalUserId;
        this.blockedExternalUserId = blockedExternalUserId;
    }


    // Getter 및 Setter
    public String getBlockerExternalUserId() {
        return blockerExternalUserId;
    }

    public void setBlockerExternalUserId(String blockerExternalUserId) {
        this.blockerExternalUserId = blockerExternalUserId;
    }

    public String getBlockedExternalUserId() {
        return blockedExternalUserId;
    }

    public void setBlockedExternalUserId(String blockedExternalUserId) {
        this.blockedExternalUserId = blockedExternalUserId;
    }
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getBlocker() {
        return blocker;
    }

    public void setBlocker(User blocker) {
        this.blocker = blocker;
    }

    public User getBlocked() {
        return blocked;
    }

    public void setBlocked(User blocked) {
        this.blocked = blocked;
    }
}
