package com.example.Capstone_project.service;

import com.example.Capstone_project.domain.FittingStatus;
import com.example.Capstone_project.dto.VirtualFittingStatusResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 가상 피팅 작업별 SSE 구독 관리.
 * task당 1연결만 유지, 타임아웃 1분.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VirtualFittingSseService {

    private static final long SSE_TIMEOUT_MS = 60_000L; // 1분

    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * taskId에 대한 구독 등록. 이미 있으면 기존 연결 완료 후 교체 (1연결 유지).
     */
    public SseEmitter register(Long taskId) {
        SseEmitter existing = emitters.remove(taskId);
        if (existing != null) {
            try {
                existing.complete();
            } catch (Exception ignored) { }
        }

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        emitters.put(taskId, emitter);

        emitter.onCompletion(() -> {
            emitters.remove(taskId, emitter);
            log.debug("SSE completed for taskId={}", taskId);
        });
        emitter.onTimeout(() -> {
            emitters.remove(taskId, emitter);
            log.debug("SSE timeout for taskId={}", taskId);
        });
        emitter.onError(e -> {
            emitters.remove(taskId, emitter);
            log.warn("SSE error for taskId={}: {}", taskId, e.getMessage());
        });

        return emitter;
    }

    /**
     * 해당 task 구독자에게 상태 이벤트 전송.
     * COMPLETED/FAILED면 전송 후 연결 완료 및 제거.
     */
    public void notifyStatus(Long taskId, VirtualFittingStatusResponse response) {
        SseEmitter emitter = emitters.get(taskId);
        if (emitter == null) return;

        try {
            String json = objectMapper.writeValueAsString(response);
            emitter.send(SseEmitter.event().name("status").data(json));
        } catch (IOException e) {
            log.warn("SSE send failed for taskId={}: {}", taskId, e.getMessage());
            emitters.remove(taskId, emitter);
            try { emitter.completeWithError(e); } catch (Exception ignored) { }
            return;
        }

        if (response.getStatus() == FittingStatus.COMPLETED || response.getStatus() == FittingStatus.FAILED) {
            emitters.remove(taskId, emitter);
            try {
                emitter.complete();
            } catch (Exception ignored) { }
        }
    }

    /**
     * 이미 완료/실패한 task에 대해 현재 상태 1회 전송 후 완료 (스트림 종료).
     */
    public void sendOnceAndComplete(SseEmitter emitter, VirtualFittingStatusResponse response) {
        try {
            String json = objectMapper.writeValueAsString(response);
            emitter.send(SseEmitter.event().name("status").data(json));
        } catch (JsonProcessingException e) {
            log.warn("SSE JSON write failed: {}", e.getMessage());
        } catch (IOException e) {
            log.warn("SSE send failed: {}", e.getMessage());
        } finally {
            try {
                emitter.complete();
            } catch (Exception ignored) { }
        }
    }
}
