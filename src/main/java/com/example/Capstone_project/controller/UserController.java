package com.example.Capstone_project.controller;

import com.example.Capstone_project.common.dto.ApiResponse;
import com.example.Capstone_project.config.CustomUserDetails;
import com.example.Capstone_project.dto.UserPublicProfileResponseDto;
import com.example.Capstone_project.dto.UserProfileResponseDto;
import com.example.Capstone_project.dto.UserSearchResponseDto;
import com.example.Capstone_project.service.FollowService;
import com.example.Capstone_project.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Tag(name = "User", description = "마이페이지 정보 추가/조회/수정")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final FollowService followService;

    @Operation(summary = "마이페이지 조회", description = "로그인 사용자의 추가정보(닉네임, 프로필 이미지, 키, 몸무게, 성별)를 조회합니다.")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponseDto>> getMyProfile(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUser().getId();
        UserProfileResponseDto profile = userService.getMyProfile(userId);
        return ResponseEntity.ok(ApiResponse.success("마이페이지 조회 성공", profile));
    }

    @Operation(
            summary = "마이 페이지 프로필 추가 (최초 설정)",
            description = "최초 로그인 후 신체정보(키, 몸무게, 성별)와 프로필 이미지를 설정합니다. 보내진 필드만 저장됩니다. 성별은 MALE 또는 FEMALE."
    )
    @PostMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserProfileResponseDto>> createMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "계정 아이디 (영문/숫자/_, 3~30자)") @RequestParam(value = "username", required = false) String username,
            @Parameter(description = "이름(닉네임)") @RequestParam(value = "nickname", required = false) String nickname,
            @Parameter(description = "키 (cm)") @RequestParam(value = "height", required = false) Float height,
            @Parameter(description = "몸무게 (kg)") @RequestParam(value = "weight", required = false) Float weight,
            @Parameter(description = "성별 (MALE, FEMALE)") @RequestParam(value = "gender", required = false) String gender,
            @Parameter(description = "프로필 이미지 파일 (선택)") @RequestParam(value = "file", required = false) MultipartFile file
    ) {
        Long userId = userDetails.getUser().getId();
        byte[] imageBytes = null;
        String imageFilename = null;
        String imageContentType = null;
        if (file != null && !file.isEmpty()) {
            try {
                imageBytes = file.getBytes();
                imageFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "profile.jpg";
                imageContentType = file.getContentType();
            } catch (IOException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ApiResponse.error("프로필 이미지 파일 읽기 실패: " + e.getMessage()));
            }
        }
        UserProfileResponseDto created = userService.updateMyProfileWithForm(
                userId, username, nickname, height, weight, gender, imageBytes, imageFilename, imageContentType
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("프로필이 설정되었습니다.", created));
    }

    @Operation(
            summary = "마이페이지 수정하기",
            description = "수정하기 버튼으로 한 번에 닉네임, 프로필 이미지, 키, 몸무게, 성별을 갱신합니다. multipart/form-data로 전송하며, 보내진 필드만 수정됩니다. 프로필 이미지 파일이 있으면 GCS에 저장 후 URL이 저장됩니다. 성별은 MALE 또는 FEMALE."
    )
    @PatchMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserProfileResponseDto>> updateMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "계정 아이디 (영문/숫자/_, 3~30자)") @RequestParam(value = "username", required = false) String username,
            @Parameter(description = "이름(닉네임)") @RequestParam(value = "nickname", required = false) String nickname,
            @Parameter(description = "키 (cm)") @RequestParam(value = "height", required = false) Float height,
            @Parameter(description = "몸무게 (kg)") @RequestParam(value = "weight", required = false) Float weight,
            @Parameter(description = "성별 (MALE, FEMALE)") @RequestParam(value = "gender", required = false) String gender,
            @Parameter(description = "프로필 이미지 파일 (선택)") @RequestParam(value = "file", required = false) MultipartFile file
    ) {
        Long userId = userDetails.getUser().getId();
        byte[] imageBytes = null;
        String imageFilename = null;
        String imageContentType = null;
        if (file != null && !file.isEmpty()) {
            try {
                imageBytes = file.getBytes();
                imageFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "profile.jpg";
                imageContentType = file.getContentType();
            } catch (IOException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ApiResponse.error("프로필 이미지 파일 읽기 실패: " + e.getMessage()));
            }
        }
        UserProfileResponseDto updated = userService.updateMyProfileWithForm(
                userId, username, nickname, height, weight, gender, imageBytes, imageFilename, imageContentType
        );
        return ResponseEntity.ok(ApiResponse.success("추가정보가 수정되었습니다.", updated));
    }

    @Operation(summary = "유저 검색", description = "계정 아이디(@username) 또는 이름으로 유저를 검색합니다. 최대 20명.")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<UserSearchResponseDto>>> searchUsers(
            @Parameter(description = "검색어 (username 또는 이름)") @RequestParam String keyword,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long requesterId = userDetails != null ? userDetails.getUser().getId() : null;
        List<UserSearchResponseDto> result = userService.searchUsers(keyword, requesterId);
        return ResponseEntity.ok(ApiResponse.success("유저 검색 성공", result));
    }

    @Operation(summary = "다른 유저 공개 프로필 조회", description = "userId로 다른 유저의 공개 프로필, 팔로워/팔로잉 수, 나와의 관계를 조회합니다.")
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserPublicProfileResponseDto>> getPublicProfile(
            @PathVariable Long userId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long requesterId = userDetails != null ? userDetails.getUser().getId() : null;
        UserPublicProfileResponseDto profile = followService.getPublicProfile(userId, requesterId);
        return ResponseEntity.ok(ApiResponse.success("프로필 조회 성공", profile));
    }
}
