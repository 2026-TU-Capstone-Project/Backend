package com.example.Capstone_project.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "가상 피팅 작업 상태")
public enum FittingStatus {
    @Schema(description = "대기 중")
    WAITING,
    @Schema(description = "AI 처리 중")
    PROCESSING,
    @Schema(description = "완료 (결과 이미지 생성됨)")
    COMPLETED,
    @Schema(description = "실패")
    FAILED
}