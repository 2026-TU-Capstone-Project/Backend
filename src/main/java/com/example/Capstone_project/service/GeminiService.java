package com.example.Capstone_project.service;

// 1. 프로젝트 파일들
import com.example.Capstone_project.common.exception.BadRequestException;
import com.example.Capstone_project.dto.*; // VirtualFittingResponse 등 모든 DTO 포함

// 2. Lombok & JSON 처리 (
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

// 3. Spring Framework
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

// 4. Java 자료구조
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// 5. Java 이미지 처리 & 파일 입출력
import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiService {
	

	private final WebClient geminiWebClient;
	private final GoogleCloudStorageService gcsService;
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Value("${gemini.api.key}")
	private String geminiApiKey;

	@Value("${gemini.api.base-url:https://generativelanguage.googleapis.com/v1beta}")
	private String geminiBaseUrl;

	@Value("${gemini.api.model:gemini-3-pro-image-preview}")
	private String model;
	
	@Value("${gemini.api.analysis-model}")
	private String analysisModel;
	
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
	 * Base64 이미지 데이터를 GCS에 저장하고 공개 URL 반환
	 */
	private String saveBase64ImageToFile(String imageBase64, String mimeType) throws IOException {
		// GCS에 업로드 (MIME 타입이 없으면 기본값으로 image/jpeg 사용)
		String contentType = mimeType != null ? mimeType : "image/jpeg";
		String gcsUrl = gcsService.uploadBase64Image(imageBase64, contentType);
		
		log.info("✅ 이미지 GCS 업로드 완료 - URL: {}", gcsUrl);
		
		// GCS 공개 URL 반환
		return gcsUrl;
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
	public VirtualFittingResponse processVirtualFitting(byte[] userImageBytes, byte[] topImageBytes, byte[] bottomImageBytes, String positivePrompt, String negativePrompt, String resolution) {
		try {
			// 1. 이미지 리사이징 (이미 구현된 메서드 활용)
			byte[] resUser = resizeImageIfNeeded(userImageBytes);
			byte[] resTop = resizeImageIfNeeded(topImageBytes);
			byte[] resBottom = resizeImageIfNeeded(bottomImageBytes);

			// 2. 형(동료)이 아래쪽에 짜놓은 진짜 요청 본문 생성기 호출! (중요)
			// 형님 파일 아래쪽에 있는 'createGeminiRequestBody' 메서드를 그대로 씁니다.
			Map<String, Object> requestBody = createGeminiRequestBody(
					Base64.getEncoder().encodeToString(resUser),
					Base64.getEncoder().encodeToString(resTop),
					Base64.getEncoder().encodeToString(resBottom),
					positivePrompt, negativePrompt
			);

			log.info("🚀 비동기 AI 요청 시작...");

// ✅ 436행: 주소 설정
			String endpoint = "/models/" + model + ":generateContent";

			log.info("📡 구글 AI에게 사진을 전달했습니다. 응답 대기 중... (최대 60초)");

			String responseString = geminiWebClient.post()
					.uri(uriBuilder -> uriBuilder
							.path(endpoint)
							.queryParam("key", geminiApiKey)
							.build())
					.contentType(MediaType.APPLICATION_JSON)
					.bodyValue(requestBody)
					.retrieve()
					.bodyToMono(String.class)
					.timeout(java.time.Duration.ofSeconds(180))
					.block();

			// ✅ 2. 이 로그가 찍히는지 보는 게 핵심입니다!
			log.info("📥 구글로부터 응답을 받았습니다! 데이터 해석을 시작합니다.");

			// ✅ 448행: 응답을 객체로 변환
			GeminiGenerateContentResponse responseObj = objectMapper.readValue(responseString, GeminiGenerateContentResponse.class);

			// ✅ 450행: 형(동료)의 진짜 이미지 추출 로직 이식 (여기서부터 중요!)
			GeminiGenerateContentResponse.Candidate candidate = responseObj.getCandidates().get(0);
			String imageBase64 = null;
			String mimeType = null;

			for (GeminiGenerateContentResponse.Part part : candidate.getContent().getParts()) {
				if (part.getInlineData() != null) {
					imageBase64 = part.getInlineData().getData();
					mimeType = part.getInlineData().getMimeType();
					break;
				}
			}

			if (imageBase64 == null) throw new BadRequestException("No image data in Gemini API response");

			// ✅ 형(동료)이 115행에 만든 진짜 파일 저장 메서드 호출!
			String imageUrl = saveBase64ImageToFile(imageBase64, mimeType);
			String imageId = "gemini-" + System.currentTimeMillis();

			log.info("💾 비동기 가상피팅 성공! 이미지 저장 경로: {}", imageUrl);

			// ✅ 최종 결과 반환 (이 값이 FittingService를 통해 DB에 저장됩니다)
			return VirtualFittingResponse.builder()
					.imageId(imageId)
					.status("completed")
					.imageUrl(imageUrl)
					.build();

		} catch (Exception e) {
			log.error("💥 가상피팅 엔진 오류: {}", e.getMessage());
			throw new RuntimeException("AI 처리 실패: " + e.getMessage());
		}
	}

	// =================================================================================
	// [유틸 메서드] 리사이징 로직 (MultipartFile -> byte[] 로 변경됨)
	// =================================================================================
	private byte[] resizeImageIfNeeded(byte[] imageData) throws IOException {
		// byte[]를 읽어서 이미지로 변환
		InputStream inputStream = new ByteArrayInputStream(imageData);
		BufferedImage originalImage = ImageIO.read(inputStream);

		if (originalImage == null) return imageData;

		int originalWidth = originalImage.getWidth();
		int originalHeight = originalImage.getHeight();

		// 형이 설정한 최대 크기 (1024)
		int maxDimension = 1024;

		if (originalWidth <= maxDimension && originalHeight <= maxDimension) {
			return imageData; // 작으면 그냥 리턴
		}

		// 비율 계산
		double ratio = Math.min((double) maxDimension / originalWidth, (double) maxDimension / originalHeight);
		int newWidth = (int) (originalWidth * ratio);
		int newHeight = (int) (originalHeight * ratio);

		// 리사이징 실행
		Image resultingImage = originalImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
		BufferedImage outputImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
		outputImage.getGraphics().drawImage(resultingImage, 0, 0, null);

		// 다시 byte[]로 변환
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(outputImage, "jpg", baos);
		return baos.toByteArray();
	}
	// ✅ 492행 쯤에 이 덩어리를 통째로 넣으세요!
	private Map<String, Object> createGeminiRequestBody(String userImg, String topImg, String bottomImg, String pPrompt, String nPrompt) {
		Map<String, Object> request = new HashMap<>();
		List<Map<String, Object>> contents = new ArrayList<>();
		Map<String, Object> content = new HashMap<>();
		List<Map<String, Object>> parts = new ArrayList<>();

		// 1. 이미지 데이터 3장 추가
		addInlineData(parts, userImg);
		addInlineData(parts, topImg);
		addInlineData(parts, bottomImg);

		// 2. 프롬프트 추가 (이게 빠져서 400 에러가 났던 겁니다!)
		Map<String, Object> textPart = new HashMap<>();
		textPart.put("text", pPrompt != null ? pPrompt : defaultPositivePrompt);
		parts.add(textPart);

		// 3. 조립
		content.put("parts", parts);
		contents.add(content);
		request.put("contents", contents);

		// 4. Generation Config 추가 (Pro 모델용 해상도 설정)
		Map<String, Object> generationConfig = new HashMap<>();
		Map<String, Object> imageConfig = new HashMap<>();
		imageConfig.put("image_size", defaultResolution);
		imageConfig.put("aspect_ratio", defaultAspectRatio);
		generationConfig.put("image_config", imageConfig);
		request.put("generationConfig", generationConfig);

		return request;
	}

	private void addInlineData(List<Map<String, Object>> parts, String base64Data) {
		Map<String, Object> part = new HashMap<>();
		Map<String, Object> inlineData = new HashMap<>();
		inlineData.put("mime_type", "image/jpeg");
		inlineData.put("data", base64Data);
		part.put("inline_data", inlineData);
		parts.add(part);
	}

	/**
	 * Gemini API를 사용하여 이미지 스타일 분석
	 * 이미지와 프롬프트를 전송하여 텍스트 분석 결과를 받아옴
	 * 
	 * @param imageBytes 분석할 이미지 바이트 배열
	 * @param prompt 분석 프롬프트
	 * @return 분석 결과 텍스트 (한글)
	 */
	public String analyzeImageStyle(byte[] imageBytes, String prompt) {
		try {
			log.info("🎨 Gemini API로 이미지 스타일 분석 시작 - 모델: {}", analysisModel);
			
			// 이미지를 Base64로 인코딩
			String imageBase64 = Base64.getEncoder().encodeToString(imageBytes);
			String mimeType = "image/jpeg";
			
			// Gemini API 요청 생성
			List<GeminiGenerateContentRequest.Part> parts = new ArrayList<>();
			
			// 이미지 추가
			parts.add(GeminiGenerateContentRequest.Part.builder()
				.inlineData(GeminiGenerateContentRequest.InlineData.builder()
					.mimeType(mimeType)
					.data(imageBase64)
					.build())
				.build());
			
			// 프롬프트 추가
			parts.add(GeminiGenerateContentRequest.Part.builder()
				.text(prompt)
				.build());
			
			// Content 생성
			GeminiGenerateContentRequest.Content content = 
				GeminiGenerateContentRequest.Content.builder()
					.parts(parts)
					.build();
			
			List<GeminiGenerateContentRequest.Content> contents = new ArrayList<>();
			contents.add(content);
			
			// 최종 요청 객체 생성 (GenerationConfig 없이 - 텍스트 응답만 필요)
			GeminiGenerateContentRequest request = GeminiGenerateContentRequest.builder()
				.contents(contents)
				.build();
			
			// Gemini API 호출
			// v1beta 대신 v1 사용 (더 안정적)
			String endpoint = "/models/" + analysisModel + ":generateContent";
			log.info("📡 Gemini API 호출 - 엔드포인트: {}, 모델: {}", endpoint, analysisModel);
			
			GeminiGenerateContentResponse response = geminiWebClient.post()
				.uri(endpoint)
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(request)
				.retrieve()
				.bodyToMono(GeminiGenerateContentResponse.class)
				.block();
			
			if (response == null || response.getCandidates() == null || response.getCandidates().isEmpty()) {
				log.error("Gemini API 분석 응답이 null이거나 비어있음");
				throw new BadRequestException("No response from Gemini API for style analysis");
			}
			
			// 응답에서 텍스트 추출
			GeminiGenerateContentResponse.Candidate candidate = response.getCandidates().get(0);
			if (candidate.getContent() == null || candidate.getContent().getParts() == null) {
				log.error("Gemini API 응답에 content 또는 parts가 없음");
				throw new BadRequestException("Invalid response from Gemini API for style analysis");
			}
			
			// parts에서 텍스트 데이터 찾기
			StringBuilder analysisText = new StringBuilder();
			for (GeminiGenerateContentResponse.Part part : candidate.getContent().getParts()) {
				if (part.getText() != null && !part.getText().trim().isEmpty()) {
					analysisText.append(part.getText().trim());
				}
			}
			
			if (analysisText.length() == 0) {
				log.warn("Gemini API 응답에 텍스트가 없음");
				return "스타일 분석 결과를 받을 수 없습니다.";
			}
			
			String result = analysisText.toString();
			log.info("✅ Gemini API 스타일 분석 완료 - 결과 길이: {} 문자", result.length());
			log.debug("분석 결과: {}", result.substring(0, Math.min(200, result.length())));
			
			return result;
			
		} catch (WebClientResponseException e) {
			String responseBody = e.getResponseBodyAsString();
			log.error("❌ Gemini API 스타일 분석 실패 - Status: {}, Response: {}, Model: {}", 
				e.getStatusCode(), responseBody, analysisModel, e);
			
			// 404 오류인 경우 더 자세한 정보 로깅
			if (e.getStatusCode().value() == 404) {
				log.error("⚠️ 모델을 찾을 수 없습니다. 모델 이름 확인 필요: {}", analysisModel);
				log.error("⚠️ API Base URL: {}", geminiBaseUrl);
				log.error("⚠️ 전체 엔드포인트: {}/models/{}:generateContent", geminiBaseUrl, analysisModel);
			}
			
			throw new BadRequestException("Failed to analyze image style with Gemini API: " + e.getMessage());
		} catch (Exception e) {
			log.error("❌ Gemini API 스타일 분석 중 예외 발생", e);
			throw new BadRequestException("Error analyzing image style: " + e.getMessage());
		}
	}
	/**
	 * [추가] Gemini API를 사용하여 옷 이미지 상세 분석 (JSON 변환 포함)
	 */
	public ClothesAnalysisResultDto analyzeClothesImage(byte[] imageBytes, String prompt) {
		try {
			// 1. 기존 analyzeImageStyle 로직을 활용해 텍스트 응답(JSON 문자열)을 받습니다.
			String jsonResponse = analyzeImageStyle(imageBytes, prompt);

			// 2. 만약 응답에 마크다운 기호(```json ... ```)가 섞여 있다면 제거합니다.
			String cleanJson = jsonResponse.replaceAll("```json", "").replaceAll("```", "").trim();

			// 3. Jackson ObjectMapper를 사용해 JSON 문자열을 DTO 객체로 바로 변환합니다!
			return objectMapper.readValue(cleanJson, ClothesAnalysisResultDto.class);

		} catch (Exception e) {
			log.error("❌ Gemini JSON 파싱 실패. 기본 객체를 반환합니다: {}", e.getMessage());
			// 파싱 실패 시 에러 방지를 위해 빈 객체라도 반환
			ClothesAnalysisResultDto fallback = new ClothesAnalysisResultDto();
			fallback.setCategory("Unknown");
			fallback.setColor("알 수 없음");
			return fallback;
		}
	}

}
