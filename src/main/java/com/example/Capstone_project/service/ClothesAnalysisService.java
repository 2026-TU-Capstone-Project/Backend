package com.example.Capstone_project.service;

import com.example.Capstone_project.domain.Clothes;
import com.example.Capstone_project.domain.User;
import com.example.Capstone_project.dto.ClothesAnalysisResultDto;
import com.example.Capstone_project.dto.ClothesRequestDto;
import com.example.Capstone_project.repository.ClothesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClothesAnalysisService {

    private final GeminiService geminiService;
    private final ClothesRepository clothesRepository;
    private final GoogleCloudStorageService gcsService;

    /**
     * 옷 분석 및 저장 내부 로직 (핵심 리팩토링 구간)
     */
    private Long analyzeAndSaveClothesInternal(byte[] imageBytes, String filename, String category, User user) throws IOException {

        // [Step 1] 이미지를 GCS에 업로드 (기존 로직 유지)
        String fileExtension = filename.contains(".") ? filename.substring(filename.lastIndexOf(".")) : ".jpg";
        String uniqueFilename = UUID.randomUUID().toString() + fileExtension;

        String imgUrl;
        if ("Top".equalsIgnoreCase(category)) {
            imgUrl = gcsService.uploadTopImage(imageBytes, uniqueFilename, "image/jpeg");
        } else if ("Bottom".equalsIgnoreCase(category)) {
            imgUrl = gcsService.uploadBottomImage(imageBytes, uniqueFilename, "image/jpeg");
        } else {
            imgUrl = gcsService.uploadImage(imageBytes, uniqueFilename, "image/jpeg");
        }

        // [Step 2] Gemini에게 "한 방에" 물어보기 (if문 300줄이 이 한 줄로 끝납니다)
        String prompt = "이 옷 사진을 분석해서 category, color, material, pattern, neckLine, sleeveType, closure, style, fit, length, texture, detail, season, thickness, occasion 정보를 " +
                "한국어 JSON 형식으로만 답변해줘. 예: {\"category\": \"상의\", \"color\": \"검정\", ...}";

        // GeminiService에 추가한 메서드 호출
        ClothesAnalysisResultDto result = geminiService.analyzeClothesImage(imageBytes, prompt);

        // [Step 3] 이름 자동 생성 (결과값 활용)
        String autoName = result.getColor() + " " + result.getMaterial() + " " + result.getCategory();

        // [Step 4] DB 저장 (Gemini가 준 데이터를 그대로 매핑)
        Clothes clothes = Clothes.builder()
                .user(user)
                .category(category)
                .name(autoName)
                .imgUrl(imgUrl)
                .color(result.getColor())
                .material(result.getMaterial())
                .pattern(result.getPattern())
                .neckLine(result.getNeckLine())
                .sleeveType(result.getSleeveType())
                .closure(result.getClosure())
                .style(result.getStyle())
                .fit(result.getFit())
                .length(result.getLength())
                .texture(result.getTexture())
                .detail(result.getDetail())
                .season(result.getSeason())
                .thickness(result.getThickness())
                .occasion(result.getOccasion())
                .brand(null)
                .price(0)
                .build();

        Clothes saved = clothesRepository.save(clothes);
        log.info("✅ Gemini 스마트 분석 완료 - ID: {}, name: {}", saved.getId(), saved.getName());

        return saved.getId();
    }

    // --- 아래의 비동기/동기 래퍼 메서드들은 기존 구조를 그대로 유지하여 에러를 방지합니다 ---

    /**
     * 비동기 옷 분석. MultipartFile 대신 byte[]를 받아야 함.
     * (MultipartFile은 요청 종료 시 임시파일이 삭제되므로 @Async에서 getBytes() 호출 시 NoSuchFileException 발생)
     */
    @Async("taskExecutor")
    @Transactional
    public void analyzeAndSaveClothesAsync(byte[] imageBytes, String filename, String category, User user) {
        try {
            analyzeAndSaveClothesInternal(imageBytes, filename, category, user);
        } catch (IOException e) {
            log.error("❌ 비동기 분석 실패", e);
        }
    }

    @Transactional
    public Long analyzeAndSaveClothes(byte[] imageBytes, String filename, String category, User user) throws IOException {
        return analyzeAndSaveClothesInternal(imageBytes, filename, category, user);
    }

    @Transactional
    public Long analyzeAndSaveClothesSync(MultipartFile file, String category, User user) throws IOException {
        return analyzeAndSaveClothesInternal(file.getBytes(), file.getOriginalFilename(), category, user);
    }
}