package com.example.Capstone_project.controller;

import com.example.Capstone_project.common.dto.ApiResponse;
import com.example.Capstone_project.domain.FittingTask;
import com.example.Capstone_project.domain.User;
import com.example.Capstone_project.config.CustomUserDetails;
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

@Tag(name = "Virtual Fitting", description = "나노바나나 프로 API를 활용한 가상 피팅 API")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/virtual-fitting")
public class VirtualFittingController {

    private final FittingService fittingService;
    private final ClothesAnalysisService clothesAnalysisService;
    private final GoogleCloudStorageService gcsService;
    private final Executor taskExecutor;

    @Value("${virtual-fitting.image.storage-path:./images/virtual-fitting}")
    private String imageStoragePath;

    @Operation(summary = "가상 피팅 요청")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<VirtualFittingTaskIdResponse>> createVirtualFitting(
            @RequestParam("user_image") MultipartFile userImage,
            @RequestParam("top_image") MultipartFile topImage,
            @RequestParam(value = "bottom_image", required = false) MultipartFile bottomImage,
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
                            clothesAnalysisService
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

    @Operation(summary = "가상 피팅 작업 상태 조회")
    @GetMapping("/status/{taskId}")
    public ResponseEntity<ApiResponse<VirtualFittingStatusResponse>> getFittingStatus(@PathVariable Long taskId) {
        FittingTask task = fittingService.checkStatus(taskId);

        if (task == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Fitting task not found: " + taskId));
        }


        VirtualFittingStatusResponse body = VirtualFittingStatusResponse.builder()
                .taskId(task.getId())
                .status(task.getStatus())
                .resultImgUrl(task.getResultImgUrl())
                .build();


        return ResponseEntity.ok(ApiResponse.success("Fitting task status retrieved", body));
    }

    @Operation(summary = "피팅 결과 내 옷장 저장")
    @PatchMapping("/{taskId}/save")
    public ResponseEntity<ApiResponse<String>> saveFittingResult(@PathVariable Long taskId) {
        FittingTask task = fittingService.checkStatus(taskId);
        if (task == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("기록 없음"));
        }

        task.setSaved(true);
        fittingService.saveTask(task);
        return ResponseEntity.ok(ApiResponse.success("저장 완료", null));
    }

    @Operation(summary = "내가 저장한 코디 목록 조회")
    @GetMapping("/my-closet")
    public ResponseEntity<ApiResponse<List<FittingTask>>> getMySavedFittings(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUser().getId();
        List<FittingTask> savedList = fittingService.getSavedFittingList(userId);
        return ResponseEntity.ok(ApiResponse.success("조회 성공", savedList));
    }

    @Operation(summary = "이미지 조회")
    @GetMapping("/images/{filename}")
    public ResponseEntity<Resource> getVirtualFittingImage(@PathVariable String filename) {
        try {
            Path filePath = Paths.get(imageStoragePath).resolve(filename);
            File file = filePath.toFile();

            if (!file.exists()) return ResponseEntity.notFound().build();

            Resource resource = new FileSystemResource(file);
            String contentType = Files.probeContentType(filePath);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType != null ? contentType : "application/octet-stream"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}