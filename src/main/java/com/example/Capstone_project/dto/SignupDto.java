package com.example.Capstone_project.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
public class SignupDto {
    private String username;
    private String email;
    private String password;
    private String nickname;
}