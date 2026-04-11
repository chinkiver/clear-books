package com.accounting.service;

import com.accounting.dto.StatisticsSummaryDto;
import com.accounting.dto.StatisticsTrendDto;
import com.accounting.entity.Category;
import com.accounting.entity.Transaction;
import com.accounting.repository.CategoryRepository;
import com.accounting.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.accounting.dto.WeekdayAnalysisDto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.IsoFields;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final UserService userService;

    @Transactional(readOnly = true)
    public StatisticsSummaryDto getSummary(String period, String date) {
        Long userId = userService.getCurrentUserId();
        
        LocalDate[] range = calculateDateRange(period, date);
        LocalDate startDate = range[0];
        LocalDate endDate = range[1];

        BigDecimal totalIncome = transactionRepository.sumAmountByTypeAndDateRange(userId, "INCOME", startDate, endDate);
        BigDecimal totalExpense = transactionRepository.sumAmountByTypeAndDateRange(userId, "EXPENSE", startDate, endDate);
        // 加上计入支出的转账金额
        BigDecimal transferAsExpense = transactionRepository.sumTransferAsExpense(userId, startDate, endDate);
        if (totalExpense == null) totalExpense = BigDecimal.ZERO;
        if (transferAsExpense == null) transferAsExpense = BigDecimal.ZERO;
        totalExpense = totalExpense.add(transferAsExpense);

        List<StatisticsSummaryDto.CategorySummary> incomeByCategory = getCategorySummary(userId, "INCOME", startDate, endDate);
        List<StatisticsSummaryDto.CategorySummary> expenseByCategory = getCategorySummary(userId, "EXPENSE", startDate, endDate);

        return StatisticsSummaryDto.builder()
                .period(period != null ? period : "MONTH")
                .startDate(startDate.toString())
                .endDate(endDate.toString())
                .totalIncome(totalIncome != null ? totalIncome : BigDecimal.ZERO)
                .totalExpense(totalExpense != null ? totalExpense : BigDecimal.ZERO)
                .balance(totalIncome.subtract(totalExpense))
                .incomeByCategory(incomeByCategory)
                .expenseByCategory(expenseByCategory)
                .build();
    }

    @Transactional(readOnly = true)
    public StatisticsTrendDto getTrend(String type, LocalDate startDate, LocalDate endDate, String groupBy) {
        Long userId = userService.getCurrentUserId();

        List<StatisticsTrendDto.TrendItem> data = new ArrayList<>();
        
        List<Transaction> transactions = transactionRepository.findByUserIdAndTransactionDateBetweenOrderByTransactionDateDesc(
                userId, startDate, endDate);

        // Group transactions
        Map<String, List<Transaction>> grouped = new LinkedHashMap<>();
        
        for (Transaction t : transactions) {
            if (!type.equals(t.getType())) continue;
            
            String key;
            switch (groupBy) {
                case "WEEK":
                    key = t.getTransactionDate().with(DayOfWeek.MONDAY).toString();
                    break;
                case "MONTH":
                    key = t.getTransactionDate().format(DateTimeFormatter.ofPattern("yyyy-MM"));
                    break;
                default: // DAY
                    key = t.getTransactionDate().toString();
            }
            
            if (!grouped.containsKey(key)) {
                grouped.put(key, new ArrayList<>());
            }
            grouped.get(key).add(t);
        }

        // Sort keys
        List<String> sortedKeys = new ArrayList<>(grouped.keySet());
        Collections.sort(sortedKeys);

        for (String key : sortedKeys) {
            List<Transaction> items = grouped.get(key);
            BigDecimal sum = items.stream()
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            data.add(StatisticsTrendDto.TrendItem.builder()
                    .date(key)
                    .amount(sum)
                    .count(items.size())
                    .build());
        }

        return StatisticsTrendDto.builder()
                .type(type)
                .groupBy(groupBy)
                .startDate(startDate.toString())
                .endDate(endDate.toString())
                .data(data)
                .build();
    }

    @Transactional(readOnly = true)
    public List<StatisticsSummaryDto.CategorySummary> getByCategory(String type, LocalDate startDate, LocalDate endDate) {
        Long userId = userService.getCurrentUserId();
        return getCategorySummary(userId, type, startDate, endDate);
    }

    @Transactional(readOnly = true)
    public Map<String, BigDecimal> getBalance(LocalDate startDate, LocalDate endDate) {
        Long userId = userService.getCurrentUserId();
        
        BigDecimal income = transactionRepository.sumAmountByTypeAndDateRange(userId, "INCOME", startDate, endDate);
        BigDecimal expense = transactionRepository.sumAmountByTypeAndDateRange(userId, "EXPENSE", startDate, endDate);

        Map<String, BigDecimal> result = new HashMap<>();
        result.put("income", income != null ? income : BigDecimal.ZERO);
        result.put("expense", expense != null ? expense : BigDecimal.ZERO);
        result.put("balance", (income != null ? income : BigDecimal.ZERO).subtract(expense != null ? expense : BigDecimal.ZERO));
        
        return result;
    }

    private LocalDate[] calculateDateRange(String period, String date) {
        LocalDate now = LocalDate.now();
        LocalDate startDate;
        LocalDate endDate;

        if (period == null || "MONTH".equals(period)) {
            YearMonth ym = date != null ? YearMonth.parse(date) : YearMonth.now();
            startDate = ym.atDay(1);
            endDate = ym.atEndOfMonth();
        } else if ("WEEK".equals(period)) {
            if (date != null) {
                startDate = LocalDate.parse(date).with(DayOfWeek.MONDAY);
            } else {
                startDate = now.with(DayOfWeek.MONDAY);
            }
            endDate = startDate.plusDays(6);
        } else if ("QUARTER".equals(period)) {
            int year = date != null ? Integer.parseInt(date.substring(0, 4)) : now.getYear();
            int quarter = date != null ? Integer.parseInt(date.substring(6)) : now.get(IsoFields.QUARTER_OF_YEAR);
            startDate = LocalDate.of(year, 1, 1).plusMonths((quarter - 1) * 3);
            endDate = startDate.plusMonths(3).minusDays(1);
        } else if ("YEAR".equals(period)) {
            int year = date != null ? Integer.parseInt(date) : now.getYear();
            startDate = LocalDate.of(year, 1, 1);
            endDate = LocalDate.of(year, 12, 31);
        } else {
            startDate = now.withDayOfMonth(1);
            endDate = YearMonth.now().atEndOfMonth();
        }

        return new LocalDate[]{startDate, endDate};
    }

    private List<StatisticsSummaryDto.CategorySummary> getCategorySummary(Long userId, String type, 
                                                                           LocalDate startDate, LocalDate endDate) {
        List<Object[]> results = transactionRepository.sumAmountByCategory(userId, type, startDate, endDate);
        
        // Get all categories for the user
        List<Category> categories = categoryRepository.findByUserIdAndType(userId, type);
        Map<Long, Category> categoryMap = categories.stream()
                .collect(Collectors.toMap(Category::getId, c -> c));

        // Calculate total
        BigDecimal total = results.stream()
                .map(r -> (BigDecimal) r[1])
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Build summary
        List<StatisticsSummaryDto.CategorySummary> summaries = new ArrayList<>();
        for (Object[] result : results) {
            Long categoryId = (Long) result[0];
            BigDecimal amount = (BigDecimal) result[1];
            Category category = categoryMap.get(categoryId);

            if (category != null) {
                double percentage = total.compareTo(BigDecimal.ZERO) > 0 
                        ? amount.multiply(new BigDecimal("100")).divide(total, 2, RoundingMode.HALF_UP).doubleValue()
                        : 0;

                summaries.add(StatisticsSummaryDto.CategorySummary.builder()
                        .categoryId(categoryId)
                        .categoryName(category.getName())
                        .icon(category.getIcon())
                        .color(category.getColor())
                        .amount(amount)
                        .percentage(percentage)
                        .build());
            }
        }

        // Sort by amount desc
        summaries.sort((a, b) -> b.getAmount().compareTo(a.getAmount()));
        
        return summaries;
    }

    /**
     * 工作日 vs 周末消费分析
     */
    @Transactional(readOnly = true)
    public WeekdayAnalysisDto getWeekdayAnalysis(LocalDate startDate, LocalDate endDate) {
        Long userId = userService.getCurrentUserId();
        
        // 获取日期范围内的所有交易
        List<Transaction> transactions = transactionRepository
                .findByUserIdAndTransactionDateBetweenOrderByTransactionDateDesc(userId, startDate, endDate);
        
        // 按星期几分组统计
        Map<Integer, WeekdayAnalysisDto.DayDetail> dayStats = new LinkedHashMap<>();
        String[] dayNames = {"", "周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        
        // 初始化每天的统计
        for (int i = 1; i <= 7; i++) {
            dayStats.put(i, WeekdayAnalysisDto.DayDetail.builder()
                    .dayOfWeek(dayNames[i])
                    .dayNumber(i)
                    .expense(BigDecimal.ZERO)
                    .income(BigDecimal.ZERO)
                    .transactionCount(0)
                    .isWeekend(i >= 6)
                    .build());
        }
        
        // 统计实际有交易的天数
        Set<LocalDate> weekdayDates = new HashSet<>();
        Set<LocalDate> weekendDates = new HashSet<>();
        
        for (Transaction t : transactions) {
            int dayOfWeek = t.getTransactionDate().getDayOfWeek().getValue();
            WeekdayAnalysisDto.DayDetail detail = dayStats.get(dayOfWeek);
            
            if ("EXPENSE".equals(t.getType())) {
                detail.setExpense(detail.getExpense().add(t.getAmount()));
            } else if ("INCOME".equals(t.getType())) {
                detail.setIncome(detail.getIncome().add(t.getAmount()));
            }
            detail.setTransactionCount(detail.getTransactionCount() + 1);
            
            // 记录有交易的具体日期
            if (dayOfWeek >= 6) {
                weekendDates.add(t.getTransactionDate());
            } else {
                weekdayDates.add(t.getTransactionDate());
            }
        }
        
        // 如果没有交易数据，计算日期范围内的所有天数
        if (weekdayDates.isEmpty() && weekendDates.isEmpty()) {
            LocalDate current = startDate;
            while (!current.isAfter(endDate)) {
                int dayOfWeek = current.getDayOfWeek().getValue();
                if (dayOfWeek >= 6) {
                    weekendDates.add(current);
                } else {
                    weekdayDates.add(current);
                }
                current = current.plusDays(1);
            }
        }
        
        // 计算汇总
        BigDecimal weekdayExpense = BigDecimal.ZERO;
        BigDecimal weekendExpense = BigDecimal.ZERO;
        BigDecimal weekdayIncome = BigDecimal.ZERO;
        BigDecimal weekendIncome = BigDecimal.ZERO;
        
        for (int i = 1; i <= 5; i++) {
            weekdayExpense = weekdayExpense.add(dayStats.get(i).getExpense());
            weekdayIncome = weekdayIncome.add(dayStats.get(i).getIncome());
        }
        for (int i = 6; i <= 7; i++) {
            weekendExpense = weekendExpense.add(dayStats.get(i).getExpense());
            weekendIncome = weekendIncome.add(dayStats.get(i).getIncome());
        }
        
        int weekdayCount = weekdayDates.size();
        int weekendCount = weekendDates.size();
        
        // 计算日均（避免除0）
        BigDecimal weekdayAvgExpense = weekdayCount > 0 
                ? weekdayExpense.divide(new BigDecimal(weekdayCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal weekendAvgExpense = weekendCount > 0 
                ? weekendExpense.divide(new BigDecimal(weekendCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        
        // 获取分类对比数据
        List<WeekdayAnalysisDto.CategoryCompare> weekdayTopCategories = getTopCategoriesByDayType(
                userId, startDate, endDate, true, 5);
        List<WeekdayAnalysisDto.CategoryCompare> weekendTopCategories = getTopCategoriesByDayType(
                userId, startDate, endDate, false, 5);
        
        // 生成洞察建议
        String insight = generateWeekdayInsight(weekdayAvgExpense, weekendAvgExpense, 
                weekdayExpense, weekendExpense, weekdayCount, weekendCount);
        
        return WeekdayAnalysisDto.builder()
                .weekdayExpense(weekdayExpense)
                .weekendExpense(weekendExpense)
                .weekdayIncome(weekdayIncome)
                .weekendIncome(weekendIncome)
                .weekdayCount(weekdayCount)
                .weekendCount(weekendCount)
                .weekdayAvgExpense(weekdayAvgExpense)
                .weekendAvgExpense(weekendAvgExpense)
                .dayDetails(new ArrayList<>(dayStats.values()))
                .weekdayTopCategories(weekdayTopCategories)
                .weekendTopCategories(weekendTopCategories)
                .insight(insight)
                .build();
    }
    
    /**
     * 获取工作日或周末的 Top 分类
     */
    private List<WeekdayAnalysisDto.CategoryCompare> getTopCategoriesByDayType(
            Long userId, LocalDate startDate, LocalDate endDate, boolean isWeekday, int limit) {
        
        List<Transaction> transactions = transactionRepository
                .findByUserIdAndTransactionDateBetweenOrderByTransactionDateDesc(userId, startDate, endDate);
        
        // 过滤工作日或周末的交易
        List<Transaction> filtered = transactions.stream()
                .filter(t -> "EXPENSE".equals(t.getType()))
                .filter(t -> {
                    int dayOfWeek = t.getTransactionDate().getDayOfWeek().getValue();
                    boolean isWeekend = dayOfWeek >= 6;
                    return isWeekday ? !isWeekend : isWeekend;
                })
                .collect(Collectors.toList());
        
        // 按分类汇总
        Map<Long, BigDecimal> categoryAmounts = new HashMap<>();
        for (Transaction t : filtered) {
            Long categoryId = t.getCategoryId();
            if (categoryId != null) {
                categoryAmounts.merge(categoryId, t.getAmount(), BigDecimal::add);
            }
        }
        
        // 获取分类名称
        List<Category> categories = categoryRepository.findByUserId(userId);
        Map<Long, String> categoryNames = categories.stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));
        
        // 计算总额和排序
        BigDecimal total = categoryAmounts.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return categoryAmounts.entrySet().stream()
                .map(e -> {
                    String name = categoryNames.getOrDefault(e.getKey(), "未知分类");
                    double percentage = total.compareTo(BigDecimal.ZERO) > 0
                            ? e.getValue().multiply(new BigDecimal("100")).divide(total, 2, RoundingMode.HALF_UP).doubleValue()
                            : 0;
                    return WeekdayAnalysisDto.CategoryCompare.builder()
                            .categoryName(name)
                            .amount(e.getValue())
                            .percentage(percentage)
                            .build();
                })
                .sorted((a, b) -> b.getAmount().compareTo(a.getAmount()))
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    /**
     * 生成工作日分析洞察
     */
    private String generateWeekdayInsight(BigDecimal weekdayAvg, BigDecimal weekendAvg,
                                          BigDecimal weekdayTotal, BigDecimal weekendTotal,
                                          int weekdayCount, int weekendCount) {
        if (weekdayCount == 0 && weekendCount == 0) {
            return "暂无数据，开始记账吧！";
        }
        
        StringBuilder insight = new StringBuilder();
        
        // 比较日均消费
        if (weekdayAvg.compareTo(BigDecimal.ZERO) > 0 && weekendAvg.compareTo(BigDecimal.ZERO) > 0) {
            double ratio = weekendAvg.doubleValue() / weekdayAvg.doubleValue();
            if (ratio > 2) {
                insight.append("周末消费是工作日的").append(String.format("%.1f", ratio)).append("倍，建议控制周末开销；");
            } else if (ratio < 0.8) {
                insight.append("周末消费较少，宅家省钱模式开启；");
            } else {
                insight.append("工作日和周末消费较为均衡；");
            }
        }
        
        // 总消费对比
        BigDecimal total = weekdayTotal.add(weekendTotal);
        if (total.compareTo(BigDecimal.ZERO) > 0) {
            double weekendPercent = weekendTotal.multiply(new BigDecimal("100"))
                    .divide(total, 2, RoundingMode.HALF_UP).doubleValue();
            if (weekendPercent > 40) {
                insight.append("周末消费占总支出的").append(String.format("%.0f", weekendPercent)).append("%，占比偏高。");
            }
        }
        
        return insight.toString();
    }
}
