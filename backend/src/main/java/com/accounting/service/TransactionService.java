package com.accounting.service;

import com.accounting.dto.*;
import com.accounting.entity.Account;
import com.accounting.entity.Category;
import com.accounting.entity.PaymentMethod;
import com.accounting.entity.Transaction;
import com.accounting.entity.SystemSetting;
import com.accounting.exception.BusinessException;
import com.accounting.exception.ResourceNotFoundException;
import com.accounting.repository.SystemSettingRepository;
import com.accounting.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final UserService userService;
    private final AccountService accountService;
    private final CategoryService categoryService;
    private final PaymentMethodService paymentMethodService;

    private static final String PRESET_TAGS_KEY_PREFIX = "user.preset_tags.";
    private static final String TAG_COLORS_KEY_PREFIX = "user.tag_colors.";
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String getPresetTagsKey(Long userId) {
        return PRESET_TAGS_KEY_PREFIX + userId;
    }

    private String getTagColorsKey(Long userId) {
        return TAG_COLORS_KEY_PREFIX + userId;
    }

    private List<String> getPresetTags(Long userId) {
        Optional<SystemSetting> setting = systemSettingRepository.findByKey(getPresetTagsKey(userId));
        if (setting.isPresent() && setting.get().getValue() != null && !setting.get().getValue().isEmpty()) {
            return Arrays.stream(setting.get().getValue().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    private void savePresetTags(Long userId, List<String> tags) {
        String key = getPresetTagsKey(userId);
        Optional<SystemSetting> existing = systemSettingRepository.findByKey(key);
        String value = tags.stream().distinct().collect(Collectors.joining(","));
        if (existing.isPresent()) {
            SystemSetting setting = existing.get();
            setting.setValue(value);
            systemSettingRepository.save(setting);
        } else {
            systemSettingRepository.save(SystemSetting.builder()
                    .key(key)
                    .value(value)
                    .description("用户预设标签")
                    .build());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> getTagColors(Long userId) {
        Optional<SystemSetting> setting = systemSettingRepository.findByKey(getTagColorsKey(userId));
        if (setting.isPresent() && setting.get().getValue() != null && !setting.get().getValue().isEmpty()) {
            try {
                return objectMapper.readValue(setting.get().getValue(), new TypeReference<Map<String, String>>() {});
            } catch (Exception e) {
                return new LinkedHashMap<>();
            }
        }
        return new LinkedHashMap<>();
    }

    private void saveTagColors(Long userId, Map<String, String> colors) {
        String key = getTagColorsKey(userId);
        Optional<SystemSetting> existing = systemSettingRepository.findByKey(key);
        try {
            String value = objectMapper.writeValueAsString(colors);
            if (existing.isPresent()) {
                SystemSetting setting = existing.get();
                setting.setValue(value);
                systemSettingRepository.save(setting);
            } else {
                systemSettingRepository.save(SystemSetting.builder()
                        .key(key)
                        .value(value)
                        .description("用户标签颜色配置")
                        .build());
            }
        } catch (Exception e) {
            throw new BusinessException("保存标签颜色失败");
        }
    }

    @Transactional
    public void updateTagColor(String tagName, String color) {
        Long userId = userService.getCurrentUserId();
        if (tagName == null || tagName.trim().isEmpty()) {
            throw new BusinessException("标签名称不能为空");
        }
        Map<String, String> colors = getTagColors(userId);
        colors.put(tagName.trim(), color);
        saveTagColors(userId, colors);
    }

    @Transactional
    public void addPresetTag(String tagName) {
        Long userId = userService.getCurrentUserId();
        if (tagName == null || tagName.trim().isEmpty()) {
            throw new BusinessException("标签名称不能为空");
        }
        List<String> tags = getPresetTags(userId);
        String trimmed = tagName.trim();
        if (tags.contains(trimmed)) {
            throw new BusinessException("标签已存在");
        }
        tags.add(trimmed);
        savePresetTags(userId, tags);
    }

    @Transactional(readOnly = true)
    public List<String> getAllTags() {
        Long userId = userService.getCurrentUserId();
        List<String> tags = getPresetTags(userId);
        tags.addAll(getUserTags());
        return tags.stream().distinct().collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PageResponse<TransactionDto> findAll(TransactionQueryRequest request) {
        Long userId = userService.getCurrentUserId();
        Sort sort = Sort.by(
                Sort.Order.desc("transactionDate"),
                Sort.Order.desc("transactionTime"),
                Sort.Order.asc("type"),
                Sort.Order.asc("categoryId"),
                Sort.Order.asc("accountId"),
                Sort.Order.desc("amount")
        );
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);

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
                request.getType(), request.getCategoryId(), request.getAccountId(),
                request.getAmount(), request.getTag(), pageable);

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

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getUserTagsWithCount() {
        Long userId = userService.getCurrentUserId();
        List<Transaction> transactions = transactionRepository.findByUserIdWithTags(userId);
        Map<String, Integer> tagCounts = new LinkedHashMap<>();
        Map<String, BigDecimal> tagAmounts = new LinkedHashMap<>();
        for (Transaction t : transactions) {
            if (t.getTags() == null || t.getTags().isEmpty()) continue;
            for (String tag : t.getTags().split(",")) {
                String trimmed = tag.trim();
                if (!trimmed.isEmpty()) {
                    tagCounts.put(trimmed, tagCounts.getOrDefault(trimmed, 0) + 1);
                    tagAmounts.put(trimmed, tagAmounts.getOrDefault(trimmed, BigDecimal.ZERO).add(t.getAmount()));
                }
            }
        }
        // 合并预设标签（无计数或计数为0）
        for (String presetTag : getPresetTags(userId)) {
            if (!tagCounts.containsKey(presetTag)) {
                tagCounts.put(presetTag, 0);
                tagAmounts.put(presetTag, BigDecimal.ZERO);
            }
        }
        Map<String, String> colors = getTagColors(userId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : tagCounts.entrySet()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("name", entry.getKey());
            map.put("count", entry.getValue());
            map.put("totalAmount", tagAmounts.getOrDefault(entry.getKey(), BigDecimal.ZERO));
            map.put("color", colors.getOrDefault(entry.getKey(), null));
            result.add(map);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<String> getUserTags() {
        Long userId = userService.getCurrentUserId();
        List<String> tagStrings = transactionRepository.findDistinctTagsByUserId(userId);
        Set<String> tags = new LinkedHashSet<>();
        for (String tagStr : tagStrings) {
            if (tagStr != null && !tagStr.isEmpty()) {
                for (String tag : tagStr.split(",")) {
                    String trimmed = tag.trim();
                    if (!trimmed.isEmpty()) {
                        tags.add(trimmed);
                    }
                }
            }
        }
        tags.addAll(getPresetTags(userId));
        return new ArrayList<>(tags);
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

    @Transactional
    public void renameTag(String oldName, String newName) {
        Long userId = userService.getCurrentUserId();
        if (oldName == null || oldName.isEmpty() || newName == null || newName.isEmpty() || oldName.equals(newName)) {
            return;
        }
        List<Transaction> transactions = transactionRepository.findByUserIdAndTag(userId, oldName);
        for (Transaction t : transactions) {
            if (t.getTags() == null || t.getTags().isEmpty()) continue;
            List<String> tags = Arrays.asList(t.getTags().split(","));
            List<String> newTags = new ArrayList<>();
            for (String tag : tags) {
                String trimmed = tag.trim();
                if (trimmed.equals(oldName)) {
                    newTags.add(newName);
                } else if (!trimmed.isEmpty()) {
                    newTags.add(trimmed);
                }
            }
            t.setTags(String.join(",", newTags));
            transactionRepository.save(t);
        }
        // 同步更新预设标签
        List<String> presetTags = getPresetTags(userId);
        if (presetTags.contains(oldName)) {
            presetTags.remove(oldName);
            if (!presetTags.contains(newName)) {
                presetTags.add(newName);
            }
            savePresetTags(userId, presetTags);
        }
        // 同步更新颜色配置
        Map<String, String> colors = getTagColors(userId);
        if (colors.containsKey(oldName)) {
            String color = colors.remove(oldName);
            colors.put(newName, color);
            saveTagColors(userId, colors);
        }
    }

    @Transactional
    public void deleteTag(String tagName) {
        Long userId = userService.getCurrentUserId();
        if (tagName == null || tagName.isEmpty()) {
            return;
        }
        List<Transaction> transactions = transactionRepository.findByUserIdAndTag(userId, tagName);
        for (Transaction t : transactions) {
            if (t.getTags() == null || t.getTags().isEmpty()) continue;
            List<String> tags = Arrays.asList(t.getTags().split(","));
            List<String> newTags = tags.stream()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty() && !s.equals(tagName))
                    .collect(Collectors.toList());
            t.setTags(newTags.isEmpty() ? null : String.join(",", newTags));
            transactionRepository.save(t);
        }
        // 同步删除预设标签
        List<String> presetTags = getPresetTags(userId);
        if (presetTags.contains(tagName)) {
            presetTags.remove(tagName);
            savePresetTags(userId, presetTags);
        }
        // 同步删除颜色配置
        Map<String, String> colors = getTagColors(userId);
        if (colors.containsKey(tagName)) {
            colors.remove(tagName);
            saveTagColors(userId, colors);
        }
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
