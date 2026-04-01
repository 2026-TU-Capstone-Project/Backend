package com.example.Capstone_project.service;

import com.example.Capstone_project.domain.Clothes;
import com.example.Capstone_project.domain.ClothesUploadStatus;
import com.example.Capstone_project.domain.ClothesUploadTask;
import com.example.Capstone_project.domain.User;
import com.example.Capstone_project.dto.ClothesAnalysisResultDto;
import com.example.Capstone_project.dto.ClothesUploadStatusResponse;
import com.example.Capstone_project.repository.ClothesRepository;
import com.example.Capstone_project.repository.ClothesUploadTaskRepository;
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
    private final ClothesUploadTaskRepository clothesUploadTaskRepository;
    private final ClothesUploadSseService clothesUploadSseService;

    /**
     * 옷 분석 및 저장. 트랜잭션 분리: GCS 업로드 + Gemini 호출은 트랜잭션 밖,
     * DB 저장만 짧은 트랜잭션으로 수행해 커넥션 풀 고갈을 방지.
     *
     * [수정 이유]
     * 기존: "한국어 JSON 형식으로만 답변해줘" → 허용값 미지정
     * → Gemini가 season을 "여름", "Summer", "여름/봄" 등 매번 다르게 응답
     * → 날씨 보너스 비교(norm()의 한국어 매핑)가 불안정하게 동작해서 보너스가 항상 0
     *
     * 수정: 한국어 유지 + 허용값 목록 명시 → DB에 "봄/여름/얇음/반팔" 등 일관된 값 저장
     */
    private Long analyzeAndSaveClothesInternal(byte[] imageBytes, String filename, String category, User user, boolean inCloset) throws IOException {

        // [Step 1] GCS 업로드 (트랜잭션 없음 - 외부 I/O)
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

        // [Step 2] Gemini 호출 (트랜잭션 없음 - 오래 걸리는 외부 API)
        // ★ 수정: 허용값 명시로 DB 저장값 일관성 보장
        String prompt = """
                이 옷 사진을 분석해서 아래 JSON 형식으로만 답변해줘. 다른 텍스트 없이 JSON만 출력해.
                
                각 필드는 반드시 아래 허용값 중 하나로만 답해줘:
                - season: 봄, 여름, 가을, 겨울, 사계절 중 하나
                - thickness: 얇음, 보통, 두꺼움 중 하나
                - sleeveType: 반팔, 긴팔, 민소매, 없음 중 하나 (하의나 해당 없으면 없음)
                - category: 상의, 하의, 신발, 아우터, 액세서리 중 하나
                - 나머지 필드: 한국어로 간결하게
                
                {
                  "category": "",
                  "color": "",
                  "material": "",
                  "pattern": "",
                  "neckLine": "",
                  "sleeveType": "",
                  "closure": "",
                  "style": "",
                  "fit": "",
                  "length": "",
                  "texture": "",
                  "detail": "",
                  "season": "",
                  "thickness": "",
                  "occasion": ""
                }
                """;

        ClothesAnalysisResultDto result = geminiService.analyzeClothesImage(imageBytes, prompt);

        // 날씨 필드 확인 로그
        log.info("🧥 옷 분석 완료 - season: {}, thickness: {}, sleeveType: {}",
                result.getSeason(), result.getThickness(), result.getSleeveType());

        // [Step 3] DB 저장만 짧은 트랜잭션
        return saveClothesToDb(imgUrl, result, category, user, inCloset);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long saveClothesToDb(String imgUrl, ClothesAnalysisResultDto result, String category, User user, boolean inCloset) {
        String autoName = result.getColor() + " " + result.getMaterial() + " " + result.getCategory();
        Clothes clothes = Clothes.builder()
                .user(user)
                .inCloset(inCloset)
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

    @Async("taskExecutor")
    public void analyzeAndSaveClothesAsync(byte[] imageBytes, String filename, String category, User user) {
        try {
            analyzeAndSaveClothesInternal(imageBytes, filename, category, user, true);
        } catch (IOException e) {
            log.error("❌ 비동기 분석 실패", e);
        }
    }

    @Async("taskExecutor")
    public void startClothesUploadAndNotify(Long taskId, byte[] imageBytes, String filename, String category, User user) {
        setTaskProcessingAndNotify(taskId);
        try {
            Long clothesId = analyzeAndSaveClothesInternal(imageBytes, filename, category, user, true);
            setTaskCompletedAndNotify(taskId, clothesId);
        } catch (Exception e) {
            log.error("❌ 옷 업로드/분석 실패 taskId={}", taskId, e);
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            if (msg != null && msg.length() > 500) msg = msg.substring(0, 500);
            setTaskFailedAndNotify(taskId, msg);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void setTaskProcessingAndNotify(Long taskId) {
        ClothesUploadTask task = clothesUploadTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("ClothesUploadTask not found: " + taskId));
        task.setStatus(ClothesUploadStatus.PROCESSING);
        clothesUploadTaskRepository.save(task);
        clothesUploadSseService.notifyStatus(taskId, ClothesUploadStatusResponse.builder()
                .taskId(taskId).status(ClothesUploadStatus.PROCESSING).build());
    }

    @Transactional(rollbackFor = Exception.class)
    public void setTaskCompletedAndNotify(Long taskId, Long clothesId) {
        ClothesUploadTask task = clothesUploadTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("ClothesUploadTask not found: " + taskId));
        task.setStatus(ClothesUploadStatus.COMPLETED);
        task.setClothesId(clothesId);
        task.setErrorMessage(null);
        clothesUploadTaskRepository.save(task);
        clothesUploadSseService.notifyStatus(taskId, ClothesUploadStatusResponse.builder()
                .taskId(taskId).status(ClothesUploadStatus.COMPLETED).clothesId(clothesId).build());
    }

    @Transactional(rollbackFor = Exception.class)
    public void setTaskFailedAndNotify(Long taskId, String errorMessage) {
        ClothesUploadTask task = clothesUploadTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("ClothesUploadTask not found: " + taskId));
        task.setStatus(ClothesUploadStatus.FAILED);
        task.setErrorMessage(errorMessage);
        clothesUploadTaskRepository.save(task);
        clothesUploadSseService.notifyStatus(taskId, ClothesUploadStatusResponse.builder()
                .taskId(taskId).status(ClothesUploadStatus.FAILED).errorMessage(errorMessage).build());
    }

    public Long analyzeAndSaveClothes(byte[] imageBytes, String filename, String category, User user) throws IOException {
        return analyzeAndSaveClothesInternal(imageBytes, filename, category, user, false);
    }

    public Long analyzeAndSaveClothesSync(MultipartFile file, String category, User user) throws IOException {
        return analyzeAndSaveClothesInternal(file.getBytes(), file.getOriginalFilename(), category, user, true);
    }
}