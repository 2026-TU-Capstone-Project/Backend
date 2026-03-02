package com.example.Capstone_project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "챗봇 메시지 요청 (대화 이력 + 새 메시지)")
public class ChatRequestDto {

    @Schema(description = "이전 대화 목록 (선택). 비우면 새 대화로 처리.")
    private List<ChatMessageDto> history = List.of();

    @NotBlank(message = "메시지를 입력해주세요.")
    @Schema(description = "사용자가 보낼 메시지", required = true, example = "결혼식에 입을 옷 추천해줘")
    private String message;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "대화 한 턴")
    public static class ChatMessageDto {
        @Schema(description = "역할: user 또는 assistant", example = "user")
        private String role;

        @Schema(description = "메시지 내용")
        private String content;
    }
}
