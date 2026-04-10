package com.accounting.service;

import java.util.concurrent.TimeUnit;

/**
 * 通用存储服务接口
 * 支持 Redis 和内存两种实现
 */
public interface StorageService {
    
    /**
     * 存储值，带过期时间
     */
    void set(String key, String value, long timeout, TimeUnit unit);
    
    /**
     * 获取值
     */
    String get(String key);
    
    /**
     * 删除值
     */
    void delete(String key);
    
    /**
     * 增加值（用于计数）
     */
    Long increment(String key);
    
    /**
     * 设置过期时间
     */
    Boolean expire(String key, long timeout, TimeUnit unit);
    
    /**
     * 检查是否可用
     */
    boolean isAvailable();
}
