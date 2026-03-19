package com.wonderful.onlineshop.order.dto;

import com.wonderful.onlineshop.order.entity.Order;

import java.util.List;

public class AdminOrderDTO extends Order {
    private String userNickname;
    private String userCompanyName;
    private String userPhone;
    private String userEmail;
    private boolean hasCopyrightWarning;
    private boolean hasCopyrightIssue;
    private boolean urgent;
    private List<String> previewProductNames;
    private int previewProductCount;

    public AdminOrderDTO(Order order) {
        this.setId(order.getId());
        this.setUserId(order.getUserId());
        this.setTotalAmount(order.getTotalAmount());
        this.setStatus(order.getStatus());
        this.setProductionStatus(order.getProductionStatus());
        this.setShippingStatus(order.getShippingStatus());
        this.setReviewStatus(order.getReviewStatus());
        this.setReviewReason(order.getReviewReason());
        this.setReviewedBy(order.getReviewedBy());
        this.setReviewedAt(order.getReviewedAt());
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

    public String getUserPhone() {
        return userPhone;
    }

    public void setUserPhone(String userPhone) {
        this.userPhone = userPhone;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public boolean isHasCopyrightWarning() {
        return hasCopyrightWarning;
    }

    public void setHasCopyrightWarning(boolean hasCopyrightWarning) {
        this.hasCopyrightWarning = hasCopyrightWarning;
    }

    public boolean isHasCopyrightIssue() {
        return hasCopyrightIssue;
    }

    public void setHasCopyrightIssue(boolean hasCopyrightIssue) {
        this.hasCopyrightIssue = hasCopyrightIssue;
    }

    public boolean isUrgent() {
        return urgent;
    }

    public void setUrgent(boolean urgent) {
        this.urgent = urgent;
    }

    public List<String> getPreviewProductNames() {
        return previewProductNames;
    }

    public void setPreviewProductNames(List<String> previewProductNames) {
        this.previewProductNames = previewProductNames;
    }

    public int getPreviewProductCount() {
        return previewProductCount;
    }

    public void setPreviewProductCount(int previewProductCount) {
        this.previewProductCount = previewProductCount;
    }
}
