package com.accounting.controller;

import com.accounting.dto.ApiResponse;
import com.accounting.dto.CategoryDto;
import com.accounting.dto.CategoryRequest;
import com.accounting.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(name = "分类管理", description = "收入/支出分类增删改查接口")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @Operation(summary = "获取分类列表")
    public ApiResponse<List<CategoryDto>> list(@RequestParam(required = false) String type,
                                                @RequestParam(required = false, defaultValue = "true") Boolean tree) {
        return ApiResponse.success(categoryService.findAll(type));
    }

    @PostMapping
    @Operation(summary = "创建分类")
    public ApiResponse<CategoryDto> create(@Valid @RequestBody CategoryRequest request) {
        return ApiResponse.success(categoryService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改分类")
    public ApiResponse<CategoryDto> update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return ApiResponse.success(categoryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除分类")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ApiResponse.success();
    }

    @PostMapping("/sort")
    @Operation(summary = "更新分类排序")
    public ApiResponse<Void> updateSortOrder(@RequestParam String type,
                                             @RequestParam(required = false) Long parentId,
                                             @RequestBody List<Long> ids) {
        categoryService.updateSortOrder(type, parentId, ids);
        return ApiResponse.success();
    }
}
