package com.example.Capstone_project.service;

import com.example.Capstone_project.common.exception.BadRequestException;
import com.example.Capstone_project.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BitStudioService {
	
	private final WebClient bitStudioWebClient;
	
	@Value("${bitstudio.polling.max-attempts:150}")
	private int maxPollingAttempts; // 최대 5분 (2초 * 150)
	
	@Value("${bitstudio.polling.interval-seconds:2}")
	private int pollingIntervalSeconds;
	
	/**
	 * 이미지를 BitStudio API에 업로드
	 * @param file 업로드할 이미지 파일
	 * @param type 이미지 타입 (virtual-try-on-person, virtual-try-on-outfit)
	 * @return 업로드된 이미지 정보
	 */
	public BitStudioImageResponse uploadImage(MultipartFile file, String type) {
		try {
			byte[] fileBytes = file.getBytes();
			ByteArrayResource resource = new ByteArrayResource(fileBytes) {
				@Override
				public String getFilename() {
					return file.getOriginalFilename();
				}
			};
			
			MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
			parts.add("file", resource);
			parts.add("type", type);
			
			return bitStudioWebClient.post()
				.uri("/images")
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.body(BodyInserters.fromMultipartData(parts))
				.retrieve()
				.bodyToMono(BitStudioImageResponse.class)
				.block();
				
		} catch (WebClientResponseException e) {
			log.error("Failed to upload image to BitStudio: {}", e.getResponseBodyAsString(), e);
			throw new BadRequestException("Failed to upload image: " + e.getMessage());
		} catch (IOException e) {
			log.error("Failed to read file", e);
			throw new BadRequestException("Failed to read file: " + e.getMessage());
		} catch (Exception e) {
			log.error("Unexpected error while uploading image", e);
			throw new BadRequestException("Failed to upload image: " + e.getMessage());
		}
	}
	
	/**
	 * Virtual Try-On 요청 생성
	 * @param personImageId Person 이미지 ID
	 * @param outfitImageId Outfit 이미지 ID
	 * @param prompt 선택적 프롬프트
	 * @param resolution 해상도 (standard or high)
	 * @param numImages 생성할 이미지 수 (1-4)
	 * @return Virtual Try-On 작업 정보
	 */
	public BitStudioVirtualTryOnResponse createVirtualTryOn(
		String personImageId,
		String outfitImageId,
		String prompt,
		String resolution,
		Integer numImages
	) {
		BitStudioVirtualTryOnRequest request = BitStudioVirtualTryOnRequest.builder()
			.personImageId(personImageId)
			.outfitImageId(outfitImageId)
			.prompt(prompt)
			.resolution(resolution)
			.numImages(numImages)
			.build();
		
		try {
			List<BitStudioVirtualTryOnResponse> responses = bitStudioWebClient.post()
				.uri("/images/virtual-try-on")
				.bodyValue(request)
				.retrieve()
				.bodyToFlux(BitStudioVirtualTryOnResponse.class)
				.collectList()
				.block();
			
			if (responses == null || responses.isEmpty()) {
				throw new BadRequestException("No response from BitStudio API");
			}
			
			return responses.get(0); // 첫 번째 결과 반환
			
		} catch (WebClientResponseException e) {
			log.error("Failed to create virtual try-on: {}", e.getResponseBodyAsString(), e);
			throw new BadRequestException("Failed to create virtual try-on: " + e.getMessage());
		} catch (Exception e) {
			log.error("Unexpected error while creating virtual try-on", e);
			throw new BadRequestException("Failed to create virtual try-on: " + e.getMessage());
		}
	}
	
	/**
	 * 이미지 상태 확인 (Polling)
	 * @param imageId 이미지 ID
	 * @return 이미지 정보 (완료 시 path 포함)
	 */
	public BitStudioImageResponse getImageStatus(String imageId) {
		try {
			return bitStudioWebClient.get()
				.uri("/images/{id}", imageId)
				.retrieve()
				.bodyToMono(BitStudioImageResponse.class)
				.block();
				
		} catch (WebClientResponseException e) {
			log.error("Failed to get image status: {}", e.getResponseBodyAsString(), e);
			throw new BadRequestException("Failed to get image status: " + e.getMessage());
		} catch (Exception e) {
			log.error("Unexpected error while getting image status", e);
			throw new BadRequestException("Failed to get image status: " + e.getMessage());
		}
	}
	
	/**
	 * Virtual Try-On 작업 완료까지 대기 (Polling)
	 * @param imageId 이미지 ID
	 * @return 완료된 이미지 정보
	 */
	public BitStudioImageResponse waitForCompletion(String imageId) {
		int attempts = 0;
		
		while (attempts < maxPollingAttempts) {
			BitStudioImageResponse imageResponse = getImageStatus(imageId);
			
			if ("completed".equals(imageResponse.getStatus())) {
				return imageResponse;
			} else if ("failed".equals(imageResponse.getStatus())) {
				throw new BadRequestException("Image generation failed");
			}
			
			attempts++;
			try {
				Thread.sleep(pollingIntervalSeconds * 1000L);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new BadRequestException("Polling interrupted");
			}
		}
		
		throw new BadRequestException("Image generation timed out after " + 
			(maxPollingAttempts * pollingIntervalSeconds) + " seconds");
	}
	
	/**
	 * 전체 Virtual Try-On 프로세스 실행
	 * 1. Person 이미지 업로드
	 * 2. Outfit 이미지 업로드
	 * 3. Virtual Try-On 요청
	 * 4. 완료까지 대기
	 * @param personImage Person 이미지 파일
	 * @param outfitImage Outfit 이미지 파일
	 * @param prompt 선택적 프롬프트
	 * @param resolution 해상도
	 * @param numImages 생성할 이미지 수
	 * @return 완료된 Virtual Try-On 결과
	 */
	public VirtualTryOnResponse processVirtualTryOn(
		MultipartFile personImage,
		MultipartFile outfitImage,
		String prompt,
		String resolution,
		Integer numImages
	) {
		// 1. Person 이미지 업로드
		log.info("Uploading person image...");
		BitStudioImageResponse personImageResponse = uploadImage(personImage, "virtual-try-on-person");
		log.info("Person image uploaded: {}", personImageResponse.getId());
		
		// 2. Outfit 이미지 업로드
		log.info("Uploading outfit image...");
		BitStudioImageResponse outfitImageResponse = uploadImage(outfitImage, "virtual-try-on-outfit");
		log.info("Outfit image uploaded: {}", outfitImageResponse.getId());
		
		// 3. Virtual Try-On 요청 생성
		log.info("Creating virtual try-on request...");
		BitStudioVirtualTryOnResponse tryOnResponse = createVirtualTryOn(
			personImageResponse.getId(),
			outfitImageResponse.getId(),
			prompt,
			resolution,
			numImages
		);
		log.info("Virtual try-on request created: {}", tryOnResponse.getId());
		
		// 4. 완료까지 대기
		log.info("Waiting for completion...");
		BitStudioImageResponse completedImage = waitForCompletion(tryOnResponse.getId());
		log.info("Virtual try-on completed: {}", completedImage.getPath());
		
		return VirtualTryOnResponse.builder()
			.imageId(completedImage.getId())
			.status(completedImage.getStatus())
			.imageUrl(completedImage.getPath())
			.creditsUsed(tryOnResponse.getCreditsUsed())
			.sourceImageIds(List.of(personImageResponse.getId(), outfitImageResponse.getId()))
			.build();
	}
}

