package com.example.Capstone_project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "refreshToken 요청 (로그아웃, 토큰 갱신 공통)")
public class RefreshTokenRequestDto {
    @NotBlank(message = "refreshToken이 필요합니다.")
    @Schema(description = "refreshToken", example = "abc123...", requiredMode = Schema.RequiredMode.REQUIRED)
    private String refreshToken;
}
