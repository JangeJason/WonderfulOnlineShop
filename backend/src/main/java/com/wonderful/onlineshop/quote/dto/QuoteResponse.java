package com.wonderful.onlineshop.quote.dto;

import java.math.BigDecimal;
import java.util.Map;

public record QuoteResponse(
        Long productId,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice,
        Map<String, BigDecimal> breakdown
) {
}
