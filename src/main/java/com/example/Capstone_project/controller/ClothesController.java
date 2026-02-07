package com.example.Capstone_project.controller;

import com.example.Capstone_project.common.dto.ApiResponse;
import com.example.Capstone_project.domain.Clothes;
import com.example.Capstone_project.repository.ClothesRepository;
import com.example.Capstone_project.service.ClothesAnalysisService;
import com.example.Capstone_project.dto.ClothesRequestDto;
import com.example.Capstone_project.service.GoogleCloudStorageService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import lombok.RequiredArgsConstructor;
import com.example.Capstone_project.config.CustomUserDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Clothes API Controller
 * 옷 등록 및 분석 기능 제공 (비동기 처리)
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/clothes")
public class ClothesController {

    private final ClothesRepository clothesRepository;
    private final ClothesAnalysisService clothesAnalysisService;
    private final GoogleCloudStorageService gcsService;

    /**
     * 옷 등록 및 분석 (AI 초정밀 분석 기능 탑재)
     * 비동기로 처리되며 즉시 202 Accepted 응답 반환
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<String>> uploadClothes(
            @RequestParam("file") MultipartFile file,
            @RequestParam("category") String category,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("Clothes upload request received - file: {}, category: {}", 
                file.getOriginalFilename(), category);

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("File is required"));
        }

        // 비동기로 옷 분석 및 저장 시작
        clothesAnalysisService.analyzeAndSaveClothesAsync(file, category, userDetails.getUser());
        
        log.info("✅ 옷 등록 요청 완료 - category: {} (비동기 처리 중)", category);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success("Clothes registration started. Processing in background.", 
                        "옷 등록이 시작되었습니다. 백그라운드에서 분석 및 저장이 진행됩니다."));
    }

    /**
     * 옷 분석 요청 (기존 메서드 유지)
     * 상의, 하의, 신발을 한 번에 분석
     */
    @PostMapping(value = "/analysis", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<String>> analyze(
            @ModelAttribute ClothesRequestDto requestDto,
            @AuthenticationPrincipal CustomUserDetails userDetails // 👈 CustomUserDetails로 이름 수정
    ) {
        log.info("👕 Clothes analysis request received");

        // 1. 유저 정보 가져오기 (userDetails에서 추출)
        com.example.Capstone_project.domain.User user = userDetails.getUser();

        try {
            // 2. DTO에서 파일을 하나씩 꺼내서 동기(Sync) 방식으로 즉시 처리
            // 상의 분석
            if (requestDto.getTop() != null && !requestDto.getTop().isEmpty()) {
                clothesAnalysisService.analyzeAndSaveClothesSync(requestDto.getTop(), "Top", user);
            }
            // 하의 분석
            if (requestDto.getBottom() != null && !requestDto.getBottom().isEmpty()) {
                clothesAnalysisService.analyzeAndSaveClothesSync(requestDto.getBottom(), "Bottom", user);
            }
            // 신발 분석
            if (requestDto.getShoes() != null && !requestDto.getShoes().isEmpty()) {
                clothesAnalysisService.analyzeAndSaveClothesSync(requestDto.getShoes(), "Shoes", user);
            }

            // 모든 작업이 끝나면 성공 응답 반환
            return ResponseEntity.ok(ApiResponse.success("모든 옷 등록 및 분석 성공!", "분석 완료"));

        } catch (Exception e) {
            log.error("❌ 분석 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("분석 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 옷 목록 조회
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Clothes>>> getAllClothes(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        // 내 옷만 최신순으로 가져오도록 수정
        List<Clothes> clothesList = clothesRepository.findByUserOrderByCreatedAtDesc(userDetails.getUser());
        return ResponseEntity.ok(ApiResponse.success("내 옷장 목록 조회 성공", clothesList));
    }

    /**
     * 옷 상세 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Clothes>> getClothesById(@PathVariable Long id) {
        Clothes clothes = clothesRepository.findById(id)
                .orElse(null);
        
        if (clothes == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Clothes not found: " + id));
        }
        
        return ResponseEntity.ok(ApiResponse.success("Clothes retrieved", clothes));
    }

    // [ClothesController.java]
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteClothes(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Clothes clothes = clothesRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("해당 옷을 찾을 수 없습니다."));

        if (!clothes.getUser().getId().equals(userDetails.getUser().getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("삭제 권한이 없습니다."));
        }

        String blobName = gcsService.extractBlobNameFromUrl(clothes.getImgUrl());
        gcsService.deleteImage(blobName);

        clothesRepository.delete(clothes);
        return ResponseEntity.ok(ApiResponse.success("옷 삭제 완료", null));
    }
}
