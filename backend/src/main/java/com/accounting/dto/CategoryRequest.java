package com.accounting.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoryRequest {

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 50, message = "分类名称长度不能超过50")
    private String name;

    @NotBlank(message = "分类类型不能为空")
    @Pattern(regexp = "^(INCOME|EXPENSE)$", message = "类型必须是INCOME或EXPENSE")
    private String type;

    private String icon;

    private Integer sortOrder;

    private Long parentId;  // 父分类ID
}
