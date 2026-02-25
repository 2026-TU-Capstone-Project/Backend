package com.example.Capstone_project.service;

import com.example.Capstone_project.domain.User;
import com.example.Capstone_project.dto.LoginDto;
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

    @Transactional
    public String signup(SignupDto signupDto) {
        if (userRepository.existsByEmail(signupDto.getEmail())) {
            return "이미 존재하는 이메일입니다.";
        }

        String encodedPassword = passwordEncoder.encode(signupDto.getPassword());

        // 닉네임은 이메일 @ 앞부분으로 자동 설정
        String nickname = signupDto.getEmail().split("@")[0];
        User user = new User(
                signupDto.getEmail(),
                encodedPassword,
                nickname,
                "ROLE_USER"
        );
        if (signupDto.getGender() != null) {
            user.setGender(signupDto.getGender());
        }
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
