package com.example.Capstone_project.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
	
	@Value("${bitstudio.api.base-url:https://api.bitstudio.ai}")
	private String baseUrl;
	
	@Value("${bitstudio.api.key}")
	private String apiKey;
	
	@Bean
	public WebClient bitStudioWebClient() {
		return WebClient.builder()
			.baseUrl(baseUrl)
			.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
			.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
			.build();
	}
}

