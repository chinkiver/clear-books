package com.accounting.controller;

import com.accounting.dto.ApiResponse;
import com.accounting.dto.UserDto;
import com.accounting.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "用户管理", description = "用户信息相关接口")
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    @Operation(summary = "获取当前用户信息")
    public ApiResponse<UserDto> getProfile() {
        return ApiResponse.success(userService.getCurrentUser());
    }

    @PutMapping("/password")
    @Operation(summary = "修改密码")
    public ApiResponse<Void> changePassword(@RequestParam String oldPassword, @RequestParam String newPassword) {
        userService.changePassword(oldPassword, newPassword);
        return ApiResponse.success();
    }
}
