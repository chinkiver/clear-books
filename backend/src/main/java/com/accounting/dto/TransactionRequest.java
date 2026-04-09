package com.accounting.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class TransactionRequest {

    @NotBlank(message = "交易类型不能为空")
    private String type;  // INCOME, EXPENSE, TRANSFER

    @NotNull(message = "金额不能为空")
    @Positive(message = "金额必须大于0")
    private BigDecimal amount;

    private Long categoryId;

    @NotNull(message = "账户不能为空")
    private Long accountId;

    private Long paymentMethodId;

    private Long toAccountId;  // For transfer

    @NotNull(message = "交易日期不能为空")
    private LocalDate transactionDate;

    private LocalTime transactionTime;

    private String description;

    private List<String> tags;

    private String location;

    private Boolean countAsExpense;  // 转账是否计入支出
}
