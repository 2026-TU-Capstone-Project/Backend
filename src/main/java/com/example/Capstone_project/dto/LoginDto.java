package com.example.Capstone_project.dto; // 여기가 핵심 (소문자 dto)

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
public class LoginDto {
    private String email; // 아이디
    private String password; // 비밀번호

}