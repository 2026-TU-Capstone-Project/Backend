package com.example.Capstone_project.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Refresh Token 관리 (Redis 저장).
 * accessToken 만료 시 refreshToken으로 갱신.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final String KEY_PREFIX = "refresh:";

    private final StringRedisTemplate redisTemplate;

    @Value("${app.auth.refresh-token-ttl-days:1}")
    private long ttlDays;

    /** refreshToken 생성 후 Redis에 저장, 토큰 값 반환 */
    public String createAndStore(String email) {
        String token = UUID.randomUUID().toString().replace("-", "");
        String key = KEY_PREFIX + token;
        redisTemplate.opsForValue().set(key, email, ttlDays, TimeUnit.DAYS);
        log.debug("RefreshToken 생성 - email: {}", email);
        return token;
    }

    /** refreshToken으로 email 조회. 유효하면 email, 아니면 null */
    public String getEmailByToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) return null;
        try {
            String key = KEY_PREFIX + refreshToken;
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("RefreshToken 조회 실패: {}", e.getMessage());
            return null;
        }
    }

    /** refreshToken 무효화 (로그아웃 시) */
    public void invalidate(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) return;
        try {
            redisTemplate.delete(KEY_PREFIX + refreshToken);
            log.debug("RefreshToken 무효화 완료");
        } catch (Exception e) {
            log.warn("RefreshToken 무효화 실패: {}", e.getMessage());
        }
    }

    /** 갱신 시 기존 토큰 삭제 후 새 토큰 발급 (rotation) */
    public String rotate(String oldRefreshToken, String email) {
        invalidate(oldRefreshToken);
        return createAndStore(email);
    }
}
