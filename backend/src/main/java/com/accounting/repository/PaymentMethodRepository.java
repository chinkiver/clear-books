package com.accounting.repository;

import com.accounting.entity.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {

    List<PaymentMethod> findByUserIdAndIsActiveTrueOrderBySortOrderAsc(Long userId);

    List<PaymentMethod> findByUserIdOrderBySortOrderAsc(Long userId);

    Optional<PaymentMethod> findByIdAndUserId(Long id, Long userId);
}
