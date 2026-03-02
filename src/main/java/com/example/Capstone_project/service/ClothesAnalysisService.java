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
     * 옷 분석 및 저장. **트랜잭션 분리**: GCS 업로드 + Gemini 호출은 트랜잭션 밖(커넥션 미사용),
     * DB 저장만 짧은 트랜잭션으로 수행해 커넥션 풀 고갈을 방지.
     * @param inCloset true=내 옷장에 표시(직접 등록), false=가상피팅 입력용(옷장에 미표시)
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

        // [Step 2] Gemini 호출 (트랜잭션 없음 - 오래 걸리는 외부 API, 커넥션 보유하지 않음)
        String prompt = "이 옷 사진을 분석해서 category, color, material, pattern, neckLine, sleeveType, closure, style, fit, length, texture, detail, season, thickness, occasion 정보를 " +
                "한국어 JSON 형식으로만 답변해줘. 예: {\"category\": \"상의\", \"color\": \"검정\", ...}";
        ClothesAnalysisResultDto result = geminiService.analyzeClothesImage(imageBytes, prompt);

        // [Step 3] DB 저장만 짧은 트랜잭션으로 수행 (커넥션 짧게 사용 후 반납)
        return saveClothesToDb(imgUrl, result, category, user, inCloset);
    }

    /**
     * 분석 결과를 DB에만 저장. 짧은 트랜잭션만 사용하여 커넥션 풀 고갈을 방지.
     */
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

    // --- 아래의 비동기/동기 래퍼 메서드들은 기존 구조를 그대로 유지하여 에러를 방지합니다 ---

    /**
     * 비동기 옷 분석. MultipartFile 대신 byte[]를 받아야 함.
     * (MultipartFile은 요청 종료 시 임시파일이 삭제되므로 @Async에서 getBytes() 호출 시 NoSuchFileException 발생)
     */
    /** 내 옷장 직접 등록용 - inCloset=true (SSE 없이 기존 동작, 하위 호환). 트랜잭션은 내부 saveClothesToDb에서만 짧게 사용. */
    @Async("taskExecutor")
    public void analyzeAndSaveClothesAsync(byte[] imageBytes, String filename, String category, User user) {
        try {
            analyzeAndSaveClothesInternal(imageBytes, filename, category, user, true);
        } catch (IOException e) {
            log.error("❌ 비동기 분석 실패", e);
        }
    }

    /**
     * 옷 업로드 작업 실행: PROCESSING → 분석 → COMPLETED/FAILED, SSE로 상태 전송.
     * 트랜잭션 분리: 상태 변경/저장만 짧은 트랜잭션, GCS+Gemini 구간은 트랜잭션 없음.
     */
    @Async("taskExecutor")
    public void startClothesUploadAndNotify(Long taskId, byte[] imageBytes, String filename, String category, User user) {
        setTaskProcessingAndNotify(taskId);

        try {
            Long clothesId = analyzeAndSaveClothesInternal(imageBytes, filename, category, user, true);
            setTaskCompletedAndNotify(taskId, clothesId);
        } catch (Exception e) {
            log.error("❌ 옷 업로드/분석 실패 taskId={}", taskId, e);
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            if (msg != null && msg.length() > 500) {
                msg = msg.substring(0, 500);
            }
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
                .taskId(taskId)
                .status(ClothesUploadStatus.PROCESSING)
                .build());
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
                .taskId(taskId)
                .status(ClothesUploadStatus.COMPLETED)
                .clothesId(clothesId)
                .build());
    }

    @Transactional(rollbackFor = Exception.class)
    public void setTaskFailedAndNotify(Long taskId, String errorMessage) {
        ClothesUploadTask task = clothesUploadTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("ClothesUploadTask not found: " + taskId));
        task.setStatus(ClothesUploadStatus.FAILED);
        task.setErrorMessage(errorMessage);
        clothesUploadTaskRepository.save(task);
        clothesUploadSseService.notifyStatus(taskId, ClothesUploadStatusResponse.builder()
                .taskId(taskId)
                .status(ClothesUploadStatus.FAILED)
                .errorMessage(errorMessage)
                .build());
    }

    /**
     * 가상피팅 입력용 - inCloset=false. 트랜잭션은 내부 saveClothesToDb에서만 짧게 사용.
     */
    public Long analyzeAndSaveClothes(byte[] imageBytes, String filename, String category, User user) throws IOException {
        return analyzeAndSaveClothesInternal(imageBytes, filename, category, user, false);
    }

    /** 내 옷장 동기 등록용. 트랜잭션은 내부 saveClothesToDb에서만 짧게 사용. */
    public Long analyzeAndSaveClothesSync(MultipartFile file, String category, User user) throws IOException {
        return analyzeAndSaveClothesInternal(file.getBytes(), file.getOriginalFilename(), category, user, true);
    }
}