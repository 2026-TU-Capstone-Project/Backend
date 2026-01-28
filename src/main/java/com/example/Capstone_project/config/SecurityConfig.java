package com.example.Capstone_project.config;

import com.example.Capstone_project.service.CustomOAuth2UserService;
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
	private final CustomOAuth2UserService customOAuth2UserService;
	private final OAuth2SuccessHandler oAuth2SuccessHandler;

	public SecurityConfig(JwtTokenProvider jwtTokenProvider,
						  @Lazy UserDetailsService userDetailsService,
						  @Lazy CustomOAuth2UserService customOAuth2UserService,
						  OAuth2SuccessHandler oAuth2SuccessHandler) {
		this.jwtTokenProvider = jwtTokenProvider;
		this.userDetailsService = userDetailsService;
		this.customOAuth2UserService = customOAuth2UserService;
		this.oAuth2SuccessHandler = oAuth2SuccessHandler;
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

						// 로그인/회원가입 경로
						.requestMatchers("/api/auth/**").permitAll()
						.requestMatchers("/login/oauth2/**", "/oauth2/**").permitAll()

						// 나머지 보호
						.anyRequest().authenticated()
				)
				.oauth2Login(oauth2 -> oauth2
						.userInfoEndpoint(userInfo -> userInfo
								.userService(customOAuth2UserService)
						)
						.successHandler(oAuth2SuccessHandler)
				)
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout") // 로그아웃을 실행할 주소
                        .logoutSuccessHandler((request, response, authentication) -> {
                            // 로그아웃 성공 시 스웨거로 보냄
                            response.sendRedirect("/swagger-ui/index.html");
                        })
                        .deleteCookies("JSESSIONID") // 세션 쿠키 삭제
                        .invalidateHttpSession(true)
                )
				// JWT 필터
				.addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider, userDetailsService),
						UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}
}