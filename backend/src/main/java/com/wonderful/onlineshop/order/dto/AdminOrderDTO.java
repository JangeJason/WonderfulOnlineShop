package com.wonderful.onlineshop.order.dto;

import com.wonderful.onlineshop.order.entity.Order;

public class AdminOrderDTO extends Order {
    private String userNickname;
    private String userCompanyName;
    private boolean hasCopyrightWarning;

    public AdminOrderDTO(Order order) {
        this.setId(order.getId());
        this.setUserId(order.getUserId());
        this.setTotalAmount(order.getTotalAmount());
        this.setStatus(order.getStatus());
        this.setRemark(order.getRemark());
        this.setReceiverName(order.getReceiverName());
        this.setReceiverPhone(order.getReceiverPhone());
        this.setReceiverAddress(order.getReceiverAddress());
        this.setCreatedAt(order.getCreatedAt());
        this.setUpdatedAt(order.getUpdatedAt());
    }

    public String getUserNickname() {
        return userNickname;
    }

    public void setUserNickname(String userNickname) {
        this.userNickname = userNickname;
    }

    public String getUserCompanyName() {
        return userCompanyName;
    }

    public void setUserCompanyName(String userCompanyName) {
        this.userCompanyName = userCompanyName;
    }

    public boolean isHasCopyrightWarning() {
        return hasCopyrightWarning;
    }

    public void setHasCopyrightWarning(boolean hasCopyrightWarning) {
        this.hasCopyrightWarning = hasCopyrightWarning;
    }
}
