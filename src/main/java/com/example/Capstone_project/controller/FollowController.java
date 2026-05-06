package com.example.Capstone_project.controller;

import com.example.Capstone_project.common.dto.ApiResponse;
import com.example.Capstone_project.config.CustomUserDetails;
import com.example.Capstone_project.dto.FollowRequestItemDto;
import com.example.Capstone_project.dto.FollowUserDto;
import com.example.Capstone_project.service.FollowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Follow", description = "팔로우 / 팔로잉 관계 관리")
@RestController
@RequestMapping("/api/v1/follows")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    @Operation(summary = "팔로우 신청", description = "대상 유저에게 팔로우 신청을 보냅니다. 상대가 수락해야 팔로우 관계가 성립됩니다.")
    @PostMapping("/{targetUserId}")
    public ResponseEntity<ApiResponse<Void>> sendFollowRequest(
            @Parameter(description = "팔로우할 유저 ID") @PathVariable Long targetUserId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        followService.sendFollowRequest(userDetails.getUser().getId(), targetUserId);
        return ResponseEntity.ok(ApiResponse.success("팔로우 신청이 완료되었습니다.", null));
    }

    @Operation(summary = "팔로우 취소 / 언팔로우",
            description = "PENDING 상태면 신청 취소, ACCEPTED 상태면 언팔로우 처리됩니다.")
    @DeleteMapping("/{targetUserId}")
    public ResponseEntity<ApiResponse<Void>> cancelOrUnfollow(
            @Parameter(description = "취소/언팔할 유저 ID") @PathVariable Long targetUserId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        followService.cancelOrUnfollow(userDetails.getUser().getId(), targetUserId);
        return ResponseEntity.ok(ApiResponse.success("처리되었습니다.", null));
    }

    @Operation(summary = "팔로우 신청 수락")
    @PatchMapping("/requests/{followId}/accept")
    public ResponseEntity<ApiResponse<Void>> acceptFollow(
            @Parameter(description = "팔로우 관계 ID") @PathVariable Long followId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        followService.acceptFollow(followId, userDetails.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success("팔로우 신청을 수락했습니다.", null));
    }

    @Operation(summary = "팔로우 신청 거절")
    @DeleteMapping("/requests/{followId}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectFollow(
            @Parameter(description = "팔로우 관계 ID") @PathVariable Long followId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        followService.rejectFollow(followId, userDetails.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success("팔로우 신청을 거절했습니다.", null));
    }

    @Operation(summary = "받은 팔로우 신청 목록", description = "아직 수락하지 않은 팔로우 신청 목록을 반환합니다.")
    @GetMapping("/requests")
    public ResponseEntity<ApiResponse<List<FollowRequestItemDto>>> getPendingRequests(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<FollowRequestItemDto> list = followService.getPendingRequests(userDetails.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success("팔로우 신청 목록 조회 성공", list));
    }

    @Operation(summary = "내 팔로워 목록")
    @GetMapping("/followers")
    public ResponseEntity<ApiResponse<List<FollowUserDto>>> getFollowers(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<FollowUserDto> list = followService.getFollowers(userDetails.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success("팔로워 목록 조회 성공", list));
    }

    @Operation(summary = "내 팔로잉 목록")
    @GetMapping("/followings")
    public ResponseEntity<ApiResponse<List<FollowUserDto>>> getFollowings(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<FollowUserDto> list = followService.getFollowings(userDetails.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success("팔로잉 목록 조회 성공", list));
    }

    @Operation(summary = "특정 유저의 팔로워 목록")
    @GetMapping("/{userId}/followers")
    public ResponseEntity<ApiResponse<List<FollowUserDto>>> getUserFollowers(
            @PathVariable Long userId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<FollowUserDto> list = followService.getFollowers(userId);
        return ResponseEntity.ok(ApiResponse.success("팔로워 목록 조회 성공", list));
    }

    @Operation(summary = "특정 유저의 팔로잉 목록")
    @GetMapping("/{userId}/followings")
    public ResponseEntity<ApiResponse<List<FollowUserDto>>> getUserFollowings(
            @PathVariable Long userId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<FollowUserDto> list = followService.getFollowings(userId);
        return ResponseEntity.ok(ApiResponse.success("팔로잉 목록 조회 성공", list));
    }
}
