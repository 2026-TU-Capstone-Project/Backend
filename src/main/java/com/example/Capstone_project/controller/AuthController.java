package com.example.Capstone_project.controller;


import com.example.Capstone_project.domain.User;
import com.example.Capstone_project.dto.LoginDto;
import com.example.Capstone_project.dto.SignupDto;
import com.example.Capstone_project.repository.UserRepository;
import com.example.Capstone_project.service.AuthService;
import com.example.Capstone_project.config.JwtTokenProvider;
import io.netty.util.AsciiString;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    @Autowired // 2. 토큰 발행기 주입
    private JwtTokenProvider jwtTokenProvider;

    // 회원가입
    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody SignupDto signupDto) {
        String result = authService.signup(signupDto);
        return ResponseEntity.ok(result);
    }


    // 로그인
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginDto loginDto) {
        // 3. 아이디(이메일)로 유저 찾기
        User user = userRepository.findByEmail(loginDto.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("아이디가 존재하지 않습니다."));

        // 4. 비밀번호 일치 확인
        if (!passwordEncoder.matches(loginDto.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        // 5. 로그인 성공 시 진짜 토큰 생성!
        String token = jwtTokenProvider.createToken(user.getEmail());

        // 6. 리턴할 데이터 보따리(Map) 구성
        Map<String, Object> response = new HashMap<>();
        response.put("accessToken", token); // 포스트맨에서 복사할 핵심!
        response.put("message", "반가워요, " + user.getNickname() + "님!");
        response.put("email", user.getEmail());

        // 7. ResponseEntity에 담아서 전송
        return ResponseEntity.ok(response);
    }
}
