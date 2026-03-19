package com.wonderful.onlineshop.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.wonderful.onlineshop.aftersale.service.AfterSaleService;
import com.wonderful.onlineshop.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/after-sales")
public class AfterSaleController {
    private final AfterSaleService afterSaleService;

    public AfterSaleController(AfterSaleService afterSaleService) {
        this.afterSaleService = afterSaleService;
    }

    public record CreateAfterSaleRequest(
            @NotNull Long orderId,
            @NotEmpty List<Long> itemIds,
            @NotBlank String requestType,
            String detailText,
            List<String> imageUrls
    ) {}

    @PostMapping
    public ApiResponse<AfterSaleService.AfterSaleDTO> create(@RequestBody @Valid CreateAfterSaleRequest req) {
        Long userId = StpUtil.getLoginIdAsLong();
        return ApiResponse.ok(afterSaleService.create(userId, new AfterSaleService.CreateAfterSaleRequest(
                req.orderId(),
                req.itemIds(),
                req.requestType(),
                req.detailText(),
                req.imageUrls()
        )));
    }

    @GetMapping("/list")
    public ApiResponse<List<AfterSaleService.AfterSaleDTO>> list() {
        Long userId = StpUtil.getLoginIdAsLong();
        return ApiResponse.ok(afterSaleService.listByUser(userId));
    }

    @GetMapping("/{id}")
    public ApiResponse<AfterSaleService.AfterSaleDetailDTO> detail(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        return ApiResponse.ok(afterSaleService.detailByUser(userId, id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        afterSaleService.deleteByUser(userId, id);
        return ApiResponse.ok(null);
    }
}
