package com.wonderful.onlineshop.cart.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("cart_item")
public class CartItem {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("product_id")
    private Long productId;

    private Integer quantity;

    @TableField("params_snapshot")
    private String paramsSnapshot;

    @TableField("unit_price")
    private BigDecimal unitPrice;

    @TableField("created_at")
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getParamsSnapshot() {
        return paramsSnapshot;
    }

    public void setParamsSnapshot(String paramsSnapshot) {
        this.paramsSnapshot = paramsSnapshot;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @TableField("design_file_url")
    private String printFileUrl;

    public String getPrintFileUrl() {
        return printFileUrl;
    }

    public void setPrintFileUrl(String printFileUrl) {
        this.printFileUrl = printFileUrl;
    }

    @TableField("proof_file_url")
    private String proofFileUrl;

    public String getProofFileUrl() {
        return proofFileUrl;
    }

    public void setProofFileUrl(String proofFileUrl) {
        this.proofFileUrl = proofFileUrl;
    }

    @TableField(exist = false)
    private String productName;

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    @TableField("has_copyright")
    private Boolean hasCopyright;

    public Boolean getHasCopyright() {
        return hasCopyright;
    }

    public void setHasCopyright(Boolean hasCopyright) {
        this.hasCopyright = hasCopyright;
    }

    @TableField("copyright_file_url")
    private String copyrightFileUrl;

    public String getCopyrightFileUrl() {
        return copyrightFileUrl;
    }

    public void setCopyrightFileUrl(String copyrightFileUrl) {
        this.copyrightFileUrl = copyrightFileUrl;
    }
}
