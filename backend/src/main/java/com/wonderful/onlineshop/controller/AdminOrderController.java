package com.wonderful.onlineshop.controller;

import com.wonderful.onlineshop.common.ApiResponse;
import com.wonderful.onlineshop.order.dto.OrderDetailResponse;
import com.wonderful.onlineshop.order.entity.Order;
import com.wonderful.onlineshop.order.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/orders")
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    public record UpdateStatusRequest(@NotBlank String status) {
    }

    @GetMapping
    public ApiResponse<com.baomidou.mybatisplus.core.metadata.IPage<com.wonderful.onlineshop.order.dto.AdminOrderDTO>> listAll(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(orderService.listAllPaged(page, size));
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
}
