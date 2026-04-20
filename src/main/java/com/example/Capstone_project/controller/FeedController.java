package com.example.Capstone_project.controller;

import com.example.Capstone_project.common.dto.ApiResponse;
import com.example.Capstone_project.config.CustomUserDetails;
import com.example.Capstone_project.dto.*;
import com.example.Capstone_project.service.FeedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Feed", description = "피드 미리보기·작성·수정·삭제·목록·상세 조회·좋아요")
@RestController
@RequestMapping("/api/v1/feeds")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;

    @Operation(summary = "피드 게시 전 미리보기")
    @GetMapping("/preview/{fittingTaskId}")
    public ResponseEntity<ApiResponse<FeedPreviewResponseDto>> getPreview(
            @PathVariable Long fittingTaskId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.success("미리보기 조회 성공",
                feedService.getPreview(fittingTaskId, userDetails.getUser().getId())));
    }

    @Operation(summary = "피드 작성")
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> create(
            @Valid @RequestBody FeedCreateRequestDto dto,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        feedService.create(userDetails.getUser().getId(), dto);
        return ResponseEntity.ok(ApiResponse.success("피드가 등록되었습니다.", null));
    }

    @Operation(summary = "피드 수정")
    @PatchMapping("/{feedId}")
    public ResponseEntity<ApiResponse<Void>> update(
            @PathVariable Long feedId,
            @Valid @RequestBody FeedUpdateRequestDto dto,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        feedService.update(feedId, userDetails.getUser().getId(), dto);
        return ResponseEntity.ok(ApiResponse.success("피드가 수정되었습니다.", null));
    }

    @Operation(summary = "피드 삭제")
    @DeleteMapping("/{feedId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long feedId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        feedService.delete(feedId, userDetails.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success("피드가 삭제되었습니다.", null));
    }

    @Operation(summary = "피드 전체 목록", description = "page, size 파라미터로 페이지네이션. 팔로잉 우선 정렬.")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<FeedListResponseDto>>> listAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails != null ? userDetails.getUser().getId() : null;
        return ResponseEntity.ok(ApiResponse.success("목록 조회 성공",
                feedService.listAll(userId, page, size)));
    }

    @Operation(summary = "내 피드 목록")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Page<FeedListResponseDto>>> listMy(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUser().getId();
        return ResponseEntity.ok(ApiResponse.success("내 피드 목록 조회 성공",
                feedService.listMy(userId, page, size)));
    }

    @Operation(summary = "피드 상세")
    @GetMapping("/{feedId}")
    public ResponseEntity<ApiResponse<FeedDetailResponseDto>> getDetail(
            @PathVariable Long feedId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUser().getId();
        return ResponseEntity.ok(ApiResponse.success("상세 조회 성공",
                feedService.getDetail(feedId, userId)));
    }

    @Operation(summary = "좋아요 토글", description = "좋아요가 없으면 추가, 있으면 취소. 결과 true=좋아요, false=취소.")
    @PostMapping("/{feedId}/like")
    public ResponseEntity<ApiResponse<Boolean>> toggleLike(
            @PathVariable Long feedId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        boolean liked = feedService.toggleLike(feedId, userDetails.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success(liked ? "좋아요 추가" : "좋아요 취소", liked));
    }
}
