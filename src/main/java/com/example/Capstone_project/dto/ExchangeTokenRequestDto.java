package com.example.Capstone_project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "소셜 로그인 토큰 교환 요청")
public class ExchangeTokenRequestDto {
    @NotBlank(message = "key가 필요합니다.")
    @Schema(description = "소셜 로그인 성공 후 받은 tempKey", example = "uuid-temp-key", requiredMode = Schema.RequiredMode.REQUIRED)
    private String key;
}
