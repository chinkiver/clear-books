package com.accounting.controller;

import com.accounting.dto.ApiResponse;
import com.accounting.dto.CaptchaResponse;
import com.accounting.service.CaptchaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 验证码控制器
 */
@RestController
@RequestMapping("/api/captcha")
@RequiredArgsConstructor
public class CaptchaController {

    private final CaptchaService captchaService;

    /**
     * 获取验证码
     */
    @GetMapping
    public ApiResponse<CaptchaResponse> getCaptcha() {
        return ApiResponse.success(captchaService.generateCaptcha());
    }
}
