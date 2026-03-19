package com.wonderful.onlineshop.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.wonderful.onlineshop.common.ApiResponse;
import com.wonderful.onlineshop.favorite.service.FavoriteService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    public record FavoriteConfigCodeRequest(@NotBlank String code) {
    }

    @PostMapping("/products/{productId}")
    public ApiResponse<Void> favoriteProduct(@PathVariable("productId") Long productId) {
        Long userId = StpUtil.getLoginIdAsLong();
        favoriteService.favoriteProduct(userId, productId);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/products/{productId}")
    public ApiResponse<Void> unfavoriteProduct(@PathVariable("productId") Long productId,
                                               @RequestParam(value = "removeConfigCodes", required = false, defaultValue = "false") boolean removeConfigCodes) {
        Long userId = StpUtil.getLoginIdAsLong();
        favoriteService.unfavoriteProduct(userId, productId, removeConfigCodes);
        return ApiResponse.ok(null);
    }

    @GetMapping("/products/{productId}/status")
    public ApiResponse<FavoriteService.ProductFavoriteStatus> productFavoriteStatus(@PathVariable("productId") Long productId) {
        Long userId = StpUtil.getLoginIdAsLong();
        return ApiResponse.ok(favoriteService.getProductFavoriteStatus(userId, productId));
    }

    @GetMapping("/products")
    public ApiResponse<List<FavoriteService.FavoriteProductItem>> listFavoriteProducts() {
        Long userId = StpUtil.getLoginIdAsLong();
        return ApiResponse.ok(favoriteService.listFavoriteProducts(userId));
    }

    @GetMapping("/products/{productId}/config-codes")
    public ApiResponse<List<FavoriteService.FavoriteConfigCodeItem>> listFavoriteConfigCodes(@PathVariable("productId") Long productId) {
        Long userId = StpUtil.getLoginIdAsLong();
        return ApiResponse.ok(favoriteService.listFavoriteConfigCodes(userId, productId));
    }

    @PostMapping("/config-codes")
    public ApiResponse<FavoriteService.FavoriteConfigCodeResponse> favoriteConfigCode(@RequestBody @Valid FavoriteConfigCodeRequest req) {
        Long userId = StpUtil.getLoginIdAsLong();
        return ApiResponse.ok(favoriteService.favoriteConfigCode(userId, req.code()));
    }

    @DeleteMapping("/config-codes/{code}")
    public ApiResponse<Void> unfavoriteConfigCode(@PathVariable("code") String code) {
        Long userId = StpUtil.getLoginIdAsLong();
        favoriteService.unfavoriteConfigCode(userId, code);
        return ApiResponse.ok(null);
    }
}
