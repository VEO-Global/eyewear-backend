package com.veo.backend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "vouchers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Voucher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String code;

    private Integer discountPercent;

    private BigDecimal maxDiscountAmount;

    private LocalDateTime  startDate;

    private LocalDateTime endDate;

    private Integer quantity;

    private boolean isActive;
}
