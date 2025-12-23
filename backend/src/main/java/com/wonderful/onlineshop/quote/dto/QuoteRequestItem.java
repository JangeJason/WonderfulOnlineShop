package com.wonderful.onlineshop.quote.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record QuoteRequestItem(
        @NotNull Long parameterId,
        String paramCode,
        BigDecimal valueNumber,
        List<Long> selectedOptionIds
) {
}
