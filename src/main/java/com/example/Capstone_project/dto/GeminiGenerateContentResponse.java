package com.example.Capstone_project.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Gemini API 이미지 생성 응답 DTO
 * 참고: https://ai.google.dev/gemini-api/docs/image-generation?hl=ko#java_1
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GeminiGenerateContentResponse {
	
	@JsonProperty("candidates")
	private List<Candidate> candidates;
	
	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Candidate {
		@JsonProperty("content")
		private Content content;
	}
	
	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Content {
		@JsonProperty("parts")
		private List<Part> parts;
	}
	
	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Part {
		private String text;
		
		@JsonProperty("inlineData")
		private InlineData inlineData;
	}
	
	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class InlineData {
		@JsonProperty("mimeType")
		private String mimeType;
		
		private String data; // Base64 인코딩된 이미지 데이터
	}
}
