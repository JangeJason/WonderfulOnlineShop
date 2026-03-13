package com.wonderful.onlineshop.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.wonderful.onlineshop.cart.entity.CartItem;
import com.wonderful.onlineshop.cart.service.CartService;
import com.wonderful.onlineshop.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    public record AddCartRequest(
            @NotNull Long productId,
            @NotNull @Min(1) Integer quantity,
            String paramsSnapshot,
            @NotNull BigDecimal unitPrice,
            String printFileUrl,
            String proofFileUrl,
            Boolean hasCopyright,
            String copyrightFileUrl) {
    }

    public record UpdateQuantityRequest(
            @NotNull @Min(1) Integer quantity) {
    }

    @GetMapping("/items")
    public ApiResponse<List<CartItem>> list() {
        Long userId = StpUtil.getLoginIdAsLong();
        return ApiResponse.ok(cartService.listByUser(userId));
    }

    @PostMapping("/items")
    public ApiResponse<CartItem> add(@RequestBody @Valid AddCartRequest req) {
        Long userId = StpUtil.getLoginIdAsLong();
        CartItem item = new CartItem();
        item.setUserId(userId);
        item.setProductId(req.productId());
        item.setQuantity(req.quantity());
        item.setParamsSnapshot(req.paramsSnapshot());
        item.setUnitPrice(req.unitPrice());
        item.setPrintFileUrl(req.printFileUrl());
        item.setProofFileUrl(req.proofFileUrl());
        item.setHasCopyright(req.hasCopyright() != null ? req.hasCopyright() : false);
        item.setCopyrightFileUrl(req.copyrightFileUrl());
        return ApiResponse.ok(cartService.addItem(item));
    }

    @PutMapping("/items/{id}")
    public ApiResponse<CartItem> updateQuantity(@PathVariable Long id,
            @RequestBody @Valid UpdateQuantityRequest req) {
        Long userId = StpUtil.getLoginIdAsLong();
        return ApiResponse.ok(cartService.updateQuantity(id, userId, req.quantity()));
    }

    @DeleteMapping("/items/{id}")
    public ApiResponse<Void> remove(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        cartService.removeItem(id, userId);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/clear")
    public ApiResponse<Void> clear() {
        Long userId = StpUtil.getLoginIdAsLong();
        cartService.clearByUser(userId);
        return ApiResponse.ok(null);
    }

    public record BatchDeleteRequest(@NotNull List<Long> ids) {
    }

    @PostMapping("/items/batch-delete")
    public ApiResponse<Void> batchDelete(@RequestBody @Valid BatchDeleteRequest req) {
        Long userId = StpUtil.getLoginIdAsLong();
        cartService.removeBatch(req.ids(), userId);
        return ApiResponse.ok(null);
    }
}
