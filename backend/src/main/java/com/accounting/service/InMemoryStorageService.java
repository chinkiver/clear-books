package com.accounting.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 内存存储实现（用于开发和测试环境）
 */
@Slf4j
@Service
public class InMemoryStorageService implements StorageService {

    private final Map<String, String> storage = new ConcurrentHashMap<>();
    private final Map<String, Long> expireTimes = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();

    public InMemoryStorageService() {
        // 启动定时清理任务，每 60 秒清理一次过期数据
        cleanupExecutor.scheduleAtFixedRate(this::cleanup, 60, 60, TimeUnit.SECONDS);
        log.info("📝 In-memory storage initialized (for dev/test only)");
    }

    @Override
    public void set(String key, String value, long timeout, TimeUnit unit) {
        storage.put(key, value);
        expireTimes.put(key, System.currentTimeMillis() + unit.toMillis(timeout));
    }

    @Override
    public String get(String key) {
        // 检查是否过期
        Long expireTime = expireTimes.get(key);
        if (expireTime != null && System.currentTimeMillis() > expireTime) {
            delete(key);
            return null;
        }
        return storage.get(key);
    }

    @Override
    public void delete(String key) {
        storage.remove(key);
        expireTimes.remove(key);
    }

    @Override
    public Long increment(String key) {
        Long expireTime = expireTimes.get(key);
        if (expireTime != null && System.currentTimeMillis() > expireTime) {
            delete(key);
        }
        
        String value = storage.get(key);
        long newValue = (value == null) ? 1 : Long.parseLong(value) + 1;
        storage.put(key, String.valueOf(newValue));
        return newValue;
    }

    @Override
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        if (!storage.containsKey(key)) {
            return false;
        }
        expireTimes.put(key, System.currentTimeMillis() + unit.toMillis(timeout));
        return true;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    /**
     * 清理过期数据
     */
    private void cleanup() {
        long now = System.currentTimeMillis();
        expireTimes.entrySet().removeIf(entry -> {
            if (now > entry.getValue()) {
                storage.remove(entry.getKey());
                return true;
            }
            return false;
        });
    }
}
