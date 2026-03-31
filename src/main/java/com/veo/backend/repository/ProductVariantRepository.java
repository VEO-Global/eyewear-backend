package com.veo.backend.repository;

import com.veo.backend.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    List<ProductVariant> findByProductId(Long id);

    List<ProductVariant> findByProductIdAndIsActive(Long id, Boolean isActive);

    List<ProductVariant> findByIsActive(Boolean isActive);

    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, Long id);

    @Query("""
            select count(v) > 0
            from ProductVariant v
            where v.product.id = :productId
              and lower(v.color) = lower(:color)
              and lower(v.size) = lower(:size)
            """)
    boolean existsByProductAndAttributes(
            @Param("productId") Long productId,
            @Param("color") String color,
            @Param("size") String size
    );

    @Query("""
            select count(v) > 0
            from ProductVariant v
            where v.product.id = :productId
              and lower(v.color) = lower(:color)
              and lower(v.size) = lower(:size)
              and v.id <> :id
            """)
    boolean existsByProductAndAttributesExcludingId(
            @Param("productId") Long productId,
            @Param("color") String color,
            @Param("size") String size,
            @Param("id") Long id
    );
}
