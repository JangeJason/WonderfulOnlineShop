package com.wonderful.onlineshop.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.wonderful.onlineshop.common.ApiResponse;
import com.wonderful.onlineshop.operationlog.entity.AdminOperationLog;
import com.wonderful.onlineshop.operationlog.service.AdminOperationLogService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/admin/operation-logs")
public class AdminOperationLogController {
    private final AdminOperationLogService service;

    public AdminOperationLogController(AdminOperationLogService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<IPage<AdminOperationLog>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long adminUserId,
            @RequestParam(required = false) String moduleName,
            @RequestParam(required = false) String actionName,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        return ApiResponse.ok(service.page(page, size, adminUserId, moduleName, actionName, keyword, from, to));
    }
}
