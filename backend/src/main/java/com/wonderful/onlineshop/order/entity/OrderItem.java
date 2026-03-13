package com.wonderful.onlineshop.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;

@TableName("order_item")
public class OrderItem {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("order_id")
    private Long orderId;

    @TableField("product_id")
    private Long productId;

    @TableField("product_name")
    private String productName;

    private Integer quantity;

    @TableField("unit_price")
    private BigDecimal unitPrice;

    @TableField("params_snapshot")
    private String paramsSnapshot;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public String getParamsSnapshot() {
        return paramsSnapshot;
    }

    public void setParamsSnapshot(String paramsSnapshot) {
        this.paramsSnapshot = paramsSnapshot;
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
