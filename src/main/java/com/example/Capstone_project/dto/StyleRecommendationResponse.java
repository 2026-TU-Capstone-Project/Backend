package com.example.Capstone_project.dto;

import com.example.Capstone_project.domain.FittingTask;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class StyleRecommendationResponse {

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
    public static class RecommendationItem {
        private final Long taskId;
        private final Double score;  // 유사도 0~1, 1에 가까울수록 유사
        private final String resultImgUrl;
        private final String styleAnalysis;
        private final Long topId;
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
