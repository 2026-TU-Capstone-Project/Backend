package com.example.Capstone_project.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger (SpringDoc OpenAPI) 설정
 * 문서 확인: /swagger-ui.html 또는 /swagger-ui/index.html
 */
@Configuration
public class SwaggerConfig {

	@Bean
	public OpenAPI openAPI() {
		return new OpenAPI()
			.info(new Info()
				.title("Capstone Project API")
				.description("""
					## 개요
					가상 피팅 및 옷장 관리 서비스 백엔드 API입니다.

					## 인증
					- **로그인/회원가입** (`/api/v1/auth/*`): 인증 불필요
					- **나머지 API**: 로그인 후 받은 `accessToken`을 **Authorize** 버튼에서 `Bearer {토큰}` 형식으로 입력하세요.

					## 주요 기능
					| 태그 | 설명 |
					|------|------|
					| Auth | 로그인, 회원가입, 소셜 로그인(Google/Kakao), 토큰 갱신, 로그아웃 |
					| Virtual Fitting | 가상 피팅 요청·상태 조회·스타일 추천 |
					| Clothes | 옷 등록·분석·조회·삭제 |
					| Clothes Set | 코디 폴더(세트) 저장·수정·삭제 |
					| Deploy | 배포 확인용 (테스트) |
					""")
				.version("v1.0")
				.contact(new Contact()
					.name("Capstone Team")
					.email("contact@example.com"))
				.license(new License()
					.name("Apache 2.0")
					.url("https://www.apache.org/licenses/LICENSE-2.0.html")))
			.addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
			.servers(java.util.List.of(
				new Server().url("/").description("현재 호스트 (상대 경로)"),
				new Server().url("http://localhost:80").description("로컬 개발 (port 80)"),
				new Server().url("http://localhost:8080").description("로컬 개발 (port 8080)")
			))
			.components(new Components()
				.addSecuritySchemes("Bearer Authentication", createAPIKeyScheme()));
	}
	
	private SecurityScheme createAPIKeyScheme() {
		return new SecurityScheme()
			.type(SecurityScheme.Type.HTTP)
			.bearerFormat("JWT")
			.scheme("bearer");
	}
}
