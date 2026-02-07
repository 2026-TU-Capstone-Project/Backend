package com.example.Capstone_project.service;

import com.example.Capstone_project.domain.User;
import com.example.Capstone_project.dto.LoginDto;
import com.example.Capstone_project.dto.SignupDto;
import com.example.Capstone_project.dto.SignupDto;
import com.example.Capstone_project.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // 1. 회원가입 기능
    @Transactional
    public String signup(SignupDto signupDto) {
        // 이메일 중복 검사
        if (userRepository.existsByEmail(signupDto.getEmail())) {
            return "이미 존재하는 이메일입니다.";
        }

        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(signupDto.getPassword());

        // 유저 생성 및 저장
        User user = new User(
                signupDto.getUsername(),
                signupDto.getEmail(),
                encodedPassword,
                signupDto.getNickname(),
                "ROLE_USER" // 기본 권한
        );
        userRepository.save(user);

        return "회원가입 성공";
    }

    // 2. 로그인 기능
    public String login(LoginDto loginDto) {
        User user = userRepository.findByEmail(loginDto.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("가입되지 않은 이메일입니다."));

        if (!passwordEncoder.matches(loginDto.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        return "로그인 성공! (토큰 발급 대기중)";
    }

    }
