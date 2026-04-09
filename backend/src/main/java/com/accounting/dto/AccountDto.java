package com.accounting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountDto {

    private Long id;
    private String name;
    private String type;
    private BigDecimal balance;
    private String currency;
    private String icon;
    private String color;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
