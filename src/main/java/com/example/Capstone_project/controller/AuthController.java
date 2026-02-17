package com.example.Capstone_project.controller;

import com.example.Capstone_project.domain.User;
import com.example.Capstone_project.dto.LoginDto;
import com.example.Capstone_project.dto.SignupDto;
import com.example.Capstone_project.repository.UserRepository;
import com.example.Capstone_project.service.AuthService;
import com.example.Capstone_project.config.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus; 
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder; 
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.redis.core.StringRedisTemplate;

@Tag(name = "Auth", description = "로그인·회원가입 및 소셜 로그인 토큰 교환")
@SecurityRequirements
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor 
public class AuthController {

    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;
    private final JwtTokenProvider jwtTokenProvider; 

    @Operation(summary = "회원가입", description = "이메일·비밀번호·닉네임으로 회원가입합니다.")
    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody SignupDto signupDto) {
        String result = authService.signup(signupDto);
        return ResponseEntity.ok(result);
    }

    @Operation(
        summary = "일반 로그인",
        description = "성공 시 **accessToken**을 반환합니다."
    )
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginDto loginDto) {
        User user = userRepository.findByEmail(loginDto.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("아이디가 존재하지 않습니다."));

        if (!passwordEncoder.matches(loginDto.getPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("비밀번호가 일치하지 않습니다.");
        }

        String token = jwtTokenProvider.createToken(user.getEmail());

        Map<String, Object> response = new HashMap<>();
        response.put("accessToken", token);
        response.put("message", "반가워요, " + user.getNickname() + "님!");
        response.put("email", user.getEmail());

        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "소셜 로그인 토큰 교환 (Exchange)",
        description = "소셜 로그인 성공 후 받은 **tempKey**를 이용해 실제 **accessToken**을 발급받습니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "교환 성공"),
        @ApiResponse(responseCode = "401", description = "유효하지 않거나 만료된 키")
    })
    @PostMapping("/token/exchange")
    public ResponseEntity<?> exchangeToken(@RequestBody Map<String, String> request) {
        String rawKey = request.get("key");
        if (rawKey == null) {
            return ResponseEntity.badRequest().body("key값이 누락되었습니다.");
        }

        String redisKey = "TEMP_AUTH:" + rawKey;
        String realToken = redisTemplate.opsForValue().get(redisKey);

        if (realToken != null) {
            redisTemplate.delete(redisKey); 
            return ResponseEntity.ok(Map.of("accessToken", realToken));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("만료되었거나 유효하지 않은 인증 코드입니다.");
        }
    }
}