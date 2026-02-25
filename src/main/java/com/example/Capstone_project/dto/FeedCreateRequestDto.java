package com.example.Capstone_project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "피드 작성 요청")
public class FeedCreateRequestDto {

    @NotNull(message = "저장된 가상 피팅(task) ID가 필요합니다.")
    @Schema(description = "내 옷장에서 선택한 가상 피팅 작업 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long fittingTaskId;

    @NotBlank(message = "피드 제목이 필요합니다.")
    @Schema(description = "피드 제목", requiredMode = Schema.RequiredMode.REQUIRED)
    private String feedTitle;

    @Schema(description = "피드 내용")
    private String feedContent;
}
