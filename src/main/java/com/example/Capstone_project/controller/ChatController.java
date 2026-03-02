package com.example.Capstone_project.controller;

import com.example.Capstone_project.common.dto.ApiResponse;
import com.example.Capstone_project.config.CustomUserDetails;
import com.example.Capstone_project.dto.ChatRequestDto;
import com.example.Capstone_project.dto.ChatResponseDto;
import com.example.Capstone_project.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Chat", description = "Gemini AI 챗봇. 대화 중 스타일 추천 요청 시 Gemini function calling(style_recommend 등)으로 추천 후 답변 생성.")
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @Operation(
            summary = "챗봇 메시지 전송",
            description = "사용자 메시지를 보내면 AI가 응답합니다. '결혼식 옷 추천해줘', '캐주얼 스타일 추천' 등 요청 시 스타일 추천 툴이 호출되어 추천 결과를 반환합니다."
    )
    @PostMapping
    public ResponseEntity<ApiResponse<ChatResponseDto>> chat(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ChatRequestDto request
    ) {
        Long userId = userDetails.getUser().getId();
        ChatResponseDto response = chatService.chat(userId, request);
        return ResponseEntity.ok(ApiResponse.success("챗봇 응답", response));
    }
}
