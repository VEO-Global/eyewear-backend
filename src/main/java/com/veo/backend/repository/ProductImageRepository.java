package com.veo.backend.repository;

import com.veo.backend.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
    Optional<ProductImage> findFirstByProductIdAndIsPrimaryTrue(Long productId);

    List<ProductImage> findByProductIdOrderByIsPrimaryDescSortOrderAscIdAsc(Long productId);
}
