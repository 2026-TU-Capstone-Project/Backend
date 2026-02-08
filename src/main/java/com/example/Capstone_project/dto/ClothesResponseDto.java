package com.example.Capstone_project.dto;

import com.example.Capstone_project.domain.Clothes;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "옷 응답 정보")
public class ClothesResponseDto {

    @Schema(description = "옷 ID")
    private Long id;
    @Schema(description = "카테고리 (Top, Bottom, Shoes)")
    private String category;
    @Schema(description = "옷 이름 (AI 분석 결과)")
    private String name;
    @Schema(description = "이미지 URL")
    private String imgUrl;
    @Schema(description = "색상")
    private String color;
    @Schema(description = "시즌")
    private String season;
    @Schema(description = "소재")
    private String material;
    @Schema(description = "두께")
    private String thickness;
    @Schema(description = "넥라인")
    private String neckLine;
    @Schema(description = "소매 타입")
    private String sleeveType;
    @Schema(description = "패턴")
    private String pattern;
    @Schema(description = "여밈 방식")
    private String closure;
    @Schema(description = "스타일")
    private String style;
    @Schema(description = "핏")
    private String fit;
    @Schema(description = "길이")
    private String length;
    @Schema(description = "텍스처")
    private String texture;
    @Schema(description = "디테일")
    private String detail;
    @Schema(description = "착용 상황")
    private String occasion;
    @Schema(description = "브랜드")
    private String brand;
    @Schema(description = "가격")
    private Integer price;
    @Schema(description = "구매 링크")
    private String buyUrl;
    @Schema(description = "등록일시")
    private LocalDateTime createdAt;

    public static ClothesResponseDto from(Clothes clothes) {
        if (clothes == null) return null;
        return ClothesResponseDto.builder()
                .id(clothes.getId())
                .category(clothes.getCategory())
                .name(clothes.getName())
                .imgUrl(clothes.getImgUrl())
                .color(clothes.getColor())
                .season(clothes.getSeason())
                .material(clothes.getMaterial())
                .thickness(clothes.getThickness())
                .neckLine(clothes.getNeckLine())
                .sleeveType(clothes.getSleeveType())
                .pattern(clothes.getPattern())
                .closure(clothes.getClosure())
                .style(clothes.getStyle())
                .fit(clothes.getFit())
                .length(clothes.getLength())
                .texture(clothes.getTexture())
                .detail(clothes.getDetail())
                .occasion(clothes.getOccasion())
                .brand(clothes.getBrand())
                .price(clothes.getPrice())
                .buyUrl(clothes.getBuyUrl())
                .createdAt(clothes.getCreatedAt())
                .build();
    }
}
