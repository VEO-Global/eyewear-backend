package com.veo.backend.repository;

import com.veo.backend.entity.Prescription;
import com.veo.backend.enums.PrescriptionReviewStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {
    @EntityGraph(attributePaths = {"order", "order.user", "lensProduct", "verifiedBy"})
    java.util.Optional<Prescription> findFirstByOrderIdOrderByIdDesc(Long orderId);

    @EntityGraph(attributePaths = {"order", "order.user", "lensProduct", "verifiedBy"})
    List<Prescription> findByOrderIdIn(Collection<Long> orderIds);

    @EntityGraph(attributePaths = {"order", "order.user", "lensProduct", "verifiedBy"})
    List<Prescription> findByReviewStatusOrderByCreatedAtAsc(PrescriptionReviewStatus reviewStatus);

    @EntityGraph(attributePaths = {"order", "order.user", "lensProduct", "verifiedBy"})
    @Query("""
            select p
            from Prescription p
            where p.reviewStatus = com.veo.backend.enums.PrescriptionReviewStatus.PENDING
               or (p.reviewStatus is null and p.verifiedBy is null and p.verifiedAt is null)
            order by p.createdAt asc, p.id asc
            """)
    List<Prescription> findPendingForStaff();
}
