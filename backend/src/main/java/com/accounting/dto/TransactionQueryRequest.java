package com.accounting.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class TransactionQueryRequest {

    private String startDate;  // 使用String接收，避免转换问题
    private String endDate;
    private String type;
    private Long categoryId;
    private Long accountId;
    private BigDecimal amount;  // 精确金额查询
    private String tag;  // 标签查询
    private Integer page = 0;
    private Integer size = 20;
}
