package com.veo.backend.repository;

import com.veo.backend.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findFirstByOrderIdOrderByIdDesc(Long orderId);

    boolean existsByTransactionCode(String transactionCode);

    List<Payment> findByOrderUserId(Long userId);

    Optional<Payment> findByOrderId(Long orderId);

    Page<Payment> findAll(Pageable pageable);
}
