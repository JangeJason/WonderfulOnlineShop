package com.wonderful.onlineshop.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.wonderful.onlineshop.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody @Valid LoginRequest req) {
        if (!"admin".equals(req.username()) || !"admin".equals(req.password())) {
            return ApiResponse.fail("invalid username or password");
        }

        StpUtil.login(1);

        Map<String, Object> data = new HashMap<>();
        data.put("tokenName", StpUtil.getTokenName());
        data.put("tokenValue", StpUtil.getTokenValue());
        data.put("loginId", StpUtil.getLoginId());
        return ApiResponse.ok(data);
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me() {
        Map<String, Object> data = new HashMap<>();
        data.put("isLogin", StpUtil.isLogin());
        data.put("loginId", StpUtil.getLoginIdDefaultNull());
        return ApiResponse.ok(data);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        StpUtil.logout();
        return ApiResponse.ok(null);
    }
}
