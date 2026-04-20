package com.example.Capstone_project.repository;

import com.example.Capstone_project.domain.FeedLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface FeedLikeRepository extends JpaRepository<FeedLike, Long> {

    Optional<FeedLike> findByFeedIdAndUserId(Long feedId, Long userId);

    long countByFeedId(Long feedId);

    @Query("SELECT fl.feedId FROM FeedLike fl WHERE fl.feedId IN :feedIds AND fl.userId = :userId")
    Set<Long> findLikedFeedIds(@Param("feedIds") List<Long> feedIds, @Param("userId") Long userId);

    @Query("SELECT fl.feedId, COUNT(fl) FROM FeedLike fl WHERE fl.feedId IN :feedIds GROUP BY fl.feedId")
    List<Object[]> countByFeedIds(@Param("feedIds") List<Long> feedIds);
}
