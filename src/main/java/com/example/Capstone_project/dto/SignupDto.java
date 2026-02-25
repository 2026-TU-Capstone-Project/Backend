package com.example.Capstone_project.dto;

import com.example.Capstone_project.domain.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "회원가입 요청")
public class SignupDto {
    @Schema(description = "이메일 (로그인 아이디)", example = "user@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;
    @Schema(description = "비밀번호", example = "password123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;
    @Schema(description = "성별 (MALE, FEMALE)", example = "MALE")
    private Gender gender;
}