package com.accounting.service;

import com.accounting.dto.PaymentMethodDto;
import com.accounting.dto.PaymentMethodRequest;
import com.accounting.entity.PaymentMethod;
import com.accounting.exception.ResourceNotFoundException;
import com.accounting.repository.PaymentMethodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentMethodService {

    private final PaymentMethodRepository paymentMethodRepository;
    private final UserService userService;

    @Transactional(readOnly = true)
    public List<PaymentMethodDto> findAll() {
        Long userId = userService.getCurrentUserId();
        return paymentMethodRepository.findByUserIdAndIsActiveTrueOrderBySortOrderAsc(userId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public PaymentMethodDto create(PaymentMethodRequest request) {
        Long userId = userService.getCurrentUserId();

        // 获取当前最大排序值
        List<PaymentMethod> existing = paymentMethodRepository.findByUserIdOrderBySortOrderAsc(userId);
        int maxSort = existing.stream()
                .mapToInt(pm -> pm.getSortOrder() != null ? pm.getSortOrder() : 0)
                .max()
                .orElse(0);

        PaymentMethod paymentMethod = PaymentMethod.builder()
                .userId(userId)
                .name(request.getName())
                .type(request.getType() != null ? request.getType() : request.getName())
                .icon(request.getIcon())
                .sortOrder(maxSort + 1)
                .isActive(true)
                .build();

        paymentMethodRepository.save(paymentMethod);
        return convertToDto(paymentMethod);
    }

    @Transactional
    public PaymentMethodDto update(Long id, PaymentMethodRequest request) {
        Long userId = userService.getCurrentUserId();
        PaymentMethod paymentMethod = paymentMethodRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> ResourceNotFoundException.of("PaymentMethod", id));

        paymentMethod.setName(request.getName());
        if (request.getType() != null) {
            paymentMethod.setType(request.getType());
        }
        paymentMethod.setIcon(request.getIcon());

        paymentMethodRepository.save(paymentMethod);
        return convertToDto(paymentMethod);
    }

    @Transactional
    public void delete(Long id) {
        Long userId = userService.getCurrentUserId();
        PaymentMethod paymentMethod = paymentMethodRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> ResourceNotFoundException.of("PaymentMethod", id));

        paymentMethod.setIsActive(false);
        paymentMethodRepository.save(paymentMethod);
    }

    @Transactional(readOnly = true)
    public PaymentMethod findEntityById(Long id) {
        Long userId = userService.getCurrentUserId();
        return paymentMethodRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> ResourceNotFoundException.of("PaymentMethod", id));
    }

    @Transactional
    public void updateSortOrder(List<Long> ids) {
        Long userId = userService.getCurrentUserId();
        for (int i = 0; i < ids.size(); i++) {
            PaymentMethod pm = paymentMethodRepository.findByIdAndUserId(ids.get(i), userId)
                    .orElse(null);
            if (pm != null) {
                pm.setSortOrder(i);
                paymentMethodRepository.save(pm);
            }
        }
    }

    private PaymentMethodDto convertToDto(PaymentMethod paymentMethod) {
        return PaymentMethodDto.builder()
                .id(paymentMethod.getId())
                .name(paymentMethod.getName())
                .type(paymentMethod.getType())
                .icon(paymentMethod.getIcon())
                .sortOrder(paymentMethod.getSortOrder())
                .isActive(paymentMethod.getIsActive())
                .createdAt(paymentMethod.getCreatedAt())
                .build();
    }
}
