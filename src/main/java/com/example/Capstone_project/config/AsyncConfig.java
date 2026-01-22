package com.example.Capstone_project.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync // ✅ 비동기 기능을 활성화합니다!
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5); // 기본 일꾼 5명
        executor.setMaxPoolSize(10); // 최대 일꾼 10명
        executor.setQueueCapacity(500); // 대기 줄 500개
        executor.setThreadNamePrefix("FittingAsync-");
        executor.initialize();
        return executor;
    }
}