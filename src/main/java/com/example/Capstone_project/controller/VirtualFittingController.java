package com.example.Capstone_project.controller;

import com.example.Capstone_project.common.dto.ApiResponse;
import com.example.Capstone_project.dto.VirtualFittingResponse;
import com.example.Capstone_project.service.GeminiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Virtual Fitting API Controller
 * 나노바나나 프로 API를 활용한 가상 피팅 서비스
 */
@Tag(name = "Virtual Fitting", description = "나노바나나 프로 API를 활용한 가상 피팅 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/virtual-fitting")
@RequiredArgsConstructor
public class VirtualFittingController {
	
	private final GeminiService geminiService;
	
	@Value("${virtual-fitting.image.storage-path:./images/virtual-fitting}")
	private String imageStoragePath;
	
	/**
	 * Virtual Fitting 요청 처리
	 * 신체 사진, 상의, 하의를 받아서 결과 이미지를 생성
	 * 상의와 하의를 동시에 자연스럽게 합성
	 */
	@Operation(
		summary = "가상 피팅 요청",
		description = "신체 사진, 상의, 하의 이미지를 받아서 가상 피팅 결과 이미지를 생성합니다. " +
			"나노바나나 프로 API를 사용하여 상의와 하의를 동시에 자연스럽게 합성합니다."
	)
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "201",
			description = "가상 피팅 성공",
			content = @Content(
				mediaType = MediaType.APPLICATION_JSON_VALUE,
				schema = @Schema(implementation = ApiResponse.class)
			)
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "400",
			description = "잘못된 요청 (필수 파일 누락 등)"
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "500",
			description = "서버 오류"
		)
	})
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ApiResponse<VirtualFittingResponse>> createVirtualFitting(
		@Parameter(description = "신체 사진 (필수)", required = true)
		@RequestParam("user_image") MultipartFile userImage,
		
		@Parameter(description = "상의 사진 (필수)", required = true)
		@RequestParam("top_image") MultipartFile topImage,
		
		@Parameter(description = "하의 사진 (필수)", required = true)
		@RequestParam("bottom_image") MultipartFile bottomImage,
		
		@Parameter(description = "긍정적 프롬프트 (선택, 기본값: application.properties에서 설정)")
		@RequestParam(value = "positive_prompt", required = false) String positivePrompt,
		
		@Parameter(description = "제외 프롬프트 (선택, 기본값: application.properties에서 설정)")
		@RequestParam(value = "negative_prompt", required = false) String negativePrompt,
		
		@Parameter(description = "해상도 (선택, 기본값: standard)")
		@RequestParam(value = "resolution", required = false) String resolution
	) {
		log.info("Virtual Fitting request received - userImage: {}, topImage: {}, bottomImage: {}, resolution: {}",
			userImage.getOriginalFilename(),
			topImage.getOriginalFilename(),
			bottomImage.getOriginalFilename(),
			resolution != null ? resolution : "default"
		);
		
		// 파일 유효성 검사
		if (userImage.isEmpty()) {
			return ResponseEntity.badRequest()
				.body(ApiResponse.error("User image is required"));
		}
		
		if (topImage.isEmpty()) {
			return ResponseEntity.badRequest()
				.body(ApiResponse.error("Top image is required"));
		}
		
		if (bottomImage.isEmpty()) {
			return ResponseEntity.badRequest()
				.body(ApiResponse.error("Bottom image is required"));
		}
		
		try {
			VirtualFittingResponse response = geminiService.processVirtualFitting(
				userImage,
				topImage,
				bottomImage,
				positivePrompt,
				negativePrompt,
				resolution
			);
			
			return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success("Virtual fitting completed successfully", response));
			
		} catch (Exception e) {
			log.error("Error processing virtual fitting request", e);
			// GlobalExceptionHandler가 처리하지만, 여기서도 로깅
			throw e;
		}
	}
	
	/**
	 * 가상 피팅 결과 이미지 다운로드
	 * 저장된 이미지 파일을 HTTP 응답으로 제공
	 */
	@Operation(
		summary = "가상 피팅 결과 이미지 조회",
		description = "생성된 가상 피팅 결과 이미지를 조회합니다."
	)
	@GetMapping("/images/{filename}")
	public ResponseEntity<Resource> getVirtualFittingImage(
		@Parameter(description = "이미지 파일명", required = true)
		@PathVariable String filename
	) {
		try {
			Path filePath = Paths.get(imageStoragePath).resolve(filename);
			File file = filePath.toFile();
			
			if (!file.exists() || !file.isFile()) {
				log.warn("Image file not found: {}", filePath.toAbsolutePath());
				return ResponseEntity.notFound().build();
			}
			
			Resource resource = new FileSystemResource(file);
			
			// MIME 타입 결정
			String contentType = Files.probeContentType(filePath);
			if (contentType == null) {
				contentType = "application/octet-stream";
			}
			
			log.debug("Serving image file: {} (contentType: {})", filePath.toAbsolutePath(), contentType);
			
			return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(contentType))
				.header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
				.body(resource);
				
		} catch (Exception e) {
			log.error("Error serving image file: {}", filename, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}
}
