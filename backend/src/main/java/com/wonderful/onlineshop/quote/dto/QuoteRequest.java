package com.wonderful.onlineshop.quote.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record QuoteRequest(
        @NotNull Long productId,
        @NotNull @Min(1) Integer quantity,
        @NotEmpty @Valid List<QuoteRequestItem> items
) {
}
