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

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;

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

        // JwtTokenProvider
        String token = jwtTokenProvider.createToken(email);

        // 토큰을 가지고 테스트용 Swagger 화면으로 리다이렉트합니다.
        String targetUrl = UriComponentsBuilder.fromUriString("http://localhost:8080/swagger-ui/index.html")
                .queryParam("token", token)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}