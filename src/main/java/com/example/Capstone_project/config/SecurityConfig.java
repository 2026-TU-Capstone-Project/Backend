package com.example.Capstone_project.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	private final JwtTokenProvider jwtTokenProvider;
	private final UserDetailsService userDetailsService;

	public SecurityConfig(JwtTokenProvider jwtTokenProvider,
						  @Lazy UserDetailsService userDetailsService) {
		this.jwtTokenProvider = jwtTokenProvider;
		this.userDetailsService = userDetailsService;
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						// Swagger 및 API 문서
						.requestMatchers("/v3/api-docs/**", "/v3/api-docs").permitAll()
						.requestMatchers("/swagger-ui/**", "/swagger-ui.html").permitAll()

						// 로그인/회원가입/로그아웃 경로
						.requestMatchers("/api/v1/auth/**").permitAll()

						// 나머지 보호
						.anyRequest().authenticated()
				)
				.addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider, userDetailsService),
						UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}
}