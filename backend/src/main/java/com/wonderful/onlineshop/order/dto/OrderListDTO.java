package com.wonderful.onlineshop.order.dto;

import com.wonderful.onlineshop.order.entity.Order;
import com.wonderful.onlineshop.order.entity.OrderItem;

import java.util.List;

public class OrderListDTO extends Order {
    private List<OrderItem> previewItems;
    private int totalProductCount;
    private boolean hasCopyrightWarning;

    public OrderListDTO(Order order) {
        this.setId(order.getId());
        this.setUserId(order.getUserId());
        this.setTotalAmount(order.getTotalAmount());
        this.setStatus(order.getStatus());
        this.setCustomName(order.getCustomName());
        this.setRemark(order.getRemark());
        this.setReceiverName(order.getReceiverName());
        this.setReceiverPhone(order.getReceiverPhone());
        this.setReceiverAddress(order.getReceiverAddress());
        this.setCreatedAt(order.getCreatedAt());
        this.setUpdatedAt(order.getUpdatedAt());
    }

    public List<OrderItem> getPreviewItems() {
        return previewItems;
    }

    public void setPreviewItems(List<OrderItem> previewItems) {
        this.previewItems = previewItems;
    }

    public int getTotalProductCount() {
        return totalProductCount;
    }

    public void setTotalProductCount(int totalProductCount) {
        this.totalProductCount = totalProductCount;
    }

    public boolean isHasCopyrightWarning() {
        return hasCopyrightWarning;
    }

    public void setHasCopyrightWarning(boolean hasCopyrightWarning) {
        this.hasCopyrightWarning = hasCopyrightWarning;
    }
}
