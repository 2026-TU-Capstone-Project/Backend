package com.example.Capstone_project.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Gemini API 이미지 생성 요청 DTO
 * 참고: https://ai.google.dev/gemini-api/docs/image-generation?hl=ko#java_1
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeminiGenerateContentRequest {
	
	@JsonProperty("contents")
	private List<Content> contents;
	
	@JsonProperty("generationConfig")
	private GenerationConfig generationConfig;
	
	@Getter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Content {
		@JsonProperty("parts")
		private List<Part> parts;
	}
	
	@Getter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Part {
		private String text;
		
		@JsonProperty("inlineData")
		private InlineData inlineData;
	}
	
	@Getter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class InlineData {
		@JsonProperty("mimeType")
		private String mimeType; // image/jpeg, image/png 등
		
		private String data; // Base64 인코딩된 이미지 데이터
	}
	
	@Getter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class GenerationConfig {
		@JsonProperty("imageConfig")
		private ImageConfig imageConfig;
	}
	
	@Getter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class ImageConfig {
		@JsonProperty("aspectRatio")
		private String aspectRatio; // 1:1, 3:4, 16:9 등
		
		@JsonProperty("imageSize")
		private String imageSize; // 1K, 2K, 4K (gemini-3-pro-image-preview 모델 지원)
	}
}
