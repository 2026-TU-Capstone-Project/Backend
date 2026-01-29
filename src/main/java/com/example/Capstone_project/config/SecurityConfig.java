package com.example.Capstone_project.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			.csrf(csrf -> csrf.disable())
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(auth -> auth
				// Swagger UI 및 API 문서 경로 허용 (순서 중요)
				.requestMatchers("/v3/api-docs/**", "/v3/api-docs").permitAll()
				.requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/swagger-ui/index.html").permitAll()
				.requestMatchers("/webjars/**", "/swagger-resources/**", "/configuration/**").permitAll()
				// API 엔드포인트 허용
				.requestMatchers("/api/v1/**").permitAll()
				.requestMatchers("/api/clothes/**").permitAll()
					.requestMatchers("/api/fitting/**").permitAll()
				.anyRequest().authenticated()
			);
		
		return http.build();
	}
}

