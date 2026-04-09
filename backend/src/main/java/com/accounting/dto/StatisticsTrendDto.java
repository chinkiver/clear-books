package com.accounting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatisticsTrendDto {

    private String type;  // INCOME, EXPENSE
    private String groupBy;  // DAY, WEEK, MONTH
    private String startDate;
    private String endDate;
    private List<TrendItem> data;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendItem {
        private String date;
        private BigDecimal amount;
        private Integer count;
    }
}
