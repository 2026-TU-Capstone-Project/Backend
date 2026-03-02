package com.example.Capstone_project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "챗봇 응답")
public class ChatResponseDto {

    @Schema(description = "AI 응답 메시지")
    private String message;

    @Schema(description = "스타일 추천 결과. 챗봇이 style_recommend 툴을 사용했을 때만 포함됨")
    private StyleRecommendationResponse recommendations;

    @Schema(description = "상의 추천 결과. 챗봇이 recommend_tops 툴을 사용했을 때만 포함됨")
    private ClothesRecommendationResponse recommendationsTops;

    @Schema(description = "하의 추천 결과. 챗봇이 recommend_bottoms 툴을 사용했을 때만 포함됨")
    private ClothesRecommendationResponse recommendationsBottoms;
}
