package com.example.Capstone_project.dto;

import com.example.Capstone_project.domain.Follow;
import com.example.Capstone_project.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "받은 팔로우 신청 항목")
public class FollowRequestItemDto {

    @Schema(description = "팔로우 관계 ID (수락/거절 시 사용)")
    private Long followId;
    @Schema(description = "신청자 사용자 ID")
    private Long requesterId;
    @Schema(description = "신청자 계정 아이디 (@username)")
    private String username;
    @Schema(description = "신청자 이름(닉네임)")
    private String nickname;
    @Schema(description = "신청자 프로필 이미지 URL")
    private String profileImageUrl;
    @Schema(description = "신청 시각")
    private LocalDateTime requestedAt;

    public static FollowRequestItemDto from(Follow follow) {
        User u = follow.getFollower();
        return FollowRequestItemDto.builder()
                .followId(follow.getId())
                .requesterId(u.getId())
                .username(u.getUsername())
                .nickname(u.getNickname())
                .profileImageUrl(u.getProfileImageUrl())
                .requestedAt(follow.getCreatedAt())
                .build();
    }
}
