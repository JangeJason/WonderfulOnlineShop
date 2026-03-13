package com.wonderful.onlineshop.controller;

import com.wonderful.onlineshop.common.ApiResponse;
import com.wonderful.onlineshop.product.entity.Category;
import com.wonderful.onlineshop.product.entity.ParameterOption;
import com.wonderful.onlineshop.product.entity.Product;
import com.wonderful.onlineshop.product.entity.ProductParameter;
import com.wonderful.onlineshop.product.dto.MaterialConfigResponse;
import com.wonderful.onlineshop.product.service.CategoryService;
import com.wonderful.onlineshop.product.service.ProductParameterService;
import com.wonderful.onlineshop.product.service.ProductService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final ProductParameterService parameterService;
    private final CategoryService categoryService;

    public ProductController(ProductService productService, ProductParameterService parameterService,
            CategoryService categoryService) {
        this.productService = productService;
        this.parameterService = parameterService;
        this.categoryService = categoryService;
    }

    @GetMapping
    public ApiResponse<IPage<Product>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(productService.listActivePaged(keyword, categoryId, page, size));
    }

    @GetMapping("/categories")
    public ApiResponse<List<Category>> categories() {
        return ApiResponse.ok(categoryService.listAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<Product> get(@PathVariable Long id) {
        return ApiResponse.ok(productService.getActiveRequired(id));
    }

    @GetMapping("/{id}/parameters")
    public ApiResponse<List<ProductParameter>> parameters(@PathVariable Long id) {
        return ApiResponse.ok(parameterService.getParametersByProductId(id));
    }

    @GetMapping("/parameters/{parameterId}/options")
    public ApiResponse<List<ParameterOption>> options(@PathVariable Long parameterId) {
        return ApiResponse.ok(parameterService.getOptionsByParamId(parameterId));
    }

    @GetMapping("/{id}/material-config")
    public ApiResponse<MaterialConfigResponse> materialConfig(
            @PathVariable("id") Long productId,
            @RequestParam(required = false) Long materialOptionId) {
        return ApiResponse.ok(parameterService.getMaterialConfig(productId, materialOptionId));
    }
}
