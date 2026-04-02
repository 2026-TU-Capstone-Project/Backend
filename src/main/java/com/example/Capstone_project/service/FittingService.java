package com.example.Capstone_project.service;

import com.example.Capstone_project.dto.SavedFittingResponseDto;
import com.example.Capstone_project.dto.StyleAnalysisResult;
import com.example.Capstone_project.dto.VirtualFittingResponse;
import com.example.Capstone_project.dto.VirtualFittingStatusResponse;
import com.example.Capstone_project.domain.FitType;
import com.example.Capstone_project.domain.FittingStatus;
import com.example.Capstone_project.domain.FittingTask;
import com.example.Capstone_project.domain.User;
import com.example.Capstone_project.repository.ClothesRepository;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@Service
@RequiredArgsConstructor
public class FittingService {

    private final GeminiService geminiService;
    private final FittingRepository fittingRepository;
    private final ClothesRepository clothesRepository;
    private final GoogleCloudStorageService gcsService;
    private final FittingCleanupService fittingCleanupService;
    private final VirtualFittingSseService virtualFittingSseService;
    @Qualifier("taskExecutor")
    private final Executor taskExecutor;

    @Value("${virtual-fitting.image.storage-path:./images/virtual-fitting}")
    private String imageStoragePath;

    @Transactional
    public FittingTask createFittingTask(Long userId, String bodyImgUrl) {
        return createFittingTask(userId, bodyImgUrl, null);
    }

    @Transactional
    public FittingTask createFittingTask(Long userId, String bodyImgUrl, FitType fitType) {
        FittingTask task = new FittingTask(FittingStatus.WAITING);
        task.setUserId(userId);
        task.setBodyImgUrl(bodyImgUrl);
        task.setFitType(fitType);
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
        processFitting(taskId, userImgData, userImageFilename, topImgData, bottomImgData, System.currentTimeMillis(), null);
    }

    public void processFitting(Long taskId, byte[] userImgData, String userImageFilename, byte[] topImgData, byte[] bottomImgData, long processStartTime) {
        processFitting(taskId, userImgData, userImageFilename, topImgData, bottomImgData, processStartTime, null);
    }

    public void processFitting(Long taskId, byte[] userImgData, String userImageFilename, byte[] topImgData, byte[] bottomImgData, long processStartTime, FitType fitType) {
        long startTime = System.currentTimeMillis();
        log.info("🚀 가상 피팅 작업 시작 - Task ID: {}, fitType: {}", taskId, fitType);

        try {
            // 1) 상태를 PROCESSING으로 먼저 변경 (짧은 트랜잭션)
            updateTaskStatus(taskId, FittingStatus.PROCESSING);

            // 2) 가장 오래 걸리는 Gemini 가상 피팅 호출
            String prompt = buildPromptWithFitType(fitType);
            VirtualFittingResponse response = geminiService.processVirtualFitting(
                    userImgData,
                    topImgData,
                    bottomImgData,
                    prompt, null, null
            );

            if (response != null && "completed".equals(response.getStatus())) {
                // 3) 결과 URL과 COMPLETED 상태를 먼저 저장 → SSE로 클라이언트에 즉시 전달
                updateFittingTaskResult(taskId, response.getImageUrl());
                long fittingElapsed = System.currentTimeMillis() - startTime;
                log.info("✅ [작업 완료] URL: {} (Gemini 피팅 소요: {}초)", response.getImageUrl(), String.format("%.1f", fittingElapsed / 1000.0));
                long userWaitTime = System.currentTimeMillis() - processStartTime;
                log.info("⚡ [사용자 반환 완료] Task ID: {} - 사용자 대기시간: {}초", taskId, String.format("%.1f", userWaitTime / 1000.0));

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

                long totalElapsed = System.currentTimeMillis() - startTime;
                log.info("🏁 가상 피팅 전체 완료 - Task ID: {}, 총 소요시간: {}초", taskId, String.format("%.1f", totalElapsed / 1000.0));
            } else {
                log.error("❌ 가상 피팅 실패 - 응답 상태: {}", response != null ? response.getStatus() : "null");
                updateTaskStatus(taskId, FittingStatus.FAILED);
            }
        } catch (Exception e) {
            long errorElapsed = System.currentTimeMillis() - startTime;
            log.error("❌ 가상 피팅 처리 중 오류 ({}초 경과): {}", String.format("%.1f", errorElapsed / 1000.0), e.getMessage(), e);
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
        VirtualFittingStatusResponse response = new VirtualFittingStatusResponse(
                task.getId(), task.getStatus(), task.getResultImgUrl());
        virtualFittingSseService.notifyStatus(taskId, response);
    }

    @Transactional
    public void updateFittingTaskResult(Long taskId, String resultImgUrl) {
        FittingTask task = fittingRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found: " + taskId));
        task.setStatus(FittingStatus.COMPLETED);
        task.setResultImgUrl(resultImgUrl);
        fittingRepository.save(task);
        VirtualFittingStatusResponse response = new VirtualFittingStatusResponse(
                task.getId(), task.getStatus(), task.getResultImgUrl());
        virtualFittingSseService.notifyStatus(taskId, response);
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
    /** 트랜잭션 없음: 내부에서 updateTaskStatus 등 짧은 트랜잭션만 사용. Gemini 대기 구간에서 커넥션 보유하지 않음. */
    @Async("taskExecutor")
    public void processVirtualFittingWithClothesAnalysis(
            Long taskId,
            byte[] userImageBytes,
            String userImageFilename,
            byte[] topImageBytes,
            String topImageFilename,
            byte[] bottomImageBytes,
            String bottomImageFilename,
            ClothesAnalysisService clothesAnalysisService,
            User user,
            FitType fitType
    ) {
        long processStartTime = System.currentTimeMillis();
        log.info("🚀 [비동기] 가상 피팅 전체 프로세스 시작 - Task ID: {}, fitType: {}", taskId, fitType);

        try {
            if (topImageBytes == null && bottomImageBytes == null) {
                log.error("❌ 상의와 하의가 모두 없습니다 - Task ID: {}", taskId);
                updateTaskStatus(taskId, FittingStatus.FAILED);
                return;
            }

            // === 1. 가상 피팅을 먼저 시작 (가장 오래 걸리는 작업) ===
            final long pStart = processStartTime;
            final CompletableFuture<Void> fittingFuture = CompletableFuture.runAsync(() -> {
                processFitting(taskId, userImageBytes, userImageFilename, topImageBytes, bottomImageBytes, pStart, fitType);
            }, taskExecutor);

            // === 2. 2초 후 옷 분석 시작 (Rate Limit 회피, 동시 2개씩만 Gemini 호출) ===
            final CompletableFuture<Long> topAnalysisFuture;
            final CompletableFuture<Long> bottomAnalysisFuture;

            // 상의 분석 (2초 후 시작)
            if (topImageBytes != null) {
                topAnalysisFuture = CompletableFuture.supplyAsync(() -> {
                    try {
                        Thread.sleep(2000);
                        log.info("💎 [지연 시작] 상의 분석 시작 - Task ID: {}", taskId);
                        return clothesAnalysisService.analyzeAndSaveClothes(topImageBytes, topImageFilename, "Top", user);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return null;
                    } catch (Exception e) {
                        log.error("❌ 상의 분석 중 오류 발생 - Task ID: {}", taskId, e);
                        return null;
                    }
                }, taskExecutor);
            } else {
                topAnalysisFuture = CompletableFuture.completedFuture(null);
            }

            // 하의 분석 (2초 후 시작, 상의와 동시)
            if (bottomImageBytes != null) {
                bottomAnalysisFuture = CompletableFuture.supplyAsync(() -> {
                    try {
                        Thread.sleep(2000);
                        log.info("💎 [지연 시작] 하의 분석 시작 - Task ID: {}", taskId);
                        return clothesAnalysisService.analyzeAndSaveClothes(bottomImageBytes, bottomImageFilename, "Bottom", user);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return null;
                    } catch (Exception e) {
                        log.error("❌ 하의 분석 중 오류 발생 - Task ID: {}", taskId, e);
                        return null;
                    }
                }, taskExecutor);
            } else {
                bottomAnalysisFuture = CompletableFuture.completedFuture(null);
            }

            // === 3. 옷 분석 완료 시 → clothes ID를 FittingTask에 연결 ===
            CompletableFuture.allOf(topAnalysisFuture, bottomAnalysisFuture).thenRunAsync(() -> {
                try {
                    Long topId = topAnalysisFuture.join();
                    Long bottomId = bottomAnalysisFuture.join();
                    long analysisElapsed = System.currentTimeMillis() - processStartTime;
                    log.info("📊 옷 분석 완료 - Task ID: {}, topId: {}, bottomId: {}, 분석 소요: {}초",
                            taskId, topId, bottomId, String.format("%.1f", analysisElapsed / 1000.0));

                    if (topId != null || bottomId != null) {
                        updateFittingTaskClothes(taskId, topId, bottomId);
                    } else {
                        log.warn("⚠️ 옷 분석 모두 실패 - Task ID: {}, clothes ID 연결 생략 (피팅 결과는 유지)", taskId);
                    }
                } catch (Exception e) {
                    log.error("❌ 옷 분석 결과 연결 중 오류 - Task ID: {}", taskId, e);
                }
            }, taskExecutor);

            // === 4. 모든 작업 완료 후 최종 로그 ===
            CompletableFuture.allOf(topAnalysisFuture, bottomAnalysisFuture, fittingFuture).thenRunAsync(() -> {
                long totalElapsed = System.currentTimeMillis() - processStartTime;
                log.info("🏁 전체 프로세스 완료 - Task ID: {}, 총 소요시간: {}초", taskId, String.format("%.1f", totalElapsed / 1000.0));
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

    private String buildPromptWithFitType(FitType fitType) {
        if (fitType == null) return null;
        String base = "Put the provided top and bottom garments on the person in the full-body photo.";
        String fitDesc = switch (fitType) {
            case SLIM_FIT -> " Show the garment as a slim fit — fitting closely to the body with a tailored, tight silhouette.";
            case REGULAR_FIT -> " Show the garment as a regular fit — standard comfortable silhouette, not too tight or too loose.";
            case OVERSIZED_FIT -> " Show the garment as an oversized fit — loose and relaxed, hanging away from the body.";
        };
        return base + fitDesc;
    }

    @Transactional
    public void saveTask(FittingTask task) {
        fittingRepository.save(task);
    }

    /**
     * 가상 피팅 결과 삭제 (닫기 시 호출). 본인 소유 task만 삭제 가능.
     * FittingTask는 즉시 삭제 후 반환. GCS/Clothes 정리는 비동기로 백그라운드 처리.
     * @return true if deleted, false if not found or not owner
     */
    @Transactional
    public boolean deleteTask(Long taskId, Long userId) {
        FittingTask task = fittingRepository.findById(taskId).orElse(null);
        if (task == null || !task.getUserId().equals(userId)) {
            return false;
        }
        Long topId = task.getTopId();
        Long bottomId = task.getBottomId();
        String bodyImgUrl = task.getBodyImgUrl();
        String resultImgUrl = task.getResultImgUrl();
        boolean taskWasSaved = task.isSaved();

        fittingRepository.delete(task);
        log.info("🗑️ FittingTask 삭제 완료 - taskId: {}, userId: {} (GCS/Clothes 정리는 비동기 처리)", taskId, userId);

        fittingCleanupService.cleanupAfterTaskDelete(bodyImgUrl, resultImgUrl, topId, bottomId, taskWasSaved);
        return true;
    }

    @Transactional(readOnly = true)
    public List<FittingTask> getSavedFittingList(Long userId) {
        return fittingRepository.findByUserIdAndIsSavedTrue(userId);
    }

    @Transactional(readOnly = true)
    public List<SavedFittingResponseDto> getSavedFittingListAsDtos(Long userId) {
        List<FittingTask> tasks = fittingRepository.findByUserIdAndIsSavedTrueWithClothes(userId);
        return tasks.stream()
                .map(SavedFittingResponseDto::from)
                .toList();
    }

}