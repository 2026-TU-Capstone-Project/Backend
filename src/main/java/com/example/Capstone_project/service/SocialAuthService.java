package com.example.Capstone_project.service;

import com.example.Capstone_project.domain.SocialProvider;
import com.example.Capstone_project.domain.User;
import com.example.Capstone_project.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.UUID;

/**
 * Native SDK + API 방식 소셜 로그인.
 * Google idToken / Kakao accessToken 검증 후 사용자 조회·생성.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SocialAuthService {

    private static final String GOOGLE_TOKENINFO = "https://oauth2.googleapis.com/tokeninfo?id_token=";
    private static final String KAKAO_USER_ME = "https://kapi.kakao.com/v2/user/me";

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final WebClient webClient = WebClient.builder().build();

    /**
     * Google idToken 검증 후 사용자 정보 반환.
     * @return Map with email, name, picture (nullable)
     */
    public Map<String, String> verifyGoogleIdToken(String idToken) {
        String url = GOOGLE_TOKENINFO + idToken;
        String body = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("Google idToken 검증 실패");
        }
        try {
            JsonNode node = objectMapper.readTree(body);
            String email = node.has("email") ? node.get("email").asText() : null;
            if (email == null || email.isBlank()) {
                throw new IllegalArgumentException("Google 응답에 email이 없습니다.");
            }
            return Map.of(
                    "email", email,
                    "name", node.has("name") ? node.get("name").asText() : "",
                    "picture", node.has("picture") ? node.get("picture").asText() : ""
            );
        } catch (Exception e) {
            if (e instanceof IllegalArgumentException) throw (IllegalArgumentException) e;
            log.warn("Google idToken 파싱 실패: {}", e.getMessage());
            throw new IllegalArgumentException("Google idToken 검증 실패", e);
        }
    }

    /**
     * Kakao accessToken으로 사용자 정보 조회.
     * @return Map with email, nickname, profileImageUrl (nullable)
     */
    public Map<String, String> verifyKakaoAccessToken(String accessToken) {
        String body = webClient.get()
                .uri(KAKAO_USER_ME)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("Kakao accessToken 검증 실패");
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode kakaoAccount = root.has("kakao_account") ? root.get("kakao_account") : null;
            JsonNode profile = kakaoAccount != null && kakaoAccount.has("profile") ? kakaoAccount.get("profile") : null;
            JsonNode properties = root.has("properties") ? root.get("properties") : null;

            String email = kakaoAccount != null && kakaoAccount.has("email") ? kakaoAccount.get("email").asText() : null;
            if (email == null || email.isBlank()) {
                throw new IllegalArgumentException("Kakao 응답에 email이 없습니다. 동의 항목에서 이메일 수집 동의가 필요합니다.");
            }
            String nickname = profile != null && profile.has("nickname") ? profile.get("nickname").asText()
                    : (properties != null && properties.has("nickname") ? properties.get("nickname").asText() : "");
            String profileImageUrl = profile != null && profile.has("profile_image_url") ? profile.get("profile_image_url").asText() : "";

            return Map.of(
                    "email", email,
                    "nickname", nickname != null ? nickname : "",
                    "profileImageUrl", profileImageUrl != null ? profileImageUrl : ""
            );
        } catch (Exception e) {
            if (e instanceof IllegalArgumentException) throw (IllegalArgumentException) e;
            log.warn("Kakao 응답 파싱 실패: {}", e.getMessage());
            throw new IllegalArgumentException("Kakao accessToken 검증 실패", e);
        }
    }

    @Transactional
    public User findOrCreateGoogleUser(String email, String name) {
        return findOrCreateSocialUser(email, name != null ? name : "", SocialProvider.GOOGLE);
    }

    @Transactional
    public User findOrCreateKakaoUser(String email, String nickname) {
        return findOrCreateSocialUser(email, nickname != null ? nickname : "", SocialProvider.KAKAO);
    }

    private User findOrCreateSocialUser(String email, String nickname, SocialProvider provider) {
        return userRepository.findByEmail(email)
                .map(user -> {
                    user.setNickname(nickname != null && !nickname.isBlank() ? nickname : user.getNickname());
                    user.setSocialProvider(provider);
                    return userRepository.save(user);
                })
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setEmail(email);
                    newUser.setNickname(nickname != null && !nickname.isBlank() ? nickname : email.split("@")[0]);
                    newUser.setSocialProvider(provider);
                    newUser.setPassword("SOCIAL_" + UUID.randomUUID().toString().substring(0, 12));
                    newUser.setUsername(provider.name() + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
                    newUser.setRole("ROLE_USER");
                    return userRepository.save(newUser);
                });
    }
}
