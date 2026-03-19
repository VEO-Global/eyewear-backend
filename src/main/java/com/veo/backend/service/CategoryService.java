package com.veo.backend.service;

import com.veo.backend.dto.request.CategoryRequest;
import com.veo.backend.dto.response.CategoryResponse;
import com.veo.backend.entity.Category;

import java.util.List;

public interface CategoryService {
    List<Category> getAllCategories();

    CategoryResponse createCategory(CategoryRequest request);

    CategoryResponse getCategoryById(Long id);

    void deleteCategory(Long id);

    CategoryResponse updateCategory(Long id, CategoryRequest request);
}
