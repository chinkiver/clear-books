package com.accounting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 工作日 vs 周末消费分析 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeekdayAnalysisDto {
    
    // 汇总数据
    private BigDecimal weekdayExpense;      // 工作日总支出
    private BigDecimal weekendExpense;      // 周末总支出
    private BigDecimal weekdayIncome;       // 工作日总收入
    private BigDecimal weekendIncome;       // 周末总收入
    
    private int weekdayCount;               // 工作日天数
    private int weekendCount;               // 周末天数
    
    private BigDecimal weekdayAvgExpense;   // 工作日日均支出
    private BigDecimal weekendAvgExpense;   // 周末日均支出
    
    // 星期分布详情
    private List<DayDetail> dayDetails;     // 每天的数据
    
    // 分类对比（工作日 vs 周末 Top5）
    private List<CategoryCompare> weekdayTopCategories;
    private List<CategoryCompare> weekendTopCategories;
    
    // 洞察建议
    private String insight;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DayDetail {
        private String dayOfWeek;           // 星期几（周一、周二...）
        private int dayNumber;              // 1-7
        private BigDecimal expense;
        private BigDecimal income;
        private int transactionCount;
        private boolean isWeekend;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryCompare {
        private String categoryName;
        private BigDecimal amount;
        private double percentage;
    }
}
