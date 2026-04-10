package com.accounting.service;

import com.accounting.dto.CaptchaResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 验证码服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaptchaService {

    private final UnifiedStorageService storageService;
    
    // Redis key 前缀
    private static final String CAPTCHA_KEY_PREFIX = "captcha:";
    // 验证码有效期（分钟）
    private static final long CAPTCHA_EXPIRE_MINUTES = 5;
    // 验证码长度
    private static final int CAPTCHA_LENGTH = 4;
    // 图片宽度
    private static final int IMAGE_WIDTH = 120;
    // 图片高度
    private static final int IMAGE_HEIGHT = 40;
    
    /**
     * 生成验证码
     */
    public CaptchaResponse generateCaptcha() {
        // 生成随机验证码
        String captchaCode = generateRandomCode();
        String captchaKey = UUID.randomUUID().toString();
        
        // 存储到 Redis 或内存
        String redisKey = CAPTCHA_KEY_PREFIX + captchaKey;
        storageService.set(redisKey, captchaCode, CAPTCHA_EXPIRE_MINUTES, TimeUnit.MINUTES);
        
        // 生成图片
        String captchaImage = generateCaptchaImage(captchaCode);
        
        return CaptchaResponse.builder()
                .captchaKey(captchaKey)
                .captchaImage(captchaImage)
                .build();
    }
    
    /**
     * 验证验证码
     */
    public boolean validateCaptcha(String captchaKey, String captchaCode) {
        if (captchaKey == null || captchaCode == null) {
            return false;
        }
        
        String redisKey = CAPTCHA_KEY_PREFIX + captchaKey;
        String storedCode = storageService.get(redisKey);
        
        if (storedCode == null) {
            return false;
        }
        
        // 验证成功后删除验证码（防止重复使用）
        if (storedCode.equalsIgnoreCase(captchaCode)) {
            storageService.delete(redisKey);
            return true;
        }
        
        return false;
    }
    
    /**
     * 生成随机验证码（字母+数字）
     */
    private String generateRandomCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        
        for (int i = 0; i < CAPTCHA_LENGTH; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        return code.toString();
    }
    
    /**
     * 生成验证码图片 Base64
     */
    private String generateCaptchaImage(String captchaCode) {
        BufferedImage image = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        Random random = new Random();
        
        // 填充背景
        g.setColor(new Color(240, 240, 240));
        g.fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
        
        // 添加干扰线
        for (int i = 0; i < 20; i++) {
            g.setColor(new Color(random.nextInt(200), random.nextInt(200), random.nextInt(200)));
            int x1 = random.nextInt(IMAGE_WIDTH);
            int y1 = random.nextInt(IMAGE_HEIGHT);
            int x2 = random.nextInt(IMAGE_WIDTH);
            int y2 = random.nextInt(IMAGE_HEIGHT);
            g.drawLine(x1, y1, x2, y2);
        }
        
        // 添加干扰点
        for (int i = 0; i < 100; i++) {
            g.setColor(new Color(random.nextInt(200), random.nextInt(200), random.nextInt(200)));
            int x = random.nextInt(IMAGE_WIDTH);
            int y = random.nextInt(IMAGE_HEIGHT);
            g.fillRect(x, y, 1, 1);
        }
        
        // 绘制文字
        g.setFont(new Font("Arial", Font.BOLD, 24));
        int x = 15;
        for (int i = 0; i < captchaCode.length(); i++) {
            // 随机颜色
            g.setColor(new Color(20 + random.nextInt(100), 20 + random.nextInt(100), 20 + random.nextInt(100)));
            // 随机旋转
            int rotate = random.nextInt(20) - 10;
            g.rotate(Math.toRadians(rotate), x, 25);
            g.drawString(String.valueOf(captchaCode.charAt(i)), x, 28);
            g.rotate(-Math.toRadians(rotate), x, 25);
            x += 25;
        }
        
        g.dispose();
        
        // 转换为 Base64
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "png", outputStream);
            byte[] imageBytes = outputStream.toByteArray();
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(imageBytes);
        } catch (IOException e) {
            log.error("生成验证码图片失败", e);
            throw new RuntimeException("生成验证码失败");
        }
    }
}
