package com.example.Capstone_project.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RedisLockService {

    private final StringRedisTemplate redisTemplate;

    public RedisLockService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /** lockKey가 없으면 생성하고 true, 이미 있으면 false */
    public boolean tryLock(String lockKey, Duration ttl) {
        Boolean ok = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", ttl);
        return Boolean.TRUE.equals(ok);
    }

    public void unlock(String lockKey) {
        redisTemplate.delete(lockKey);
    }
}