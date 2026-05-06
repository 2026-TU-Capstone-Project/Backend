package com.example.Capstone_project.controller;

import com.example.Capstone_project.common.dto.ApiResponse;
import com.example.Capstone_project.config.CustomUserDetails;
import com.example.Capstone_project.dto.FeedListResponseDto;
import com.example.Capstone_project.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/favorites/feeds")
public class FavoriteController {

    private final FavoriteService favoriteService;

    // 즐겨찾기 추가/삭제 토글
    @PostMapping("/{feedId}")
    public ApiResponse<Boolean> toggleFavorite(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long feedId) {
        return ApiResponse.success(favoriteService.toggleFeedFavorite(userDetails.getId(), feedId));
    }

    // 나의 즐겨찾기 피드 목록 조회
    @GetMapping("/me")
    public ApiResponse<List<FeedListResponseDto>> getMyFavorites(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.success(favoriteService.getMyFavoriteFeeds(userDetails.getId()));
    }
}