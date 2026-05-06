package com.example.Capstone_project.repository;

import com.example.Capstone_project.domain.Feed;
import com.example.Capstone_project.domain.FeedFavorite;
import com.example.Capstone_project.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface FeedFavoriteRepository extends JpaRepository<FeedFavorite, Long> {
    // 이미 즐겨찾기 했는지 확인용
    Optional<FeedFavorite> findByUserAndFeed(User user, Feed feed);

    // 유저별 즐겨찾기 목록 조회용 (조인 성능을 위해 Feed 정보도 같이 가져오게 확장 가능)
    @Query("SELECT ff FROM FeedFavorite ff JOIN FETCH ff.feed WHERE ff.user = :user")
    List<FeedFavorite> findAllByUserWithFeed(@Param("user") User user);
}