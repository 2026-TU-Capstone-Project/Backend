package com.example.Capstone_project.service;

import com.example.Capstone_project.repository.ClothesRepository;
import com.example.Capstone_project.repository.FittingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FittingCleanupService {

    private final ClothesRepository clothesRepository;
    private final FittingRepository fittingRepository;
    private final GoogleCloudStorageService gcsService;

    /**
     * FittingTask 삭제 후 GCS 이미지 및 Clothes 정리 (비동기).
     * 즉시 반환되며 백그라운드에서 실행됩니다.
     */
    @Async("taskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cleanupAfterTaskDelete(String bodyImgUrl, String resultImgUrl, Long topId, Long bottomId, boolean taskWasSaved) {
        log.info("[Async] FittingTask resource cleanup started");
        try {
            if (!taskWasSaved) {
                boolean hasVirtualFittingOnlyClothes = isVirtualFittingOnlyClothes(topId) || isVirtualFittingOnlyClothes(bottomId);
                if (hasVirtualFittingOnlyClothes) {
                    deleteGcsImageIfPresent(bodyImgUrl, "bodyImg");
                    deleteGcsImageIfPresent(resultImgUrl, "resultImg");
                    deleteClothesIfVirtualFittingOnly(topId);
                    deleteClothesIfVirtualFittingOnly(bottomId);
                } else {
                    log.info("Skip GCS/body/result + Clothes cleanup because related clothes are not virtual-fitting-only");
                }
            } else {
                log.info("Skip virtual-fitting Clothes cleanup because taskWasSaved=true");
            }
            log.info("[Async] FittingTask resource cleanup completed");
        } catch (Exception e) {
            log.error("[Async] FittingTask resource cleanup error", e);
        }
    }

    private boolean isVirtualFittingOnlyClothes(Long clothesId) {
        if (clothesId == null) return false;
        return clothesRepository.findById(clothesId)
                .map(clothes -> !clothes.isInCloset())
                .orElse(false);
    }

    private void deleteGcsImageIfPresent(String url, String label) {
        if (url == null || url.isBlank() || !url.contains("storage.googleapis.com")) return;
        try {
            String blobName = gcsService.extractBlobNameFromUrl(url);
            gcsService.deleteImage(blobName);
            log.info("GCS {} image deleted: {}", label, blobName);
        } catch (Exception e) {
            log.warn("GCS {} 이미지 삭제 스킵: {}", label, e.getMessage());
        }
    }

    private void deleteClothesIfVirtualFittingOnly(Long clothesId) {
        if (clothesId == null) return;

        // 다른 FittingTask가 참조 중이면 삭제 금지 (saved=true 포함)
        if (fittingRepository.existsByTopId(clothesId) || fittingRepository.existsByBottomId(clothesId)) {
            log.info("Skip deleting clothesId={} because it is still referenced by fitting_tasks", clothesId);
            return;
        }

        clothesRepository.findById(clothesId).ifPresent(clothes -> {
            if (!clothes.isInCloset()) {
                try {
                    String blobName = gcsService.extractBlobNameFromUrl(clothes.getImgUrl());
                    gcsService.deleteImage(blobName);
                } catch (Exception e) {
                    log.warn("GCS 이미지 삭제 스킵 (clothesId={}): {}", clothesId, e.getMessage());
                }
                clothesRepository.delete(clothes);
                log.info("🗑️ 가상피팅용 Clothes 삭제 완료 - clothesId: {}", clothesId);
            }
        });
    }
}
