package com.accounting.controller;

import com.accounting.dto.*;
import com.accounting.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Tag(name = "流水管理", description = "交易流水增删改查接口")
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping
    @Operation(summary = "获取流水列表")
    public ApiResponse<PageResponse<TransactionDto>> list(TransactionQueryRequest request) {
        return ApiResponse.success(transactionService.findAll(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取流水详情")
    public ApiResponse<TransactionDto> getById(@PathVariable Long id) {
        return ApiResponse.success(transactionService.findById(id));
    }

    @PostMapping
    @Operation(summary = "创建流水")
    public ApiResponse<TransactionDto> create(@Valid @RequestBody TransactionRequest request) {
        return ApiResponse.success(transactionService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改流水")
    public ApiResponse<TransactionDto> update(@PathVariable Long id, @Valid @RequestBody TransactionRequest request) {
        return ApiResponse.success(transactionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除流水")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        transactionService.delete(id);
        return ApiResponse.success();
    }

    @GetMapping("/tags")
    @Operation(summary = "获取用户所有标签")
    public ApiResponse<List<String>> getTags() {
        return ApiResponse.success(transactionService.getUserTags());
    }

    @GetMapping("/tags/with-count")
    @Operation(summary = "获取用户所有标签及使用次数")
    public ApiResponse<List<java.util.Map<String, Object>>> getTagsWithCount() {
        return ApiResponse.success(transactionService.getUserTagsWithCount());
    }

    @PostMapping("/tags/preset")
    @Operation(summary = "添加预设标签")
    public ApiResponse<Void> addPresetTag(@RequestParam String tagName) {
        transactionService.addPresetTag(tagName);
        return ApiResponse.success();
    }

    @PutMapping("/tags/color")
    @Operation(summary = "更新标签颜色")
    public ApiResponse<Void> updateTagColor(@RequestParam String tagName, @RequestParam String color) {
        transactionService.updateTagColor(tagName, color);
        return ApiResponse.success();
    }

    @PutMapping("/tags/rename")
    @Operation(summary = "重命名标签")
    public ApiResponse<Void> renameTag(@RequestParam String oldName, @RequestParam String newName) {
        transactionService.renameTag(oldName, newName);
        return ApiResponse.success();
    }

    @DeleteMapping("/tags/{tagName}")
    @Operation(summary = "删除标签")
    public ApiResponse<Void> deleteTag(@PathVariable String tagName) {
        transactionService.deleteTag(tagName);
        return ApiResponse.success();
    }
}
