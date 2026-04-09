package com.accounting.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AccountRequest {

    @NotBlank(message = "账户名称不能为空")
    @Size(max = 50, message = "账户名称长度不能超过50")
    private String name;

    @NotBlank(message = "账户类型不能为空")
    private String type;

    private BigDecimal balance;

    private String currency;

    private String icon;

    private String color;
}
