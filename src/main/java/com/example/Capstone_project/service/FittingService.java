package com.example.Capstone_project.service;

import com.example.Capstone_project.dto.StyleAnalysisResult;
import com.example.Capstone_project.dto.VirtualFittingResponse;
import com.example.Capstone_project.domain.FittingStatus;
import com.example.Capstone_project.domain.FittingTask;
import com.example.Capstone_project.domain.User;
import com.example.Capstone_project.repository.FittingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@Service
@RequiredArgsConstructor
public class FittingService {

    private final GeminiService geminiService;
    private final FittingRepository fittingRepository;
    private final GoogleCloudStorageService gcsService;
    @Qualifier("taskExecutor")
    private final Executor taskExecutor;

    @Value("${virtual-fitting.image.storage-path:./images/virtual-fitting}")
    private String imageStoragePath;

    @Transactional
    public FittingTask createFittingTask(Long userId, String bodyImgUrl) {
        FittingTask task = new FittingTask(FittingStatus.WAITING);
        task.setUserId(userId);
        task.setBodyImgUrl(bodyImgUrl);
        return fittingRepository.save(task);
    }

    @Transactional
    public void updateFittingTaskClothes(Long taskId, Long topId, Long bottomId) {
        FittingTask task = fittingRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found: " + taskId));
        task.setTopId(topId);
        task.setBottomId(bottomId);
        fittingRepository.save(task);
    }

    public void processFitting(Long taskId, byte[] userImgData, String userImageFilename, byte[] topImgData, byte[] bottomImgData) {
        log.info("🚀 가상 피팅 작업 시작 - Task ID: {}", taskId);

        try {
            // 1) 상태를 PROCESSING으로 먼저 변경 (짧은 트랜잭션)
            updateTaskStatus(taskId, FittingStatus.PROCESSING);

            // 2) 가장 오래 걸리는 Gemini 가상 피팅 호출
            VirtualFittingResponse response = geminiService.processVirtualFitting(
                    userImgData,
                    topImgData,
                    bottomImgData,
                    null, null, null
            );

            if (response != null && "completed".equals(response.getStatus())) {
                // 3) 결과 URL과 COMPLETED 상태를 먼저 저장 (빠르게 커밋)
                updateFittingTaskResult(taskId, response.getImageUrl());
                log.info("✅ [작업 완료] URL: {}", response.getImageUrl());

                // 4) 전신 사진 업로드 및 스타일 분석은 후처리 (DB 트랜잭션과 분리)
                String bodyImgUrl = null;
                try {
                    String filename = (userImageFilename != null && !userImageFilename.isEmpty())
                            ? userImageFilename
                            : java.util.UUID.randomUUID().toString() + ".jpg";

                    bodyImgUrl = gcsService.uploadUserBodyImage(
                            userImgData,
                            filename,
                            "image/jpeg"
                    );
                    log.info("✅ 전신 사진 GCS 업로드 완료 - Task ID: {}, URL: {}", taskId, bodyImgUrl);
                } catch (Exception e) {
                    log.error("❌ 전신 사진 GCS 업로드 실패 - Task ID: {}, 오류: {}", taskId, e.getMessage(), e);
                    // 전신 사진 업로드 실패해도 가상 피팅은 성공으로 처리
                }

                StyleAnalysisResult styleResult = null;
                try {
                    styleResult = analyzeVirtualFittingResultImage(response.getImageUrl());
                    log.info("✅ [스타일 분석 완료] Task ID: {}, resultGender: {}", taskId, styleResult.getResultGender());
                } catch (Exception e) {
                    log.error("❌ 스타일 분석 중 오류 발생 - Task ID: {}, 오류: {}", taskId, e.getMessage(), e);
                    // 스타일 분석 실패해도 가상 피팅은 성공으로 처리
                }

                // 5) 스타일/전신 사진 정보 및 임베딩은 별도의 짧은 트랜잭션으로 저장
                if (bodyImgUrl != null || styleResult != null) {
                    float[] styleEmbedding = null;
                    String styleAnalysis = styleResult != null ? styleResult.getStyleAnalysis() : null;
                    if (styleAnalysis != null) {
                        try {
                            styleEmbedding = geminiService.embedText(styleAnalysis, "RETRIEVAL_DOCUMENT");
                        } catch (Exception e) {
                            log.warn("❌ 스타일 임베딩 생성 실패 - Task ID: {}, 텍스트만 저장", taskId, e);
                        }
                    }
                    updateFittingTaskStyleAndBody(taskId, bodyImgUrl, styleAnalysis, styleEmbedding,
                        styleResult != null ? styleResult.getResultGender() : null);
                }
            } else {
                log.error("❌ 가상 피팅 실패 - 응답 상태: {}", response != null ? response.getStatus() : "null");
                updateTaskStatus(taskId, FittingStatus.FAILED);
            }
        } catch (Exception e) {
            log.error("❌ 가상 피팅 처리 중 오류: {}", e.getMessage(), e);
            updateTaskStatus(taskId, FittingStatus.FAILED);
        }
    }

    @Transactional(readOnly = true)
    public FittingTask checkStatus(Long id) {
        return fittingRepository.findById(id).orElse(null);
    }

    @Transactional
    public void updateTaskStatus(Long taskId, FittingStatus status) {
        FittingTask task = fittingRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found: " + taskId));
        task.setStatus(status);
        fittingRepository.save(task);
    }

    @Transactional
    public void updateFittingTaskResult(Long taskId, String resultImgUrl) {
        FittingTask task = fittingRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found: " + taskId));
        task.setStatus(FittingStatus.COMPLETED);
        task.setResultImgUrl(resultImgUrl);
        fittingRepository.save(task);
    }

    @Transactional
    public void updateFittingTaskStyleAndBody(Long taskId, String bodyImgUrl, String styleAnalysis,
            float[] styleEmbedding, com.example.Capstone_project.domain.Gender resultGender) {
        FittingTask task = fittingRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found: " + taskId));

        if (bodyImgUrl != null) {
            task.setBodyImgUrl(bodyImgUrl);
        }
        if (styleAnalysis != null) {
            task.setStyleAnalysis(styleAnalysis);
        }
        if (styleEmbedding != null) {
            task.setStyleEmbedding(styleEmbedding);
        }
        if (resultGender != null) {
            task.setResultGender(resultGender);
        }

        fittingRepository.save(task);
    }

    @Transactional
    public void updateFittingTaskStyleAndBody(Long taskId, String bodyImgUrl, String styleAnalysis) {
        updateFittingTaskStyleAndBody(taskId, bodyImgUrl, styleAnalysis, null, null);
    }

    /**
     * 가상 피팅 전체 프로세스 (동기) - VirtualFittingController에서 호출
     * 옷 분석과 가상 피팅을 모두 완료할 때까지 대기하고 완료된 FittingTask 반환
     * 
     * @param taskId FittingTask ID
     * @param userImageBytes 전신 사진 바이트 배열
     * @param topImageBytes 상의 사진 바이트 배열
     * @param topImageFilename 상의 사진 파일명
     * @param bottomImageBytes 하의 사진 바이트 배열
     * @param bottomImageFilename 하의 사진 파일명
     * @param clothesAnalysisService 옷 분석 서비스 (순환 참조 방지를 위해 파라미터로 전달)
     * @return 완료된 FittingTask (resultImgUrl 포함)
     */
    @Transactional
    public FittingTask processVirtualFittingWithClothesAnalysisSync(
            Long taskId,
            byte[] userImageBytes,
            byte[] topImageBytes,
            String topImageFilename,
            byte[] bottomImageBytes,
            String bottomImageFilename,
            ClothesAnalysisService clothesAnalysisService,
            FittingTask task,
            User user
    ) {
        log.info("🚀 [동기] 가상 피팅 전체 프로세스 시작 - Task ID: {}", taskId);

        try {
            final User currentUser = task.getUser();
            // 1. 옷 분석 시작 (병렬 처리 - 동일 taskExecutor 사용)
            CompletableFuture<Long> topAnalysisFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    log.info("🔄 [동기] 상의 분석 시작 - Task ID: {}", taskId);
                    return clothesAnalysisService.analyzeAndSaveClothes(topImageBytes, topImageFilename, "Top", currentUser);
                } catch (Exception e) {
                    log.error("❌ 상의 분석 중 오류 발생 - Task ID: {}", taskId, e);
                    return null;
                }
            }, taskExecutor);

            CompletableFuture<Long> bottomAnalysisFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    log.info("🔄 [동기] 하의 분석 시작 - Task ID: {}", taskId);
                    return clothesAnalysisService.analyzeAndSaveClothes(bottomImageBytes, bottomImageFilename, "Bottom", currentUser);
                } catch (Exception e) {
                    log.error("❌ 하의 분석 중 오류 발생 - Task ID: {}", taskId, e);
                    return null;
                }
            }, taskExecutor);

            // 2. 옷 분석 완료 대기
            CompletableFuture.allOf(topAnalysisFuture, bottomAnalysisFuture).join();
            Long topId = topAnalysisFuture.join();
            Long bottomId = bottomAnalysisFuture.join();

            if (topId == null || bottomId == null) {
                log.error("❌ 옷 분석 실패로 인해 가상 피팅을 시작할 수 없습니다 - Task ID: {}, topId: {}, bottomId: {}", 
                        taskId, topId, bottomId);
                updateTaskStatus(taskId, FittingStatus.FAILED);
                return fittingRepository.findById(taskId).orElse(null);
            }

            // FittingTask에 옷 ID 연결
            updateFittingTaskClothes(taskId, topId, bottomId);
            log.info("✅ FittingTask에 옷 정보 연결 완료 - Task ID: {}, topId: {}, bottomId: {}", 
                    taskId, topId, bottomId);

            // 3. 가상 피팅 처리 (동기)
            final FittingTask dbtask = fittingRepository.findById(taskId)
                    .orElseThrow(() -> new RuntimeException("Task not found: " + taskId));

            task.setStatus(FittingStatus.PROCESSING);
            fittingRepository.save(task);

            VirtualFittingResponse response = geminiService.processVirtualFitting(
                    userImageBytes,
                    topImageBytes,
                    bottomImageBytes,
                    null, null, null
            );

            if (response != null && "completed".equals(response.getStatus())) {
                task.setStatus(FittingStatus.COMPLETED);
                task.setResultImgUrl(response.getImageUrl());
                log.info("✅ [작업 완료] URL: {}", response.getImageUrl());

                // 4. 가상 피팅 결과 이미지 스타일 분석 및 임베딩 저장
                try {
                    StyleAnalysisResult styleResult = analyzeVirtualFittingResultImage(response.getImageUrl());
                    task.setStyleAnalysis(styleResult.getStyleAnalysis());
                    task.setResultGender(styleResult.getResultGender());
                    try {
                        float[] embedding = geminiService.embedText(styleResult.getStyleAnalysis(), "RETRIEVAL_DOCUMENT");
                        task.setStyleEmbedding(embedding);
                    } catch (Exception embEx) {
                        log.warn("❌ 스타일 임베딩 생성 실패 - Task ID: {}, 텍스트만 저장", taskId, embEx);
                    }
                    log.info("✅ [스타일 분석 완료] Task ID: {}, resultGender: {}", taskId, styleResult.getResultGender());
                } catch (Exception e) {
                    log.error("❌ 스타일 분석 중 오류 발생 - Task ID: {}, 오류: {}", taskId, e.getMessage(), e);
                    // 스타일 분석 실패해도 가상 피팅은 성공으로 처리
                }
            } else {
                task.setStatus(FittingStatus.FAILED);
                log.error("❌ 가상 피팅 실패 - 응답 상태: {}", response != null ? response.getStatus() : "null");
            }
            
            fittingRepository.save(task);
            return task;

        } catch (Exception e) {
            log.error("❌ 가상 피팅 전체 프로세스 중 오류 발생 - Task ID: {}", taskId, e);
            updateTaskStatus(taskId, FittingStatus.FAILED);
            return fittingRepository.findById(taskId).orElse(null);
        }
    }

    /**
     * 가상 피팅 전체 프로세스 시작 (비동기) - 기존 메서드 유지 (하위 호환성)
     * 옷 분석과 가상 피팅을 모두 비동기로 처리하는 통합 메서드
     * 
     * @param taskId FittingTask ID
     * @param userImageBytes 전신 사진 바이트 배열
     * @param userImageFilename 전신 사진 파일명
     * @param topImageBytes 상의 사진 바이트 배열
     * @param topImageFilename 상의 사진 파일명
     * @param bottomImageBytes 하의 사진 바이트 배열
     * @param bottomImageFilename 하의 사진 파일명
     * @param clothesAnalysisService 옷 분석 서비스 (순환 참조 방지를 위해 파라미터로 전달)
     */
    @Async("taskExecutor")
    @Transactional
    public void processVirtualFittingWithClothesAnalysis(
            Long taskId,
            byte[] userImageBytes,
            String userImageFilename,
            byte[] topImageBytes,
            String topImageFilename,
            byte[] bottomImageBytes,
            String bottomImageFilename,
            ClothesAnalysisService clothesAnalysisService,
            User user
    ) {
        log.info("🚀 [비동기] 가상 피팅 전체 프로세스 시작 - Task ID: {}", taskId);

        final User dbUser = user;


        try {
            // 최소 하나는 있어야 함 (컨트롤러에서 검증하지만 이중 체크)
            if (topImageBytes == null && bottomImageBytes == null) {
                log.error("❌ 상의와 하의가 모두 없습니다 - Task ID: {}", taskId);
                updateTaskStatus(taskId, FittingStatus.FAILED);
                return;
            }

            // 1. 옷 분석 시작 (병렬 처리 - taskExecutor 사용, null 체크 포함)
            List<CompletableFuture<Long>> analysisFutures = new ArrayList<>();

            // final 변수로 선언하여 람다에서 안전하게 참조 가능하도록 함
            final CompletableFuture<Long> topAnalysisFuture;
            if (topImageBytes != null) {
                topAnalysisFuture = CompletableFuture.supplyAsync(() -> {
                    try {
                        log.info("💎 [비동기] 상의 분석 시작 - Task ID: {}", taskId);
                        return clothesAnalysisService.analyzeAndSaveClothes(topImageBytes, topImageFilename, "Top", user);
                    } catch (Exception e) {
                        log.error("❌ 상의 분석 중 오류 발생 - Task ID: {}", taskId, e);
                        return null;
                    }
                }, taskExecutor);
                analysisFutures.add(topAnalysisFuture);
            } else {
                topAnalysisFuture = CompletableFuture.completedFuture(null);
            }

            final CompletableFuture<Long> bottomAnalysisFuture;
            if (bottomImageBytes != null) {
                bottomAnalysisFuture = CompletableFuture.supplyAsync(() -> {
                    try {
                        log.info("💎 [비동기] 하의 분석 시작 - Task ID: {}", taskId);
                        return clothesAnalysisService.analyzeAndSaveClothes(bottomImageBytes, bottomImageFilename, "Bottom", user);
                    } catch (Exception e) {
                        log.error("❌ 하의 분석 중 오류 발생 - Task ID: {}", taskId, e);
                        return null;
                    }
                }, taskExecutor);
                analysisFutures.add(bottomAnalysisFuture);
            } else {
                bottomAnalysisFuture = CompletableFuture.completedFuture(null);
            }

            // 2. 옷 분석 완료 대기 및 가상 피팅 시작 (동일 taskExecutor에서 실행)
            CompletableFuture.allOf(analysisFutures.toArray(new CompletableFuture[0])).thenRunAsync(() -> {
                try {
                    Long topId = topAnalysisFuture.join();
                    Long bottomId = bottomAnalysisFuture.join();

                    // 최소 하나는 성공해야 함
                    if (topId != null || bottomId != null) {
                        // FittingTask에 옷 ID 연결 (null일 수 있음)
                        updateFittingTaskClothes(taskId, topId, bottomId);
                        log.info("✅ FittingTask에 옷 정보 연결 완료 - Task ID: {}, topId: {}, bottomId: {}",
                                taskId, topId, bottomId);

                        // 가상 피팅 처리 시작 (비동기)
                        processFitting(taskId, userImageBytes, userImageFilename, topImageBytes, bottomImageBytes);
                        log.info("🚀 가상 피팅 작업 시작 - Task ID: {}", taskId);
                    } else {
                        log.error("❌ 옷 분석 실패로 인해 가상 피팅을 시작할 수 없습니다 - Task ID: {}, topId: {}, bottomId: {}",
                                taskId, topId, bottomId);
                        updateTaskStatus(taskId, FittingStatus.FAILED);
                    }
                } catch (Exception e) {
                    log.error("❌ 가상 피팅 작업 시작 중 오류 발생 - Task ID: {}", taskId, e);
                    updateTaskStatus(taskId, FittingStatus.FAILED);
                }
            }, taskExecutor);

        } catch (Exception e) {
            log.error("❌ 가상 피팅 전체 프로세스 시작 중 오류 발생 - Task ID: {}", taskId, e);
            updateTaskStatus(taskId, FittingStatus.FAILED);
        }
    }

    /**
     * 가상 피팅 결과 이미지의 스타일 분석 + 이미지 속 인물 성별 판별
     * Gemini가 스타일 설명과 함께 사진 속 인물이 남성/여성인지 판별함
     *
     * @param resultImgUrl 가상 피팅 결과 이미지 URL (GCS URL 또는 로컬 경로)
     * @return 스타일 분석 + 성별 (resultGender)
     */
    private StyleAnalysisResult analyzeVirtualFittingResultImage(String resultImgUrl) throws IOException {
        log.info("🎨 가상 피팅 결과 이미지 스타일 분석 시작 - URL: {}", resultImgUrl);
        
        byte[] imageBytes;
        
        // GCS URL인지 확인 (storage.googleapis.com 포함)
        if (resultImgUrl != null && resultImgUrl.contains("storage.googleapis.com")) {
            // GCS에서 이미지 다운로드
            String blobName = gcsService.extractBlobNameFromUrl(resultImgUrl);
            imageBytes = gcsService.downloadImage(blobName);
            log.info("📸 GCS에서 이미지 다운로드 완료 - 크기: {} bytes", imageBytes.length);
        } else {
            // 로컬 파일 시스템에서 읽기 (하위 호환성)
            String filename = resultImgUrl.substring(resultImgUrl.lastIndexOf("/") + 1);
            Path imagePath = Paths.get(imageStoragePath).resolve(filename);
            
            if (!Files.exists(imagePath)) {
                log.warn("⚠️ 이미지 파일을 찾을 수 없습니다: {}", imagePath.toAbsolutePath());
                throw new IOException("Image file not found: " + imagePath.toAbsolutePath());
            }
            
            imageBytes = Files.readAllBytes(imagePath);
            log.info("📸 로컬 파일에서 이미지 읽기 완료 - 크기: {} bytes", imageBytes.length);
        }
        
        StyleAnalysisResult result = geminiService.analyzeImageStyleWithGender(imageBytes);
        log.info("✅ Gemini API 스타일+성별 분석 완료 - resultGender: {}", result.getResultGender());
        return result;
    }

    @Transactional
    public void saveTask(FittingTask task) {
        fittingRepository.save(task);
    }
    @Transactional(readOnly = true)
    public List<FittingTask> getSavedFittingList(Long userId) {
        return fittingRepository.findByUserIdAndIsSavedTrue(userId);
    }

    public void processVirtualFittingWithClothesAnalysis(Long id, byte[] userImageBytes, String userImageFilename, byte[] topImageBytes, String topImageFilename, byte[] bottomImageBytes, String bottomImageFilename, ClothesAnalysisService clothesAnalysisService) {
    }
}