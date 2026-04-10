package com.accounting.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 注册请求
 */
@Data
public class RegisterRequest {
    
    @NotBlank(message = "用户名不能为空")
    private String username;
    
    @NotBlank(message = "密码不能为空")
    private String password;
    
    private String nickname;
    
    private String email;
    
    @NotBlank(message = "验证码不能为空")
    private String captchaCode;
    
    @NotBlank(message = "验证码标识不能为空")
    private String captchaKey;
}
