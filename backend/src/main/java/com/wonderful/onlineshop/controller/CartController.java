package com.wonderful.onlineshop.controller;

import com.wonderful.onlineshop.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    @GetMapping("/items")
    public ApiResponse<List<Map<String, Object>>> items() {
        return ApiResponse.ok(List.of());
    }
}
