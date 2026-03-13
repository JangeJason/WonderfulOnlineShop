package com.wonderful.onlineshop.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.wonderful.onlineshop.common.ApiResponse;
import com.wonderful.onlineshop.user.service.UserCertificateService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/certificates")
public class UserCertificateController {

    private final UserCertificateService userCertificateService;

    public UserCertificateController(UserCertificateService userCertificateService) {
        this.userCertificateService = userCertificateService;
    }

    public record CreateCertificateRequest(
            @NotBlank String certificateType,
            String trademarkType,
            @NotBlank String trademarkContent,
            @NotBlank String principal,
            @NotBlank String endDate,
            @NotEmpty List<String> fileUrls,
            List<String> fileNames) {
    }

    @GetMapping("/common")
    public ApiResponse<List<UserCertificateService.CertificateDTO>> listCommon() {
        Long userId = StpUtil.getLoginIdAsLong();
        return ApiResponse.ok(userCertificateService.listByUser(userId));
    }

    @PostMapping("/common")
    public ApiResponse<UserCertificateService.CertificateDTO> createCommon(
            @RequestBody @Valid CreateCertificateRequest req) {
        Long userId = StpUtil.getLoginIdAsLong();
        UserCertificateService.CreateCertificateRequest serviceReq = new UserCertificateService.CreateCertificateRequest(
                req.certificateType(),
                req.trademarkType(),
                req.trademarkContent(),
                req.principal(),
                req.endDate(),
                req.fileUrls(),
                req.fileNames());
        return ApiResponse.ok(userCertificateService.create(userId, serviceReq));
    }

    @PutMapping("/common/{id}")
    public ApiResponse<UserCertificateService.CertificateDTO> updateCommon(
            @PathVariable("id") Long id,
            @RequestBody @Valid CreateCertificateRequest req) {
        Long userId = StpUtil.getLoginIdAsLong();
        UserCertificateService.CreateCertificateRequest serviceReq = new UserCertificateService.CreateCertificateRequest(
                req.certificateType(),
                req.trademarkType(),
                req.trademarkContent(),
                req.principal(),
                req.endDate(),
                req.fileUrls(),
                req.fileNames());
        return ApiResponse.ok(userCertificateService.update(userId, id, serviceReq));
    }
}
