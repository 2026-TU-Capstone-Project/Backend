package com.example.Capstone_project.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Configuration
public class WebClientConfig {
	
	// BitStudio API
	@Value("${bitstudio.api.base-url:https://api.bitstudio.ai}")
	private String bitStudioBaseUrl;
	
	@Value("${bitstudio.api.key:bs_ZeFNv6yw9AoSmnrR95lAXrIpH5Y1ijl}")
	private String bitStudioApiKey;
	
	// Gemini API (Nano Banana Pro)
	@Value("${gemini.api.base-url:https://generativelanguage.googleapis.com/v1beta}")
	private String geminiBaseUrl;
	
	@Value("${gemini.api.key:}")
	private String geminiApiKey;
	
	@Bean
	public WebClient bitStudioWebClient() {
		log.info("Creating BitStudio WebClient with baseUrl: {}", bitStudioBaseUrl);
		log.info("API Key configured: {}", bitStudioApiKey.isEmpty() ? "NOT SET" : "SET");
		return WebClient.builder()
			.baseUrl(bitStudioBaseUrl)
			.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + bitStudioApiKey)
			.build();
	}
	
	@Bean
	public WebClient geminiWebClient() {
		log.info("Creating Gemini API WebClient with baseUrl: {}", geminiBaseUrl);
		log.info("API Key configured: {}", geminiApiKey.isEmpty() ? "NOT SET" : "SET");
		
		// 이미지 응답이 크므로 버퍼 크기 제한 증가 (10MB)
		ExchangeStrategies strategies = ExchangeStrategies.builder()
			.codecs(configurer -> {
				configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024); // 10MB
			})
			.build();
		
		return WebClient.builder()
			.baseUrl(geminiBaseUrl)
			.defaultHeader("x-goog-api-key", geminiApiKey) // Gemini API는 x-goog-api-key 헤더 사용
			.exchangeStrategies(strategies) // 버퍼 크기 제한 설정
			.build();
	}
}

