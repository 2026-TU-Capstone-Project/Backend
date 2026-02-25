package com.example.Capstone_project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "피드 게시 전 미리보기 (내 옷장 스타일 선택 시)")
public class FeedPreviewResponseDto {

    @Schema(description = "스타일 이미지 URL (가상 피팅 결과)")
    private String styleImageUrl;
    @Schema(description = "상의 이미지 URL")
    private String topImageUrl;
    @Schema(description = "상의 이름")
    private String topName;
    @Schema(description = "하의 이미지 URL")
    private String bottomImageUrl;
    @Schema(description = "하의 이름")
    private String bottomName;
}
