package com.example.Capstone_project.service;

import com.example.Capstone_project.dto.VirtualFittingResponse;
import com.example.Capstone_project.domain.FittingStatus; // ✅ domain으로 정확히 수정
import com.example.Capstone_project.domain.FittingTask;   // ✅ domain으로 정확히 수정
import com.example.Capstone_project.repository.FittingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FittingService {

    private final GeminiService geminiService;
    private final FittingRepository fittingRepository;

    @Transactional
    public FittingTask createFittingTask() {
        // ✅ FittingTask 생성자 구조에 맞게 수정
        FittingTask task = new FittingTask(FittingStatus.WAITING);
        return fittingRepository.save(task);
    }

    @Async("taskExecutor")
    @Transactional
    public void processFitting(Long taskId, byte[] userImgData, byte[] topImgData, byte[] bottomImgData) {
        FittingTask task = fittingRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found: " + taskId));

        try {
            log.info("🚀 [비동기 시작] 데이터 복사본(byte[])으로 안전하게 작업을 시작합니다. ID: {}", taskId);


            VirtualFittingResponse response = geminiService.processVirtualFitting(
                    userImgData,
                    topImgData,
                    bottomImgData,
                    null, null, null
            );

            if (response != null && "completed".equals(response.getStatus())) {
                task.setStatus(FittingStatus.COMPLETED);
                task.setResultImgUrl(response.getImageUrl());
                log.info("✅ [작업 완료] URL: {}", response.getImageUrl());
            } else {
                task.setStatus(FittingStatus.FAILED);
            }
        } catch (Exception e) {
            log.error("❌ [비동기 에러] : {}", e.getMessage());
            task.setStatus(FittingStatus.FAILED);
        }
        fittingRepository.save(task);
    }

    @Transactional(readOnly = true)
    public FittingTask checkStatus(Long id) {
        return fittingRepository.findById(id).orElse(null);
    }
}