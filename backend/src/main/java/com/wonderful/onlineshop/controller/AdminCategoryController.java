package com.wonderful.onlineshop.controller;

import com.wonderful.onlineshop.common.ApiResponse;
import com.wonderful.onlineshop.product.entity.Category;
import com.wonderful.onlineshop.product.service.CategoryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/categories")
public class AdminCategoryController {

    private final CategoryService categoryService;

    public AdminCategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    public record CreateCategoryRequest(@NotBlank String name, Integer sortOrder, Long parentId) {
    }

    public record UpdateCategoryRequest(String name, Integer sortOrder, Long parentId) {
    }

    @GetMapping
    public ApiResponse<List<Category>> list() {
        return ApiResponse.ok(categoryService.listAll());
    }

    @PostMapping
    public ApiResponse<Category> create(@RequestBody @Valid CreateCategoryRequest req) {
        Category c = new Category();
        c.setName(req.name());
        c.setSortOrder(req.sortOrder() != null ? req.sortOrder() : 0);
        c.setParentId(req.parentId());
        return ApiResponse.ok(categoryService.create(c));
    }

    @PutMapping("/{id}")
    public ApiResponse<Category> update(@PathVariable Long id, @RequestBody @Valid UpdateCategoryRequest req) {
        Category patch = new Category();
        patch.setName(req.name());
        patch.setSortOrder(req.sortOrder());
        patch.setParentId(req.parentId());
        return ApiResponse.ok(categoryService.update(id, patch));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ApiResponse.ok(null);
    }
}
