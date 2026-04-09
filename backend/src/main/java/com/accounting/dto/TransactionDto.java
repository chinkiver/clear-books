package com.accounting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDto {

    private Long id;
    private String type;
    private BigDecimal amount;
    private Long categoryId;
    private String categoryName;
    private Long accountId;
    private String accountName;
    private Long paymentMethodId;
    private String paymentMethodName;
    private Long toAccountId;
    private String toAccountName;
    private LocalDate transactionDate;
    private LocalTime transactionTime;
    private String description;
    private List<String> tags;
    private String location;
    private String attachments;
    private Boolean countAsExpense;  // 转账是否计入支出
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
