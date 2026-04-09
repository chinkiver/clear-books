package com.accounting.controller;

import com.accounting.dto.ApiResponse;
import com.accounting.dto.PaymentMethodDto;
import com.accounting.dto.PaymentMethodRequest;
import com.accounting.service.PaymentMethodService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payment-methods")
@RequiredArgsConstructor
@Tag(name = "支付方式管理", description = "支付方式增删改查接口")
public class PaymentMethodController {

    private final PaymentMethodService paymentMethodService;

    @GetMapping
    @Operation(summary = "获取支付方式列表")
    public ApiResponse<List<PaymentMethodDto>> list() {
        return ApiResponse.success(paymentMethodService.findAll());
    }

    @PostMapping
    @Operation(summary = "创建支付方式")
    public ApiResponse<PaymentMethodDto> create(@Valid @RequestBody PaymentMethodRequest request) {
        return ApiResponse.success(paymentMethodService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改支付方式")
    public ApiResponse<PaymentMethodDto> update(@PathVariable Long id, @Valid @RequestBody PaymentMethodRequest request) {
        return ApiResponse.success(paymentMethodService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除支付方式")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        paymentMethodService.delete(id);
        return ApiResponse.success();
    }

    @PostMapping("/sort")
    @Operation(summary = "更新排序")
    public ApiResponse<Void> updateSortOrder(@RequestBody Map<String, List<Long>> request) {
        paymentMethodService.updateSortOrder(request.get("ids"));
        return ApiResponse.success();
    }
}
