package com.accounting.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Redis 存储实现
 */
@Slf4j
@Service
public class RedisStorageService implements StorageService {

    private StringRedisTemplate redisTemplate;

    @Autowired(required = false)
    public void setRedisTemplate(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        if (redisTemplate != null) {
            log.info("✅ Redis storage initialized");
        }
    }

    @Override
    public void set(String key, String value, long timeout, TimeUnit unit) {
        if (redisTemplate != null) {
            redisTemplate.opsForValue().set(key, value, timeout, unit);
        }
    }

    @Override
    public String get(String key) {
        if (redisTemplate == null) {
            return null;
        }
        return redisTemplate.opsForValue().get(key);
    }

    @Override
    public void delete(String key) {
        if (redisTemplate != null) {
            redisTemplate.delete(key);
        }
    }

    @Override
    public Long increment(String key) {
        if (redisTemplate == null) {
            return null;
        }
        return redisTemplate.opsForValue().increment(key);
    }

    @Override
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        if (redisTemplate == null) {
            return false;
        }
        return redisTemplate.expire(key, timeout, unit);
    }

    @Override
    public boolean isAvailable() {
        return redisTemplate != null;
    }
}
