package com.wonderful.onlineshop.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.wonderful.onlineshop.common.ApiResponse;
import com.wonderful.onlineshop.user.entity.Address;
import com.wonderful.onlineshop.user.service.AddressService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
@SaCheckLogin
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping
    public ApiResponse<List<Address>> list() {
        Long userId = StpUtil.getLoginIdAsLong();
        return ApiResponse.ok(addressService.listByUserId(userId));
    }

    @PostMapping
    public ApiResponse<Address> add(@RequestBody Address address) {
        Long userId = StpUtil.getLoginIdAsLong();
        return ApiResponse.ok(addressService.addAddress(userId, address));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody Address address) {
        Long userId = StpUtil.getLoginIdAsLong();
        addressService.updateAddress(userId, id, address);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        addressService.deleteAddress(userId, id);
        return ApiResponse.ok(null);
    }

    @PutMapping("/{id}/default")
    public ApiResponse<Void> setDefault(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        addressService.setDefault(userId, id);
        return ApiResponse.ok(null);
    }
}
