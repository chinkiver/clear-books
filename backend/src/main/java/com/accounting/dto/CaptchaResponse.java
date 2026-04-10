package com.accounting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 验证码响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaResponse {
    
    /**
     * 验证码唯一标识
     */
    private String captchaKey;
    
    /**
     * 验证码图片 Base64（data:image/png;base64,xxx）
     */
    private String captchaImage;
}
