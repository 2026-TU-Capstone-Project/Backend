package com.example.Capstone_project.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(
    name = "follows",
    uniqueConstraints = @UniqueConstraint(columnNames = {"follower_id", "following_id"}),
    indexes = {
        @Index(name = "idx_follows_following_status", columnList = "following_id, status"),
        @Index(name = "idx_follows_follower_status", columnList = "follower_id, status")
    }
)
public class Follow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "follower_id", nullable = false)
    private User follower;   // 팔로우 요청자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "following_id", nullable = false)
    private User following;  // 팔로우 대상

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private FollowStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static Follow of(User follower, User following) {
        Follow f = new Follow();
        f.follower = follower;
        f.following = following;
        f.status = FollowStatus.PENDING;
        return f;
    }

    public void accept() {
        this.status = FollowStatus.ACCEPTED;
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
