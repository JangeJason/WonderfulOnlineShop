package com.wonderful.onlineshop.controller;

import com.wonderful.onlineshop.common.ApiResponse;
import com.wonderful.onlineshop.product.entity.ProductParameter;
import com.wonderful.onlineshop.product.service.ProductParameterService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/product-parameters")
public class AdminProductParameterController {

    private final ProductParameterService parameterService;

    public AdminProductParameterController(ProductParameterService parameterService) {
        this.parameterService = parameterService;
    }

    @GetMapping
    public ApiResponse<List<ProductParameter>> list(@RequestParam Long productId) {
        return ApiResponse.success(parameterService.getParametersByProductId(productId));
    }

    @PostMapping
    public ApiResponse<ProductParameter> create(@RequestBody ProductParameter parameter) {
        return ApiResponse.success(parameterService.createParameter(parameter));
    }

    @PutMapping("/{id}")
    public ApiResponse<ProductParameter> update(@PathVariable Long id, @RequestBody ProductParameter parameter) {
        return ApiResponse.success(parameterService.updateParameter(id, parameter));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        parameterService.deleteParameter(id);
        return ApiResponse.success(null);
    }
}
