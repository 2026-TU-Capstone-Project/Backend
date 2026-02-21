package com.example.Capstone_project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Kakao Native SDK 로그인 요청 (accessToken)")
public class KakaoLoginRequestDto {
    @NotBlank(message = "accessToken이 필요합니다.")
    @Schema(description = "Kakao SDK에서 발급받은 액세스 토큰", requiredMode = Schema.RequiredMode.REQUIRED)
    private String accessToken;
}
