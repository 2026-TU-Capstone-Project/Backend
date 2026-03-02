package com.example.Capstone_project.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
public class RedisLockService {

    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private volatile boolean warnedNoRedis = false;

    public RedisLockService(ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        this.redisTemplateProvider = redisTemplateProvider;
    }

    /** lockKey가 없으면 생성하고 true, 이미 있으면 false */
    public boolean tryLock(String lockKey, Duration ttl) {
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            if (!warnedNoRedis) {
                warnedNoRedis = true;
                log.warn("Redis is not configured; RedisLockService will allow all locks (lockKey={})", lockKey);
            }
            return true; // Redis 미구성 시 락을 강제하지 않음
        }
        Boolean ok = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", ttl);
        return Boolean.TRUE.equals(ok);
    }

    public void unlock(String lockKey) {
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            return;
        }
        redisTemplate.delete(lockKey);
    }
}