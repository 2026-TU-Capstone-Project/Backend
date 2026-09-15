package com.example.Capstone_project.storage;

import java.util.Base64;
import java.util.UUID;

/**
 * 이미지 저장소 추상화.
 *
 * <p>구현체는 {@code storage.type} 프로퍼티로 선택된다.
 * <ul>
 *   <li>{@code gcs}   (기본값) : {@link com.example.Capstone_project.service.GoogleCloudStorageService} — 배포 환경</li>
 *   <li>{@code local}          : {@link LocalImageStorageService} — 로컬 시연/개발 환경 (디스크 저장 + 정적 서빙)</li>
 * </ul>
 *
 * <p>모든 upload 계열 메서드는 프론트에서 그대로 열 수 있는 <b>공개 URL</b>을 반환한다.
 * objectName 은 "폴더/파일명" 형태(예: {@code top-img/uuid.jpg})로, GCS 와 로컬 구현이 동일한 규칙을 쓴다.
 */
public interface ImageStorageService {

    /** 폴더 이름 — GCS 버킷 내 경로와 로컬 디스크 하위 폴더에 동일하게 사용 */
    String FOLDER_PROFILE = "profile-images";
    String FOLDER_USER_BODY = "user-body-img";
    String FOLDER_TOP = "top-img";
    String FOLDER_BOTTOM = "bottom-img";

    /** 가상 피팅 결과 이미지 업로드 (기본 폴더: gcs.folder-path / storage.local.fitting-folder) */
    String uploadImage(byte[] imageBytes, String filename, String contentType);

    String uploadProfileImage(byte[] imageBytes, String filename, String contentType);

    String uploadUserBodyImage(byte[] imageBytes, String filename, String contentType);

    String uploadTopImage(byte[] imageBytes, String filename, String contentType);

    String uploadBottomImage(byte[] imageBytes, String filename, String contentType);

    /** objectName("폴더/파일명")으로 저장된 이미지 바이트를 읽는다. 없으면 RuntimeException. */
    byte[] downloadImage(String objectName);

    /** objectName("폴더/파일명")으로 저장된 이미지를 삭제한다. 실패해도 예외를 던지지 않는다. */
    void deleteImage(String objectName);

    /** 이 저장소가 발급한 URL 인지 여부 (다른 저장소 URL·로컬 레거시 경로 구분용) */
    boolean isManagedUrl(String url);

    /** 공개 URL → objectName("폴더/파일명"). 이 저장소의 URL 이 아니면 IllegalArgumentException. */
    String extractObjectNameFromUrl(String url);

    /**
     * Base64 이미지를 가상 피팅 결과 폴더에 업로드하고 공개 URL 반환.
     * 파일명은 UUID 로 생성하며 확장자는 MIME 타입에서 결정한다.
     */
    default String uploadBase64Image(String imageBase64, String mimeType) {
        String extension = "jpg";
        if (mimeType != null) {
            if (mimeType.contains("png")) {
                extension = "png";
            } else if (mimeType.contains("jpeg") || mimeType.contains("jpg")) {
                extension = "jpg";
            }
        }
        String filename = UUID.randomUUID() + "." + extension;
        byte[] imageBytes = Base64.getDecoder().decode(imageBase64);
        return uploadImage(imageBytes, filename, mimeType != null ? mimeType : "image/jpeg");
    }
}
