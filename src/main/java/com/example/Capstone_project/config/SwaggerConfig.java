package com.example.Capstone_project.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger (SpringDoc OpenAPI) 설정
 */
@Configuration
public class SwaggerConfig {
	
	@Bean
	public OpenAPI openAPI() {
		return new OpenAPI()
			.info(new Info()
				.title("Capstone Project API")
				.description("Spring Boot 기반 백엔드 API 문서\n\n" +
					"주요 기능:\n" +
					"- Virtual Try-On API (BitStudio)\n" +
					"- Virtual Fitting API (Nano Banana Pro)\n" +
					"- RESTful API")
				.version("v1.0")
				.contact(new Contact()
					.name("Capstone Team")
					.email("contact@example.com"))
				.license(new License()
					.name("Apache 2.0")
					.url("https://www.apache.org/licenses/LICENSE-2.0.html")))
			.addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
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
