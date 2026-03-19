package com.wonderful.onlineshop.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.wonderful.onlineshop.aftersale.service.AfterSaleService;
import com.wonderful.onlineshop.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/after-sales")
public class AdminAfterSaleController {
    private final AfterSaleService afterSaleService;

    public AdminAfterSaleController(AfterSaleService afterSaleService) {
        this.afterSaleService = afterSaleService;
    }

    public record UpdateStatusRequest(@NotBlank String status, String adminRemark) {}

    @GetMapping
    public ApiResponse<IPage<AfterSaleService.AfterSaleDTO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.ok(afterSaleService.listAdmin(page, size, status, orderId, keyword));
    }

    @GetMapping("/{id}")
    public ApiResponse<AfterSaleService.AfterSaleDetailDTO> detail(@PathVariable Long id) {
        return ApiResponse.ok(afterSaleService.detailAdmin(id));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(@PathVariable Long id, @RequestBody @Valid UpdateStatusRequest req) {
        afterSaleService.updateStatus(id, req.status(), req.adminRemark());
        return ApiResponse.ok(null);
    }
}
