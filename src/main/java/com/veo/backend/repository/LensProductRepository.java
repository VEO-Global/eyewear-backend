package com.veo.backend.repository;

import com.veo.backend.entity.LensProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LensProductRepository extends JpaRepository<LensProduct, Long> {
    List<LensProduct> findByIsActiveTrue();
}
