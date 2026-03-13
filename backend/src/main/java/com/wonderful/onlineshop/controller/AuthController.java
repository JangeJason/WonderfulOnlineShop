package com.wonderful.onlineshop.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.wonderful.onlineshop.common.ApiResponse;
import com.wonderful.onlineshop.user.entity.User;
import com.wonderful.onlineshop.user.service.UserService;
import com.wonderful.onlineshop.user.service.WechatAuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final WechatAuthService wechatAuthService;

    public AuthController(UserService userService, WechatAuthService wechatAuthService) {
        this.userService = userService;
        this.wechatAuthService = wechatAuthService;
    }

    public record LoginRequest(String phone, String email, String username, @NotBlank String password) {
    }

    public record MobileLoginRequest(@NotBlank String phone) {
    }

    public record WechatPhoneLoginRequest(String code, String jsCode, String encryptedData, String iv, String nickname,
            String avatarUrl) {
    }

    public record RegisterRequest(
            @NotBlank @Size(min = 3, max = 20) String username,
            @NotBlank @Size(min = 4, max = 50) String password,
            String nickname,
            @NotBlank String phone,
            @NotBlank String email) {
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody @Valid LoginRequest req) {
        User user = userService.login(req.phone(), req.email(), req.username(), req.password());
        StpUtil.login(user.getId());
        return ApiResponse.ok(buildAuthData(user));
    }

    @PostMapping("/mobile-login")
    public ApiResponse<Map<String, Object>> mobileLogin(@RequestBody @Valid MobileLoginRequest req) {
        User user = userService.loginByPhone(req.phone());
        StpUtil.login(user.getId());
        return ApiResponse.ok(buildAuthData(user));
    }

    @PostMapping("/wechat/phone-login")
    public ApiResponse<Map<String, Object>> wechatPhoneLogin(@RequestBody @Valid WechatPhoneLoginRequest req) {
        String phone;
        try {
            if (req.code() != null && !req.code().isBlank()) {
                phone = wechatAuthService.resolvePhoneByCode(req.code());
            } else {
                phone = wechatAuthService.resolvePhoneByEncryptedData(req.jsCode(), req.encryptedData(), req.iv());
            }
        } catch (RuntimeException ex) {
            if (req.jsCode() != null && !req.jsCode().isBlank()
                    && req.encryptedData() != null && !req.encryptedData().isBlank()
                    && req.iv() != null && !req.iv().isBlank()) {
                phone = wechatAuthService.resolvePhoneByEncryptedData(req.jsCode(), req.encryptedData(), req.iv());
            } else {
                throw ex;
            }
        }
        User user = userService.loginOrCreateWechatUserByPhone(phone);
        userService.applyWechatProfile(user.getId(), req.nickname(), req.avatarUrl());
        user = userService.getById(user.getId());
        StpUtil.login(user.getId());
        return ApiResponse.ok(buildAuthData(user));
    }

    @PostMapping("/register")
    public ApiResponse<Map<String, Object>> register(@RequestBody @Valid RegisterRequest req) {
        User user = userService.register(req.username(), req.password(), req.nickname(), req.phone(), req.email());
        // Auto-login after registration
        StpUtil.login(user.getId());
        return ApiResponse.ok(buildAuthData(user));
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me() {
        Map<String, Object> data = new HashMap<>();
        data.put("isLogin", StpUtil.isLogin());
        if (StpUtil.isLogin()) {
            Long userId = StpUtil.getLoginIdAsLong();
            User user = userService.getById(userId);
            if (user != null) {
                data.put("loginId", userId);
                data.put("nickname", user.getNickname());
                data.put("companyName", user.getCompanyName());
                data.put("avatarUrl", user.getAvatarUrl());
                data.put("phone", user.getPhone());
                data.put("email", user.getEmail());
                data.put("role", user.getRole());
                data.put("username", user.getUsername());
                data.put("passwordSet", userService.isPasswordSet(user));
            }
        }
        return ApiResponse.ok(data);
    }

    public record UpdateProfileRequest(@NotBlank String nickname, String companyName, String avatarUrl, String phone,
            String email) {
    }

    public record ChangePasswordRequest(
            @NotBlank String oldPassword,
            @NotBlank @Size(min = 4, max = 50) String newPassword) {
    }

    public record InitPasswordRequest(@NotBlank @Size(min = 4, max = 50) String newPassword) {
    }

    public record WechatProfileSyncRequest(String nickname, String avatarUrl) {
    }

    @PutMapping("/profile")
    public ApiResponse<Void> updateProfile(@RequestBody @Valid UpdateProfileRequest req) {
        Long userId = StpUtil.getLoginIdAsLong();
        userService.updateProfile(userId, req.nickname(), req.companyName(), req.avatarUrl(), req.phone(), req.email());
        return ApiResponse.ok(null);
    }

    @PutMapping("/password")
    public ApiResponse<Void> changePassword(@RequestBody @Valid ChangePasswordRequest req) {
        Long userId = StpUtil.getLoginIdAsLong();
        userService.changePassword(userId, req.oldPassword(), req.newPassword());
        return ApiResponse.ok(null);
    }

    @PutMapping("/password/init")
    public ApiResponse<Void> initPassword(@RequestBody @Valid InitPasswordRequest req) {
        Long userId = StpUtil.getLoginIdAsLong();
        userService.initPassword(userId, req.newPassword());
        return ApiResponse.ok(null);
    }

    @PutMapping("/wechat/profile-sync")
    public ApiResponse<Void> syncWechatProfile(@RequestBody WechatProfileSyncRequest req) {
        Long userId = StpUtil.getLoginIdAsLong();
        userService.applyWechatProfile(userId, req.nickname(), req.avatarUrl());
        return ApiResponse.ok(null);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        StpUtil.logout();
        return ApiResponse.ok(null);
    }

    private Map<String, Object> buildAuthData(User user) {
        Map<String, Object> data = new HashMap<>();
        data.put("tokenName", StpUtil.getTokenName());
        data.put("tokenValue", StpUtil.getTokenValue());
        data.put("loginId", user.getId());
        data.put("nickname", user.getNickname());
        data.put("companyName", user.getCompanyName());
        data.put("avatarUrl", user.getAvatarUrl());
        data.put("phone", user.getPhone());
        data.put("email", user.getEmail());
        data.put("role", user.getRole());
        data.put("passwordSet", userService.isPasswordSet(user));
        return data;
    }
}
