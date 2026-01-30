package com.example.Capstone_project.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
public class ClothesAnalysisResultDto {
    private String category;    // 카테고리 (Top, Bottom, Shoes)
    private String color;       // 색상
    private String material;    // 소재
    private String pattern;     // 무늬
    private String neckLine;    // 넥라인
    private String sleeveType;  // 소매
    private String closure;     // 여밈
    private String style;       // 스타일
    private String fit;         // 핏
    private String length;      // 길이
    private String texture;     // 질감
    private String detail;      // 디테일
    private String season;      // 계절
    private String thickness;   // 두께
    private String occasion;    // 상황
}