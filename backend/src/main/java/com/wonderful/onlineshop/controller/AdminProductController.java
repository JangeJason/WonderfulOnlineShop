package com.wonderful.onlineshop.controller;

import com.wonderful.onlineshop.common.ApiResponse;
import com.wonderful.onlineshop.product.entity.Product;
import com.wonderful.onlineshop.product.service.ProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/admin/products")
public class AdminProductController {

    private final ProductService productService;

    public AdminProductController(ProductService productService) {
        this.productService = productService;
    }

    public record CreateProductRequest(
            @NotBlank String name,
            @NotNull @DecimalMin("0.00") BigDecimal basePrice,
            Integer status
    ) {
    }

    public record UpdateProductRequest(
            @NotBlank String name,
            @NotNull @DecimalMin("0.00") BigDecimal basePrice,
            Integer status
    ) {
    }

    @GetMapping
    public ApiResponse<List<Product>> listAll() {
        return ApiResponse.ok(productService.listAll());
    }

    @PostMapping
    public ApiResponse<Product> create(@RequestBody @Valid CreateProductRequest req) {
        Product p = new Product();
        p.setName(req.name());
        p.setBasePrice(req.basePrice());
        p.setStatus(req.status());
        return ApiResponse.ok(productService.create(p));
    }

    @PutMapping("/{id}")
    public ApiResponse<Product> update(@PathVariable Long id, @RequestBody @Valid UpdateProductRequest req) {
        Product patch = new Product();
        patch.setName(req.name());
        patch.setBasePrice(req.basePrice());
        patch.setStatus(req.status());
        return ApiResponse.ok(productService.update(id, patch));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ApiResponse.ok(null);
    }
}
