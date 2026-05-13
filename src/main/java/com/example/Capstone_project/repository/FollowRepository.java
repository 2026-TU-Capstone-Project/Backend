package com.example.Capstone_project.repository;

import com.example.Capstone_project.domain.Follow;
import com.example.Capstone_project.domain.FollowStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface FollowRepository extends JpaRepository<Follow, Long> {

    Optional<Follow> findByFollowerIdAndFollowingId(Long followerId, Long followingId);

    // 받은 팔로우 신청 목록 (PENDING)
    @Query("SELECT f FROM Follow f JOIN FETCH f.follower WHERE f.following.id = :userId AND f.status = 'PENDING' ORDER BY f.createdAt DESC")
    List<Follow> findPendingRequestsToMe(@Param("userId") Long userId);

    // 팔로워 목록 (ACCEPTED)
    @Query("SELECT f FROM Follow f JOIN FETCH f.follower WHERE f.following.id = :userId AND f.status = 'ACCEPTED' ORDER BY f.updatedAt DESC")
    List<Follow> findFollowers(@Param("userId") Long userId);

    // 팔로잉 목록 (ACCEPTED)
    @Query("SELECT f FROM Follow f JOIN FETCH f.following WHERE f.follower.id = :userId AND f.status = 'ACCEPTED' ORDER BY f.updatedAt DESC")
    List<Follow> findFollowings(@Param("userId") Long userId);

    // 팔로워 수 / 팔로잉 수
    long countByFollowingIdAndStatus(Long followingId, FollowStatus status);
    long countByFollowerIdAndStatus(Long followerId, FollowStatus status);

    // 커서 기반 팔로워 목록 (followId < cursor, id DESC)
    @Query("SELECT f FROM Follow f JOIN FETCH f.follower WHERE f.following.id = :userId AND f.status = 'ACCEPTED' AND f.id < :cursor ORDER BY f.id DESC")
    List<Follow> findFollowersCursor(@Param("userId") Long userId, @Param("cursor") Long cursor, Pageable pageable);

    // 커서 기반 팔로잉 목록 (followId < cursor, id DESC)
    @Query("SELECT f FROM Follow f JOIN FETCH f.following WHERE f.follower.id = :userId AND f.status = 'ACCEPTED' AND f.id < :cursor ORDER BY f.id DESC")
    List<Follow> findFollowingsCursor(@Param("userId") Long userId, @Param("cursor") Long cursor, Pageable pageable);

    // 피드 visibility 필터용: 내가 팔로우하는(ACCEPTED) 유저 ID 목록
    @Query("SELECT f.following.id FROM Follow f WHERE f.follower.id = :userId AND f.status = 'ACCEPTED'")
    Set<Long> findFollowingIds(@Param("userId") Long userId);
}
