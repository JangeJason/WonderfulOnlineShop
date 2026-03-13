package com.wonderful.onlineshop.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@TableName(value = "product_parameters", autoResultMap = true)
public class ProductParameter {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("product_id")
    private Long productId;

    @TableField("param_name")
    private String paramName;

    @TableField("param_type")
    private String paramType; // INPUT, SELECT

    @TableField("is_required")
    @JsonProperty("isRequired")
    @JsonAlias({ "required" })
    private Boolean isRequired;

    @TableField("is_multiple")
    @JsonProperty("isMultiple")
    @JsonAlias({ "multiple" })
    private Boolean isMultiple;

    @TableField("is_dynamic_by_material")
    @JsonProperty("isDynamicByMaterial")
    @JsonAlias({ "dynamicByMaterial" })
    private Boolean isDynamicByMaterial;

    @TableField("unit")
    private String unit;

    @TableField(value = "pricing_rule", typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> pricingRule;

    @TableField(value = "validation_rules", typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> validationRules;

    @TableField("sort_order")
    private Integer sortOrder;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getParamName() {
        return paramName;
    }

    public void setParamName(String paramName) {
        this.paramName = paramName;
    }

    public String getParamType() {
        return paramType;
    }

    public void setParamType(String paramType) {
        this.paramType = paramType;
    }

    public Boolean getIsRequired() {
        return isRequired;
    }

    public void setIsRequired(Boolean isRequired) {
        this.isRequired = isRequired;
    }

    // Backward-compatible accessors for existing tests/callers
    public Boolean getRequired() {
        return isRequired;
    }

    public void setRequired(Boolean required) {
        this.isRequired = required;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Boolean getIsMultiple() {
        return isMultiple;
    }

    public void setIsMultiple(Boolean isMultiple) {
        this.isMultiple = isMultiple;
    }

    public Boolean getMultiple() {
        return isMultiple;
    }

    public void setMultiple(Boolean multiple) {
        this.isMultiple = multiple;
    }

    public Boolean getIsDynamicByMaterial() {
        return isDynamicByMaterial;
    }

    public void setIsDynamicByMaterial(Boolean isDynamicByMaterial) {
        this.isDynamicByMaterial = isDynamicByMaterial;
    }

    public Boolean getDynamicByMaterial() {
        return isDynamicByMaterial;
    }

    public void setDynamicByMaterial(Boolean dynamicByMaterial) {
        this.isDynamicByMaterial = dynamicByMaterial;
    }

    public Map<String, Object> getPricingRule() {
        return pricingRule;
    }

    public void setPricingRule(Map<String, Object> pricingRule) {
        this.pricingRule = pricingRule;
    }

    public Map<String, Object> getValidationRules() {
        return validationRules;
    }

    public void setValidationRules(Map<String, Object> validationRules) {
        this.validationRules = validationRules;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
