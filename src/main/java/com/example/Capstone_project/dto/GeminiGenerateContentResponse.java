package com.example.Capstone_project.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true) // ✅ 1. 전체 응답 무시 설정
public class GeminiGenerateContentResponse {

	@JsonProperty("candidates")
	private List<Candidate> candidates;

	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	@JsonIgnoreProperties(ignoreUnknown = true) // ✅ 2. Candidate 무시 설정
	public static class Candidate {
		@JsonProperty("content")
		private Content content;
	}

	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	@JsonIgnoreProperties(ignoreUnknown = true) // ✅ 3. Content 무시 설정
	public static class Content {
		@JsonProperty("parts")
		private List<Part> parts;
	}

	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	@JsonIgnoreProperties(ignoreUnknown = true) // ✅ 4. 여기가 핵심! Part 클래스에도 무시 설정 추가
	public static class Part {
		@JsonProperty("text")
		private String text;

		@JsonProperty("inlineData")
		private InlineData inlineData;
	}

	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	@JsonIgnoreProperties(ignoreUnknown = true) // ✅ 5. 혹시 모르니 InlineData까지 설정
	public static class InlineData {
		@JsonProperty("mimeType")
		private String mimeType;

		@JsonProperty("data")
		private String data;
	}
}