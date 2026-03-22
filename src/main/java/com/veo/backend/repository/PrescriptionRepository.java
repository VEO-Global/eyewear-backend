package com.veo.backend.repository;

import com.veo.backend.entity.Prescription;
import com.veo.backend.enums.PrescriptionReviewStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {
    @EntityGraph(attributePaths = {"order", "order.user", "lensProduct", "verifiedBy"})
    java.util.Optional<Prescription> findByOrderId(Long orderId);

    @EntityGraph(attributePaths = {"order", "order.user", "lensProduct", "verifiedBy"})
    List<Prescription> findByReviewStatusOrderByCreatedAtAsc(PrescriptionReviewStatus reviewStatus);
}
