package com.veo.backend.repository;

import com.veo.backend.entity.FavoriteProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteProductRepository extends JpaRepository<FavoriteProduct, Long> {
    Optional<FavoriteProduct> findByUserIdAndProductId(Long userId, Long productId);

    boolean existsByUserIdAndProductId(Long userId, Long productId);

    void deleteByUserIdAndProductId(Long userId, Long productId);

    @Query("""
            select distinct f
            from FavoriteProduct f
            join fetch f.product p
            left join fetch p.images
            left join fetch p.variants
            where f.user.id = :userId
            order by f.createdAt desc
            """)
    List<FavoriteProduct> findAllByUserIdWithProduct(Long userId);
}
