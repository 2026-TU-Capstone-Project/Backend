package com.example.Capstone_project.service;

import com.example.Capstone_project.repository.ClothesRepository;
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
    private final GoogleCloudStorageService gcsService;

    /**
     * FittingTask 삭제 후 GCS 이미지 및 Clothes 정리 (비동기).
     * 즉시 반환되며 백그라운드에서 실행됩니다.
     */
    @Async("taskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cleanupAfterTaskDelete(String bodyImgUrl, String resultImgUrl, Long topId, Long bottomId) {
        log.info("🧹 [비동기] FittingTask 연관 리소스 정리 시작");
        try {
            deleteGcsImageIfPresent(bodyImgUrl, "bodyImg");
            deleteGcsImageIfPresent(resultImgUrl, "resultImg");
            deleteClothesIfVirtualFittingOnly(topId);
            deleteClothesIfVirtualFittingOnly(bottomId);
            log.info("🧹 [비동기] FittingTask 연관 리소스 정리 완료");
        } catch (Exception e) {
            log.error("❌ [비동기] FittingTask 연관 리소스 정리 중 오류", e);
        }
    }

    private void deleteGcsImageIfPresent(String url, String label) {
        if (url == null || url.isBlank() || !url.contains("storage.googleapis.com")) return;
        try {
            String blobName = gcsService.extractBlobNameFromUrl(url);
            gcsService.deleteImage(blobName);
            log.info("🗑️ GCS {} 이미지 삭제 완료: {}", label, blobName);
        } catch (Exception e) {
            log.warn("GCS {} 이미지 삭제 스킵: {}", label, e.getMessage());
        }
    }

    private void deleteClothesIfVirtualFittingOnly(Long clothesId) {
        if (clothesId == null) return;
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
