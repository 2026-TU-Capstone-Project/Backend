package com.example.Capstone_project.service;

import com.example.Capstone_project.common.exception.BadRequestException;
import com.example.Capstone_project.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import java.util.Base64;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.imageio.ImageIO;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiService {
	
	private final WebClient geminiWebClient;
	
	@Value("${gemini.api.model:gemini-3-pro-image-preview}")
	private String model;
	
	@Value("${gemini.prompt.positive:Put the provided top and bottom garments on the person in the full-body photo.}")
	private String defaultPositivePrompt;
	
	@Value("${gemini.resolution:1K}")
	private String defaultResolution;
	
	@Value("${gemini.aspect-ratio:3:4}")
	private String defaultAspectRatio;
	
	@Value("${virtual-fitting.image.storage-path:./images/virtual-fitting}")
	private String imageStoragePath;
	
	@Value("${virtual-fitting.image.url-path:/api/v1/virtual-fitting/images}")
	private String imageUrlPath;
	
	@Value("${virtual-fitting.image.max-width:1024}")
	private int maxImageWidth;
	
	@Value("${virtual-fitting.image.max-height:1024}")
	private int maxImageHeight;
	
	/**
	 * 이미지 리사이징 (성능 최적화)
	 * 큰 이미지를 적절한 크기로 리사이징하여 전송 시간과 처리 시간 단축
	 * @return 리사이징된 이미지 바이트 배열 (항상 JPEG 형식)
	 */
	private byte[] resizeImageIfNeeded(MultipartFile file) throws IOException {
		InputStream inputStream = file.getInputStream();
		BufferedImage originalImage = ImageIO.read(inputStream);
		
		if (originalImage == null) {
			log.warn("Failed to read image, using original file");
			return file.getBytes();
		}
		
		int originalWidth = originalImage.getWidth();
		int originalHeight = originalImage.getHeight();
		
		// 리사이징이 필요한지 확인
		if (originalWidth <= maxImageWidth && originalHeight <= maxImageHeight) {
			log.debug("Image size {}x{} is within limits, skipping resize", originalWidth, originalHeight);
			return file.getBytes();
		}
		
		// 비율 유지하며 리사이징
		double widthRatio = (double) maxImageWidth / originalWidth;
		double heightRatio = (double) maxImageHeight / originalHeight;
		double ratio = Math.min(widthRatio, heightRatio);
		
		int newWidth = (int) (originalWidth * ratio);
		int newHeight = (int) (originalHeight * ratio);
		
		log.info("Resizing image from {}x{} to {}x{} (ratio: {:.2f})", 
			originalWidth, originalHeight, newWidth, newHeight, ratio);
		
		BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = resizedImage.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
		g.dispose();
		
		// JPEG로 변환 (PNG는 더 큰 파일 크기)
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(resizedImage, "jpg", baos);
		
		int originalSize = file.getBytes().length;
		int resizedSize = baos.size();
		log.info("Image resized: {} bytes -> {} bytes (saved {}%)", 
			originalSize, resizedSize, (originalSize - resizedSize) * 100 / originalSize);
		
		return baos.toByteArray();
	}
	
	/**
	 * Base64 이미지 데이터를 파일로 저장하고 URL 반환
	 */
	private String saveBase64ImageToFile(String imageBase64, String mimeType) throws IOException {
		// 저장 디렉토리 생성
		Path storageDir = Paths.get(imageStoragePath);
		if (!Files.exists(storageDir)) {
			Files.createDirectories(storageDir);
			log.info("Created image storage directory: {}", storageDir.toAbsolutePath());
		}
		
		// 파일 확장자 결정
		String extension = "jpg";
		if (mimeType != null) {
			if (mimeType.contains("png")) {
				extension = "png";
			} else if (mimeType.contains("jpeg") || mimeType.contains("jpg")) {
				extension = "jpg";
			}
		}
		
		// 고유한 파일명 생성
		String filename = UUID.randomUUID().toString() + "." + extension;
		Path filePath = storageDir.resolve(filename);
		
		// Base64 디코딩 및 파일 저장
		byte[] imageBytes = Base64.getDecoder().decode(imageBase64);
		try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
			fos.write(imageBytes);
		}
		
		log.info("Image saved to: {} ({} bytes)", filePath.toAbsolutePath(), imageBytes.length);
		
		// URL 경로 반환 (파일명만 포함)
		return imageUrlPath + "/" + filename;
	}
	
	/**
	 * Gemini API를 사용하여 Virtual Fitting 이미지 생성
	 * user_image, top_image, bottom_image를 입력받아 합성 이미지 생성
	 * 
	 * @param userImage User 이미지 파일
	 * @param topImage Top 이미지 파일
	 * @param bottomImage Bottom 이미지 파일
	 * @param positivePrompt 선택적 긍정적 프롬프트
	 * @param negativePrompt 제외 프롬프트 (Gemini API는 지원하지 않음, 무시됨)
	 * @param resolution 선택적 해상도 (1K, 2K, 4K) - Pro 모델만 지원
	 * @return 생성된 이미지 정보
	 */
	public VirtualFittingResponse processVirtualFitting(
		MultipartFile userImage,
		MultipartFile topImage,
		MultipartFile bottomImage,
		String positivePrompt,
		String negativePrompt,
		String resolution
	) {
		long startTime = System.currentTimeMillis();
		
		try {
			// 프롬프트와 해상도 설정
			String finalPrompt = (positivePrompt != null && !positivePrompt.trim().isEmpty()) 
				? positivePrompt : defaultPositivePrompt;
			String finalResolution = (resolution != null && !resolution.trim().isEmpty()) 
				? resolution : defaultResolution;
			
			// 이미지 리사이징 및 Base64 인코딩
			long imageProcessingStart = System.currentTimeMillis();
			log.info("Processing and encoding images...");
			
			byte[] userImageBytes = resizeImageIfNeeded(userImage);
			byte[] topImageBytes = resizeImageIfNeeded(topImage);
			byte[] bottomImageBytes = resizeImageIfNeeded(bottomImage);
			
			String userImageBase64 = Base64.getEncoder().encodeToString(userImageBytes);
			String topImageBase64 = Base64.getEncoder().encodeToString(topImageBytes);
			String bottomImageBase64 = Base64.getEncoder().encodeToString(bottomImageBytes);
			
			long imageProcessingTime = System.currentTimeMillis() - imageProcessingStart;
			log.info("Image processing completed in {} ms", imageProcessingTime);
			
			// 리사이징된 이미지는 항상 JPEG 형식이므로 MIME 타입을 "image/jpeg"로 설정
			String userMimeType = "image/jpeg";
			String topMimeType = "image/jpeg";
			String bottomMimeType = "image/jpeg";
			
			// Gemini API 요청 생성
			// 여러 이미지를 parts에 추가하여 전송
			List<GeminiGenerateContentRequest.Part> parts = new ArrayList<>();
			
			// User 이미지 추가
			parts.add(GeminiGenerateContentRequest.Part.builder()
				.inlineData(GeminiGenerateContentRequest.InlineData.builder()
					.mimeType(userMimeType)
					.data(userImageBase64)
					.build())
				.build());
			
			// Top 이미지 추가
			parts.add(GeminiGenerateContentRequest.Part.builder()
				.inlineData(GeminiGenerateContentRequest.InlineData.builder()
					.mimeType(topMimeType)
					.data(topImageBase64)
					.build())
				.build());
			
			// Bottom 이미지 추가
			parts.add(GeminiGenerateContentRequest.Part.builder()
				.inlineData(GeminiGenerateContentRequest.InlineData.builder()
					.mimeType(bottomMimeType)
					.data(bottomImageBase64)
					.build())
				.build());
			
			// 프롬프트 추가
			parts.add(GeminiGenerateContentRequest.Part.builder()
				.text(finalPrompt)
				.build());
			
			// GenerationConfig 설정 (Pro 모델은 imageSize 지원)
			GeminiGenerateContentRequest.ImageConfig imageConfig = 
				GeminiGenerateContentRequest.ImageConfig.builder()
					.aspectRatio(defaultAspectRatio)
					.imageSize(finalResolution) // 1K, 2K, 4K
					.build();
			
			GeminiGenerateContentRequest.GenerationConfig generationConfig = 
				GeminiGenerateContentRequest.GenerationConfig.builder()
					.imageConfig(imageConfig)
					.build();
			
			// Content 생성
			GeminiGenerateContentRequest.Content content = 
				GeminiGenerateContentRequest.Content.builder()
					.parts(parts)
					.build();
			
			List<GeminiGenerateContentRequest.Content> contents = new ArrayList<>();
			contents.add(content);
			
			// 최종 요청 객체 생성
			GeminiGenerateContentRequest request = GeminiGenerateContentRequest.builder()
				.contents(contents)
				.generationConfig(generationConfig)
				.build();
			
			log.info("Sending request to Gemini API - model: {}, resolution: {}, aspectRatio: {}", 
				model, finalResolution, defaultAspectRatio);
			
			// Gemini API 호출
			long apiCallStart = System.currentTimeMillis();
			String endpoint = "/models/" + model + ":generateContent";
			
			// 요청 로깅 (디버깅용)
			log.debug("API Endpoint: {}", endpoint);
			log.debug("Request parts count: {}", parts.size());
			
			GeminiGenerateContentResponse response = geminiWebClient.post()
				.uri(endpoint)
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(request)
				.retrieve()
				.bodyToMono(GeminiGenerateContentResponse.class)
				.block();
			
			long apiCallTime = System.currentTimeMillis() - apiCallStart;
			log.info("Gemini API call completed in {} ms ({} seconds)", apiCallTime, apiCallTime / 1000.0);
			
			if (response == null || response.getCandidates() == null || response.getCandidates().isEmpty()) {
				log.error("Gemini API returned null or empty response");
				throw new BadRequestException("No response from Gemini API");
			}
			
			// 응답에서 이미지 추출
			GeminiGenerateContentResponse.Candidate candidate = response.getCandidates().get(0);
			if (candidate.getContent() == null || candidate.getContent().getParts() == null) {
				log.error("Gemini API response has no content or parts");
				throw new BadRequestException("Invalid response from Gemini API");
			}
			
			// parts에서 이미지 데이터 찾기
			String imageBase64 = null;
			String mimeType = null;
			int totalParts = candidate.getContent().getParts().size();
			log.debug("Total parts in response: {}", totalParts);
			
			for (GeminiGenerateContentResponse.Part part : candidate.getContent().getParts()) {
				if (part.getInlineData() != null) {
					imageBase64 = part.getInlineData().getData();
					mimeType = part.getInlineData().getMimeType();
					
					// Base64 데이터 길이 로깅 (디버깅용)
					int base64Length = imageBase64 != null ? imageBase64.length() : 0;
					log.info("Found image data - mimeType: {}, base64Length: {} characters (approx. {} KB)", 
						mimeType, base64Length, base64Length / 1024);
					
					// Base64 데이터 시작 부분 확인 (JPEG는 /9j/4AA로 시작)
					if (imageBase64 != null && imageBase64.length() > 10) {
						String prefix = imageBase64.substring(0, Math.min(10, imageBase64.length()));
						log.debug("Base64 data prefix: {}", prefix);
					}
					
					break;
				} else if (part.getText() != null) {
					log.debug("Found text part (ignoring): {}", part.getText().substring(0, Math.min(100, part.getText().length())));
				}
			}
			
			if (imageBase64 == null) {
				log.error("No image data found in Gemini API response");
				throw new BadRequestException("No image data in Gemini API response");
			}
			
			log.info("Virtual fitting completed successfully - mimeType: {}", mimeType);
			
			// Base64 이미지를 파일로 저장하고 URL 생성
			long saveStart = System.currentTimeMillis();
			String imageUrl = saveBase64ImageToFile(imageBase64, mimeType);
			String imageId = "gemini-" + System.currentTimeMillis();
			long saveTime = System.currentTimeMillis() - saveStart;
			
			long totalTime = System.currentTimeMillis() - startTime;
			log.info("Image saved successfully - URL: {}, imageId: {} (save time: {} ms)", imageUrl, imageId, saveTime);
			log.info("=== Total processing time: {} ms ({} seconds) ===", totalTime, totalTime / 1000.0);
			
			return VirtualFittingResponse.builder()
				.imageId(imageId)
				.status("completed")
				.imageUrl(imageUrl) // 파일 저장 후 URL 반환
				.imageBase64(null) // Base64는 더 이상 제공하지 않음 (URL만 제공)
				.creditsUsed(null) // Gemini API는 크레딧 정보를 응답에 포함하지 않음
				.build();
			
		} catch (WebClientResponseException e) {
			String responseBody = e.getResponseBodyAsString();
			log.error("Failed to generate image with Gemini API - Status: {}, Response: {}", 
				e.getStatusCode(), responseBody, e);
			
			// 상세한 에러 정보 로깅
			log.error("Request details - Model: {}, Endpoint: {}", model, "/models/" + model + ":generateContent");
			
			// 429 Too Many Requests 에러 처리
			if (e.getStatusCode().value() == 429) {
				String errorMessage = "Gemini API 호출 한도가 초과되었습니다. " +
					"Rate Limit (호출 빈도 제한) 또는 Quota (할당량)를 초과했을 수 있습니다. " +
					"잠시 후 다시 시도하거나, Google Cloud Console에서 할당량을 확인해주세요.";
				
				if (e.getResponseBodyAsString() != null && !e.getResponseBodyAsString().isEmpty()) {
					log.error("Gemini API 429 Error Response: {}", e.getResponseBodyAsString());
				}
				
				throw new BadRequestException(errorMessage);
			}
			
			// 기타 HTTP 에러 처리
			String errorMessage = "Failed to generate image";
			
			if (responseBody != null && !responseBody.trim().isEmpty()) {
				log.error("Gemini API error response: {}", responseBody);
				errorMessage += ": " + responseBody;
			} else {
				errorMessage += ": HTTP " + e.getStatusCode() + " - " + e.getMessage();
			}
			
			throw new BadRequestException(errorMessage);
		} catch (IOException e) {
			log.error("Failed to read image file", e);
			throw new BadRequestException("Failed to read image file: " + e.getMessage());
		} catch (org.springframework.web.reactive.function.client.WebClientException e) {
			String errorMessage = e.getMessage();
			log.error("Network error while calling Gemini API: {}", errorMessage, e);
			
			if (errorMessage != null && errorMessage.contains("Failed to resolve")) {
				throw new BadRequestException(
					"Gemini API 엔드포인트를 찾을 수 없습니다. " +
					"DNS 해석 실패 - .env 파일에 GEMINI_API_KEY를 설정해주세요. " +
					"오류: " + errorMessage);
			}
			throw new BadRequestException("Network error: " + errorMessage);
		} catch (Exception e) {
			log.error("Unexpected error while generating image: {}", e.getMessage(), e);
			throw new BadRequestException("Failed to generate image: " + e.getMessage());
		}
	}
}
