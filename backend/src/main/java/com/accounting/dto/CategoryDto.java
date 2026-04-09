package com.accounting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDto {

    private Long id;
    private String name;
    private String type;
    private String icon;
    private String color;
    private Integer sortOrder;
    private Long parentId;  // 父分类ID
    private String parentName;  // 父分类名称
    private Boolean isActive;
    private LocalDateTime createdAt;
    private List<CategoryDto> children;  // 子分类列表
}
