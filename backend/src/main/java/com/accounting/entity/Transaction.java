package com.accounting.entity;

import javax.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 10)
    private String type;  // INCOME, EXPENSE, TRANSFER

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "payment_method_id")
    private Long paymentMethodId;

    @Column(name = "to_account_id")
    private Long toAccountId;  // For transfer type

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "transaction_time")
    private LocalTime transactionTime;

    @Column(length = 500)
    private String description;

    @Column(length = 200)
    private String tags;

    @Column(length = 100)
    private String location;

    @Column(length = 500)
    private String attachments;

    @Column(name = "count_as_expense")
    private Boolean countAsExpense;  // 转账是否计入支出

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (transactionDate == null) {
            transactionDate = LocalDate.now();
        }
        if (transactionTime == null) {
            transactionTime = LocalTime.now();
        }
        if (countAsExpense == null) {
            countAsExpense = false;  // 默认不计入支出
        }
    }
}
