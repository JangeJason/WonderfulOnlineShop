package com.wonderful.onlineshop.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.wonderful.onlineshop.common.ApiResponse;
import com.wonderful.onlineshop.order.dto.OrderDetailResponse;
import com.wonderful.onlineshop.order.dto.OrderPayCreateResponse;
import com.wonderful.onlineshop.order.entity.Order;
import com.wonderful.onlineshop.order.entity.OrderItem;
import com.wonderful.onlineshop.order.service.OrderPaymentService;
import com.wonderful.onlineshop.order.service.OrderService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrdersController {

    private final OrderService orderService;
    private final OrderPaymentService orderPaymentService;

    public OrdersController(OrderService orderService, OrderPaymentService orderPaymentService) {
        this.orderService = orderService;
        this.orderPaymentService = orderPaymentService;
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

    public record CreatePayRequest(String channel, String jsCode, String clientIp) {
    }

    @PostMapping("/{id}/pay/create")
    public ApiResponse<OrderPayCreateResponse> createPay(@PathVariable("id") Long id, @RequestBody(required = false) CreatePayRequest req) {
        Long userId = StpUtil.getLoginIdAsLong();
        String channel = req == null ? null : req.channel();
        String jsCode = req == null ? null : req.jsCode();
        String clientIp = req == null ? null : req.clientIp();
        return ApiResponse.ok(orderPaymentService.createPayment(id, userId, channel, jsCode, clientIp));
    }

    @PostMapping("/{id}/pay/sync")
    public ApiResponse<Order> syncPayStatus(@PathVariable("id") Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        return ApiResponse.ok(orderPaymentService.syncPayment(id, userId));
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

    @PutMapping("/{id}/after-sale")
    public ApiResponse<Order> applyAfterSale(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        return ApiResponse.ok(orderService.applyAfterSale(id, userId));
    }

    @PutMapping("/{id}/resubmit-review")
    public ApiResponse<Order> resubmitReview(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        return ApiResponse.ok(orderService.resubmitReview(id, userId));
    }

    public record CustomNameRequest(String customName) {
    }

    @PutMapping("/{id}/custom-name")
    public ApiResponse<Order> updateCustomName(@PathVariable("id") Long id, @RequestBody CustomNameRequest req) {
        System.out.println("DEBUG ARRIVED PUT custom-name for id=" + id);
        Long userId = StpUtil.getLoginIdAsLong();
        return ApiResponse.ok(orderService.updateCustomName(id, userId, req.customName()));
    }

    public record UpdateRejectedOrderItemRequest(
            Integer quantity,
            String paramsSnapshot,
            BigDecimal unitPrice,
            String printFileUrl,
            String proofFileUrl,
            Boolean hasCopyright,
            String copyrightFileUrl
    ) {
    }

    public record UpdateOrderItemFilesRequest(
            String printFileUrl,
            String printFileName,
            String proofFileUrl,
            String proofFileName
    ) {
    }

    @PutMapping("/{id}/items/{itemId}")
    public ApiResponse<OrderItem> updateRejectedOrderItem(
            @PathVariable("id") Long id,
            @PathVariable("itemId") Long itemId,
            @RequestBody UpdateRejectedOrderItemRequest req
    ) {
        Long userId = StpUtil.getLoginIdAsLong();
        return ApiResponse.ok(orderService.updateRejectedOrderItem(
                id,
                itemId,
                userId,
                req.quantity(),
                req.paramsSnapshot(),
                req.unitPrice(),
                req.printFileUrl(),
                req.proofFileUrl(),
                req.hasCopyright(),
                req.copyrightFileUrl()
        ));
    }

    @PutMapping("/{id}/items/{itemId}/files")
    public ApiResponse<OrderItem> updateOrderItemFiles(
            @PathVariable("id") Long id,
            @PathVariable("itemId") Long itemId,
            @RequestBody UpdateOrderItemFilesRequest req
    ) {
        Long userId = StpUtil.getLoginIdAsLong();
        return ApiResponse.ok(orderService.updateOrderItemFiles(
                id,
                itemId,
                userId,
                req.printFileUrl(),
                req.printFileName(),
                req.proofFileUrl(),
                req.proofFileName()
        ));
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
