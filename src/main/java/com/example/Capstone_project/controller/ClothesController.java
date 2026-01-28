package com.example.Capstone_project.controller;

import com.example.Capstone_project.common.dto.ApiResponse;
import com.example.Capstone_project.domain.Clothes;
import com.example.Capstone_project.repository.ClothesRepository;
import com.example.Capstone_project.service.ClothesAnalysisService;
import com.example.Capstone_project.dto.ClothesRequestDto;
import com.example.Capstone_project.domain.User;
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
    public ResponseEntity<ApiResponse<String>> analyze(@ModelAttribute ClothesRequestDto requestDto,@AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("Clothes analysis request received");
        
        // 서비스 호출 (비동기 처리)
        clothesAnalysisService.analyzeClothes(requestDto, userDetails.getUser());

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success("Analysis request completed. Processing in background.", "분석 요청 완료!"));
    }

    /**
     * 옷 목록 조회
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Clothes>>> getAllClothes() {
        List<Clothes> clothesList = clothesRepository.findAll();
        return ResponseEntity.ok(ApiResponse.success("Clothes list retrieved", clothesList));
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
}
