package com.example.Capstone_project.service;

import com.example.Capstone_project.domain.ClothesUploadStatus;
import com.example.Capstone_project.dto.ClothesUploadStatusResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 옷 업로드 작업별 SSE 구독 관리.
 * 가상 피팅과 동일: task당 1연결, 이벤트 name="status".
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClothesUploadSseService {

	private static final long SSE_TIMEOUT_MS = 60_000L; // 1분

	private final ObjectMapper objectMapper;
	private final ConcurrentHashMap<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

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
			log.debug("SSE completed for clothes upload taskId={}", taskId);
		});
		emitter.onTimeout(() -> {
			emitters.remove(taskId, emitter);
			log.debug("SSE timeout for clothes upload taskId={}", taskId);
		});
		emitter.onError(e -> {
			emitters.remove(taskId, emitter);
			log.warn("SSE error for clothes upload taskId={}: {}", taskId, e.getMessage());
		});

		return emitter;
	}

	public void notifyStatus(Long taskId, ClothesUploadStatusResponse response) {
		SseEmitter emitter = emitters.get(taskId);
		if (emitter == null) return;

		try {
			String json = objectMapper.writeValueAsString(response);
			emitter.send(SseEmitter.event().name("status").data(json));
		} catch (IOException e) {
			log.warn("SSE send failed for clothes upload taskId={}: {}", taskId, e.getMessage());
			emitters.remove(taskId, emitter);
			try {
				emitter.completeWithError(e);
			} catch (Exception ignored) { }
			return;
		}

		if (response.getStatus() == ClothesUploadStatus.COMPLETED || response.getStatus() == ClothesUploadStatus.FAILED) {
			emitters.remove(taskId, emitter);
			try {
				emitter.complete();
			} catch (Exception ignored) { }
		}
	}

	public void sendOnceAndComplete(SseEmitter emitter, ClothesUploadStatusResponse response) {
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
