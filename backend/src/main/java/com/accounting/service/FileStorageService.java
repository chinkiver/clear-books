package com.accounting.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * 文件存储服务
 * 处理图片上传到本地文件系统
 */
@Slf4j
@Service
public class FileStorageService {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Value("${file.base-url:/uploads}")
    private String baseUrl;

    private Path fileStoragePath;

    @PostConstruct
    public void init() {
        this.fileStoragePath = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(fileStoragePath);
            log.info("📁 File storage directory: {}", fileStoragePath);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory", e);
        }
    }

    /**
     * 存储文件并返回访问 URL
     * 
     * @param file 上传的文件
     * @param subDir 子目录（如 logos, icons）
     * @return 文件访问 URL
     */
    public String storeFile(MultipartFile file, String subDir) {
        try {
            // 验证文件
            if (file.isEmpty()) {
                throw new RuntimeException("Failed to store empty file");
            }

            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);
            String newFilename = UUID.randomUUID().toString() + extension;

            // 创建子目录
            Path targetDir = fileStoragePath.resolve(subDir);
            Files.createDirectories(targetDir);

            // 保存文件
            Path targetLocation = targetDir.resolve(newFilename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // 返回访问 URL
            String fileUrl = baseUrl + "/" + subDir + "/" + newFilename;
            log.info("📤 File uploaded: {} -> {}", originalFilename, fileUrl);
            
            return fileUrl;
            
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }

    /**
     * 删除文件
     */
    public void deleteFile(String fileUrl) {
        try {
            if (fileUrl == null || fileUrl.isEmpty()) {
                return;
            }
            
            // 从 URL 提取相对路径
            String relativePath = fileUrl.replace(baseUrl, "");
            if (relativePath.startsWith("/")) {
                relativePath = relativePath.substring(1);
            }
            
            Path filePath = fileStoragePath.resolve(relativePath).normalize();
            
            // 安全检查：确保文件在 upload 目录内
            if (!filePath.startsWith(fileStoragePath)) {
                log.warn("⚠️ Invalid file path (security): {}", fileUrl);
                return;
            }
            
            Files.deleteIfExists(filePath);
            log.info("🗑️ File deleted: {}", filePath);
            
        } catch (IOException e) {
            log.error("Failed to delete file: {}", fileUrl, e);
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}
