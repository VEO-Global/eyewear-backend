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

@Entity
@Table(name = "lens_products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LensProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String type;

    private BigDecimal refractionIndex;

    private String description;

    private BigDecimal price;

    private Boolean isActive;
}
