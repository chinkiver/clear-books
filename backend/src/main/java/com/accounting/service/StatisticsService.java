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
}
