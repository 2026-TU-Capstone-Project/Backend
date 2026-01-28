package com.example.Capstone_project.controller;

import com.example.Capstone_project.common.dto.ApiResponse;
import com.example.Capstone_project.domain.FittingStatus;
import com.example.Capstone_project.domain.FittingTask;
import com.example.Capstone_project.config.CustomUserDetails;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import java.util.List;
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
	public ResponseEntity<ApiResponse<FittingTask>> createVirtualFitting(
		@Parameter(description = "신체 사진 (필수)", required = true)
		@RequestParam("user_image") MultipartFile userImage,
		
		@Parameter(description = "상의 사진 (필수)", required = true)
		@RequestParam("top_image") MultipartFile topImage,
		
		@Parameter(description = "하의 사진 (필수)", required = false)
		@RequestParam("bottom_image") MultipartFile bottomImage,
		
		@Parameter(description = "사용자 ID (선택)")
		@RequestParam(value = "user_id", required = false) Long userId,
		
		@Parameter(description = "긍정적 프롬프트 (선택, 기본값: application.properties에서 설정)")
		@RequestParam(value = "positive_prompt", required = false) String positivePrompt,
		
		@Parameter(description = "제외 프롬프트 (선택, 기본값: application.properties에서 설정)")
		@RequestParam(value = "negative_prompt", required = false) String negativePrompt,
		
		@Parameter(description = "해상도 (선택, 기본값: standard)")
		@RequestParam(value = "resolution", required = false) String resolution,

		@AuthenticationPrincipal CustomUserDetails userDetails

	) {
		log.info("Virtual Fitting request received - userImage: {}, topImage: {}, bottomImage: {}, userId: {}",
			userImage.getOriginalFilename(),
			topImage.getOriginalFilename(),
			bottomImage.getOriginalFilename(),
			userId
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
			// 1. 이미지를 byte[]로 변환
			byte[] userImageBytes = userImage.getBytes();
			byte[] topImageBytes = topImage.getBytes();
			byte[] bottomImageBytes = bottomImage.getBytes();
			
			// 2. FittingTask 생성 (bodyImgUrl은 가상 피팅 완료 후 저장됨)
			FittingTask task = fittingService.createFittingTask(userDetails.getUser().getId(), null);
			log.info("✅ FittingTask 생성 완료 - ID: {}", task.getId());
			
			// 3. 비동기 처리 - 가상 피팅 작업 시작 (백그라운드에서 진행)
			fittingService.processVirtualFittingWithClothesAnalysis(
				task.getId(),
				userImageBytes,
				userImage.getOriginalFilename(),
				topImageBytes,
				topImage.getOriginalFilename(),
				bottomImageBytes,
				bottomImage.getOriginalFilename(),
				clothesAnalysisService,
					userDetails.getUser()
			);
			
			log.info("🚀 가상 피팅 작업 시작됨 - Task ID: {} (백그라운드에서 처리 중입니다)", task.getId());
			
			return ResponseEntity.status(HttpStatus.ACCEPTED)
				.body(ApiResponse.success("Virtual fitting task created. Processing in background.", task));
			
		} catch (IOException e) {
			log.error("Error reading image files", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ApiResponse.error("Failed to read image files: " + e.getMessage()));
		} catch (Exception e) {
			log.error("Error processing virtual fitting request", e);
			throw e;
		}
	}
	
	/**
	 * 가상 피팅 작업 상태 조회
	 */
	@Operation(
		summary = "가상 피팅 작업 상태 조회",
		description = "가상 피팅 작업의 현재 상태를 조회합니다."
	)
	@GetMapping("/status/{taskId}")
	public ResponseEntity<ApiResponse<FittingTask>> getFittingStatus(
		@Parameter(description = "작업 ID", required = true)
		@PathVariable Long taskId
	) {
		FittingTask task = fittingService.checkStatus(taskId);
		if (task == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(ApiResponse.error("Fitting task not found: " + taskId));
		}
		return ResponseEntity.ok(ApiResponse.success("Fitting task status retrieved", task));
	}



	@Operation(summary = "피팅 결과 내 옷장 저장", description = "마음에 드는 피팅 결과를 사용자의 옷장에 영구 저장합니다.")
	@PatchMapping("/{taskId}/save")
	public ResponseEntity<ApiResponse<String>> saveFittingResult(
			@Parameter(description = "저장할 작업 ID", required = true)
			@PathVariable Long taskId) {

		// 1. 해당 피팅 기록 찾기
		FittingTask task = fittingService.checkStatus(taskId);

		if (task == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(ApiResponse.error("해당 피팅 기록을 찾을 수 없습니다. ID: " + taskId));
		}

		// 2. 저장 상태 true로 변경 (FittingTask.java에 isSaved 필드 추가하셨죠?)
		task.setSaved(true);

		// 3. 서비스의 저장 로직 호출
		fittingService.saveTask(task);

		return ResponseEntity.ok(ApiResponse.success("내 코디 목록에 성공적으로 저장되었습니다.", null));
	}

	/**
	 * 내가 저장한 코디 목록 조회
	 */
	@Operation(summary = "저장된 코디 목록 조회", description = "사용자가 '저장'한 피팅 결과들만 모아서 보여줍니다.")
	@GetMapping("/my-closet")
	public ResponseEntity<ApiResponse<List<FittingTask>>> getMySavedFittings(
			@AuthenticationPrincipal CustomUserDetails userDetails) {

		// 1. 현재 로그인한 유저의 ID 가져오기
		Long userId = userDetails.getUser().getId();

		// 2. 서비스 호출해서 'is_saved = true'인 목록만 가져오기
		List<FittingTask> savedList = fittingService.getSavedFittingList(userId);

		return ResponseEntity.ok(ApiResponse.success("내 코디 목록 조회 성공", savedList));
	}

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
