package com.veo.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "product_bundles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductBundle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String description;

    private BigDecimal bundlePrice;

    private boolean isActive = true;

    @ElementCollection
    @CollectionTable(name = "product_bundle_variant_ids", joinColumns = @JoinColumn(name = "bundle_id"))
    @Column(name = "variant_id")
    private List<Long> productVariantIds = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "product_bundle_lens_ids", joinColumns = @JoinColumn(name = "bundle_id"))
    @Column(name = "lens_id")
    private List<Long> lensProductIds = new ArrayList<>();
}