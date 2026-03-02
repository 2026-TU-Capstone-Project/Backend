package com.example.Capstone_project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "상의/하의 추천 결과 (스타일 추천과 동일한 유사도 검색, 해당 카테고리 옷만 반환)")
public class ClothesRecommendationResponse {

    @Schema(description = "추천 옷 목록 (유사도 순)")
    private List<ClothesRecommendationItem> items;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "추천 옷 1건 (옷 정보 + 유사도 점수)")
    public static class ClothesRecommendationItem {
        @Schema(description = "옷 정보")
        private ClothesResponseDto clothes;
        @Schema(description = "유사도 점수 (0~1)")
        private Double score;
    }
}
