package com.example.Capstone_project.controller;

import com.example.Capstone_project.common.dto.ApiResponse;
import com.example.Capstone_project.domain.FittingTask;
import com.example.Capstone_project.domain.User;
import com.example.Capstone_project.config.CustomUserDetails;
import com.example.Capstone_project.dto.StyleRecommendationResponse;
import com.example.Capstone_project.dto.VirtualFittingStatusResponse;
import com.example.Capstone_project.dto.VirtualFittingTaskIdResponse;
import com.example.Capstone_project.service.ClothesAnalysisService;
import com.example.Capstone_project.service.FittingService;
import com.example.Capstone_project.service.GoogleCloudStorageService;
import com.example.Capstone_project.service.StyleRecommendationService;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Tag(name = "Virtual Fitting", description = "가상 피팅 요청·상태 조회·스타일 추천. 전신 사진 + 상의/하의 사진으로 AI 가상 피팅을 수행합니다.")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/virtual-fitting")
public class VirtualFittingController {
	
	private final FittingService fittingService;
	private final ClothesAnalysisService clothesAnalysisService;
	private final GoogleCloudStorageService gcsService;
    private final Executor taskExecutor;
	private final StyleRecommendationService styleRecommendationService;
	
	@Value("${virtual-fitting.image.storage-path:./images/virtual-fitting}")
	private String imageStoragePath;

    @Operation(
        summary = "가상 피팅 요청",
        description = "전신 사진 + 상의(필수) + 하의(선택)를 업로드하여 가상 피팅을 요청합니다. **비동기 처리** → 즉시 202 Accepted + taskId 반환. 이후 `/status/{taskId}`로 진행 상태를 폴링하세요."
    )
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<VirtualFittingTaskIdResponse>> createVirtualFitting(
            @Parameter(description = "전신 사진 (착용할 사람)", required = true) @RequestParam("user_image") MultipartFile userImage,
            @Parameter(description = "상의 이미지", required = true) @RequestParam("top_image") MultipartFile topImage,
            @Parameter(description = "하의 이미지 (선택)") @RequestParam(value = "bottom_image", required = false) MultipartFile bottomImage,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userImage.isEmpty() || topImage.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("User image and top image are required"));
        }

        try {
            final byte[] userImageBytes = userImage.getBytes();
            final byte[] topImageBytes = topImage.getBytes();
            final String topImageFilename = topImage.getOriginalFilename();
            final byte[] bottomImageBytes = (bottomImage != null && !bottomImage.isEmpty()) ? bottomImage.getBytes() : null;
            final String bottomImageFilename = (bottomImage != null) ? bottomImage.getOriginalFilename() : null;

            final User user = userDetails.getUser();
            final FittingTask task = fittingService.createFittingTask(user.getId(), null);

            CompletableFuture.runAsync(() -> {
                String userImageFilename = userImage.getOriginalFilename();
                try {
                    fittingService.processVirtualFittingWithClothesAnalysis(
                            task.getId(),
                            userImageBytes,
                            userImageFilename,
                            topImageBytes,
                            topImageFilename,
                            bottomImageBytes,
                            bottomImageFilename,
                            clothesAnalysisService,
                            user
                    );

                } catch (Exception e) {
                    log.error("비동기 처리 오류", e);
                }
            }, taskExecutor);

            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(ApiResponse.success("가상 피팅 요청 성공", new VirtualFittingTaskIdResponse(task.getId())));

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("파일 읽기 실패: " + e.getMessage()));
        }
    }


    @Operation(summary = "내가 저장한 코디 목록", description = "사용자가 저장한 가상 피팅 결과 목록을 조회합니다.")
    @GetMapping("/my-closet")
    public ResponseEntity<ApiResponse<List<FittingTask>>> getMySavedFittings(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUser().getId();
        List<FittingTask> savedList = fittingService.getSavedFittingList(userId);
        return ResponseEntity.ok(ApiResponse.success("조회 성공", savedList));
    }


	@Operation(
		summary = "가상 피팅 작업 상태 조회",
		description = "가상 피팅 요청 후 반환된 taskId로 진행 상태를 조회합니다. status가 COMPLETED가 될 때까지 폴링하세요."
	)
	@GetMapping("/status/{taskId}")
	public ResponseEntity<ApiResponse<VirtualFittingStatusResponse>> getFittingStatus(
		@Parameter(description = "가상 피팅 작업 ID (createVirtualFitting 응답의 taskId)", required = true) @PathVariable Long taskId) {
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

	@Operation(
		summary = "스타일 추천",
		description = "검색어(자연어)와 유사한 가상 피팅 결과를 최대 10개 추천합니다. userId를 넣으면 해당 사용자 성별과 같은 스타일만 반환됩니다."
	)
	@GetMapping("/recommend")
	public ResponseEntity<ApiResponse<StyleRecommendationResponse>> recommendByStyle(
		@Parameter(description = "검색어 (예: 결혼식에 입고 갈 단정하고 깔끔한 스타일 추천해줘)", required = true)
		@RequestParam("query") String query,
		@Parameter(description = "최소 유사도 점수 (0~1). 이 점수 이상만 반환. 생략 시 필터 없음.")
		@RequestParam(value = "minScore", required = false) Double minScore,
		@Parameter(description = "사용자 ID. 로그인 유저 ID를 보내면 해당 성별의 스타일만 추천됨. 생략 시 성별 필터 없음.")
		@RequestParam(value = "userId", required = false) Long userId
	) {
		var recommendations = styleRecommendationService.recommendByStyle(query, minScore, userId);
		StyleRecommendationResponse body = StyleRecommendationResponse.from(recommendations);
		return ResponseEntity.ok(ApiResponse.success("스타일 추천 결과", body));
	}
}
