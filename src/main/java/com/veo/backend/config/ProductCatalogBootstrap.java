package com.veo.backend.config;

import com.veo.backend.entity.Product;
import com.veo.backend.entity.ProductVariant;
import com.veo.backend.enums.ProductCatalogType;
import com.veo.backend.repository.ProductRepository;
import com.veo.backend.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductCatalogBootstrap {
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void backfillCatalogTypeForExistingData() {
        long preorderProductCount = productRepository.countByIsActiveTrueAndCatalogType(ProductCatalogType.NEW);
        if (preorderProductCount > 0) {
            return;
        }

        List<Product> activeProducts = productRepository.findByIsActiveTrue();
        if (activeProducts.isEmpty()) {
            return;
        }

        Set<Long> preorderProductIds = productRepository.findTop6ByIsActiveTrueOrderByIdAsc()
                .stream()
                .map(Product::getId)
                .collect(Collectors.toSet());

        boolean hasChanges = false;
        for (Product product : activeProducts) {
            ProductCatalogType targetCatalogType = preorderProductIds.contains(product.getId())
                    ? ProductCatalogType.NEW
                    : ProductCatalogType.OLD;

            if (product.getCatalogType() != targetCatalogType) {
                product.setCatalogType(targetCatalogType);
                hasChanges = true;
            }

            if (targetCatalogType == ProductCatalogType.NEW && product.getVariants() != null) {
                for (ProductVariant variant : product.getVariants()) {
                    if (variant.getStockQuantity() != null && variant.getStockQuantity() != 0) {
                        variant.setStockQuantity(0);
                        productVariantRepository.save(variant);
                        hasChanges = true;
                    }
                }
            }
        }

        if (hasChanges) {
            productRepository.saveAll(activeProducts);
            log.info("Backfilled product catalog types: {} preorder products marked as NEW", preorderProductIds.size());
        }
    }
}
