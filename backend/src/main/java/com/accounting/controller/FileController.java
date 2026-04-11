package com.accounting.controller;

import com.accounting.dto.ApiResponse;
import com.accounting.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * 文件上传控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    /**
     * 检查当前用户是否是管理员
     */
    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && "admin".equals(auth.getName());
    }

    /**
     * 上传 Logo 图片
     * 
     * @param file 图片文件 (PNG, JPG, SVG, 最大 100KB)
     */
    @PostMapping("/logo")
    public ApiResponse<Map<String, String>> uploadLogo(
            @RequestParam("file") MultipartFile file) {
        
        if (!isAdmin()) {
            return ApiResponse.error(403, "没有权限");
        }

        // 验证文件类型
        String contentType = file.getContentType();
        if (contentType == null || !(contentType.equals("image/png") 
                || contentType.equals("image/jpeg") 
                || contentType.equals("image/jpg")
                || contentType.equals("image/svg+xml"))) {
            return ApiResponse.error(400, "仅支持 PNG, JPG, SVG 格式");
        }

        // 验证文件大小 (100KB)
        if (file.getSize() > 100 * 1024) {
            return ApiResponse.error(400, "文件大小不能超过 100KB");
        }

        try {
            String fileUrl = fileStorageService.storeFile(file, "logos");
            
            Map<String, String> result = new HashMap<>();
            result.put("url", fileUrl);
            result.put("filename", file.getOriginalFilename());
            
            return ApiResponse.success(result);
            
        } catch (Exception e) {
            log.error("Failed to upload logo", e);
            return ApiResponse.error(500, "上传失败: " + e.getMessage());
        }
    }

    /**
     * 上传 Favicon 图标
     * 
     * @param file 图标文件 (PNG, ICO, 最大 50KB)
     */
    @PostMapping("/icon")
    public ApiResponse<Map<String, String>> uploadIcon(
            @RequestParam("file") MultipartFile file) {
        
        if (!isAdmin()) {
            return ApiResponse.error(403, "没有权限");
        }

        // 验证文件类型
        String contentType = file.getContentType();
        if (contentType == null || !(contentType.equals("image/png") 
                || contentType.equals("image/x-icon")
                || contentType.equals("image/vnd.microsoft.icon"))) {
            return ApiResponse.error(400, "仅支持 PNG, ICO 格式");
        }

        // 验证文件大小 (50KB)
        if (file.getSize() > 50 * 1024) {
            return ApiResponse.error(400, "文件大小不能超过 50KB");
        }

        try {
            String fileUrl = fileStorageService.storeFile(file, "icons");
            
            Map<String, String> result = new HashMap<>();
            result.put("url", fileUrl);
            result.put("filename", file.getOriginalFilename());
            
            return ApiResponse.success(result);
            
        } catch (Exception e) {
            log.error("Failed to upload icon", e);
            return ApiResponse.error(500, "上传失败: " + e.getMessage());
        }
    }
}
