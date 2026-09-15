package com.example.Capstone_project.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * {@code storage.type=local} 일 때 로컬 저장소 폴더를 정적 리소스로 서빙한다.
 * 예) GET http://localhost:8080/files/top-img/uuid.jpg → {root-dir}/top-img/uuid.jpg
 *
 * <p>인증 없이 접근 가능해야 앱에서 이미지를 바로 띄울 수 있으므로
 * SecurityConfig 에서 {@code storage.local.url-path}/** 를 permitAll 처리한다.
 */
@Configuration
@ConditionalOnProperty(name = "storage.type", havingValue = "local")
public class LocalStorageWebConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(LocalStorageWebConfig.class);

    private final LocalImageStorageService localImageStorageService;

    public LocalStorageWebConfig(LocalImageStorageService localImageStorageService) {
        this.localImageStorageService = localImageStorageService;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String pattern = localImageStorageService.getUrlPath() + "/**";
        String location = localImageStorageService.getRootDir().toUri().toString(); // file:/abs/path/
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        registry.addResourceHandler(pattern).addResourceLocations(location);
        log.info("📁 로컬 이미지 정적 서빙 등록 - {} → {}", pattern, location);
    }
}
