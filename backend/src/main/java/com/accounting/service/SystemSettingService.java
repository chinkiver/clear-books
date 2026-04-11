package com.accounting.service;

import com.accounting.dto.SystemSettingDTO;
import com.accounting.entity.SystemSetting;
import com.accounting.repository.SystemSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SystemSettingService {

    private final SystemSettingRepository systemSettingRepository;

    // 系统设置键名常量
    public static final String KEY_SYSTEM_NAME = "system.name";
    public static final String KEY_SYSTEM_LOGO = "system.logo";
    public static final String KEY_SYSTEM_ICON = "system.icon";

    // 默认值
    public static final String DEFAULT_SYSTEM_NAME = "Clear Books";

    /**
     * 获取所有设置
     */
    public List<SystemSettingDTO.Response> getAllSettings() {
        return systemSettingRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 根据键名获取设置值
     */
    public String getSettingValue(String key, String defaultValue) {
        String value = systemSettingRepository.findByKey(key)
                .map(SystemSetting::getValue)
                .orElse(defaultValue);

        return value;
    }

    /**
     * 更新或创建设置
     */
    @Transactional
    public SystemSettingDTO.Response updateSetting(SystemSettingDTO.Request request) {
        SystemSetting setting = systemSettingRepository.findByKey(request.getKey())
                .orElse(SystemSetting.builder()
                        .key(request.getKey())
                        .build());

        setting.setValue(request.getValue());
        setting.setDescription(request.getDescription());

        SystemSetting saved = systemSettingRepository.save(setting);
        return toResponse(saved);
    }

    /**
     * 批量更新设置
     */
    @Transactional
    public void updateSettings(List<SystemSettingDTO.Request> requests) {
        for (SystemSettingDTO.Request request : requests) {
            updateSetting(request);
        }
    }

    /**
     * 获取公开的系统信息（不需要认证）
     */
    public SystemSettingDTO.PublicInfo getPublicInfo() {
        String systemName = getSettingValue(KEY_SYSTEM_NAME, DEFAULT_SYSTEM_NAME);
        String logo = getSettingValue(KEY_SYSTEM_LOGO, null);
        String icon = getSettingValue(KEY_SYSTEM_ICON, null);

        return SystemSettingDTO.PublicInfo.builder()
                .systemName(systemName)
                .logo(logo)
                .icon(icon)
                .build();
    }

    private SystemSettingDTO.Response toResponse(SystemSetting setting) {
        return SystemSettingDTO.Response.builder()
                .id(setting.getId())
                .key(setting.getKey())
                .value(setting.getValue())
                .description(setting.getDescription())
                .updatedAt(setting.getUpdatedAt())
                .build();
    }
}
