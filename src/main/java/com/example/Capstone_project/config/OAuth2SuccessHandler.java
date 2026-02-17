package com.example.Capstone_project.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate redisTemplate;

    @Value("${app.auth.redirect-uri}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        //  소셜 로그인 유저 정보를 꺼냄
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();


        String email;
        Map<String, Object> attributes = oAuth2User.getAttributes();

        if (attributes.containsKey("kakao_account")) { // 카카오인 경우
            Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
            email = (String) kakaoAccount.get("email");
        } else { // 구글인 경우
            email = (String) attributes.get("email");
        }

        // 1. 진짜 JWT 생성
        String realToken = jwtTokenProvider.createToken(email);
        
        // 2. 임시 코드(Key) 생성 (UUID 등 활용)
        String tempKey = UUID.randomUUID().toString();

        System.out.println("레디스에 저장 시도 중... Key: " + tempKey);
        
        // 3. Redis에 저장 (Key: tempKey, Value: realToken, 유효시간: 2분)
        redisTemplate.opsForValue().set(
            "TEMP_AUTH:" + tempKey, 
            realToken, 
            Duration.ofMinutes(2)
        );

        // 4. 플러터 앱 스킴으로 리다이렉트
        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("key", tempKey)
                .build().toUriString();

        System.out.println("레디스 저장 완료!");
        
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}