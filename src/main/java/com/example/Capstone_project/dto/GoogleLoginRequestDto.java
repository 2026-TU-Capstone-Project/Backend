package com.example.Capstone_project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Google Native SDK 로그인 요청 (idToken)")
public class GoogleLoginRequestDto {
    @NotBlank(message = "idToken이 필요합니다.")
    @Schema(description = "Google Sign-In SDK에서 발급받은 ID 토큰", requiredMode = Schema.RequiredMode.REQUIRED)
    private String idToken;
}
