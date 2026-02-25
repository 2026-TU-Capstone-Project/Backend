package com.example.Capstone_project.dto;

import com.example.Capstone_project.domain.Gender;
import com.example.Capstone_project.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "마이페이지 조회 응답 (닉네임, 프로필 이미지, 키, 몸무게, 성별 등)")
public class UserProfileResponseDto {

    @Schema(description = "사용자 ID")
    private Long userId;
    @Schema(description = "이메일")
    private String email;
    @Schema(description = "닉네임")
    private String nickname;
    @Schema(description = "프로필 이미지 URL")
    private String profileImageUrl;
    @Schema(description = "키 (cm)")
    private Float height;
    @Schema(description = "몸무게 (kg)")
    private Float weight;
    @Schema(description = "성별 (MALE, FEMALE)")
    private Gender gender;

    public static UserProfileResponseDto from(User user) {
        if (user == null) return null;
        return UserProfileResponseDto.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .height(user.getHeight())
                .weight(user.getWeight())
                .gender(user.getGender())
                .build();
    }
}
