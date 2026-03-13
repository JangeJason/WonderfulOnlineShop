package com.wonderful.onlineshop.product.dto;

import java.util.List;
import java.util.Map;

public record MaterialConfigResponse(
        Long materialParameterId,
        Long materialOptionId,
        List<Long> enabledDynamicParamIds,
        Map<Long, List<Long>> enabledOptionIds
) {
}
