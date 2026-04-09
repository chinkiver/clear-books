package com.accounting.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.Data;

@Data
public class PaymentMethodRequest {

    @NotBlank(message = "支付方式不能为空")
    @Size(max = 50, message = "支付方式长度不能超过50")
    private String name;

    private String type;

    private String icon;
}
