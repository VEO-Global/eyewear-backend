package com.veo.backend.repository;

import com.veo.backend.entity.Order;
import com.veo.backend.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    @EntityGraph(attributePaths = {
            "items",
            "items.productVariant",
            "items.productVariant.product",
            "items.lensProduct"
    })
    List<Order> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    @EntityGraph(attributePaths = {
            "items",
            "items.productVariant",
            "items.productVariant.product",
            "items.lensProduct"
    })
    List<Order> findAllByUserIdAndStatusInOrderByCreatedAtDesc(Long userId, Collection<OrderStatus> statuses);

    @EntityGraph(attributePaths = {
            "items",
            "items.productVariant",
            "items.productVariant.product",
            "items.lensProduct"
    })
    Optional<Order> findByIdAndUserId(Long id, Long userId);
}
