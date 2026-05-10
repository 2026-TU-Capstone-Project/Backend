package com.example.Capstone_project.repository;

import com.example.Capstone_project.domain.ClothesBookmark;
import com.example.Capstone_project.domain.Feed;
import com.example.Capstone_project.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClothesBookmarkRepository extends JpaRepository<ClothesBookmark, Long> {

    @Query("SELECT cb FROM ClothesBookmark cb JOIN FETCH cb.feed WHERE cb.user = :user ORDER BY cb.savedAt DESC")
    List<ClothesBookmark> findByUserWithFeed(@Param("user") User user);

    @Query("SELECT cb FROM ClothesBookmark cb JOIN FETCH cb.feed WHERE cb.user = :user AND cb.position = :position ORDER BY cb.savedAt DESC")
    List<ClothesBookmark> findByUserAndPositionWithFeed(@Param("user") User user, @Param("position") String position);

    Optional<ClothesBookmark> findByIdAndUser(Long id, User user);

    boolean existsByUserAndFeedAndPosition(User user, Feed feed, String position);

    @Modifying
    @Query("DELETE FROM ClothesBookmark cb WHERE cb.feed.id = :feedId")
    void deleteAllByFeedId(@Param("feedId") Long feedId);
}
