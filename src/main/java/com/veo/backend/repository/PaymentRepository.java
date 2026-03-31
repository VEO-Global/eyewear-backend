package com.veo.backend.repository;

import com.veo.backend.entity.Payment;
import com.veo.backend.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findFirstByOrderIdOrderByIdDesc(Long orderId);

    boolean existsByTransactionCode(String transactionCode);

    List<Payment> findByOrderUserId(Long userId);

    Optional<Payment> findByOrderId(Long orderId);

    List<Payment> findByStatus(PaymentStatus status);

    Page<Payment> findAll(Pageable pageable);
}
