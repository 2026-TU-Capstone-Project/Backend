package com.example.Capstone_project.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Getter @Setter
public class Member {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 기본 로그인 정보
    private String email;       // 이메일
    private String password;    // 비밀번호
    private String name;        // 이름
    private String nickname;    // 닉네임

    // 신체 정보
    private String gender;      // 성별 (MALE, FEMALE)
    private Double height;      // 키 (cm)
    private Double weight;      // 몸무게 (kg)
    private String profileImgUrl; // 프로필 사진

    // 관리용
    private String role;        // 권한
    private LocalDateTime createdAt; // 가입일

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}