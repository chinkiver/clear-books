package com.accounting.controller;

import com.accounting.dto.ApiResponse;
import com.accounting.dto.SystemSettingDTO;
import com.accounting.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SystemSettingController {

    private final SystemSettingService systemSettingService;

    /**
     * 检查当前用户是否是管理员（用户名为 admin）
     */
    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && "admin".equals(auth.getName());
    }

    /**
     * 获取所有系统设置（需要管理员权限）
     */
    @GetMapping
    public ApiResponse<List<SystemSettingDTO.Response>> getAllSettings() {
        if (!isAdmin()) {
            return ApiResponse.error(403, "没有权限访问该资源");
        }
        return ApiResponse.success(systemSettingService.getAllSettings());
    }

    /**
     * 获取公开的系统信息（不需要认证）
     */
    @GetMapping("/public")
    public ApiResponse<SystemSettingDTO.PublicInfo> getPublicInfo() {
        return ApiResponse.success(systemSettingService.getPublicInfo());
    }

    /**
     * 更新单个设置（需要管理员权限）
     */
    @PutMapping
    public ApiResponse<SystemSettingDTO.Response> updateSetting(
            @Valid @RequestBody SystemSettingDTO.Request request) {
        if (!isAdmin()) {
            return ApiResponse.error(403, "没有权限访问该资源");
        }
        return ApiResponse.success(systemSettingService.updateSetting(request));
    }

    /**
     * 批量更新设置（需要管理员权限）
     */
    @PutMapping("/batch")
    public ApiResponse<Void> updateSettings(
            @Valid @RequestBody List<SystemSettingDTO.Request> requests) {
        if (!isAdmin()) {
            return ApiResponse.error(403, "没有权限访问该资源");
        }
        systemSettingService.updateSettings(requests);
        return ApiResponse.success();
    }

    /**
     * 获取系统名称
     */
    @GetMapping("/system-name")
    public ApiResponse<Map<String, String>> getSystemName() {
        String name = systemSettingService.getSettingValue(
                SystemSettingService.KEY_SYSTEM_NAME,
                SystemSettingService.DEFAULT_SYSTEM_NAME);
        Map<String, String> result = new HashMap<>();
        result.put("name", name);
        return ApiResponse.success(result);
    }
}
