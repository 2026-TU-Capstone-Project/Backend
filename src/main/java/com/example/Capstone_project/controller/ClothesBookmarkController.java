package com.example.Capstone_project.controller;

import com.example.Capstone_project.common.dto.ApiResponse;
import com.example.Capstone_project.config.CustomUserDetails;
import com.example.Capstone_project.dto.ClothesBookmarkResponseDto;
import com.example.Capstone_project.service.ClothesBookmarkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Clothes Bookmark", description = "피드에서 마음에 든 옷을 저장·조회·삭제합니다. 원본 피드나 FittingTask가 삭제돼도 독립적으로 유지됩니다.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/clothes-bookmarks")
public class ClothesBookmarkController {

    private final ClothesBookmarkService clothesBookmarkService;

    @Operation(
            summary = "피드 옷 북마크 저장",
            description = "피드의 상의(TOP) 또는 하의(BOTTOM)를 북마크로 저장합니다. " +
                    "원본 피드가 삭제되면 북마크도 함께 삭제됩니다."
    )
    @PostMapping
    public ResponseEntity<ApiResponse<ClothesBookmarkResponseDto>> save(
            @Parameter(description = "출처 피드 ID", required = true) @RequestParam Long feedId,
            @Parameter(description = "저장할 포지션: TOP 또는 BOTTOM", required = true) @RequestParam String position,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        ClothesBookmarkResponseDto result = clothesBookmarkService.save(feedId, position, userDetails.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("북마크 저장 완료", result));
    }

    @Operation(summary = "내 옷 북마크 목록 조회", description = "저장한 옷 북마크를 최신순으로 조회합니다. position 파라미터로 TOP/BOTTOM 분리 조회 가능합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ClothesBookmarkResponseDto>>> getMyBookmarks(
            @Parameter(description = "포지션 필터: TOP 또는 BOTTOM (생략 시 전체 조회)") @RequestParam(required = false) String position,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<ClothesBookmarkResponseDto> result = clothesBookmarkService.getMyBookmarks(userDetails.getId(), position);
        return ResponseEntity.ok(ApiResponse.success("북마크 목록 조회 성공", result));
    }

    @Operation(summary = "옷 북마크 삭제", description = "본인이 저장한 북마크만 삭제할 수 있습니다.")
    @DeleteMapping("/{bookmarkId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @Parameter(description = "북마크 ID", required = true) @PathVariable Long bookmarkId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        clothesBookmarkService.delete(bookmarkId, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("북마크 삭제 완료", null));
    }
}
