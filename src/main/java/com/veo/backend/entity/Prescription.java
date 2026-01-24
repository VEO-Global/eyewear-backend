package com.veo.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;

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

    private String prescriptionImageUrl;

    private BigDecimal sphereOd;

    private BigDecimal sphereOs;

    private BigDecimal cylinderOd;

    private BigDecimal cylinderOs;

    private Integer axisOd;

    private Integer axisOs;

    private BigDecimal pd;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by")
    private User verifiedBy;

    private LocalDateTime verifiedAt;

    private String staffNote;
}
