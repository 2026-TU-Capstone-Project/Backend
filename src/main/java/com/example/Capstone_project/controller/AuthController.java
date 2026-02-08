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
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "로그인·회원가입 (인증 불필요)")
@SecurityRequirements
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Operation(summary = "회원가입", description = "이메일·비밀번호·닉네임으로 회원가입합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "가입 성공 메시지"),
        @ApiResponse(responseCode = "400", description = "중복 이메일 등 유효성 오류")
    })
    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody SignupDto signupDto) {
        String result = authService.signup(signupDto);
        return ResponseEntity.ok(result);
    }

    @Operation(
        summary = "로그인",
        description = "이메일·비밀번호로 로그인합니다. 성공 시 **accessToken**을 반환합니다. 이후 API 호출 시 `Authorization: Bearer {accessToken}` 헤더에 넣어 보내세요."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "로그인 성공",
            content = @Content(schema = @Schema(description = "accessToken, message, email 포함")))
    })
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
