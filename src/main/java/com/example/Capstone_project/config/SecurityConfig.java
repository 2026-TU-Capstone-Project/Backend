package com.example.Capstone_project.config;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
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

	/** storage.type=local 일 때 로컬 저장 이미지가 서빙되는 경로 (GCS 공개 URL 과 동일하게 인증 없이 접근) */
	@Value("${storage.local.url-path:/files}")
	private String localFilesUrlPath;

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
						.dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.ASYNC).permitAll()

						.requestMatchers("/error").permitAll()
						.requestMatchers("/actuator/health", "/actuator/info").permitAll()
						.requestMatchers("/api/v1/virtual-fitting/*/stream").permitAll()
						.requestMatchers("/v3/api-docs/**", "/v3/api-docs").permitAll()
						.requestMatchers("/swagger-ui/**", "/swagger-ui.html").permitAll()
						.requestMatchers("/api/v1/auth/**").permitAll()
						.requestMatchers(localFilesUrlPath + "/**").permitAll()

						.anyRequest().authenticated()
				)
				.exceptionHandling(ex -> ex
						.authenticationEntryPoint((request, response, authException) -> {
							if (!response.isCommitted()) {
								response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
								response.setContentType("application/json;charset=UTF-8");
								response.getWriter().write("{\"success\":false,\"message\":\"인증이 필요합니다.\"}");
							}
						})
						.accessDeniedHandler((request, response, accessDeniedException) -> {
							if (!response.isCommitted()) {
								response.setStatus(HttpServletResponse.SC_FORBIDDEN);
								response.setContentType("application/json;charset=UTF-8");
								response.getWriter().write("{\"success\":false,\"message\":\"접근 권한이 없습니다.\"}");
							}
						})
				)
				.addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider, userDetailsService),
						UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}
}
