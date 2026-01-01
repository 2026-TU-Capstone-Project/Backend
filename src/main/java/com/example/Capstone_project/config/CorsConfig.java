package com.example.Capstone_project.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {

	@Bean
	public CorsFilter corsFilter() {
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		CorsConfiguration config = new CorsConfiguration();
		
		// Flutter 앱에서 접근 허용
		config.setAllowCredentials(true);
		config.addAllowedOriginPattern("*"); // 개발 환경용, 프로덕션에서는 특정 도메인 지정
		config.addAllowedHeader("*");
		config.addAllowedMethod("*");
		
		source.registerCorsConfiguration("/api/v1/**", config);
		return new CorsFilter(source);
	}
}

