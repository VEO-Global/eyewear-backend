package com.veo.backend.service.impl;

import com.veo.backend.dto.request.ProductCreateRequest;
import com.veo.backend.dto.request.ProductImageRequest;
import com.veo.backend.dto.request.ProductSearchRequest;
import com.veo.backend.dto.request.ProductUpdateRequest;
import com.veo.backend.dto.response.PagedResponse;
import com.veo.backend.dto.response.ProductResponse;
import com.veo.backend.entity.Category;
import com.veo.backend.entity.Product;
import com.veo.backend.entity.ProductImage;
import com.veo.backend.enums.ProductCatalogType;
import com.veo.backend.enums.ProductStatus;
import com.veo.backend.repository.CategoryRepository;
import com.veo.backend.repository.ProductImageRepository;
import com.veo.backend.repository.ProductRepository;
import com.veo.backend.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductResponseMapper productResponseMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts(String status) {
        if (status == null || status.isBlank()) {
            return productRepository.findByIsActiveTrue()
                    .stream()
                    .sorted(productDisplayOrder())
                    .map(productResponseMapper::map)
                    .toList();
        }

        ProductStatus productStatus = parseStatus(status);
        return productRepository.findByIsActiveTrueAndStatus(productStatus)
                .stream()
                .sorted(productDisplayOrder())
                .map(productResponseMapper::map)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getPreorderProducts() {
        return productRepository.findByIsActiveTrueAndCatalogType(ProductCatalogType.NEW)
                .stream()
                .map(productResponseMapper::map)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> searchProducts(ProductSearchRequest request) {
        List<ProductResponse> filteredProducts = productRepository.findAll().stream()
                .filter(product -> request.getActive() == null || Objects.equals(product.getIsActive(), request.getActive()))
                .filter(product -> request.getStatus() == null || product.getStatus() == request.getStatus())
                .filter(product -> request.getCatalogType() == null || product.getCatalogType() == request.getCatalogType())
                .filter(product -> request.getCategoryId() == null || (product.getCategory() != null && request.getCategoryId().equals(product.getCategory().getId())))
                .filter(product -> matchesKeyword(product, request.getKeyword()))
                .sorted(productDisplayOrder())
                .map(productResponseMapper::map)
                .toList();

        int safePage = Math.max(request.getPage(), 0);
        int safeSize = Math.max(request.getSize(), 1);
        int fromIndex = Math.min(safePage * safeSize, filteredProducts.size());
        int toIndex = Math.min(fromIndex + safeSize, filteredProducts.size());
        int totalPages = filteredProducts.isEmpty() ? 0 : (int) Math.ceil((double) filteredProducts.size() / safeSize);

        return PagedResponse.<ProductResponse>builder()
                .content(filteredProducts.subList(fromIndex, toIndex))
                .page(safePage)
                .size(safeSize)
                .totalElements(filteredProducts.size())
                .totalPages(totalPages)
                .last(toIndex >= filteredProducts.size())
                .build();
    }

    @Override
    public ProductResponse createProduct(ProductCreateRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + request.getCategoryId()));

        Product product = Product.builder()
                .name(request.getName())
                .brand(request.getBrand())
                .description(request.getDescription())
                .basePrice(request.getBasePrice())
                .material(request.getMaterial())
                .gender(request.getGender())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .status(request.getStatus() != null ? request.getStatus() : ProductStatus.AVAILABLE)
                .createdAt(LocalDateTime.now())
                .category(category)
                .model3dUrl(request.getModel3dUrl())
                .catalogType(request.getCatalogType() != null ? request.getCatalogType() : ProductCatalogType.OLD)
                .build();

        List<ProductImageRequest> imageRequests = resolveCreateImageRequests(request);
        product.setImages(buildProductImages(product, imageRequests));

        Product savedProduct = productRepository.save(product);

        return productResponseMapper.map(savedProduct);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        return productResponseMapper.map(product);
    }

    @Override
    public ProductResponse updateProduct(Long id, ProductUpdateRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found with id: " + request.getCategoryId()));
            product.setCategory(category);
        }

        if (request.getName() != null) product.setName(request.getName());
        if (request.getBrand() != null) product.setBrand(request.getBrand());
        if (request.getDescription() != null) product.setDescription(request.getDescription());
        if (request.getBasePrice() != null) product.setBasePrice(request.getBasePrice());
        if (request.getMaterial() != null) product.setMaterial(request.getMaterial());
        if (request.getGender() != null) product.setGender(request.getGender());
        if (request.getModel3dUrl() != null) product.setModel3dUrl(request.getModel3dUrl());
        if (request.getIsActive() != null) product.setIsActive(request.getIsActive());
        if (request.getStatus() != null) product.setStatus(request.getStatus());
        if (request.getCatalogType() != null) product.setCatalogType(request.getCatalogType());
        applyImageUpdate(product, request);

        return productResponseMapper.map(productRepository.save(product));
    }

    @Override
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        product.setIsActive(false);
        productRepository.save(product);
    }

    private Comparator<Product> productDisplayOrder() {
        return Comparator
                .comparing((Product product) -> product.getCatalogType() == ProductCatalogType.NEW)
                .thenComparing(Product::getId, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    private ProductStatus parseStatus(String status) {
        try {
            return ProductStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("Invalid product status: " + status);
        }
    }

    private List<ProductImage> buildProductImages(Product product, List<ProductImageRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return new ArrayList<>();
        }

        List<ProductImageRequest> normalizedRequests = requests.stream()
                .filter(Objects::nonNull)
                .filter(request -> request.getUrl() != null && !request.getUrl().isBlank())
                .toList();

        if (normalizedRequests.isEmpty()) {
            return new ArrayList<>();
        }

        long primaryCount = normalizedRequests.stream()
                .filter(request -> Boolean.TRUE.equals(request.getIsPrimary()))
                .count();
        if (primaryCount == 0) {
            normalizedRequests.get(0).setIsPrimary(true);
        }

        List<ProductImage> productImages = new ArrayList<>();
        for (int index = 0; index < normalizedRequests.size(); index++) {
            ProductImageRequest request = normalizedRequests.get(index);
            ProductImage productImage = request.getId() == null
                    ? new ProductImage()
                    : resolveExistingProductImage(product, request.getId());

            productImage.setProduct(product);
            productImage.setImageUrl(request.getUrl().trim());
            productImage.setAltText(trimToNull(request.getAlt()));
            boolean isPrimary = primaryCount == 0
                    ? index == 0
                    : Boolean.TRUE.equals(request.getIsPrimary()) && productImages.stream().noneMatch(image -> Boolean.TRUE.equals(image.getIsPrimary()));
            productImage.setIsPrimary(isPrimary);
            productImage.setIsThumbnail(isPrimary);
            productImage.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : index);
            productImages.add(productImage);
        }

        if (productImages.stream().noneMatch(image -> Boolean.TRUE.equals(image.getIsPrimary()))) {
            productImages.get(0).setIsPrimary(true);
        }

        for (ProductImage image : productImages) {
            if (!Boolean.TRUE.equals(image.getIsPrimary())) {
                image.setIsPrimary(false);
                image.setIsThumbnail(false);
            } else {
                image.setIsThumbnail(true);
            }
        }

        return productImages;
    }

    private List<ProductImageRequest> resolveCreateImageRequests(ProductCreateRequest request) {
        String primaryImageUrl = firstNonBlank(request.getImageUrl(), null);
        return composeImageRequests(request.getImages(), request.getImageUrls(), primaryImageUrl);
    }

    private void applyImageUpdate(Product product, ProductUpdateRequest request) {
        String primaryImageUrl = firstNonBlank(request.getImageUrl(), null);
        boolean hasStructuredUpdate = request.getImages() != null || request.getImageUrls() != null;

        if (hasStructuredUpdate) {
            List<ProductImageRequest> imageRequests = composeImageRequests(request.getImages(), request.getImageUrls(), primaryImageUrl);
            product.setImages(buildProductImages(product, imageRequests));
            return;
        }

        if (primaryImageUrl == null) {
            return;
        }

        List<ProductImage> existingImages = product.getImages();
        if (existingImages == null || existingImages.isEmpty()) {
            List<ProductImageRequest> fallbackRequests = composeImageRequests(null, null, primaryImageUrl);
            product.setImages(buildProductImages(product, fallbackRequests));
            return;
        }

        ProductImage primaryImage = existingImages.stream()
                .filter(Objects::nonNull)
                .filter(image -> Boolean.TRUE.equals(image.getIsPrimary()) || Boolean.TRUE.equals(image.getIsThumbnail()))
                .findFirst()
                .orElse(existingImages.get(0));
        primaryImage.setImageUrl(primaryImageUrl);
    }

    private List<ProductImageRequest> composeImageRequests(List<ProductImageRequest> images, List<String> imageUrls, String primaryImageUrl) {
        List<ProductImageRequest> requests = new ArrayList<>();

        if (images != null) {
            requests.addAll(images);
        } else if (imageUrls != null) {
            int index = 0;
            for (String url : imageUrls) {
                String normalizedUrl = trimToNull(url);
                if (normalizedUrl == null) {
                    continue;
                }
                requests.add(new ProductImageRequest(null, normalizedUrl, null, index == 0, index));
                index++;
            }
        }

        if (primaryImageUrl == null) {
            return requests;
        }

        for (ProductImageRequest request : requests) {
            if (request == null) {
                continue;
            }

            String requestUrl = trimToNull(request.getUrl());
            if (primaryImageUrl.equals(requestUrl)) {
                request.setIsPrimary(true);
                if (request.getSortOrder() == null) {
                    request.setSortOrder(0);
                }
                return requests;
            }
        }

        ProductImageRequest primaryRequest = new ProductImageRequest();
        primaryRequest.setUrl(primaryImageUrl);
        primaryRequest.setIsPrimary(true);
        primaryRequest.setSortOrder(0);
        requests.add(0, primaryRequest);

        return requests;
    }

    private ProductImage resolveExistingProductImage(Product product, Long imageId) {
        ProductImage productImage = productImageRepository.findById(imageId).orElse(new ProductImage());
        if (productImage.getId() != null
                && productImage.getProduct() != null
                && product.getId() != null
                && !product.getId().equals(productImage.getProduct().getId())) {
            throw new RuntimeException("Product image does not belong to this product");
        }
        return productImage;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String firstNonBlank(String first, String second) {
        String normalizedFirst = trimToNull(first);
        if (normalizedFirst != null) {
            return normalizedFirst;
        }
        return trimToNull(second);
    }

    private boolean matchesKeyword(Product product, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }

        String normalizedKeyword = keyword.trim().toLowerCase(Locale.ROOT);
        return contains(product.getName(), normalizedKeyword)
                || contains(product.getBrand(), normalizedKeyword)
                || contains(product.getDescription(), normalizedKeyword)
                || contains(product.getMaterial(), normalizedKeyword);
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }
}

