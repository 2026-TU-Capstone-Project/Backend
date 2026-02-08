package com.example.Capstone_project.dto;

import com.example.Capstone_project.domain.FittingTask;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
@Schema(description = "스타일 추천 결과 (최대 10개)")
public class StyleRecommendationResponse {
    @Schema(description = "추천 목록")
    private final List<RecommendationItem> recommendations;

    public static StyleRecommendationResponse from(List<FittingTaskWithScore> tasksWithScore) {
        List<RecommendationItem> items = tasksWithScore.stream()
                .map(RecommendationItem::from)
                .collect(Collectors.toList());
        return StyleRecommendationResponse.builder()
                .recommendations(items)
                .build();
    }

    @Getter
    @Builder
    @Schema(description = "추천 항목 1건")
    public static class RecommendationItem {
        @Schema(description = "피팅 작업 ID")
        private final Long taskId;
        @Schema(description = "유사도 점수 (0~1, 1에 가까울수록 유사)")
        private final Double score;
        @Schema(description = "가상 피팅 결과 이미지 URL")
        private final String resultImgUrl;
        @Schema(description = "스타일 분석 텍스트")
        private final String styleAnalysis;
        @Schema(description = "상의(Clothes) ID")
        private final Long topId;
        @Schema(description = "하의(Clothes) ID")
        private final Long bottomId;

        public static RecommendationItem from(FittingTaskWithScore tws) {
            FittingTask task = tws.getTask();
            return RecommendationItem.builder()
                    .taskId(task.getId())
                    .score(Math.round(tws.getScore() * 100.0) / 100.0)
                    .resultImgUrl(task.getResultImgUrl())
                    .styleAnalysis(task.getStyleAnalysis())
                    .topId(task.getTopId())
                    .bottomId(task.getBottomId())
                    .build();
        }
    }
}
