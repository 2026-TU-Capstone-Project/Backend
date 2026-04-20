package com.example.Capstone_project.service;

import com.example.Capstone_project.common.exception.ForbiddenException;
import com.example.Capstone_project.common.exception.ResourceNotFoundException;
import com.example.Capstone_project.domain.Feed;
import com.example.Capstone_project.domain.FeedLike;
import com.example.Capstone_project.domain.FittingStatus;
import com.example.Capstone_project.domain.FittingTask;
import com.example.Capstone_project.domain.User;
import com.example.Capstone_project.dto.*;
import com.example.Capstone_project.repository.FeedLikeRepository;
import com.example.Capstone_project.repository.FeedRepository;
import com.example.Capstone_project.repository.FittingRepository;
import com.example.Capstone_project.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeedService {

    private final FeedRepository feedRepository;
    private final FeedLikeRepository feedLikeRepository;
    private final FittingRepository fittingRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public FeedPreviewResponseDto getPreview(Long fittingTaskId, Long userId) {
        FittingTask task = fittingRepository.findByIdWithClothes(fittingTaskId)
                .orElseThrow(() -> new ResourceNotFoundException("가상 피팅 작업을 찾을 수 없습니다. id=" + fittingTaskId));
        if (!task.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("본인의 저장된 스타일만 피드 미리보기에 사용할 수 있습니다.");
        }
        if (!task.isSaved()) {
            throw new IllegalArgumentException("저장된 스타일만 피드에 등록할 수 있습니다.");
        }
        if (task.getStatus() != FittingStatus.COMPLETED) {
            throw new IllegalArgumentException("완료된 가상 피팅만 피드에 등록할 수 있습니다.");
        }
        return FeedPreviewResponseDto.builder()
                .styleImageUrl(task.getResultImgUrl())
                .topImageUrl(task.getTop() != null ? task.getTop().getImgUrl() : null)
                .topName(task.getTop() != null ? task.getTop().getName() : null)
                .bottomImageUrl(task.getBottom() != null ? task.getBottom().getImgUrl() : null)
                .bottomName(task.getBottom() != null ? task.getBottom().getName() : null)
                .build();
    }

    @Transactional
    public void create(Long userId, FeedCreateRequestDto dto) {
        FittingTask task = fittingRepository.findByIdWithClothes(dto.getFittingTaskId())
                .orElseThrow(() -> new ResourceNotFoundException("가상 피팅 작업을 찾을 수 없습니다. id=" + dto.getFittingTaskId()));
        if (!task.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("본인의 저장된 스타일만 피드에 등록할 수 있습니다.");
        }
        if (!task.isSaved()) {
            throw new IllegalArgumentException("저장된 스타일만 피드에 등록할 수 있습니다.");
        }
        if (task.getStatus() != FittingStatus.COMPLETED) {
            throw new IllegalArgumentException("완료된 가상 피팅만 피드에 등록할 수 있습니다.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));

        Feed feed = new Feed();
        feed.setUser(user);
        feed.setFeedTitle(dto.getFeedTitle());
        feed.setFeedContent(dto.getFeedContent());
        feed.setStyleImageUrl(task.getResultImgUrl());
        feed.setFittingTaskId(task.getId());
        feed.setTopImageUrl(task.getTop() != null ? task.getTop().getImgUrl() : null);
        feed.setTopName(task.getTop() != null ? task.getTop().getName() : null);
        feed.setTopClothesId(task.getTopId());
        feed.setBottomImageUrl(task.getBottom() != null ? task.getBottom().getImgUrl() : null);
        feed.setBottomName(task.getBottom() != null ? task.getBottom().getName() : null);
        feed.setBottomClothesId(task.getBottomId());
        feedRepository.save(feed);
    }

    @Transactional
    public void update(Long feedId, Long userId, FeedUpdateRequestDto dto) {
        Feed feed = feedRepository.findByIdAndDeletedAtIsNull(feedId)
                .orElseThrow(() -> new ResourceNotFoundException("피드를 찾을 수 없습니다. id=" + feedId));
        if (!feed.getUser().getId().equals(userId)) {
            throw new ForbiddenException("본인 피드만 수정할 수 있습니다.");
        }
        if (dto.getFeedTitle() != null && !dto.getFeedTitle().isBlank()) {
            feed.setFeedTitle(dto.getFeedTitle());
        }
        if (dto.getFeedContent() != null) {
            feed.setFeedContent(dto.getFeedContent());
        }
        feedRepository.save(feed);
    }

    @Transactional
    public void delete(Long feedId, Long userId) {
        Feed feed = feedRepository.findByIdAndDeletedAtIsNull(feedId)
                .orElseThrow(() -> new ResourceNotFoundException("피드를 찾을 수 없습니다. id=" + feedId));
        if (!feed.getUser().getId().equals(userId)) {
            throw new ForbiddenException("본인 피드만 삭제할 수 있습니다.");
        }
        feed.setDeletedAt(LocalDateTime.now());
        feedRepository.save(feed);
    }

    @Transactional(readOnly = true)
    public Page<FeedListResponseDto> listAll(Long userId, int page, int size) {
        Page<Feed> feeds = feedRepository.findAllByDeletedAtIsNullWithUser(PageRequest.of(page, size));
        return enrichWithLikes(feeds, userId);
    }

    @Transactional(readOnly = true)
    public Page<FeedListResponseDto> listMy(Long userId, int page, int size) {
        Page<Feed> feeds = feedRepository.findAllByUserIdAndDeletedAtIsNullWithUser(userId, PageRequest.of(page, size));
        return enrichWithLikes(feeds, userId);
    }

    @Transactional(readOnly = true)
    public FeedDetailResponseDto getDetail(Long feedId, Long userId) {
        Feed feed = feedRepository.findByIdAndDeletedAtIsNullWithUser(feedId)
                .orElseThrow(() -> new ResourceNotFoundException("피드를 찾을 수 없습니다. id=" + feedId));
        long likeCount = feedLikeRepository.countByFeedId(feedId);
        boolean isLiked = feedLikeRepository.findByFeedIdAndUserId(feedId, userId).isPresent();
        FeedDetailResponseDto dto = FeedDetailResponseDto.from(feed);
        dto.setLikeCount((int) likeCount);
        dto.setLiked(isLiked);
        return dto;
    }

    @Transactional
    public boolean toggleLike(Long feedId, Long userId) {
        feedRepository.findByIdAndDeletedAtIsNull(feedId)
                .orElseThrow(() -> new ResourceNotFoundException("피드를 찾을 수 없습니다. id=" + feedId));
        return feedLikeRepository.findByFeedIdAndUserId(feedId, userId)
                .map(like -> { feedLikeRepository.delete(like); return false; })
                .orElseGet(() -> { feedLikeRepository.save(new FeedLike(feedId, userId)); return true; });
    }

    private Page<FeedListResponseDto> enrichWithLikes(Page<Feed> feeds, Long userId) {
        List<Long> feedIds = feeds.map(Feed::getId).getContent();
        if (feedIds.isEmpty()) return feeds.map(FeedListResponseDto::from);

        Map<Long, Long> likeCounts = feedLikeRepository.countByFeedIds(feedIds).stream()
                .collect(Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1]));
        Set<Long> likedIds = userId != null
                ? feedLikeRepository.findLikedFeedIds(feedIds, userId)
                : Set.of();

        return feeds.map(feed -> {
            FeedListResponseDto dto = FeedListResponseDto.from(feed);
            dto.setLikeCount(likeCounts.getOrDefault(feed.getId(), 0L).intValue());
            dto.setLiked(likedIds.contains(feed.getId()));
            return dto;
        });
    }
}
