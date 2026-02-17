package com.example.Capstone_project.controller;

import com.example.Capstone_project.domain.User;
import com.example.Capstone_project.dto.LoginDto;
import com.example.Capstone_project.dto.RefreshTokenRequestDto;
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
import jakarta.validation.Valid;
import org.springframework.data.redis.core.StringRedisTemplate;

@Tag(name = "Auth", description = "로그인·회원가입·로그아웃 및 소셜 로그인 토큰 교환")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor 
public class AuthController {

    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;
    private final JwtTokenProvider jwtTokenProvider;
    private final com.example.Capstone_project.service.RefreshTokenService refreshTokenService; 

    @Operation(summary = "회원가입", description = "이메일·비밀번호·닉네임으로 회원가입합니다.")
    @SecurityRequirements
    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody SignupDto signupDto) {
        String result = authService.signup(signupDto);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "일반 로그인", description = "성공 시 **accessToken**과 **refreshToken**을 반환합니다. accessToken 만료 시 refreshToken으로 /token/refresh 호출.")
    @SecurityRequirements
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginDto loginDto) {
        User user = userRepository.findByEmail(loginDto.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("아이디가 존재하지 않습니다."));

        if (!passwordEncoder.matches(loginDto.getPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("비밀번호가 일치하지 않습니다.");
        }

        String accessToken = jwtTokenProvider.createToken(user.getEmail());
        String refreshToken = refreshTokenService.createAndStore(user.getEmail());

        Map<String, Object> response = new HashMap<>();
        response.put("accessToken", accessToken);
        response.put("refreshToken", refreshToken);
        response.put("message", "반가워요, " + user.getNickname() + "님!");
        response.put("email", user.getEmail());

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "소셜 로그인 토큰 교환 (Exchange)", description = "소셜 로그인 성공 후 받은 **tempKey**로 **accessToken**과 **refreshToken** 발급.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "교환 성공"),
        @ApiResponse(responseCode = "401", description = "유효하지 않거나 만료된 키")
    })
    @SecurityRequirements
    @PostMapping("/token/exchange")
    public ResponseEntity<?> exchangeToken(@RequestBody Map<String, String> request) {
        String rawKey = request.get("key");
        if (rawKey == null) {
            return ResponseEntity.badRequest().body("key값이 누락되었습니다.");
        }

        String redisKey = "TEMP_AUTH:" + rawKey;
        String accessToken = redisTemplate.opsForValue().get(redisKey);

        if (accessToken != null) {
            redisTemplate.delete(redisKey);
            String email = jwtTokenProvider.getSubject(accessToken);
            String refreshToken = refreshTokenService.createAndStore(email);
            return ResponseEntity.ok(Map.of(
                    "accessToken", accessToken,
                    "refreshToken", refreshToken
            ));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("만료되었거나 유효하지 않은 인증 코드입니다.");
        }
    }

    @Operation(summary = "토큰 갱신", description = "refreshToken으로 새 accessToken과 refreshToken을 발급합니다. accessToken 만료 시 클라이언트에서 자동 호출.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "갱신 성공"),
        @ApiResponse(responseCode = "401", description = "유효하지 않거나 만료된 refreshToken")
    })
    @SecurityRequirements
    @PostMapping("/token/refresh")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequestDto request) {
        String refreshToken = request.getRefreshToken();
        String email = refreshTokenService.getEmailByToken(refreshToken);
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("만료되었거나 유효하지 않은 refreshToken입니다.");
        }

        String newAccessToken = jwtTokenProvider.createToken(email);
        String newRefreshToken = refreshTokenService.rotate(refreshToken, email);

        return ResponseEntity.ok(Map.of(
                "accessToken", newAccessToken,
                "refreshToken", newRefreshToken
        ));
    }

    @Operation(summary = "로그아웃", description = "Redis에 저장된 refreshToken만 삭제.")
    @SecurityRequirements
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@Valid @RequestBody RefreshTokenRequestDto request) {
        refreshTokenService.invalidate(request.getRefreshToken());
        return ResponseEntity.ok(Map.of("message", "로그아웃되었습니다."));
    }
}