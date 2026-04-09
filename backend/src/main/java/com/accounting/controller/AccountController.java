package com.accounting.controller;

import com.accounting.dto.AccountDto;
import com.accounting.dto.AccountRequest;
import com.accounting.dto.ApiResponse;
import com.accounting.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Tag(name = "账户管理", description = "账户增删改查接口")
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    @Operation(summary = "获取账户列表")
    public ApiResponse<List<AccountDto>> list() {
        return ApiResponse.success(accountService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取账户详情")
    public ApiResponse<AccountDto> getById(@PathVariable Long id) {
        return ApiResponse.success(accountService.findById(id));
    }

    @PostMapping
    @Operation(summary = "创建账户")
    public ApiResponse<AccountDto> create(@Valid @RequestBody AccountRequest request) {
        return ApiResponse.success(accountService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改账户")
    public ApiResponse<AccountDto> update(@PathVariable Long id, @Valid @RequestBody AccountRequest request) {
        return ApiResponse.success(accountService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除账户")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        accountService.delete(id);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/adjust")
    @Operation(summary = "调整账户余额")
    public ApiResponse<AccountDto> adjustBalance(@PathVariable Long id, @RequestParam BigDecimal amount) {
        return ApiResponse.success(accountService.adjustBalance(id, amount));
    }
}
