package com.example.Capstone_project.service;

import com.example.Capstone_project.domain.FittingTask;
import com.example.Capstone_project.repository.ClothesRepository;
import com.example.Capstone_project.repository.FeedRepository;
import com.example.Capstone_project.repository.FittingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Soft delete된 FittingTask의 GCS 이미지와 가상피팅용 Clothes를 주기적으로 정리합니다.
 * deleted_at이 1주일 이상 지난 task를 대상으로 실행됩니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FittingCleanupService {

    private final ClothesRepository clothesRepository;
    private final FittingRepository fittingRepository;
    private final FeedRepository feedRepository;
    private final GoogleCloudStorageService gcsService;

    @Scheduled(fixedDelay = 7 * 24 * 60 * 60 * 1000L) // 1주일마다 실행
    @Transactional
    public void cleanupSoftDeletedTasks() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(7);
        List<FittingTask> targets = fittingRepository.findAllSoftDeletedBefore(threshold);

        if (targets.isEmpty()) return;

        log.info("[Cleanup] soft delete된 FittingTask {} 건 정리 시작", targets.size());

        for (FittingTask task : targets) {
            try {
                deleteGcsImageIfPresent(task.getBodyImgUrl(), "bodyImg", task.getId());
                deleteGcsImageIfPresent(task.getResultImgUrl(), "resultImg", task.getId());
                deleteClothesIfVirtualFittingOnly(task.getTopId(), task.getId());
                deleteClothesIfVirtualFittingOnly(task.getBottomId(), task.getId());
                fittingRepository.delete(task);
                log.info("[Cleanup] FittingTask 물리 삭제 완료 - taskId: {}", task.getId());
            } catch (Exception e) {
                log.error("[Cleanup] FittingTask 정리 실패 - taskId: {}", task.getId(), e);
            }
        }

        log.info("[Cleanup] 정리 완료");
    }

    private void deleteGcsImageIfPresent(String url, String label, Long taskId) {
        if (url == null || url.isBlank() || !url.contains("storage.googleapis.com")) return;
        try {
            String blobName = gcsService.extractBlobNameFromUrl(url);
            gcsService.deleteImage(blobName);
            log.info("[Cleanup] GCS {} 삭제 완료 - taskId: {}, blob: {}", label, taskId, blobName);
        } catch (Exception e) {
            log.warn("[Cleanup] GCS {} 삭제 스킵 - taskId: {}: {}", label, taskId, e.getMessage());
        }
    }

    private void deleteClothesIfVirtualFittingOnly(Long clothesId, Long taskId) {
        if (clothesId == null) return;

        // 다른 활성 FittingTask가 같은 Clothes를 참조 중이면 삭제 금지
        if (fittingRepository.existsByTopId(clothesId) || fittingRepository.existsByBottomId(clothesId)) {
            log.info("[Cleanup] clothesId={} 는 다른 task가 참조 중이므로 삭제 스킵 (taskId: {})", clothesId, taskId);
            return;
        }

        // 피드가 이 Clothes를 topClothesId 또는 bottomClothesId로 참조 중이면 삭제 금지
        if (feedRepository.existsActiveFeedReferencingClothes(clothesId)) {
            log.info("[Cleanup] clothesId={} 는 피드가 참조 중이므로 삭제 스킵 (taskId: {})", clothesId, taskId);
            return;
        }

        clothesRepository.findByIdForUpdate(clothesId).ifPresent(clothes -> {
            if (!clothes.isInCloset()) {
                try {
                    String blobName = gcsService.extractBlobNameFromUrl(clothes.getImgUrl());
                    gcsService.deleteImage(blobName);
                } catch (Exception e) {
                    log.warn("[Cleanup] GCS 이미지 삭제 스킵 (clothesId={}): {}", clothesId, e.getMessage());
                }
                clothesRepository.delete(clothes);
                log.info("[Cleanup] 가상피팅용 Clothes 삭제 완료 - clothesId: {}", clothesId);
            }
        });
    }
}
