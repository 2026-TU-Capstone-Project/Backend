package com.example.Capstone_project.dto;

import com.example.Capstone_project.domain.Follow;
import com.example.Capstone_project.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "팔로워 / 팔로잉 목록 항목")
public class FollowUserDto {

    @Schema(description = "팔로우 관계 ID")
    private Long followId;
    @Schema(description = "사용자 ID")
    private Long userId;
    @Schema(description = "계정 아이디 (@username)")
    private String username;
    @Schema(description = "이름(닉네임)")
    private String nickname;
    @Schema(description = "프로필 이미지 URL")
    private String profileImageUrl;

    public static FollowUserDto fromFollower(Follow follow) {
        User u = follow.getFollower();
        return FollowUserDto.builder()
                .followId(follow.getId())
                .userId(u.getId())
                .username(u.getUsername())
                .nickname(u.getNickname())
                .profileImageUrl(u.getProfileImageUrl())
                .build();
    }

    public static FollowUserDto fromFollowing(Follow follow) {
        User u = follow.getFollowing();
        return FollowUserDto.builder()
                .followId(follow.getId())
                .userId(u.getId())
                .username(u.getUsername())
                .nickname(u.getNickname())
                .profileImageUrl(u.getProfileImageUrl())
                .build();
    }
}
