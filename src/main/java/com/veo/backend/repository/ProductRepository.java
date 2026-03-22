package com.veo.backend.repository;

import com.veo.backend.entity.Product;
import com.veo.backend.enums.ProductCatalogType;
import com.veo.backend.enums.ProductStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    @EntityGraph(attributePaths = {"images", "variants", "category"})
    List<Product> findByIsActiveTrue();

    @EntityGraph(attributePaths = {"images", "variants", "category"})
    List<Product> findByIsActiveTrueAndStatus(ProductStatus status);

    @EntityGraph(attributePaths = {"images", "variants", "category"})
    List<Product> findByIsActiveTrueAndCatalogType(ProductCatalogType catalogType);

    long countByIsActiveTrueAndCatalogType(ProductCatalogType catalogType);

    List<Product> findTop6ByIsActiveTrueOrderByIdAsc();

    List<Product> findByCategoryId(Long id);

    @Override
    @EntityGraph(attributePaths = {"images", "variants", "category"})
    Optional<Product> findById(Long id);
}
