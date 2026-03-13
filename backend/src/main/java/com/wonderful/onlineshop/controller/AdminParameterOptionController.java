package com.wonderful.onlineshop.controller;

import com.wonderful.onlineshop.common.ApiResponse;
import com.wonderful.onlineshop.product.entity.ParameterOption;
import com.wonderful.onlineshop.product.service.ProductParameterService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/parameter-options")
public class AdminParameterOptionController {

    private final ProductParameterService parameterService;

    public AdminParameterOptionController(ProductParameterService parameterService) {
        this.parameterService = parameterService;
    }

    @GetMapping
    public ApiResponse<List<ParameterOption>> list(@RequestParam Long parameterId) {
        return ApiResponse.success(parameterService.getOptionsByParamId(parameterId));
    }

    @PostMapping
    public ApiResponse<ParameterOption> create(@RequestBody ParameterOption option) {
        return ApiResponse.success(parameterService.createOption(option));
    }

    @PutMapping("/{id}")
    public ApiResponse<ParameterOption> update(@PathVariable Long id, @RequestBody ParameterOption option) {
        return ApiResponse.success(parameterService.updateOption(id, option));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        parameterService.deleteOption(id);
        return ApiResponse.success(null);
    }
}
