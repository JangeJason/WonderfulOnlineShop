package com.wonderful.onlineshop.quote.service;

import com.wonderful.onlineshop.quote.dto.QuoteRequest;
import com.wonderful.onlineshop.quote.dto.QuoteRequestItem;
import com.wonderful.onlineshop.quote.dto.QuoteResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class QuoteService {

    public QuoteResponse quote(QuoteRequest req) {
        BigDecimal basePrice = new BigDecimal("800.00");

        BigDecimal optionAdjust = calcOptionAdjust(req.items());
        BigDecimal formulaPart = calcFormulaPart(req.items());

        BigDecimal unitPrice = basePrice.add(optionAdjust).add(formulaPart)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalPrice = unitPrice.multiply(new BigDecimal(req.quantity()))
                .setScale(2, RoundingMode.HALF_UP);

        Map<String, BigDecimal> breakdown = new HashMap<>();
        breakdown.put("basePrice", basePrice);
        breakdown.put("optionAdjust", optionAdjust);
        breakdown.put("formulaPart", formulaPart);

        return new QuoteResponse(req.productId(), req.quantity(), unitPrice, totalPrice, breakdown);
    }

    private BigDecimal calcOptionAdjust(List<QuoteRequestItem> items) {
        BigDecimal perOptionAdjust = new BigDecimal("50.00");
        BigDecimal sum = BigDecimal.ZERO;
        for (QuoteRequestItem item : items) {
            if (item.selectedOptionIds() == null || item.selectedOptionIds().isEmpty()) {
                continue;
            }
            sum = sum.add(perOptionAdjust.multiply(new BigDecimal(item.selectedOptionIds().size())));
        }
        return sum;
    }

    private BigDecimal calcFormulaPart(List<QuoteRequestItem> items) {
        BigDecimal L = null;
        BigDecimal W = null;

        for (QuoteRequestItem item : items) {
            if (item.paramCode() == null) {
                continue;
            }
            if (Objects.equals(item.paramCode(), "L") && item.valueNumber() != null) {
                L = item.valueNumber();
            }
            if (Objects.equals(item.paramCode(), "W") && item.valueNumber() != null) {
                W = item.valueNumber();
            }
        }

        if (L == null || W == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal coef = new BigDecimal("0.0002");
        return L.multiply(W).multiply(coef).setScale(2, RoundingMode.HALF_UP);
    }
}
