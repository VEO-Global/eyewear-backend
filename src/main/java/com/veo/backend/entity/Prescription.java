package com.veo.backend.entity;

import com.veo.backend.enums.PrescriptionReviewStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "prescriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Prescription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lens_product_id")
    private LensProduct lensProduct;

    private String prescriptionImageUrl;

    private String lensNameSnapshot;

    private BigDecimal lensPriceSnapshot;

    private String lensDescriptionSnapshot;

    private BigDecimal sphereOd;

    private BigDecimal sphereOs;

    private BigDecimal cylinderOd;

    private BigDecimal cylinderOs;

    private Integer axisOd;

    private Integer axisOs;

    private BigDecimal pd;

    @Enumerated(EnumType.STRING)
    private PrescriptionReviewStatus reviewStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by")
    private User verifiedBy;

    private LocalDateTime verifiedAt;

    @Column(name = "staff_note")
    private String reviewNote;

    private LocalDateTime createdAt;
}
