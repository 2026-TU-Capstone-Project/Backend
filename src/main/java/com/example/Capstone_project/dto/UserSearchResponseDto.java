package com.example.Capstone_project.dto;

import com.example.Capstone_project.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "유저 검색 결과 항목")
public class UserSearchResponseDto {

    @Schema(description = "사용자 ID")
    private Long userId;
    @Schema(description = "계정 아이디 (@username)")
    private String username;
    @Schema(description = "이름(닉네임)")
    private String nickname;
    @Schema(description = "프로필 이미지 URL")
    private String profileImageUrl;
    @Schema(description = "나와의 팔로우 상태: null=팔로우 안함, PENDING=신청 중, ACCEPTED=팔로우 중")
    private String followStatus;

    public static UserSearchResponseDto from(User user, String followStatus) {
        return UserSearchResponseDto.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .followStatus(followStatus)
                .build();
    }
}
