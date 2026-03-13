package com.wonderful.onlineshop.controller;

import com.wonderful.onlineshop.common.ApiResponse;
import com.wonderful.onlineshop.product.dto.AdminMaterialRulesDto;
import com.wonderful.onlineshop.product.service.ProductParameterService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/products/{productId}/material-rules")
public class AdminProductMaterialRuleController {

    private final ProductParameterService parameterService;

    public AdminProductMaterialRuleController(ProductParameterService parameterService) {
        this.parameterService = parameterService;
    }

    @GetMapping
    public ApiResponse<AdminMaterialRulesDto.MaterialRulesResponse> getRules(
            @PathVariable Long productId,
            @RequestParam(required = false) Long materialOptionId) {
        return ApiResponse.ok(parameterService.getAdminMaterialRules(productId, materialOptionId));
    }

    @PutMapping
    public ApiResponse<AdminMaterialRulesDto.MaterialRulesResponse> saveRules(
            @PathVariable Long productId,
            @RequestBody AdminMaterialRulesDto.MaterialRulesPayload payload) {
        return ApiResponse.ok(parameterService.saveAdminMaterialRules(productId, payload));
    }
}
