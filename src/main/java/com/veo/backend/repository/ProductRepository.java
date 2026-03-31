package com.veo.backend.repository;

import com.veo.backend.entity.Product;
import com.veo.backend.enums.ProductCatalogType;
import com.veo.backend.enums.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    List<Product> findByIsActiveTrue();

    List<Product> findByIsActiveTrueAndStatus(ProductStatus status);

    List<Product> findByIsActiveTrueAndCatalogType(ProductCatalogType catalogType);

    long countByIsActiveTrueAndCatalogType(ProductCatalogType catalogType);

    List<Product> findTop6ByIsActiveTrueOrderByIdAsc();

    List<Product> findByCategoryId(Long id);

    @Override
    Optional<Product> findById(Long id);
}
