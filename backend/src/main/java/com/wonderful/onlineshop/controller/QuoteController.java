package com.wonderful.onlineshop.controller;

import com.wonderful.onlineshop.common.ApiResponse;
import com.wonderful.onlineshop.quote.service.QuoteService;
import com.wonderful.onlineshop.quote.dto.QuoteRequest;
import com.wonderful.onlineshop.quote.dto.QuoteResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class QuoteController {

    private final QuoteService quoteService;

    public QuoteController(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    @PostMapping("/quote")
    public ApiResponse<QuoteResponse> quote(@RequestBody @Valid QuoteRequest req) {
        return ApiResponse.ok(quoteService.quote(req));
    }
}
