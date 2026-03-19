package com.wonderful.onlineshop.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.wonderful.onlineshop.common.ApiResponse;
import com.wonderful.onlineshop.order.dto.OrderDetailResponse;
import com.wonderful.onlineshop.order.entity.Order;
import com.wonderful.onlineshop.order.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/admin/orders")
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    public record UpdateStatusRequest(@NotBlank String status) {
    }
    public record UpdateShippingStatusRequest(@NotBlank String shippingStatus) {
    }
    public record UpdateProductionStatusRequest(@NotBlank String productionStatus) {
    }
    public record RejectReviewRequest(@NotBlank String reason) {
    }

    @GetMapping
    public ApiResponse<com.baomidou.mybatisplus.core.metadata.IPage<com.wonderful.onlineshop.order.dto.AdminOrderDTO>> listAll(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String customerKeyword,
            @RequestParam(required = false) String regionKeyword,
            @RequestParam(required = false) Boolean hasCopyrightFile,
            @RequestParam(required = false) Boolean urgent,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdTo) {
        OrderService.AdminOrderFilter filter = new OrderService.AdminOrderFilter(
                orderId,
                status,
                customerKeyword,
                regionKeyword,
                hasCopyrightFile,
                urgent,
                createdFrom,
                createdTo
        );
        return ApiResponse.ok(orderService.listAllPaged(page, size, filter));
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderDetailResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(orderService.getDetail(id));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<Order> updateStatus(@PathVariable Long id,
            @RequestBody @Valid UpdateStatusRequest req) {
        return ApiResponse.ok(orderService.updateStatus(id, req.status()));
    }

    @PutMapping("/{id}/ship")
    public ApiResponse<Order> ship(@PathVariable Long id) {
        return ApiResponse.ok(orderService.ship(id));
    }

    @PutMapping("/{id}/shipping-status")
    public ApiResponse<Order> updateShippingStatus(@PathVariable Long id,
            @RequestBody @Valid UpdateShippingStatusRequest req) {
        return ApiResponse.ok(orderService.updateShippingStatus(id, req.shippingStatus()));
    }

    @PutMapping("/{id}/production-status")
    public ApiResponse<Order> updateProductionStatus(@PathVariable Long id,
            @RequestBody @Valid UpdateProductionStatusRequest req) {
        return ApiResponse.ok(orderService.updateProductionStatus(id, req.productionStatus()));
    }

    @PutMapping("/{id}/review/approve")
    public ApiResponse<Order> approveReview(@PathVariable Long id) {
        Long adminUserId = StpUtil.getLoginIdAsLong();
        return ApiResponse.ok(orderService.approveToProduction(id, adminUserId));
    }

    @PutMapping("/{id}/review/reject")
    public ApiResponse<Order> rejectReview(@PathVariable Long id,
            @RequestBody @Valid RejectReviewRequest req) {
        Long adminUserId = StpUtil.getLoginIdAsLong();
        return ApiResponse.ok(orderService.rejectProduction(id, adminUserId, req.reason()));
    }

    @PutMapping("/{id}/production/next")
    public ApiResponse<Order> advanceProduction(@PathVariable Long id) {
        return ApiResponse.ok(orderService.advanceProductionStatus(id));
    }
}
