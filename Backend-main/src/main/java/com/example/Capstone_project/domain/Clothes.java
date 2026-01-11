package com.example.Capstone_project.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
public class Clothes {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;        // 옷 이름

    private String imgUrl;      // 옷 사진 주소 (S3 URL)

    // Ai 자동 태깅
    private String category;    // 종류
    private String color;       // 색상
    private String material;    // 소재
    private String thickness;   // 두께

    // 내 옷장
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;
}