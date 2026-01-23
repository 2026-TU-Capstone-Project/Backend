package com.example.Capstone_project.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 실제 사진 저장 폴더 (GeminiService의 imageStoragePath와 일치해야 함)
        Path imageDir = Paths.get("images", "virtual-fitting");
        String imagePath = imageDir.toFile().getAbsolutePath();

        // 브라우저에서 /api/v1/virtual-fitting/images/파일명.jpg 로 접속 허용
        registry.addResourceHandler("/api/v1/virtual-fitting/images/**")
                .addResourceLocations("file:" + imagePath + "/");
    }
}