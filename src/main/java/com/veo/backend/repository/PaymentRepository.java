package com.veo.backend.repository;

import com.veo.backend.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findFirstByOrderIdOrderByIdDesc(Long orderId);

    boolean existsByTransactionCode(String transactionCode);
}
