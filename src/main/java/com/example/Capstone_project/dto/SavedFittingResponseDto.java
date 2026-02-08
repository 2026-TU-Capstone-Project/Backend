package com.example.Capstone_project.dto;

import com.example.Capstone_project.domain.Clothes;
import com.example.Capstone_project.domain.FittingStatus;
import com.example.Capstone_project.domain.FittingTask;
import com.example.Capstone_project.domain.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "저장한 코디(가상 피팅 결과) 응답 정보")
public class SavedFittingResponseDto {

    @Schema(description = "피팅 작업 ID")
    private Long id;
    @Schema(description = "사용자 ID")
    private Long userId;
    @Schema(description = "상의(Clothes) ID")
    private Long topId;
    @Schema(description = "하의(Clothes) ID")
    private Long bottomId;
    @Schema(description = "가상 피팅 결과 이미지 URL")
    private String resultImgUrl;
    @Schema(description = "작업 상태")
    private FittingStatus status;
    @Schema(description = "스타일 분석 텍스트")
    private String styleAnalysis;
    @Schema(description = "결과 이미지 인물 성별")
    private Gender resultGender;
    @Schema(description = "전신 사진 URL")
    private String bodyImgUrl;
    @Schema(description = "저장 여부")
    private boolean saved;
    @Schema(description = "상의 정보 (순환 참조 방지용 단순 DTO)")
    private ClothesSummary topClothes;
    @Schema(description = "하의 정보 (순환 참조 방지용 단순 DTO)")
    private ClothesSummary bottomClothes;

    @Getter
    @Builder
    @Schema(description = "옷 요약 정보")
    public static class ClothesSummary {
        private Long id;
        private String name;
        private String category;
        private String imgUrl;
    }

    public static SavedFittingResponseDto from(FittingTask task) {
        if (task == null) return null;

        ClothesSummary topSummary = null;
        if (task.getTop() != null) {
            Clothes top = task.getTop();
            topSummary = ClothesSummary.builder()
                    .id(top.getId())
                    .name(top.getName())
                    .category(top.getCategory())
                    .imgUrl(top.getImgUrl())
                    .build();
        }

        ClothesSummary bottomSummary = null;
        if (task.getBottom() != null) {
            Clothes bottom = task.getBottom();
            bottomSummary = ClothesSummary.builder()
                    .id(bottom.getId())
                    .name(bottom.getName())
                    .category(bottom.getCategory())
                    .imgUrl(bottom.getImgUrl())
                    .build();
        }

        return SavedFittingResponseDto.builder()
                .id(task.getId())
                .userId(task.getUserId())
                .topId(task.getTopId())
                .bottomId(task.getBottomId())
                .resultImgUrl(task.getResultImgUrl())
                .status(task.getStatus())
                .styleAnalysis(task.getStyleAnalysis())
                .resultGender(task.getResultGender())
                .bodyImgUrl(task.getBodyImgUrl())
                .saved(task.isSaved())
                .topClothes(topSummary)
                .bottomClothes(bottomSummary)
                .build();
    }
}
