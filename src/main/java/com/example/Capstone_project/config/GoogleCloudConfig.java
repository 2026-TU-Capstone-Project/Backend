package com.example.Capstone_project.config;

import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

/**
 * Google Cloud 인증 설정.
 * google-key.json 로딩은 이 Config에서만 수행하며, test 프로필에서는 빈이 생성되지 않음.
 */
@Configuration
@Profile("!test")
public class GoogleCloudConfig {

    private static final String DEFAULT_CREDENTIALS_PATH = "google-key.json";

    @Bean
    public GoogleCredentials googleCredentials() throws IOException {
        try (InputStream keyStream = new ClassPathResource(DEFAULT_CREDENTIALS_PATH).getInputStream()) {
            return GoogleCredentials.fromStream(keyStream);
        }
    }
}
