package com.example.Capstone_project.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
public class Clothes {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 1. 기본 정보 (Basic)
    private String category;  // 사용자가 입력 (Top, Bottom...)
    private String name;      // AI 자동 생성 (Black Leather Jacket...)
    private String imgUrl;    // 이미지 주소
    private String member;    // 사용자 ID (나중에 로그인 구현 시 사용)

    // 2. 핵심 분석 정보 (Key Specs)
    private String color;     // 색상 (Black, Navy...)
    private String season;    // 계절 (Spring/Fall, Summer...)
    private String material;  // 소재 (Cotton, Denim, Leather...)
    private String thickness; // 두께 (Thin, Medium, Thick)

    // 3. 디자인 상세 (Design Detail)
    private String neckLine;  // 넥라인 (Round, V-neck, Collar...)
    private String sleeveType;// 소매 (Long, Short, Sleeveless...)
    private String pattern;   // 패턴 (Solid, Stripe, Check...)
    private String closure;   // 여밈 (Zipper, Button, None...)
    private String style;     // 스타일 (Casual, Formal, Sporty...)

    // 4. 구조 및 텍스처 (Structure & Texture)
    private String fit;       // 핏 (Oversized, Slim, Regular...)
    private String length;    // 기장 (Crop, Midi, Maxi...)
    private String texture;   // 질감 (Shiny, Soft, Rough...)
    private String detail;    // 디테일 (Pocket, Ruffle, Distressed...)

    // 5. 맥락 정보 (Context - T.P.O)
    private String occasion;  // 상황 (Date, Office, Home...)
    private String brand;     // 브랜드
    private Integer price;    // 가격
    private String buyUrl;    // 구매 링크
}