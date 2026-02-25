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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Feed", description = "피드 미리보기·작성·수정·삭제·목록·상세 조회")
@RestController
@RequestMapping("/api/v1/feeds")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;

    @Operation(summary = "피드 게시 전 미리보기", description = "내 옷장에서 선택한 가상 피팅(task) ID로 스타일/상의/하의 이미지·이름을 미리 봅니다.")
    @GetMapping("/preview/{fittingTaskId}")
    public ResponseEntity<ApiResponse<FeedPreviewResponseDto>> getPreview(
            @Parameter(description = "저장된 가상 피팅 작업 ID") @PathVariable Long fittingTaskId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUser().getId();
        FeedPreviewResponseDto preview = feedService.getPreview(fittingTaskId, userId);
        return ResponseEntity.ok(ApiResponse.success("미리보기 조회 성공", preview));
    }

    @Operation(summary = "피드 작성", description = "저장된 가상 피팅을 선택해 피드 제목·내용과 함께 게시합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> create(
            @Valid @RequestBody FeedCreateRequestDto dto,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUser().getId();
        feedService.create(userId, dto);
        return ResponseEntity.ok(ApiResponse.success("피드가 등록되었습니다.", null));
    }

    @Operation(summary = "피드 수정", description = "피드 제목·내용만 수정합니다.")
    @PatchMapping("/{feedId}")
    public ResponseEntity<ApiResponse<Void>> update(
            @Parameter(description = "피드 ID") @PathVariable Long feedId,
            @RequestBody FeedUpdateRequestDto dto,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUser().getId();
        feedService.update(feedId, userId, dto);
        return ResponseEntity.ok(ApiResponse.success("피드가 수정되었습니다.", null));
    }

    @Operation(summary = "피드 삭제", description = "피드를 소프트 삭제합니다.")
    @DeleteMapping("/{feedId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @Parameter(description = "피드 ID") @PathVariable Long feedId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUser().getId();
        feedService.delete(feedId, userId);
        return ResponseEntity.ok(ApiResponse.success("피드가 삭제되었습니다.", null));
    }

    @Operation(summary = "피드 전체 목록", description = "삭제되지 않은 전체 피드 목록. feedId, 피드 제목, 스타일 이미지 반환.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<FeedListResponseDto>>> listAll() {
        List<FeedListResponseDto> list = feedService.listAll();
        return ResponseEntity.ok(ApiResponse.success("목록 조회 성공", list));
    }

    @Operation(summary = "내 피드 목록", description = "로그인 사용자가 작성한 피드만 조회. feedId, 피드 제목, 스타일 이미지 반환.")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<FeedListResponseDto>>> listMy(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUser().getId();
        List<FeedListResponseDto> list = feedService.listMy(userId);
        return ResponseEntity.ok(ApiResponse.success("내 피드 목록 조회 성공", list));
    }

    @Operation(summary = "피드 상세", description = "작성자 id·닉네임, 스타일 이미지·styleImageId, 상의·하의 이미지·이름·ID, 제목·내용 조회.")
    @GetMapping("/{feedId}")
    public ResponseEntity<ApiResponse<FeedDetailResponseDto>> getDetail(
            @Parameter(description = "피드 ID") @PathVariable Long feedId
    ) {
        FeedDetailResponseDto detail = feedService.getDetail(feedId);
        return ResponseEntity.ok(ApiResponse.success("상세 조회 성공", detail));
    }
}
