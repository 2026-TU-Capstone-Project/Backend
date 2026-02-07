package com.example.Capstone_project;

import com.example.Capstone_project.service.GoogleCloudStorageService;
import com.example.Capstone_project.service.GoogleVisionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import static org.mockito.Mockito.mock;

/**
 * test 프로필에서 GoogleVisionService, GoogleCloudStorageService 빈이 생성되지 않으므로
 * 의존하는 서비스/컨트롤러를 위해 mock 빈을 제공합니다.
 */
@Configuration
@Profile("test")
public class TestConfig {

	@Bean
	public GoogleVisionService googleVisionService() {
		return mock(GoogleVisionService.class);
	}

	@Bean
	public GoogleCloudStorageService googleCloudStorageService() {
		return mock(GoogleCloudStorageService.class);
	}
}
