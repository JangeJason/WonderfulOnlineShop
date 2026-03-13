package com.wonderful.onlineshop.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;

import java.math.BigDecimal;
import java.util.List;

@TableName(value = "product", autoResultMap = true)
public class Product {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String name;

    private String description;

    @TableField("image_url")
    private String imageUrl;

    @TableField(value = "image_urls", typeHandler = JacksonTypeHandler.class)
    private List<String> imageUrls;

    @TableField("base_price")
    private BigDecimal basePrice;

    private Integer status;

    @TableField("pricing_formula")
    private String pricingFormula;

    @TableField("category_id")
    private Long categoryId;

    @TableField("setup_fee")
    private BigDecimal setupFee;

    @TableField("free_setup_quantity")
    private Integer freeSetupQuantity;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getPricingFormula() {
        return pricingFormula;
    }

    public void setPricingFormula(String pricingFormula) {
        this.pricingFormula = pricingFormula;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public BigDecimal getSetupFee() {
        return setupFee;
    }

    public void setSetupFee(BigDecimal setupFee) {
        this.setupFee = setupFee;
    }

    public Integer getFreeSetupQuantity() {
        return freeSetupQuantity;
    }

    public void setFreeSetupQuantity(Integer freeSetupQuantity) {
        this.freeSetupQuantity = freeSetupQuantity;
    }
}
