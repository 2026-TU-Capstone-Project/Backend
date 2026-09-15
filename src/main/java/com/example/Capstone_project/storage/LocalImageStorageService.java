package com.example.Capstone_project.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 로컬 디스크 이미지 저장소 ({@code storage.type=local}).
 *
 * <p>클라우드(GCS) 없이 백엔드를 단독 실행할 때 사용한다.
 * <ul>
 *   <li>파일은 {@code storage.local.root-dir}/폴더/파일명 에 저장</li>
 *   <li>URL 은 {@code storage.local.public-base-url + storage.local.url-path + /폴더/파일명}
 *       (예: {@code http://localhost:8080/files/top-img/uuid.jpg})</li>
 *   <li>{@link LocalStorageWebConfig} 가 같은 경로를 정적 리소스로 서빙하고,
 *       SecurityConfig 에서 해당 경로를 permitAll 처리한다.</li>
 * </ul>
 */
@Service
@ConditionalOnProperty(name = "storage.type", havingValue = "local")
public class LocalImageStorageService implements ImageStorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalImageStorageService.class);

    private final Path rootDir;
    private final String urlPath;        // 예: /files
    private final String publicBaseUrl;  // 예: http://localhost:8080
    private final String fittingFolder;  // 예: virtual-fitting-img

    public LocalImageStorageService(
            @Value("${storage.local.root-dir:./local-storage}") String rootDir,
            @Value("${storage.local.url-path:/files}") String urlPath,
            @Value("${storage.local.public-base-url:http://localhost:8080}") String publicBaseUrl,
            @Value("${storage.local.fitting-folder:${gcs.folder-path:virtual-fitting-img}}") String fittingFolder
    ) {
        this.rootDir = Paths.get(rootDir).toAbsolutePath().normalize();
        this.urlPath = normalizeUrlPath(urlPath);
        this.publicBaseUrl = stripTrailingSlash(publicBaseUrl);
        this.fittingFolder = stripSlashes(fittingFolder);
        try {
            Files.createDirectories(this.rootDir);
        } catch (IOException e) {
            throw new IllegalStateException("로컬 저장소 폴더를 만들 수 없습니다: " + this.rootDir, e);
        }
        log.info("✅ Local image storage 초기화 완료 - 디렉토리: {}, URL: {}{}/**", this.rootDir, this.publicBaseUrl, this.urlPath);
    }

    // ------------------------------------------------------------------
    // upload
    // ------------------------------------------------------------------

    @Override
    public String uploadImage(byte[] imageBytes, String filename, String contentType) {
        return store(fittingFolder, filename, imageBytes);
    }

    @Override
    public String uploadProfileImage(byte[] imageBytes, String filename, String contentType) {
        return store(FOLDER_PROFILE, filename, imageBytes);
    }

    @Override
    public String uploadUserBodyImage(byte[] imageBytes, String filename, String contentType) {
        return store(FOLDER_USER_BODY, filename, imageBytes);
    }

    @Override
    public String uploadTopImage(byte[] imageBytes, String filename, String contentType) {
        return store(FOLDER_TOP, filename, imageBytes);
    }

    @Override
    public String uploadBottomImage(byte[] imageBytes, String filename, String contentType) {
        return store(FOLDER_BOTTOM, filename, imageBytes);
    }

    private String store(String folder, String filename, byte[] imageBytes) {
        String safeName = sanitizeFilename(filename);
        String objectName = folder + "/" + safeName;
        Path target = resolveSafely(objectName);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, imageBytes);
        } catch (IOException e) {
            log.error("❌ 로컬 이미지 저장 실패: {}", target, e);
            throw new RuntimeException("Failed to save image to local storage: " + e.getMessage(), e);
        }
        // 파일명에 한글·공백 등이 있어도 앱에서 바로 열 수 있도록 path segment 인코딩
        String url = publicBaseUrl + urlPath + "/" + folder + "/"
                + UriUtils.encodePathSegment(safeName, StandardCharsets.UTF_8);
        log.info("✅ 이미지 로컬 저장 완료 - URL: {}, 크기: {} bytes", url, imageBytes.length);
        return url;
    }

    // ------------------------------------------------------------------
    // download / delete
    // ------------------------------------------------------------------

    @Override
    public byte[] downloadImage(String objectName) {
        Path path = resolveSafely(objectName);
        try {
            byte[] bytes = Files.readAllBytes(path);
            log.info("✅ 로컬 이미지 읽기 완료 - 경로: {}, 크기: {} bytes", objectName, bytes.length);
            return bytes;
        } catch (IOException e) {
            log.error("❌ 로컬 이미지 읽기 실패: {}", path, e);
            throw new RuntimeException("Failed to read image from local storage: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteImage(String objectName) {
        try {
            Path path = resolveSafely(objectName);
            boolean deleted = Files.deleteIfExists(path);
            if (deleted) {
                log.info("✅ 로컬 이미지 삭제 완료: {}", objectName);
            } else {
                log.warn("⚠️ 로컬 이미지 삭제 실패 (파일 없음): {}", objectName);
            }
        } catch (Exception e) {
            log.error("❌ 로컬 이미지 삭제 중 에러 발생: {}", e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // URL <-> objectName
    // ------------------------------------------------------------------

    @Override
    public boolean isManagedUrl(String url) {
        return url != null && url.contains(urlPath + "/");
    }

    @Override
    public String extractObjectNameFromUrl(String url) {
        if (!isManagedUrl(url)) {
            throw new IllegalArgumentException("Invalid local storage URL: " + url);
        }
        String marker = urlPath + "/";
        String rest = url.substring(url.indexOf(marker) + marker.length());
        int cut = rest.indexOf('?');
        if (cut >= 0) rest = rest.substring(0, cut);
        cut = rest.indexOf('#');
        if (cut >= 0) rest = rest.substring(0, cut);
        String objectName = URLDecoder.decode(rest, StandardCharsets.UTF_8);
        if (objectName.isBlank()) {
            throw new IllegalArgumentException("Cannot extract object name from URL: " + url);
        }
        return objectName;
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    /** rootDir 바깥으로 나가는 경로(../ 등)를 차단한다. */
    private Path resolveSafely(String objectName) {
        Path path = rootDir.resolve(objectName).normalize();
        if (!path.startsWith(rootDir)) {
            throw new IllegalArgumentException("Invalid object name: " + objectName);
        }
        return path;
    }

    /**
     * 디스크에 저장할 파일명 생성.
     * <ul>
     *   <li>업로드 원본 파일명(한글·공백·경로 포함 가능)이 그대로 들어오므로 ASCII 안전 문자만 남긴다
     *       (JVM 기본 인코딩이 UTF-8 이 아닌 환경에서 InvalidPathException 방지).</li>
     *   <li>같은 이름의 사진(예: image.jpg)이 서로 덮어쓰지 않도록 짧은 UUID 접두어를 붙인다.</li>
     * </ul>
     */
    private static String sanitizeFilename(String filename) {
        String name = (filename == null || filename.isBlank()) ? "image.jpg" : filename;
        name = name.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1);
        name = name.replaceAll("[^A-Za-z0-9._-]", "_");
        if (name.isBlank() || name.equals(".") || name.equals("..") || name.startsWith(".")) name = "image.jpg";
        if (name.length() > 120) name = name.substring(name.length() - 120);
        String prefix = UUID.randomUUID().toString().substring(0, 8);
        return prefix + "-" + name;
    }

    private static String normalizeUrlPath(String p) {
        String s = stripSlashes(p);
        return s.isEmpty() ? "/files" : "/" + s;
    }

    private static String stripSlashes(String s) {
        if (s == null) return "";
        String t = s.trim();
        while (t.startsWith("/")) t = t.substring(1);
        while (t.endsWith("/")) t = t.substring(0, t.length() - 1);
        return t;
    }

    private static String stripTrailingSlash(String s) {
        if (s == null) return "";
        String t = s.trim();
        while (t.endsWith("/")) t = t.substring(0, t.length() - 1);
        return t;
    }

    public Path getRootDir() {
        return rootDir;
    }

    public String getUrlPath() {
        return urlPath;
    }
}
