package com.example.Capstone_project.repository;

import com.example.Capstone_project.domain.Feed;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FeedRepository extends JpaRepository<Feed, Long> {

    List<Feed> findAllByDeletedAtIsNullOrderByCreatedAtDesc();

    List<Feed> findAllByUser_IdAndDeletedAtIsNullOrderByCreatedAtDesc(Long userId);

    // 비로그인: PUBLIC 피드만
    @Query("SELECT f FROM Feed f JOIN FETCH f.user WHERE f.deletedAt IS NULL AND f.visibility = 'PUBLIC' ORDER BY f.createdAt DESC")
    Page<Feed> findPublicFeedsWithUser(Pageable pageable);

    // 로그인: PUBLIC + 내 피드 + 팔로우 중인 사람의 FOLLOWERS_ONLY 피드
    @Query(value = """
            SELECT f FROM Feed f JOIN FETCH f.user
            WHERE f.deletedAt IS NULL
            AND (
                f.visibility = 'PUBLIC'
                OR f.user.id = :userId
                OR (f.visibility = 'FOLLOWERS_ONLY' AND EXISTS (
                    SELECT 1 FROM Follow fl
                    WHERE fl.follower.id = :userId
                    AND fl.following.id = f.user.id
                    AND fl.status = 'ACCEPTED'
                ))
            )
            ORDER BY f.createdAt DESC
            """,
           countQuery = """
            SELECT COUNT(f) FROM Feed f
            WHERE f.deletedAt IS NULL
            AND (
                f.visibility = 'PUBLIC'
                OR f.user.id = :userId
                OR (f.visibility = 'FOLLOWERS_ONLY' AND EXISTS (
                    SELECT 1 FROM Follow fl
                    WHERE fl.follower.id = :userId
                    AND fl.following.id = f.user.id
                    AND fl.status = 'ACCEPTED'
                ))
            )
            """)
    Page<Feed> findFeedsVisibleToUser(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT f FROM Feed f JOIN FETCH f.user WHERE f.user.id = :userId AND f.deletedAt IS NULL ORDER BY f.createdAt DESC")
    Page<Feed> findAllByUserIdAndDeletedAtIsNullWithUser(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT f FROM Feed f JOIN FETCH f.user WHERE f.id = :id AND f.deletedAt IS NULL")
    Optional<Feed> findByIdAndDeletedAtIsNullWithUser(@Param("id") Long id);

    Optional<Feed> findByIdAndDeletedAtIsNull(Long id);

    // 스케줄러용: 삭제되지 않은 피드에서 해당 clothesId를 topClothesId 또는 bottomClothesId로 참조하는지 확인
    @Query("SELECT COUNT(f) > 0 FROM Feed f WHERE f.deletedAt IS NULL AND (f.topClothesId = :clothesId OR f.bottomClothesId = :clothesId)")
    boolean existsActiveFeedReferencingClothes(@Param("clothesId") Long clothesId);
}
