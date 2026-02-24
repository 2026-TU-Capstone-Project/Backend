package com.example.Capstone_project.controller;

import com.example.Capstone_project.common.dto.ApiResponse;
import com.example.Capstone_project.config.CustomUserDetails;
import com.example.Capstone_project.dto.UserProfileResponseDto;
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

@Tag(name = "User", description = "마이페이지·추가정보 조회/수정")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "마이페이지 조회", description = "로그인 사용자의 추가정보(닉네임, 프로필 이미지, 키, 몸무게, 성별)를 조회합니다.")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponseDto>> getMyProfile(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUser().getId();
        UserProfileResponseDto profile = userService.getMyProfile(userId);
        return ResponseEntity.ok(ApiResponse.success("마이페이지 조회 성공", profile));
    }

    @Operation(
            summary = "마이페이지 수정하기",
            description = "수정하기 버튼으로 한 번에 닉네임, 프로필 이미지, 키, 몸무게를 갱신합니다. multipart/form-data로 전송하며, 보내진 필드만 수정됩니다. 프로필 이미지 파일이 있으면 GCS에 저장 후 URL이 저장됩니다."
    )
    @PatchMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserProfileResponseDto>> updateMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "닉네임") @RequestParam(value = "nickname", required = false) String nickname,
            @Parameter(description = "키 (cm)") @RequestParam(value = "height", required = false) Float height,
            @Parameter(description = "몸무게 (kg)") @RequestParam(value = "weight", required = false) Float weight,
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
                userId, nickname, height, weight, imageBytes, imageFilename, imageContentType
        );
        return ResponseEntity.ok(ApiResponse.success("추가정보가 수정되었습니다.", updated));
    }
}
