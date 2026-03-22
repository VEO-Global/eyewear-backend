package com.veo.backend.repository;

import com.veo.backend.entity.ReturnRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {
    @EntityGraph(attributePaths = {
            "items",
            "items.orderItem",
            "items.orderItem.productVariant",
            "items.orderItem.productVariant.product"
    })
    Page<ReturnRequest> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {
            "items",
            "items.orderItem",
            "items.orderItem.productVariant",
            "items.orderItem.productVariant.product"
    })
    Optional<ReturnRequest> findByIdAndUserId(Long id, Long userId);
}
