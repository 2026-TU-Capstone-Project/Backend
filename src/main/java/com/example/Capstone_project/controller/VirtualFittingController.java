package com.example.Capstone_project.controller;

import com.example.Capstone_project.common.dto.ApiResponse;
import com.example.Capstone_project.domain.FittingStatus;
import com.example.Capstone_project.domain.FittingTask;
import com.example.Capstone_project.dto.VirtualFittingStatusResponse;
import com.example.Capstone_project.dto.VirtualFittingTaskIdResponse;
import com.example.Capstone_project.service.ClothesAnalysisService;
import com.example.Capstone_project.service.FittingService;
import com.example.Capstone_project.service.GoogleCloudStorageService;
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
import java.io.IOException;
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
	
	private final FittingService fittingService;
	private final ClothesAnalysisService clothesAnalysisService;
	private final GoogleCloudStorageService gcsService;
	
	@Value("${virtual-fitting.image.storage-path:./images/virtual-fitting}")
	private String imageStoragePath;

	@Operation(
		summary = "가상 피팅 요청",
		description = "신체 사진, 상의, 하의 이미지를 받아서 가상 피팅 결과 이미지를 생성합니다. " +
			"나노바나나 프로 API를 사용하여 상의와 하의를 동시에 자연스럽게 합성합니다."
	)
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "202",
			description = "가상 피팅 요청 접수 (비동기 처리 시작)"
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
	public ResponseEntity<ApiResponse<VirtualFittingTaskIdResponse>> createVirtualFitting(
		@Parameter(description = "신체 사진 (필수)", required = true)
		@RequestParam("user_image") MultipartFile userImage,
		
		@Parameter(description = "상의 사진 (선택)", required = false)
		@RequestParam(value = "top_image", required = false) MultipartFile topImage,
		
		@Parameter(description = "하의 사진 (선택)", required = false)
		@RequestParam(value = "bottom_image", required = false) MultipartFile bottomImage,
		
		@Parameter(description = "사용자 ID (선택)")
		@RequestParam(value = "user_id", required = false) Long userId
	) {
		log.info("Virtual Fitting request received - userImage: {}, topImage: {}, bottomImage: {}, userId: {}",
			userImage != null ? userImage.getOriginalFilename() : "null",
			topImage != null ? topImage.getOriginalFilename() : "null",
			bottomImage != null ? bottomImage.getOriginalFilename() : "null",
			userId
		);
		
		// 파일 유효성 검사
		if (userImage.isEmpty()) {
			return ResponseEntity.badRequest()
				.body(ApiResponse.error("User image is required"));
		}
		
		// 상의와 하의 중 최소 하나는 필요
		boolean hasTop = topImage != null && !topImage.isEmpty();
		boolean hasBottom = bottomImage != null && !bottomImage.isEmpty();
		
		if (!hasTop && !hasBottom) {
			return ResponseEntity.badRequest()
				.body(ApiResponse.error("At least one of top_image or bottom_image is required"));
		}
		
		try {
			// 1. 이미지를 byte[]로 변환
			byte[] userImageBytes = userImage.getBytes();
			byte[] topImageBytes = hasTop ? topImage.getBytes() : null;
			byte[] bottomImageBytes = hasBottom ? bottomImage.getBytes() : null;
			
			String topImageFilename = hasTop ? topImage.getOriginalFilename() : null;
			String bottomImageFilename = hasBottom ? bottomImage.getOriginalFilename() : null;
			
			// 2. FittingTask 생성 (bodyImgUrl은 가상 피팅 완료 후 저장됨)
			FittingTask task = fittingService.createFittingTask(userId, null);
			log.info("✅ FittingTask 생성 완료 - ID: {}", task.getId());
			
			// 3. 비동기 처리 - 가상 피팅 작업 시작 (백그라운드에서 진행)
			fittingService.processVirtualFittingWithClothesAnalysis(
				task.getId(),
				userImageBytes,
				userImage.getOriginalFilename(),
				topImageBytes,
				topImageFilename,
				bottomImageBytes,
				bottomImageFilename,
				clothesAnalysisService
			);
			
			log.info("🚀 가상 피팅 작업 시작됨 - Task ID: {} (백그라운드에서 처리 중입니다)", task.getId());
			
			VirtualFittingTaskIdResponse body = new VirtualFittingTaskIdResponse(task.getId());
			return ResponseEntity.status(HttpStatus.ACCEPTED)
				.body(ApiResponse.success("가상 피팅 요청 성공", body));
			
		} catch (IOException e) {
			log.error("Error reading image files", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ApiResponse.error("Failed to read image files: " + e.getMessage()));
		} catch (Exception e) {
			log.error("Error processing virtual fitting request", e);
			throw e;
		}
	}


	@Operation(
		summary = "가상 피팅 작업 상태 조회",
		description = "가상 피팅 작업의 현재 상태를 조회합니다."
	)
	@GetMapping("/status/{taskId}")
	public ResponseEntity<ApiResponse<VirtualFittingStatusResponse>> getFittingStatus(
		@Parameter(description = "작업 ID", required = true)
		@PathVariable Long taskId
	) {
		FittingTask task = fittingService.checkStatus(taskId);
		if (task == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(ApiResponse.error("Fitting task not found: " + taskId));
		}
		VirtualFittingStatusResponse body = new VirtualFittingStatusResponse(
			task.getId(),
			task.getStatus(),
			task.getResultImgUrl()
		);
		return ResponseEntity.ok(ApiResponse.success("Fitting task status retrieved", body));
	}
}
