package com.example.Capstone_project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Virtual Fitting API 응답 DTO
 */
@Schema(description = "가상 피팅 응답")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VirtualFittingResponse {
	@Schema(description = "이미지 ID", example = "IMG_123")
	private String imageId;
	
	@Schema(description = "상태", example = "completed")
	private String status;
	
	@Schema(description = "결과 이미지 URL", example = "https://example.com/image.jpg")
	private String imageUrl;
	
	@Schema(description = "Base64 인코딩된 이미지 (선택적)", example = "data:image/jpeg;base64,...")
	private String imageBase64;
	
	@Schema(description = "사용된 크레딧", example = "5")
	private Integer creditsUsed;
}
