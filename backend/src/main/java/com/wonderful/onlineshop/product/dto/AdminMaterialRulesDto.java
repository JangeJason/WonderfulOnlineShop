package com.wonderful.onlineshop.product.dto;

import java.util.List;

public class AdminMaterialRulesDto {

    public record ParamRuleItem(Long paramId, Boolean enabled, List<Long> optionIds) {
    }

    public record MaterialRulesPayload(Long materialOptionId, List<ParamRuleItem> rules) {
    }

    public record MaterialRulesResponse(Long materialParameterId, Long materialOptionId, List<ParamRuleItem> rules) {
    }
}
