package com.wonderful.onlineshop.controller;

import com.wonderful.onlineshop.common.ApiResponse;
import com.wonderful.onlineshop.product.entity.Product;
import com.wonderful.onlineshop.product.service.ProductService;
import com.baomidou.mybatisplus.core.metadata.IPage;
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
            String description,
            String imageUrl,
            List<String> imageUrls,
            @NotNull @DecimalMin("0.00") BigDecimal basePrice,
            BigDecimal setupFee,
            Integer freeSetupQuantity,
            Integer status,
            String pricingFormula,
            Long categoryId) {
    }

    public record UpdateProductRequest(
            String name,
            String description,
            String imageUrl,
            List<String> imageUrls,
            BigDecimal basePrice,
            BigDecimal setupFee,
            Integer freeSetupQuantity,
            Integer status,
            String pricingFormula,
            Long categoryId) {
    }

    @GetMapping
    public ApiResponse<IPage<Product>> listAll(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(productService.listAllPaged(page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<Product> getById(@PathVariable("id") Long id) {
        return ApiResponse.ok(productService.getById(id));
    }

    @PostMapping
    public ApiResponse<Product> create(@RequestBody @Valid CreateProductRequest req) {
        Product p = new Product();
        p.setName(req.name());
        p.setDescription(req.description());
        p.setImageUrl(req.imageUrl());
        p.setImageUrls(req.imageUrls());
        p.setBasePrice(req.basePrice());
        p.setSetupFee(req.setupFee());
        p.setFreeSetupQuantity(req.freeSetupQuantity());
        p.setStatus(req.status());
        p.setPricingFormula(req.pricingFormula());
        p.setCategoryId(req.categoryId());
        return ApiResponse.ok(productService.create(p));
    }

    @PutMapping("/{id}")
    public ApiResponse<Product> update(@PathVariable Long id, @RequestBody @Valid UpdateProductRequest req) {
        Product patch = new Product();
        if (req.name() != null)
            patch.setName(req.name());
        patch.setDescription(req.description());
        patch.setImageUrl(req.imageUrl());
        patch.setImageUrls(req.imageUrls());
        if (req.basePrice() != null)
            patch.setBasePrice(req.basePrice());
        if (req.setupFee() != null)
            patch.setSetupFee(req.setupFee());
        if (req.freeSetupQuantity() != null)
            patch.setFreeSetupQuantity(req.freeSetupQuantity());
        patch.setStatus(req.status());
        patch.setPricingFormula(req.pricingFormula());
        patch.setCategoryId(req.categoryId());
        return ApiResponse.ok(productService.update(id, patch));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ApiResponse.ok(null);
    }
}
