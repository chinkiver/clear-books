package com.accounting.controller;

import com.accounting.dto.ApiResponse;
import com.accounting.dto.ExpenseForecastDto;
import com.accounting.dto.StatisticsSummaryDto;
import com.accounting.dto.StatisticsTrendDto;
import com.accounting.dto.WeekdayAnalysisDto;
import com.accounting.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
@Tag(name = "统计报表", description = "收支统计和分析接口")
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/summary")
    @Operation(summary = "收支概览")
    public ApiResponse<StatisticsSummaryDto> summary(
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String date) {
        return ApiResponse.success(statisticsService.getSummary(period, date));
    }

    @GetMapping("/trend")
    @Operation(summary = "趋势分析")
    public ApiResponse<StatisticsTrendDto> trend(
            @RequestParam String type,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            @RequestParam(defaultValue = "DAY") String groupBy) {
        return ApiResponse.success(statisticsService.getTrend(type, startDate, endDate, groupBy));
    }

    @GetMapping("/by-category")
    @Operation(summary = "分类统计")
    public ApiResponse<List<StatisticsSummaryDto.CategorySummary>> byCategory(
            @RequestParam String type,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        return ApiResponse.success(statisticsService.getByCategory(type, startDate, endDate));
    }

    @GetMapping("/balance")
    @Operation(summary = "获取指定日期范围的总收支")
    public ApiResponse<Map<String, BigDecimal>> balance(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        return ApiResponse.success(statisticsService.getBalance(startDate, endDate));
    }
    
    @GetMapping("/weekday-analysis")
    @Operation(summary = "工作日 vs 周末消费分析")
    public ApiResponse<WeekdayAnalysisDto> weekdayAnalysis(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        return ApiResponse.success(statisticsService.getWeekdayAnalysis(startDate, endDate));
    }
    
    @GetMapping("/expense-forecast")
    @Operation(summary = "本月支出预测")
    public ApiResponse<ExpenseForecastDto> expenseForecast() {
        return ApiResponse.success(statisticsService.getExpenseForecast());
    }
}
