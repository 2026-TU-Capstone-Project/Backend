package com.example.Capstone_project.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@Profile("!test")
public class GoogleCloudStorageService {

    private final Storage storage;
    private final String bucketName;
    private final String folderPath;

    public GoogleCloudStorageService(
            GoogleCredentials googleCredentials,
            @Value("${gcs.bucket-name:tu-capstone-project}") String bucketName,
            @Value("${gcs.folder-path:virtual-fitting-img}") String folderPath,
            @Value("${gcs.project-id:tu-capstone-project}") String projectId
    ) {
        this.bucketName = bucketName;
        this.folderPath = folderPath;
        this.storage = StorageOptions.newBuilder()
                .setProjectId(projectId)
                .setCredentials(googleCredentials)
                .build()
                .getService();
        log.info("✅ Google Cloud Storage 초기화 완료 - 버킷: {}, 폴더: {}, 프로젝트: {}", bucketName, folderPath, projectId);
    }

    /**
     * 이미지를 GCS에 업로드하고 공개 URL 반환
     * 
     * @param imageBytes 이미지 바이트 배열
     * @param filename 파일명 (확장자 포함)
     * @param contentType MIME 타입 (예: "image/jpeg")
     * @return GCS에 저장된 이미지의 공개 URL
     */
    public String uploadImage(byte[] imageBytes, String filename, String contentType) {
        try {
            // 폴더 경로와 파일명을 결합하여 전체 경로 생성
            String blobName = folderPath + "/" + filename;
            
            // Blob 정보 생성 (버킷의 기본 권한/Uniform access 사용)
            BlobId blobId = BlobId.of(bucketName, blobName);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                    .setContentType(contentType)
                    .build();
            
            // 이미지 업로드
            storage.create(blobInfo, imageBytes);
            
            // 공개 URL 생성 (gs:// 형식이 아닌 https:// 형식)
            String publicUrl = String.format("https://storage.googleapis.com/%s/%s", bucketName, blobName);
            
            log.info("✅ 이미지 GCS 업로드 완료 - URL: {}, 크기: {} bytes", publicUrl, imageBytes.length);
            
            return publicUrl;
        } catch (Exception e) {
            log.error("❌ GCS 이미지 업로드 실패: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload image to GCS: " + e.getMessage(), e);
        }
    }

    /**
     * Base64 이미지를 GCS에 업로드하고 공개 URL 반환
     * 
     * @param imageBase64 Base64 인코딩된 이미지 문자열
     * @param mimeType MIME 타입 (예: "image/jpeg")
     * @return GCS에 저장된 이미지의 공개 URL
     */
    public String uploadBase64Image(String imageBase64, String mimeType) {
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
        
        // Base64 디코딩
        byte[] imageBytes = java.util.Base64.getDecoder().decode(imageBase64);
        
        // GCS에 업로드
        return uploadImage(imageBytes, filename, mimeType != null ? mimeType : "image/jpeg");
    }

    /**
     * GCS에서 이미지 다운로드
     * 
     * @param blobName GCS 내 파일 경로 (폴더/파일명)
     * @return 이미지 바이트 배열
     */
    public byte[] downloadImage(String blobName) {
        try {
            BlobId blobId = BlobId.of(bucketName, blobName);
            byte[] imageBytes = storage.readAllBytes(blobId);
            log.info("✅ GCS 이미지 다운로드 완료 - 경로: {}, 크기: {} bytes", blobName, imageBytes.length);
            return imageBytes;
        } catch (Exception e) {
            log.error("❌ GCS 이미지 다운로드 실패: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to download image from GCS: " + e.getMessage(), e);
        }
    }

    /**
     * 전신 사진을 GCS에 업로드하고 공개 URL 반환
     * user-body-img 폴더에 저장
     * 
     * @param imageBytes 이미지 바이트 배열
     * @param filename 파일명 (확장자 포함)
     * @param contentType MIME 타입 (예: "image/jpeg")
     * @return GCS에 저장된 이미지의 공개 URL
     */
    public String uploadUserBodyImage(byte[] imageBytes, String filename, String contentType) {
        try {
            // user-body-img 폴더 경로와 파일명을 결합하여 전체 경로 생성
            String blobName = "user-body-img/" + filename;
            
            // Blob 정보 생성 (버킷의 기본 권한/Uniform access 사용)
            BlobId blobId = BlobId.of(bucketName, blobName);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                    .setContentType(contentType)
                    .build();
            
            // 이미지 업로드
            storage.create(blobInfo, imageBytes);
            
            // 공개 URL 생성
            String publicUrl = String.format("https://storage.googleapis.com/%s/%s", bucketName, blobName);
            
            log.info("✅ 전신 사진 GCS 업로드 완료 - URL: {}, 크기: {} bytes", publicUrl, imageBytes.length);
            
            return publicUrl;
        } catch (Exception e) {
            log.error("❌ GCS 전신 사진 업로드 실패: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload user body image to GCS: " + e.getMessage(), e);
        }
    }

    /**
     * 상의 이미지를 GCS에 업로드하고 공개 URL 반환
     * top-img 폴더에 저장
     * 
     * @param imageBytes 이미지 바이트 배열
     * @param filename 파일명 (확장자 포함)
     * @param contentType MIME 타입 (예: "image/jpeg")
     * @return GCS에 저장된 이미지의 공개 URL
     */
    public String uploadTopImage(byte[] imageBytes, String filename, String contentType) {
        try {
            // top-img 폴더 경로와 파일명을 결합하여 전체 경로 생성
            String blobName = "top-img/" + filename;
            
            // Blob 정보 생성 (버킷의 기본 권한/Uniform access 사용)
            BlobId blobId = BlobId.of(bucketName, blobName);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                    .setContentType(contentType)
                    .build();
            
            // 이미지 업로드
            storage.create(blobInfo, imageBytes);
            
            // 공개 URL 생성
            String publicUrl = String.format("https://storage.googleapis.com/%s/%s", bucketName, blobName);
            
            log.info("✅ 상의 이미지 GCS 업로드 완료 - URL: {}, 크기: {} bytes", publicUrl, imageBytes.length);
            
            return publicUrl;
        } catch (Exception e) {
            log.error("❌ GCS 상의 이미지 업로드 실패: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload top image to GCS: " + e.getMessage(), e);
        }
    }

    /**
     * 하의 이미지를 GCS에 업로드하고 공개 URL 반환
     * bottom-img 폴더에 저장
     * 
     * @param imageBytes 이미지 바이트 배열
     * @param filename 파일명 (확장자 포함)
     * @param contentType MIME 타입 (예: "image/jpeg")
     * @return GCS에 저장된 이미지의 공개 URL
     */
    public String uploadBottomImage(byte[] imageBytes, String filename, String contentType) {
        try {
            // bottom-img 폴더 경로와 파일명을 결합하여 전체 경로 생성
            String blobName = "bottom-img/" + filename;
            
            // Blob 정보 생성 (버킷의 기본 권한/Uniform access 사용)
            BlobId blobId = BlobId.of(bucketName, blobName);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                    .setContentType(contentType)
                    .build();
            
            // 이미지 업로드
            storage.create(blobInfo, imageBytes);
            
            // 공개 URL 생성
            String publicUrl = String.format("https://storage.googleapis.com/%s/%s", bucketName, blobName);
            
            log.info("✅ 하의 이미지 GCS 업로드 완료 - URL: {}, 크기: {} bytes", publicUrl, imageBytes.length);
            
            return publicUrl;
        } catch (Exception e) {
            log.error("❌ GCS 하의 이미지 업로드 실패: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload bottom image to GCS: " + e.getMessage(), e);
        }
    }

    /**
     * GCS URL에서 Blob 이름 추출
     * 예: https://storage.googleapis.com/tu-capstone-project/virtual-fitting-img/uuid.jpg
     * -> virtual-fitting-img/uuid.jpg
     * 
     * @param gcsUrl GCS 공개 URL
     * @return Blob 이름 (폴더/파일명)
     */
    public String extractBlobNameFromUrl(String gcsUrl) {
        if (gcsUrl == null || !gcsUrl.contains("storage.googleapis.com")) {
            throw new IllegalArgumentException("Invalid GCS URL: " + gcsUrl);
        }
        
        // URL에서 버킷 이름 이후 부분 추출
        String prefix = "storage.googleapis.com/" + bucketName + "/";
        int index = gcsUrl.indexOf(prefix);
        if (index == -1) {
            throw new IllegalArgumentException("Cannot extract blob name from URL: " + gcsUrl);
        }
        
        return gcsUrl.substring(index + prefix.length());
    }
}
