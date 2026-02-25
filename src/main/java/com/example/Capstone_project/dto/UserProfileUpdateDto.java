package com.example.Capstone_project.dto;

import com.example.Capstone_project.domain.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "마이페이지 추가정보 수정 요청 (닉네임, 프로필 이미지, 키, 몸무게, 성별). 보내진 필드만 수정.")
public class UserProfileUpdateDto {

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
}
