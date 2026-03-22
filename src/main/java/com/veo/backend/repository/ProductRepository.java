package com.veo.backend.repository;

import com.veo.backend.entity.Product;
import com.veo.backend.enums.ProductCatalogType;
import com.veo.backend.enums.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByIsActiveTrue();

    List<Product> findByIsActiveTrueAndStatus(ProductStatus status);

    List<Product> findByIsActiveTrueAndCatalogType(ProductCatalogType catalogType);

    List<Product> findByCategoryId(Long id);
}
