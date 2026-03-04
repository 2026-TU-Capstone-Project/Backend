package com.example.Capstone_project.controller;

import com.example.Capstone_project.common.dto.ApiResponse;
import com.example.Capstone_project.common.exception.BadRequestException;
import com.example.Capstone_project.common.exception.ResourceNotFoundException;
import com.example.Capstone_project.domain.Clothes;
import com.example.Capstone_project.domain.ClothesUploadStatus;
import com.example.Capstone_project.domain.ClothesUploadTask;
import com.example.Capstone_project.domain.User;
import com.example.Capstone_project.dto.ClothesResponseDto;
import com.example.Capstone_project.dto.ClothesUploadStatusResponse;
import com.example.Capstone_project.dto.ClothesUploadTaskIdResponse;
import com.example.Capstone_project.repository.ClothesRepository;
import com.example.Capstone_project.repository.ClothesUploadTaskRepository;
import com.example.Capstone_project.repository.FittingRepository;
import com.example.Capstone_project.service.ClothesAnalysisService;
import com.example.Capstone_project.service.ClothesUploadSseService;
import com.example.Capstone_project.service.GoogleCloudStorageService;
import com.example.Capstone_project.service.RedisLockService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.Capstone_project.config.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.time.Duration;

@Tag(name = "Clothes", description = "옷 등록·분석·조회·삭제")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/clothes")
public class ClothesController {

    private final ClothesRepository clothesRepository;
    private final ClothesUploadTaskRepository clothesUploadTaskRepository;
    private final FittingRepository fittingRepository;
    private final ClothesAnalysisService clothesAnalysisService;
    private final ClothesUploadSseService clothesUploadSseService;
    private final GoogleCloudStorageService gcsService;
    private final RedisLockService redisLockService;
    private final ObjectMapper objectMapper;

    @Operation(
        summary = "옷 등록",
        description = "옷 사진 1장을 업로드하여 AI 분석 후 저장합니다. **비동기 + SSE** → 202 Accepted와 taskId 반환. " +
                "GET /api/v1/clothes/upload/{taskId}/stream 으로 진행 상황(이벤트 name=status)을 실시간 수신하세요."
    )
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ClothesUploadTaskIdResponse>> uploadClothes(
            @Parameter(description = "옷 이미지 파일", required = true) @RequestParam("file") MultipartFile file,
            @Parameter(description = "카테고리 (Top / Bottom / Shoes)", example = "Top", required = true) @RequestParam("category") String category,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("Clothes upload request received - file: {}, category: {}",
                file.getOriginalFilename(), category);

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("File is required"));
        }

        final Long userId = userDetails.getUser().getId();
        final String lockKey = "lock:clothes-upload:" + userId;

        if (!redisLockService.tryLock(lockKey, Duration.ofSeconds(8))) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("이미 옷 등록이 처리 중입니다. 잠시 후 다시 시도해주세요."));
        }

        try {
            byte[] imageBytes = file.getBytes();
            String filename = file.getOriginalFilename();

            ClothesUploadTask task = ClothesUploadTask.builder()
                    .userId(userId)
                    .category(category)
                    .status(ClothesUploadStatus.WAITING)
                    .build();
            task = clothesUploadTaskRepository.save(task);
            Long taskId = task.getId();

            clothesAnalysisService.startClothesUploadAndNotify(taskId, imageBytes, filename, category, userDetails.getUser());

            ClothesUploadTaskIdResponse body = new ClothesUploadTaskIdResponse(taskId);
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(ApiResponse.success(
                            "옷 등록이 시작되었습니다. GET /api/v1/clothes/upload/" + taskId + "/stream 으로 진행 상황을 확인하세요.",
                            body));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("파일 읽기 실패: " + e.getMessage()));
        }
    }

    @Operation(
        summary = "옷 업로드 진행 상황 스트림 (SSE)",
        description = "taskId에 대한 상태 변경을 실시간으로 수신합니다. 이벤트 name=status (가상 피팅과 동일). " +
                "이미 COMPLETED/FAILED면 현재 상태 1회 전송 후 종료."
    )
    @GetMapping(value = "/upload/{taskId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamClothesUploadStatus(
            @Parameter(description = "업로드 작업 ID", required = true) @PathVariable Long taskId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUser().getId();
        ClothesUploadTask task = clothesUploadTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Clothes upload task not found: " + taskId));
        if (!userId.equals(task.getUserId())) {
            throw new BadRequestException("해당 작업에 대한 권한이 없습니다.");
        }

        ClothesUploadStatusResponse current = ClothesUploadStatusResponse.builder()
                .taskId(task.getId())
                .status(task.getStatus())
                .clothesId(task.getClothesId())
                .errorMessage(task.getErrorMessage())
                .build();

        if (task.getStatus() == ClothesUploadStatus.COMPLETED || task.getStatus() == ClothesUploadStatus.FAILED) {
            SseEmitter emitter = new SseEmitter(60_000L);
            clothesUploadSseService.sendOnceAndComplete(emitter, current);
            return emitter;
        }

        SseEmitter registered = clothesUploadSseService.register(taskId);
        try {
            registered.send(SseEmitter.event().name("status").data(objectMapper.writeValueAsString(current)));
        } catch (IOException e) {
            log.warn("SSE initial send failed for clothes upload taskId={}", taskId, e);
            clothesUploadSseService.sendOnceAndComplete(registered, current);
        }
        return registered;
    }

    // @Operation(
    //     summary = "옷 일괄 분석",
    //     description = "상의·하의·신발을 한 번에 업로드하여 동기로 분석·저장합니다. 각 필드는 선택적으로 업로드 가능합니다."
    // )
    // @PostMapping(value = "/analysis", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    // public ResponseEntity<ApiResponse<String>> analyze(
    //         @ModelAttribute ClothesRequestDto requestDto,
    //         @AuthenticationPrincipal CustomUserDetails userDetails
    // ) {
    //     log.info("👕 Clothes analysis request received");

    //     // 1. 유저 정보 가져오기 (userDetails에서 추출)
    //     com.example.Capstone_project.domain.User user = userDetails.getUser();

    //     try {
    //         // 2. DTO에서 파일을 하나씩 꺼내서 동기(Sync) 방식으로 즉시 처리
    //         // 상의 분석
    //         if (requestDto.getTop() != null && !requestDto.getTop().isEmpty()) {
    //             clothesAnalysisService.analyzeAndSaveClothesSync(requestDto.getTop(), "Top", user);
    //         }
    //         // 하의 분석
    //         if (requestDto.getBottom() != null && !requestDto.getBottom().isEmpty()) {
    //             clothesAnalysisService.analyzeAndSaveClothesSync(requestDto.getBottom(), "Bottom", user);
    //         }
    //         // 신발 분석
    //         if (requestDto.getShoes() != null && !requestDto.getShoes().isEmpty()) {
    //             clothesAnalysisService.analyzeAndSaveClothesSync(requestDto.getShoes(), "Shoes", user);
    //         }

    //         // 모든 작업이 끝나면 성공 응답 반환
    //         return ResponseEntity.ok(ApiResponse.success("모든 옷 등록 및 분석 성공!", "분석 완료"));

    //     } catch (Exception e) {
    //         log.error("❌ 분석 중 오류 발생", e);
    //         return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
    //                 .body(ApiResponse.error("분석 중 오류가 발생했습니다: " + e.getMessage()));
    //     }
    // }

    @Operation(
        summary = "내 옷장 카테고리별 목록 조회",
        description = "로그인한 사용자의 옷 목록을 최신순으로 조회합니다. category로 필터링 가능 (전체/Top/Bottom/Shoes)"
    )
    @GetMapping
    public ResponseEntity<ApiResponse<List<ClothesResponseDto>>> getAllClothes(
            @Parameter(description = "카테고리 필터: 전체(생략가능), Top, Bottom, Shoes") @RequestParam(value = "category", required = false) String category,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        User user = userDetails.getUser();
        List<Clothes> clothesList;

        if (category == null || category.isBlank() || "전체".equals(category)) {
            clothesList = clothesRepository.findByUserAndInClosetTrueOrderByCreatedAtDesc(user);
        } else {
            clothesList = clothesRepository.findByUserAndInClosetTrueAndCategoryOrderByCreatedAtDesc(user, category);
        }

        List<ClothesResponseDto> dtos = clothesList.stream()
                .map(ClothesResponseDto::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("내 옷장 목록 조회 성공", dtos));
    }

    @Operation(summary = "옷 상세 조회")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ClothesResponseDto>> getClothesById(
            @Parameter(description = "옷(Clothes) ID") @PathVariable Long id) {
        Clothes clothes = clothesRepository.findById(id)
                .orElse(null);

        if (clothes == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Clothes not found: " + id));
        }

        return ResponseEntity.ok(ApiResponse.success("Clothes retrieved", ClothesResponseDto.from(clothes)));
    }

    @Operation(summary = "옷 삭제", description = "본인 소유 옷만 삭제 가능합니다.")
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<String>> deleteClothes(
            @Parameter(description = "옷(Clothes) ID") @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Clothes clothes = clothesRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("해당 옷을 찾을 수 없습니다."));

        if (!clothes.getUser().getId().equals(userDetails.getUser().getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("삭제 권한이 없습니다."));
        }

        fittingRepository.clearTopIdByClothesId(id);
        fittingRepository.clearBottomIdByClothesId(id);

        String blobName = gcsService.extractBlobNameFromUrl(clothes.getImgUrl());
        gcsService.deleteImage(blobName);

        clothesRepository.delete(clothes);
        return ResponseEntity.ok(ApiResponse.success("옷 삭제 완료", null));
    }
}
