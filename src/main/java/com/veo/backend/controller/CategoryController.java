package com.veo.backend.controller;

import com.veo.backend.dto.request.CategoryRequest;
import com.veo.backend.dto.response.CategoryResponse;
import com.veo.backend.entity.Category;
import com.veo.backend.service.CategoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    private  final CategoryService service;

    public CategoryController(CategoryService service) {
        this.service = service;
    }

    @GetMapping
    public List<Category> getAllCategories(){
        return service.getAllCategories();
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(@RequestBody CategoryRequest request){
        return   ResponseEntity.ok(service.createCategory(request));
    }

    @GetMapping("/{id}")
    public CategoryResponse getCategoryById(@PathVariable Long id){
        return service.getCategoryById(id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public void deleteCategory(@PathVariable Long id){
        service.deleteCategory(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public CategoryResponse updateCategory(@PathVariable Long id, @RequestBody CategoryRequest request){
        return service.updateCategory(id, request);
    }
}
