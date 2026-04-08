package com.veo.backend.repository;

import com.veo.backend.entity.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {
    List<OrderStatusHistory> findByOrderIdOrderByCreatedAtAscIdAsc(Long orderId);

    @Query("""
            select distinct h.order.id
            from OrderStatusHistory h
            where h.order.id in :orderIds
              and lower(coalesce(h.note, '')) like lower(concat('%', :keyword, '%'))
            """)
    Set<Long> findOrderIdsByHandoffKeyword(@Param("orderIds") Collection<Long> orderIds,
                                           @Param("keyword") String keyword);

    @Query("""
            select count(h) > 0
            from OrderStatusHistory h
            where h.order.id = :orderId
              and lower(coalesce(h.note, '')) like lower(concat('%', :keyword, '%'))
            """)
    boolean existsHandoffMarker(@Param("orderId") Long orderId,
                                @Param("keyword") String keyword);
}
