package com.accounting.service;

import com.accounting.dto.*;
import com.accounting.entity.Account;
import com.accounting.entity.Category;
import com.accounting.entity.PaymentMethod;
import com.accounting.entity.Transaction;
import com.accounting.exception.BusinessException;
import com.accounting.exception.ResourceNotFoundException;
import com.accounting.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserService userService;
    private final AccountService accountService;
    private final CategoryService categoryService;
    private final PaymentMethodService paymentMethodService;

    @Transactional(readOnly = true)
    public PageResponse<TransactionDto> findAll(TransactionQueryRequest request) {
        Long userId = userService.getCurrentUserId();
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), 
                Sort.by("transactionDate").descending().and(Sort.by("transactionTime").descending()));

        // 将String日期转换为LocalDate
        LocalDate startDate = null;
        LocalDate endDate = null;
        if (request.getStartDate() != null && !request.getStartDate().isEmpty()) {
            startDate = LocalDate.parse(request.getStartDate());
        }
        if (request.getEndDate() != null && !request.getEndDate().isEmpty()) {
            endDate = LocalDate.parse(request.getEndDate());
        }

        Page<Transaction> page = transactionRepository.findByFilters(
                userId, startDate, endDate,
                request.getType(), request.getCategoryId(), request.getAccountId(), pageable);

        return PageResponse.of(page.map(this::convertToDto));
    }

    @Transactional(readOnly = true)
    public TransactionDto findById(Long id) {
        Long userId = userService.getCurrentUserId();
        Transaction transaction = transactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Transaction", id));
        return convertToDto(transaction);
    }

    @Transactional
    public TransactionDto create(TransactionRequest request) {
        Long userId = userService.getCurrentUserId();

        // Validate account
        Account account = accountService.findEntityById(request.getAccountId());

        // Validate category if provided
        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryService.findEntityById(request.getCategoryId());
        }

        // Validate payment method if provided
        PaymentMethod paymentMethod = null;
        if (request.getPaymentMethodId() != null) {
            paymentMethod = paymentMethodService.findEntityById(request.getPaymentMethodId());
        }

        // Validate transfer
        Account toAccount = null;
        if ("TRANSFER".equals(request.getType()) && request.getToAccountId() != null) {
            toAccount = accountService.findEntityById(request.getToAccountId());
        }

        // Create transaction
        Transaction transaction = Transaction.builder()
                .userId(userId)
                .type(request.getType())
                .amount(request.getAmount())
                .categoryId(request.getCategoryId())
                .accountId(request.getAccountId())
                .paymentMethodId(request.getPaymentMethodId())
                .toAccountId(request.getToAccountId())
                .transactionDate(request.getTransactionDate())
                .transactionTime(request.getTransactionTime())
                .description(request.getDescription())
                .tags(request.getTags() != null ? String.join(",", request.getTags()) : null)
                .location(request.getLocation())
                .countAsExpense(request.getCountAsExpense() != null ? request.getCountAsExpense() : false)
                .build();

        transactionRepository.save(transaction);

        // Update account balance
        updateAccountBalance(account, request.getType(), request.getAmount(), true);
        if (toAccount != null) {
            updateAccountBalance(toAccount, "INCOME", request.getAmount(), true);
        }

        return convertToDto(transaction);
    }

    @Transactional
    public TransactionDto update(Long id, TransactionRequest request) {
        Long userId = userService.getCurrentUserId();
        Transaction transaction = transactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Transaction", id));

        // Revert old balance
        Account oldAccount = accountService.findEntityById(transaction.getAccountId());
        updateAccountBalance(oldAccount, transaction.getType(), transaction.getAmount(), false);
        if (transaction.getToAccountId() != null) {
            Account oldToAccount = accountService.findEntityById(transaction.getToAccountId());
            updateAccountBalance(oldToAccount, "INCOME", transaction.getAmount(), false);
        }

        // Validate new account
        Account account = accountService.findEntityById(request.getAccountId());

        // Validate new category if provided
        if (request.getCategoryId() != null) {
            categoryService.findEntityById(request.getCategoryId());
        }

        // Validate new payment method if provided
        if (request.getPaymentMethodId() != null) {
            paymentMethodService.findEntityById(request.getPaymentMethodId());
        }

        // Validate transfer
        Account toAccount = null;
        if ("TRANSFER".equals(request.getType()) && request.getToAccountId() != null) {
            toAccount = accountService.findEntityById(request.getToAccountId());
        }

        // Update transaction
        transaction.setType(request.getType());
        transaction.setAmount(request.getAmount());
        transaction.setCategoryId(request.getCategoryId());
        transaction.setAccountId(request.getAccountId());
        transaction.setPaymentMethodId(request.getPaymentMethodId());
        transaction.setToAccountId(request.getToAccountId());
        transaction.setTransactionDate(request.getTransactionDate());
        transaction.setTransactionTime(request.getTransactionTime());
        transaction.setDescription(request.getDescription());
        transaction.setTags(request.getTags() != null ? String.join(",", request.getTags()) : null);
        transaction.setLocation(request.getLocation());
        transaction.setCountAsExpense(request.getCountAsExpense() != null ? request.getCountAsExpense() : false);

        transactionRepository.save(transaction);

        // Apply new balance
        updateAccountBalance(account, request.getType(), request.getAmount(), true);
        if (toAccount != null) {
            updateAccountBalance(toAccount, "INCOME", request.getAmount(), true);
        }

        return convertToDto(transaction);
    }

    @Transactional
    public void delete(Long id) {
        Long userId = userService.getCurrentUserId();
        Transaction transaction = transactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Transaction", id));

        // Revert balance
        Account account = accountService.findEntityById(transaction.getAccountId());
        updateAccountBalance(account, transaction.getType(), transaction.getAmount(), false);
        if (transaction.getToAccountId() != null) {
            Account toAccount = accountService.findEntityById(transaction.getToAccountId());
            updateAccountBalance(toAccount, "INCOME", transaction.getAmount(), false);
        }

        transactionRepository.delete(transaction);
    }

    private void updateAccountBalance(Account account, String type, BigDecimal amount, boolean isAdd) {
        BigDecimal newBalance;
        if ("INCOME".equals(type)) {
            newBalance = isAdd ? account.getBalance().add(amount) : account.getBalance().subtract(amount);
        } else if ("EXPENSE".equals(type)) {
            newBalance = isAdd ? account.getBalance().subtract(amount) : account.getBalance().add(amount);
        } else {
            // TRANSFER - handled separately
            return;
        }
        account.setBalance(newBalance);
    }

    private TransactionDto convertToDto(Transaction transaction) {
        TransactionDto dto = TransactionDto.builder()
                .id(transaction.getId())
                .type(transaction.getType())
                .amount(transaction.getAmount())
                .categoryId(transaction.getCategoryId())
                .accountId(transaction.getAccountId())
                .paymentMethodId(transaction.getPaymentMethodId())
                .toAccountId(transaction.getToAccountId())
                .transactionDate(transaction.getTransactionDate())
                .transactionTime(transaction.getTransactionTime())
                .description(transaction.getDescription())
                .tags(transaction.getTags() != null ? Arrays.asList(transaction.getTags().split(",")) : null)
                .location(transaction.getLocation())
                .attachments(transaction.getAttachments())
                .countAsExpense(transaction.getCountAsExpense())
                .createdAt(transaction.getCreatedAt())
                .updatedAt(transaction.getUpdatedAt())
                .build();

        // Set names from related entities
        if (transaction.getCategoryId() != null) {
            try {
                Category category = categoryService.findEntityById(transaction.getCategoryId());
                dto.setCategoryName(category.getName());
            } catch (Exception e) {
                dto.setCategoryName("未知分类");
            }
        }

        try {
            Account account = accountService.findEntityById(transaction.getAccountId());
            dto.setAccountName(account.getName());
        } catch (Exception e) {
            dto.setAccountName("未知账户");
        }

        if (transaction.getPaymentMethodId() != null) {
            try {
                PaymentMethod pm = paymentMethodService.findEntityById(transaction.getPaymentMethodId());
                dto.setPaymentMethodName(pm.getName());
            } catch (Exception e) {
                dto.setPaymentMethodName("未知支付方式");
            }
        }

        if (transaction.getToAccountId() != null) {
            try {
                Account toAccount = accountService.findEntityById(transaction.getToAccountId());
                dto.setToAccountName(toAccount.getName());
            } catch (Exception e) {
                dto.setToAccountName("未知账户");
            }
        }

        return dto;
    }
}
