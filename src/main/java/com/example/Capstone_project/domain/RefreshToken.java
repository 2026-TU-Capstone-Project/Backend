package com.example.Capstone_project.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
public class RefreshToken {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String token;       // 토큰 값 (긴 문자열)

    private String keyEmail;    // 누구의 토큰인지 (사용자 이메일)

    // 이 부분은 나중에 "로그인 유지 시간"을 설정할 때 사용할 부분ㄴ
}