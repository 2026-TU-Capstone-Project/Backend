package com.example.Capstone_project.dto;

import com.example.Capstone_project.domain.Gender;
import lombok.Builder;
import lombok.Getter;

/**
 * Gemini 스타일 분석 결과 (스타일 텍스트 + 이미지 속 인물 성별)
 */
@Getter
@Builder
public class StyleAnalysisResult {
    private final String styleAnalysis;
    private final Gender resultGender;
}
