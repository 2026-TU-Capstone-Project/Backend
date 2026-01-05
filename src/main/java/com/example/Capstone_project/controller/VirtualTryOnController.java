package com.example.Capstone_project.controller;

import com.example.Capstone_project.common.dto.ApiResponse;
import com.example.Capstone_project.dto.VirtualTryOnResponse;
import com.example.Capstone_project.service.BitStudioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/v1/virtual-try-on")
@RequiredArgsConstructor
public class VirtualTryOnController {
	
	private final BitStudioService bitStudioService;
	
	/**
	 * Virtual Try-On 요청 처리
	 * 신체 사진과 옷 사진을 받아서 결과 이미지를 생성
	 * 
	 * @param personImage 신체 사진 (MultipartFile)
	 * @param outfitImage 옷 사진 (MultipartFile)
	 * @param prompt 선택적 프롬프트 (예: "professional portrait, high quality")
	 * @param resolution 해상도 (standard or high, 기본값: standard)
	 * @param numImages 생성할 이미지 수 (1-4, 기본값: 1)
	 * @return 생성된 이미지 정보
	 */
	@PostMapping
	public ResponseEntity<ApiResponse<VirtualTryOnResponse>> createVirtualTryOn(
		@RequestParam("personImage") MultipartFile personImage,
		@RequestParam("outfitImage") MultipartFile outfitImage,
		@RequestParam(value = "prompt", required = false) String prompt,
		@RequestParam(value = "resolution", required = false, defaultValue = "standard") String resolution,
		@RequestParam(value = "numImages", required = false, defaultValue = "1") Integer numImages
	) {
		log.info("Virtual Try-On request received - personImage: {}, outfitImage: {}, resolution: {}, numImages: {}",
			personImage.getOriginalFilename(),
			outfitImage.getOriginalFilename(),
			resolution,
			numImages
		);
		
		// 파일 유효성 검사
		if (personImage.isEmpty()) {
			return ResponseEntity.badRequest()
				.body(ApiResponse.error("Person image is required"));
		}
		
		if (outfitImage.isEmpty()) {
			return ResponseEntity.badRequest()
				.body(ApiResponse.error("Outfit image is required"));
		}
		
		// 해상도 유효성 검사
		if (!resolution.equals("standard") && !resolution.equals("high")) {
			return ResponseEntity.badRequest()
				.body(ApiResponse.error("Resolution must be 'standard' or 'high'"));
		}
		
		// 이미지 수 유효성 검사
		if (numImages < 1 || numImages > 4) {
			return ResponseEntity.badRequest()
				.body(ApiResponse.error("Number of images must be between 1 and 4"));
		}
		
		VirtualTryOnResponse response = bitStudioService.processVirtualTryOn(
			personImage,
			outfitImage,
			prompt,
			resolution,
			numImages
		);
		
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(ApiResponse.success("Virtual try-on completed successfully", response));
	}
}

