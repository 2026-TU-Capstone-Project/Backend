package com.example.Capstone_project.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder // 데이터 넣을 때 필수!
public class Clothes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;



    // --- [1. 기본 정보 (Basic)] ---
    private String category;    // Top, Bottom, Shoes (사용자 선택)
    private String name;        // AI가 지어준 이름 (예: 시크한 블랙 라이더 자켓)
    private String imgUrl;      // S3 이미지 주소
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false) // DB에 user_id라는 이름으로 주인님 번호가 저장됩니다.
    private User user;

    // --- [2. 핵심 스펙 (Key Specs)] ---
    private String color;       // Black, Navy...
    private String season;      // Spring, Summer...
    private String material;    // Cotton, Leather...
    private String thickness;   // Thin, Thick... (두께)

    // --- [3. 디자인 상세 (Design Detail)] ---
    private String neckLine;    // Round, V-neck...
    private String sleeveType;  // Long, Short...
    private String pattern;     // Solid, Stripe...
    private String closure;     // Zipper, Button... (여밈)
    private String style;       // Casual, Street...

    // --- [4. 구조 및 텍스처 (Structure & Texture)] ---
    private String fit;         // Oversized, Slim...
    private String length;      // Crop, Long...
    private String texture;     // Soft, Rough...
    private String detail;      // Pocket, Distressed...

    // --- [5. 맥락 정보 (Context)] ---
    private String occasion;    // Date, Office...
    private String brand;       // Nike, Zara...
    private Integer price;      // 가격
    private String buyUrl;      // 구매 링크

    // 생성 시간 자동 기록
    private LocalDateTime createdAt;

    @PrePersist
    public void createDate() {
        this.createdAt = LocalDateTime.now();
    }
}