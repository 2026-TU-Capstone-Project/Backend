package com.example.Capstone_project.dto;

import com.example.Capstone_project.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "다른 유저 공개 프로필 조회 응답")
public class UserPublicProfileResponseDto {

    @Schema(description = "사용자 ID")
    private Long userId;
    @Schema(description = "계정 아이디 (@username)")
    private String username;
    @Schema(description = "이름(닉네임)")
    private String nickname;
    @Schema(description = "프로필 이미지 URL")
    private String profileImageUrl;
    @Schema(description = "팔로워 수")
    private long followerCount;
    @Schema(description = "팔로잉 수")
    private long followingCount;
    @Schema(description = "나와의 팔로우 상태: null=팔로우 안함, PENDING=신청 중, ACCEPTED=팔로우 중")
    private String followStatus;
    @Schema(description = "본인 여부")
    private boolean isMe;
    @Schema(description = "상대가 나를 팔로우하는지 여부 (맞팔 확인용). 비로그인 또는 본인 조회 시 false.")
    private boolean followsMeBack;

    public static UserPublicProfileResponseDto from(User user, long followerCount, long followingCount,
                                                     String followStatus, boolean isMe, boolean followsMeBack) {
        return UserPublicProfileResponseDto.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .followerCount(followerCount)
                .followingCount(followingCount)
                .followStatus(followStatus)
                .isMe(isMe)
                .followsMeBack(followsMeBack)
                .build();
    }
}
