package com.accounting.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 统一存储服务
 * 自动选择 Redis（优先）或内存存储
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UnifiedStorageService implements StorageService {

    private final RedisStorageService redisStorage;
    private final InMemoryStorageService memoryStorage;

    /**
     * 获取实际使用的存储服务
     */
    private StorageService getStorage() {
        return redisStorage.isAvailable() ? redisStorage : memoryStorage;
    }

    @Override
    public void set(String key, String value, long timeout, TimeUnit unit) {
        getStorage().set(key, value, timeout, unit);
    }

    @Override
    public String get(String key) {
        return getStorage().get(key);
    }

    @Override
    public void delete(String key) {
        getStorage().delete(key);
    }

    @Override
    public Long increment(String key) {
        return getStorage().increment(key);
    }

    @Override
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        return getStorage().expire(key, timeout, unit);
    }

    @Override
    public boolean isAvailable() {
        return true; // 总是可用（至少内存可用）
    }

    /**
     * 检查是否在使用 Redis
     */
    public boolean isUsingRedis() {
        return redisStorage.isAvailable();
    }
}
