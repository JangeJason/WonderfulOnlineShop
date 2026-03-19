package com.wonderful.onlineshop.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.wonderful.onlineshop.common.ApiResponse;
import com.wonderful.onlineshop.configcode.service.ProductConfigCodeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/config-codes")
public class ProductConfigCodeController {

    private final ProductConfigCodeService productConfigCodeService;

    public ProductConfigCodeController(ProductConfigCodeService productConfigCodeService) {
        this.productConfigCodeService = productConfigCodeService;
    }

    public record CreateConfigCodeRequest(
            @NotNull Long productId,
            @NotBlank String paramsSnapshot,
            @NotNull Integer expireDays) {
    }

    @PostMapping
    public ApiResponse<ProductConfigCodeService.CreateResponse> create(@RequestBody @Valid CreateConfigCodeRequest req) {
        Long userId = StpUtil.getLoginIdAsLong();
        ProductConfigCodeService.CreateRequest serviceReq = new ProductConfigCodeService.CreateRequest(
                req.productId(),
                req.paramsSnapshot(),
                req.expireDays());
        return ApiResponse.ok(productConfigCodeService.create(userId, serviceReq));
    }

    @GetMapping("/{code}")
    public ApiResponse<ProductConfigCodeService.ResolveResponse> resolve(@PathVariable("code") String code) {
        return ApiResponse.ok(productConfigCodeService.resolve(code));
    }
}

