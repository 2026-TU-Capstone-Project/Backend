package com.example.Capstone_project.service;

import com.example.Capstone_project.domain.Feed;
import com.example.Capstone_project.domain.FeedFavorite;
import com.example.Capstone_project.domain.User;
import com.example.Capstone_project.dto.FeedListResponseDto; // 기존 피드 목록용 DTO 활용
import com.example.Capstone_project.repository.FeedRepository;
import com.example.Capstone_project.repository.FeedFavoriteRepository;
import com.example.Capstone_project.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FavoriteService {

    private final FeedFavoriteRepository feedFavoriteRepository;
    private final UserRepository userRepository;
    private final FeedRepository feedRepository;

    /**
     * 피드 즐겨찾기 토글
     */
    @Transactional
    public boolean toggleFeedFavorite(Long userId, Long feedId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new IllegalArgumentException("피드를 찾을 수 없습니다."));

        return feedFavoriteRepository.findByUserAndFeed(user, feed)
                .map(favorite -> {
                    feedFavoriteRepository.delete(favorite);
                    return false; // 삭제됨
                })
                .orElseGet(() -> {
                    feedFavoriteRepository.save(new FeedFavorite(user, feed));
                    return true; // 저장됨
                });
    }

    /**
     * 내가 즐겨찾기한 피드 목록 상세 조회
     */
    public List<FeedListResponseDto> getMyFavoriteFeeds(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        List<FeedFavorite> favorites = feedFavoriteRepository.findAllByUserWithFeed(user);

        // Favorite 엔티티에서 Feed 정보만 추출하여 DTO로 변환
        return favorites.stream()
                .map(favorite -> FeedListResponseDto.from(favorite.getFeed()))
                .collect(Collectors.toList());
    }
}