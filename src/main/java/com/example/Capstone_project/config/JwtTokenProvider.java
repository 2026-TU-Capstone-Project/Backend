package com.example.Capstone_project.config; // 패키지 경로 확인!

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.util.Date;


@Component
public class JwtTokenProvider {
    private final String secretKey = "vmlmZS1zZWNyZXQta2V5LWZvci1jYXBzdG9uZS1wcm9qZWN0LWZpdHRpbmctc2VydmljZQ=="; // 충분히 긴 비밀키
    private final long validityInMilliseconds = 3600000; // 1시간 유지

    // 토큰 생성
    public String createToken(String email) {
        Claims claims = Jwts.claims().setSubject(email);
        Date now = new Date();
        Date validity = new Date(now.getTime() + validityInMilliseconds);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(Keys.hmacShaKeyFor(secretKey.getBytes()), SignatureAlgorithm.HS256)
                .compact();
    }
    // 토큰에서 회원 정보 추출
    public String getSubject(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(secretKey.getBytes()))
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    /** 토큰 만료까지 남은 초 수 (만료됐으면 0) */
    public long getTtlSecondsUntilExpiry(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(Keys.hmacShaKeyFor(secretKey.getBytes()))
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            Date exp = claims.getExpiration();
            if (exp == null) return 0;
            long remaining = (exp.getTime() - System.currentTimeMillis()) / 1000;
            return Math.max(0, remaining);
        } catch (Exception e) {
            return 0;
        }
    }
}