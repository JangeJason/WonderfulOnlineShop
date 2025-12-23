package com.wonderful.onlineshop.controller;

import com.wonderful.onlineshop.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrdersController {

    @GetMapping("/list")
    public ApiResponse<List<Map<String, Object>>> list() {
        return ApiResponse.ok(List.of());
    }
}
