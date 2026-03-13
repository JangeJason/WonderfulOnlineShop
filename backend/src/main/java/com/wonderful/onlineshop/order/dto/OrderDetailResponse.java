package com.wonderful.onlineshop.order.dto;

import com.wonderful.onlineshop.order.entity.OrderItem;

import java.util.List;

public record OrderDetailResponse(
                AdminOrderDTO order,
                List<OrderItem> items) {
}
