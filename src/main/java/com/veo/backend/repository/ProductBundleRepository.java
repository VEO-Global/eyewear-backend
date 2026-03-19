package com.veo.backend.repository;

import com.veo.backend.entity.ProductBundle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductBundleRepository extends JpaRepository<ProductBundle, Long> {
    List<ProductBundle> findByIsActiveTrue();
}