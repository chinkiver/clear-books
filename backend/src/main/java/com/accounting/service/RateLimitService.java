package com.accounting.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 限流服务
 */
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final UnifiedStorageService storageService;
    
    // Redis key 前缀
    private static final String REGISTER_LIMIT_PREFIX = "register:limit:";
    
    // 每小时最多注册次数
    private static final int MAX_REGISTER_PER_HOUR = 5;
    
    // 限制时间窗口（小时）
    private static final int LIMIT_WINDOW_HOURS = 1;
    
    /**
     * 检查是否允许注册
     * @param ip 用户 IP 地址
     * @return true 允许注册，false 超过限制
     */
    public boolean allowRegister(String ip) {
        String key = REGISTER_LIMIT_PREFIX + ip;
        String count = storageService.get(key);
        
        if (count == null) {
            // 第一次注册，设置计数为 1，1小时后过期
            storageService.set(key, "1", LIMIT_WINDOW_HOURS, TimeUnit.HOURS);
            return true;
        }
        
        int currentCount = Integer.parseInt(count);
        if (currentCount >= MAX_REGISTER_PER_HOUR) {
            return false;
        }
        
        // 增加计数
        storageService.increment(key);
        return true;
    }
    
    /**
     * 获取剩余可注册次数
     */
    public int getRemainingRegisterCount(String ip) {
        String key = REGISTER_LIMIT_PREFIX + ip;
        String count = storageService.get(key);
        
        if (count == null) {
            return MAX_REGISTER_PER_HOUR;
        }
        
        int currentCount = Integer.parseInt(count);
        return Math.max(0, MAX_REGISTER_PER_HOUR - currentCount);
    }
}
