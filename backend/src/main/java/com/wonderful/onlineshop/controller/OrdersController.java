package com.wonderful.onlineshop.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.wonderful.onlineshop.common.ApiResponse;
import com.wonderful.onlineshop.order.dto.OrderDetailResponse;
import com.wonderful.onlineshop.order.entity.Order;
import com.wonderful.onlineshop.order.service.OrderService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrdersController {

    private final OrderService orderService;

    public OrdersController(OrderService orderService) {
        this.orderService = orderService;
    }

    public record CreateOrderRequest(String remark, List<Long> cartItemIds, Long addressId) {
    }

    @PostMapping
    public ApiResponse<Order> create(@RequestBody(required = false) CreateOrderRequest req) {
        Long userId = StpUtil.getLoginIdAsLong();
        String remark = req != null ? req.remark() : null;
        List<Long> cartItemIds = req != null ? req.cartItemIds() : null;
        Long addressId = req != null ? req.addressId() : null;
        return ApiResponse.ok(orderService.createFromCart(userId, remark, cartItemIds, addressId));
    }

    @GetMapping("/list")
    public ApiResponse<List<com.wonderful.onlineshop.order.dto.OrderListDTO>> list() {
        Long userId = StpUtil.getLoginIdAsLong();
        return ApiResponse.ok(orderService.listByUser(userId));
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderDetailResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(orderService.getDetail(id));
    }

    @PutMapping("/{id}/pay")
    public ApiResponse<Order> pay(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        return ApiResponse.ok(orderService.pay(id, userId));
    }

    @PutMapping("/{id}/cancel")
    public ApiResponse<Order> cancel(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        return ApiResponse.ok(orderService.cancel(id, userId));
    }

    @PutMapping("/{id}/confirm")
    public ApiResponse<Order> confirmReceive(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        return ApiResponse.ok(orderService.confirmReceive(id, userId));
    }

    public record CustomNameRequest(String customName) {
    }

    @PutMapping("/{id}/custom-name")
    public ApiResponse<Order> updateCustomName(@PathVariable("id") Long id, @RequestBody CustomNameRequest req) {
        System.out.println("DEBUG ARRIVED PUT custom-name for id=" + id);
        Long userId = StpUtil.getLoginIdAsLong();
        return ApiResponse.ok(orderService.updateCustomName(id, userId, req.customName()));
    }

    @PostMapping("/{id}/reorder")
    public ApiResponse<Integer> reorder(@PathVariable("id") Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        return ApiResponse.ok(orderService.reorderToCart(id, userId));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteCancelledOrder(@PathVariable("id") Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        orderService.deleteCancelledOrder(id, userId);
        return ApiResponse.ok(null);
    }
}
