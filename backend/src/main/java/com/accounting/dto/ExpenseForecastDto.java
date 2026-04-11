package com.accounting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 支出预测 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseForecastDto {
    
    // 当前月份信息
    private String currentMonth;           // 当前月份 (2026-04)
    private int totalDays;                 // 本月总天数
    private int passedDays;                // 已过天数
    private int remainingDays;             // 剩余天数
    
    // 实际支出
    private BigDecimal actualExpense;      // 本月已产生支出
    private BigDecimal dailyAverage;       // 日均支出
    
    // 预测数据
    private BigDecimal forecastExpense;    // 预计本月总支出
    private BigDecimal forecastRangeLow;   // 保守预测（最低）
    private BigDecimal forecastRangeHigh;  // 乐观预测（最高）
    
    // 对比分析
    private BigDecimal lastMonthExpense;   // 上月同期支出
    private BigDecimal lastMonthTotal;     // 上月总支出
    private BigDecimal avgMonthlyExpense;  // 近3个月平均支出
    
    // 状态评估
    private String status;                 // 状态：NORMAL(正常)、WARNING(警告)、DANGER(危险)
    private String statusText;             // 状态描述
    private String suggestion;             // 建议
    
    // 趋势数据（用于图表）
    private List<DailyForecast> dailyForecasts;  // 每日预测数据
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyForecast {
        private LocalDate date;            // 日期
        private BigDecimal actual;         // 实际支出（已过日期）
        private BigDecimal forecast;       // 预测支出（未来日期）
        private BigDecimal cumulative;     // 累计支出
        private boolean isPassed;          // 是否已过
        private boolean isToday;           // 是否今天
    }
}
