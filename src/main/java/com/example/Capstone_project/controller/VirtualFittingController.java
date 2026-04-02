package com.example.Capstone_project.controller;

import com.example.Capstone_project.common.dto.ApiResponse;
import com.example.Capstone_project.domain.FitType;
import com.example.Capstone_project.domain.FittingTask;
import com.example.Capstone_project.domain.User;
import com.example.Capstone_project.config.CustomUserDetails;
import com.example.Capstone_project.dto.SavedFittingResponseDto;
import com.example.Capstone_project.dto.StyleRecommendationResponse;
import com.example.Capstone_project.dto.VirtualFittingStatusResponse;
import com.example.Capstone_project.dto.VirtualFittingTaskIdResponse;
import com.example.Capstone_project.service.ClothesAnalysisService;
import com.example.Capstone_project.service.FittingService;
import com.example.Capstone_project.service.RedisLockService;
import com.example.Capstone_project.service.StyleRecommendationService;
import com.example.Capstone_project.service.VirtualFittingSseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.io.IOException;
import java.time.Duration;
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
    private final Executor taskExecutor;
    private final StyleRecommendationService styleRecommendationService;
    private final RedisLockService RedisLockService;
    private final VirtualFittingSseService virtualFittingSseService;
    private final ObjectProvider<StringRedisTemplate> stringRedisTemplateProvider;
    private final ObjectMapper objectMapper;

    @Operation(
            summary = "가상 피팅 요청",
            description = "전신 사진 + 상의(필수) + 하의(선택)를 업로드하여 가상 피팅을 요청합니다. **비동기 처리** → 즉시 202 Accepted + taskId 반환. 이후 `/status/{taskId}`로 진행 상태를 폴링하세요."
    )
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<VirtualFittingTaskIdResponse>> createVirtualFitting(
            @Parameter(description = "전신 사진 (착용할 사람)", required = true) @RequestParam("user_image") MultipartFile userImage,
            @Parameter(description = "상의 이미지", required = true) @RequestParam("top_image") MultipartFile topImage,
            @Parameter(description = "하의 이미지 (선택)") @RequestParam(value = "bottom_image", required = false) MultipartFile bottomImage,
            @Parameter(description = "핏 타입 (SLIM_FIT, REGULAR_FIT, OVERSIZED_FIT). 기본값: REGULAR_FIT") @RequestParam(value = "fit_type", required = false, defaultValue = "REGULAR_FIT") FitType fitType,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userImage.isEmpty() || topImage.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("User image and top image are required"));
        }

        // ✅ 중복 요청 방지 (Redis Lock)
        final Long userId = userDetails.getUser().getId();
        final String lockKey = "lock:fitting-create:" + userId;

        // 30초 동안 동일 유저의 피팅 생성 요청은 1개만 허용
        if (!RedisLockService.tryLock(lockKey, Duration.ofSeconds(30))) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("이미 가상 피팅 요청이 처리 중입니다. 잠시 후 다시 시도해주세요."));
        }

        try {
            final byte[] userImageBytes = userImage.getBytes();
            final byte[] topImageBytes = topImage.getBytes();
            final String topImageFilename = topImage.getOriginalFilename();
            final byte[] bottomImageBytes = (bottomImage != null && !bottomImage.isEmpty()) ? bottomImage.getBytes() : null;
            final String bottomImageFilename = (bottomImage != null) ? bottomImage.getOriginalFilename() : null;

            final User user = userDetails.getUser();
            final FitType finalFitType = fitType;
            final FittingTask task = fittingService.createFittingTask(user.getId(), null, fitType);

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
                            user,
                            finalFitType
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
    public ResponseEntity<ApiResponse<List<SavedFittingResponseDto>>> getMySavedFittings(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUser().getId();
        List<SavedFittingResponseDto> savedList = fittingService.getSavedFittingListAsDtos(userId);
        return ResponseEntity.ok(ApiResponse.success("조회 성공", savedList));
    }

    @Operation(summary = "피팅 결과 내 옷장 저장(저장하기)", description = "가상 피팅 결과를 옷장에 저장합니다.")
    @PatchMapping("/{taskId}")
    public ResponseEntity<ApiResponse<String>> saveFittingResult(@PathVariable Long taskId) {
        FittingTask task = fittingService.checkStatus(taskId);
        if (task == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("기록 없음"));
        }

        task.setSaved(true);
        fittingService.saveTask(task);
        return ResponseEntity.ok(ApiResponse.success("저장 완료", null));
    }

    @Operation(summary = "피팅 결과 삭제 (닫기)", description = "가상 피팅 결과를 삭제합니다. 닫기 버튼 클릭 시 호출. 본인 소유 task만 삭제 가능.")
    @DeleteMapping("/{taskId}")
    public ResponseEntity<ApiResponse<String>> discardFittingResult(
            @Parameter(description = "삭제할 피팅 작업 ID", required = true) @PathVariable Long taskId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUser().getId();
        boolean deleted = fittingService.deleteTask(taskId, userId);
        if (!deleted) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("기록 없음 또는 삭제 권한 없음"));
        }
        return ResponseEntity.ok(ApiResponse.success("삭제 완료", null));
    }


    @Operation(
            summary = "가상 피팅 작업 상태 스트림 (SSE)",
            description = "taskId에 대한 상태 변경을 실시간으로 수신합니다. 연결 시 이미 COMPLETED/FAILED면 현재 상태 1회 전송 후 종료. task당 1연결, 타임아웃 1분."
    )
    @GetMapping(value = "/{taskId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamFittingStatus(
            @Parameter(description = "가상 피팅 작업 ID", required = true) @PathVariable Long taskId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUser().getId();
        FittingTask task = fittingService.checkStatus(taskId);
        if (task == null) {
            throw new com.example.Capstone_project.common.exception.ResourceNotFoundException("Fitting task not found: " + taskId);
        }
        if (!userId.equals(task.getUserId())) {
            throw new com.example.Capstone_project.common.exception.BadRequestException("해당 작업에 대한 권한이 없습니다.");
        }

        SseEmitter emitter = new SseEmitter(300_000L);
        VirtualFittingStatusResponse current = new VirtualFittingStatusResponse(
                task.getId(),
                task.getStatus(),
                task.getResultImgUrl()
        );

        if (task.getStatus() == com.example.Capstone_project.domain.FittingStatus.COMPLETED
                || task.getStatus() == com.example.Capstone_project.domain.FittingStatus.FAILED) {
            virtualFittingSseService.sendOnceAndComplete(emitter, current);
            return emitter;
        }

        SseEmitter registered = virtualFittingSseService.register(taskId);
        try {
            registered.send(SseEmitter.event().name("status").data(objectMapper.writeValueAsString(current)));
        } catch (IOException e) {
            log.warn("SSE initial send failed for taskId={}", taskId, e);
            virtualFittingSseService.sendOnceAndComplete(registered, current);
        }
        return registered;
    }

    @Operation(
            summary = "가상 피팅 작업 상태 조회 (폴링)",
            description = "가상 피팅 요청 후 반환된 taskId로 진행 상태를 조회합니다. SSE 사용 시 GET /{taskId}/stream 을 사용하세요."
    )
    @GetMapping("/{taskId}/status")
    public ResponseEntity<ApiResponse<VirtualFittingStatusResponse>> getFittingStatus(
            @PathVariable Long taskId) {

        final String cacheKey = "cache:fitting-status:" + taskId;

        try {
            StringRedisTemplate redis = stringRedisTemplateProvider.getIfAvailable();
            if (redis != null) {
                // 1) Redis 먼저 조회
                String cached = redis.opsForValue().get(cacheKey);
                if (cached != null) {
                    VirtualFittingStatusResponse cachedBody =
                            objectMapper.readValue(cached, VirtualFittingStatusResponse.class);

                    return ResponseEntity.ok(
                            ApiResponse.success("Fitting task status retrieved (cached)", cachedBody)
                    );
                }
            }

            // 2) Redis에 없으면 DB 조회
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

            // 3) 10초 캐시 저장(폴링 방지)
            String json = objectMapper.writeValueAsString(body);
            if (redis != null) {
                redis.opsForValue().set(cacheKey, json, Duration.ofSeconds(10));
            }

            return ResponseEntity.ok(
                    ApiResponse.success("Fitting task status retrieved", body)
            );

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Status retrieval failed: " + e.getMessage()));
        }
    }

// =========================================================================
    // 아래 메서드를 기존 recommendByWeatherStyle() 와 교체하세요
    // 위치: VirtualFittingController.java 맨 아래 } 바로 위
    // =========================================================================

    @Operation(
            summary = "날씨 기반 스타일 추천",
            description = "기온 + 강수량 + 적설량 + 풍속 + 습도를 반영한 날씨 맞춤 스타일 추천. OpenWeatherMap 응답값을 그대로 넘겨주면 됩니다."
    )
    @GetMapping("/recommendation/weather-style")
    public ResponseEntity<ApiResponse<StyleRecommendationResponse>> recommendByWeatherStyle(
            @RequestParam("query") String query,
            @RequestParam("temp") double temp,
            @RequestParam(value = "rain", defaultValue = "0.0") double rain,
            @RequestParam(value = "snow", defaultValue = "0.0") double snow,
            @RequestParam(value = "windSpeed", defaultValue = "0.0") double windSpeed,
            @RequestParam(value = "humidity", defaultValue = "0") int humidity,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUser().getId();

        // 캐시 키에 날씨 조건 전체 포함
        String tempKey = String.format(java.util.Locale.US, "%.1f_r%.1f_s%.1f_w%.1f_h%d",
                temp, rain, snow, windSpeed, humidity);
        final String cacheKey = "cache:weather-style:" + userId + ":" + query + ":" + tempKey;

        try {
            StringRedisTemplate redis = stringRedisTemplateProvider.getIfAvailable();

            if (redis != null) {
                String cached = redis.opsForValue().get(cacheKey);
                if (cached != null) {
                    StyleRecommendationResponse cachedBody =
                            objectMapper.readValue(cached, StyleRecommendationResponse.class);
                    return ResponseEntity.ok(
                            ApiResponse.success("날씨 기반 스타일 추천 결과 (cached)", cachedBody)
                    );
                }
            }

            // WeatherCondition 객체로 변환 후 서비스 호출
            StyleRecommendationService.WeatherCondition condition =
                    new StyleRecommendationService.WeatherCondition(temp, rain, snow, windSpeed, humidity);

            var recommendations = styleRecommendationService
                    .recommendByWeatherStyleFull(query, 0.7, userId, condition);

            StyleRecommendationResponse body = StyleRecommendationResponse.from(recommendations);

            if (redis != null) {
                String json = objectMapper.writeValueAsString(body);
                redis.opsForValue().set(cacheKey, json, java.time.Duration.ofSeconds(60));
            }

            return ResponseEntity.ok(ApiResponse.success("날씨 기반 스타일 추천 결과", body));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("추천 처리 실패: " + e.getMessage()));
        }
    }
}
