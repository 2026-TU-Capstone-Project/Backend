package com.example.Capstone_project.service;

import com.example.Capstone_project.common.exception.ResourceNotFoundException;
import com.example.Capstone_project.domain.ClothesBookmark;
import com.example.Capstone_project.domain.Feed;
import com.example.Capstone_project.domain.User;
import com.example.Capstone_project.dto.ClothesBookmarkResponseDto;
import com.example.Capstone_project.repository.ClothesBookmarkRepository;
import com.example.Capstone_project.repository.FeedRepository;
import com.example.Capstone_project.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClothesBookmarkService {

    private final ClothesBookmarkRepository clothesBookmarkRepository;
    private final FeedRepository feedRepository;
    private final UserRepository userRepository;

    /**
     * 피드의 상의(TOP) 또는 하의(BOTTOM)를 북마크로 저장.
     * Feed를 직접 참조하므로 피드 삭제 시 북마크도 함께 삭제됨.
     */
    @Transactional
    public ClothesBookmarkResponseDto save(Long feedId, String position, Long userId) {
        Feed feed = feedRepository.findByIdAndDeletedAtIsNull(feedId)
                .orElseThrow(() -> new ResourceNotFoundException("피드를 찾을 수 없습니다. id=" + feedId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));

        String normalizedPosition = position.toUpperCase();
        validatePosition(normalizedPosition, feed);

        if (clothesBookmarkRepository.existsByUserAndFeedAndPosition(user, feed, normalizedPosition)) {
            throw new IllegalStateException("이미 저장한 옷입니다.");
        }

        ClothesBookmark bookmark = ClothesBookmark.builder()
                .user(user)
                .feed(feed)
                .position(normalizedPosition)
                .build();

        return ClothesBookmarkResponseDto.from(clothesBookmarkRepository.save(bookmark));
    }

    /**
     * 내 옷 북마크 목록 조회 (최신순).
     */
    public List<ClothesBookmarkResponseDto> getMyBookmarks(Long userId, String position) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));

        List<ClothesBookmark> bookmarks = (position != null && !position.isBlank())
                ? clothesBookmarkRepository.findByUserAndPositionWithFeed(user, position.toUpperCase())
                : clothesBookmarkRepository.findByUserWithFeed(user);

        return bookmarks.stream()
                .map(ClothesBookmarkResponseDto::from)
                .toList();
    }

    /**
     * 북마크 삭제 (본인 소유만 가능).
     */
    @Transactional
    public void delete(Long bookmarkId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));

        ClothesBookmark bookmark = clothesBookmarkRepository.findByIdAndUser(bookmarkId, user)
                .orElseThrow(() -> new ResourceNotFoundException("북마크를 찾을 수 없습니다. id=" + bookmarkId));

        clothesBookmarkRepository.delete(bookmark);
    }

    private void validatePosition(String position, Feed feed) {
        if ("TOP".equals(position) && feed.getTopImageUrl() == null) {
            throw new IllegalArgumentException("해당 피드에 상의 정보가 없습니다.");
        }
        if ("BOTTOM".equals(position) && feed.getBottomImageUrl() == null) {
            throw new IllegalArgumentException("해당 피드에 하의 정보가 없습니다.");
        }
        if (!"TOP".equals(position) && !"BOTTOM".equals(position)) {
            throw new IllegalArgumentException("position은 TOP 또는 BOTTOM이어야 합니다.");
        }
    }
}
