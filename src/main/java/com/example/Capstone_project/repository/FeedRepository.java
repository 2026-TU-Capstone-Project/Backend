package com.example.Capstone_project.repository;

import com.example.Capstone_project.domain.Feed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FeedRepository extends JpaRepository<Feed, Long> {

    List<Feed> findAllByDeletedAtIsNullOrderByCreatedAtDesc();

    List<Feed> findAllByUser_IdAndDeletedAtIsNullOrderByCreatedAtDesc(Long userId);

    @Query("SELECT f FROM Feed f JOIN FETCH f.user WHERE f.id = :id AND f.deletedAt IS NULL")
    Optional<Feed> findByIdAndDeletedAtIsNullWithUser(@Param("id") Long id);
}
