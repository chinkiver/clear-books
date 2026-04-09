package com.accounting.service;

import com.accounting.dto.AccountDto;
import com.accounting.dto.AccountRequest;
import com.accounting.entity.Account;
import com.accounting.exception.BusinessException;
import com.accounting.exception.ResourceNotFoundException;
import com.accounting.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserService userService;

    @Transactional(readOnly = true)
    public List<AccountDto> findAll() {
        Long userId = userService.getCurrentUserId();
        return accountRepository.findByUserIdAndIsActiveTrue(userId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AccountDto findById(Long id) {
        Long userId = userService.getCurrentUserId();
        Account account = accountRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Account", id));
        return convertToDto(account);
    }

    @Transactional
    public AccountDto create(AccountRequest request) {
        Long userId = userService.getCurrentUserId();

        Account account = Account.builder()
                .userId(userId)
                .name(request.getName())
                .type(request.getType())
                .balance(request.getBalance() != null ? request.getBalance() : BigDecimal.ZERO)
                .currency(request.getCurrency() != null ? request.getCurrency() : "CNY")
                .icon(request.getIcon())
                .color(request.getColor())
                .isActive(true)
                .build();

        accountRepository.save(account);
        return convertToDto(account);
    }

    @Transactional
    public AccountDto update(Long id, AccountRequest request) {
        Long userId = userService.getCurrentUserId();
        Account account = accountRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Account", id));

        account.setName(request.getName());
        account.setType(request.getType());
        if (request.getBalance() != null) {
            account.setBalance(request.getBalance());
        }
        if (request.getCurrency() != null) {
            account.setCurrency(request.getCurrency());
        }
        account.setIcon(request.getIcon());
        account.setColor(request.getColor());

        accountRepository.save(account);
        return convertToDto(account);
    }

    @Transactional
    public void delete(Long id) {
        Long userId = userService.getCurrentUserId();
        Account account = accountRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Account", id));

        account.setIsActive(false);
        accountRepository.save(account);
    }

    @Transactional
    public AccountDto adjustBalance(Long id, BigDecimal newBalance) {
        Long userId = userService.getCurrentUserId();
        Account account = accountRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Account", id));

        account.setBalance(newBalance);
        accountRepository.save(account);
        return convertToDto(account);
    }

    @Transactional(readOnly = true)
    public Account findEntityById(Long id) {
        Long userId = userService.getCurrentUserId();
        return accountRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Account", id));
    }

    private AccountDto convertToDto(Account account) {
        return AccountDto.builder()
                .id(account.getId())
                .name(account.getName())
                .type(account.getType())
                .balance(account.getBalance())
                .currency(account.getCurrency())
                .icon(account.getIcon())
                .color(account.getColor())
                .isActive(account.getIsActive())
                .createdAt(account.getCreatedAt())
                .build();
    }
}
