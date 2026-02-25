package com.example.Capstone_project.service;

import com.example.Capstone_project.common.exception.ResourceNotFoundException;
import com.example.Capstone_project.domain.Feed;
import com.example.Capstone_project.domain.FittingStatus;
import com.example.Capstone_project.domain.FittingTask;
import com.example.Capstone_project.domain.User;
import com.example.Capstone_project.dto.*;
import com.example.Capstone_project.repository.FeedRepository;
import com.example.Capstone_project.repository.FittingRepository;
import com.example.Capstone_project.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeedService {

    private final FeedRepository feedRepository;
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
        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new ResourceNotFoundException("피드를 찾을 수 없습니다. id=" + feedId));
        if (!feed.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("본인 피드만 수정할 수 있습니다.");
        }
        if (feed.getDeletedAt() != null) {
            throw new ResourceNotFoundException("삭제된 피드는 수정할 수 없습니다.");
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
        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new ResourceNotFoundException("피드를 찾을 수 없습니다. id=" + feedId));
        if (!feed.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("본인 피드만 삭제할 수 있습니다.");
        }
        feed.setDeletedAt(LocalDateTime.now());
        feedRepository.save(feed);
    }

    @Transactional(readOnly = true)
    public List<FeedListResponseDto> listAll() {
        return feedRepository.findAllByDeletedAtIsNullOrderByCreatedAtDesc().stream()
                .map(FeedListResponseDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FeedListResponseDto> listMy(Long userId) {
        return feedRepository.findAllByUser_IdAndDeletedAtIsNullOrderByCreatedAtDesc(userId).stream()
                .map(FeedListResponseDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FeedDetailResponseDto getDetail(Long feedId) {
        Feed feed = feedRepository.findByIdAndDeletedAtIsNullWithUser(feedId)
                .orElseThrow(() -> new ResourceNotFoundException("피드를 찾을 수 없습니다. id=" + feedId));
        return FeedDetailResponseDto.from(feed);
    }
}
