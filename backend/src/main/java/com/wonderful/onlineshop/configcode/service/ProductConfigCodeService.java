package com.wonderful.onlineshop.configcode.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wonderful.onlineshop.common.BusinessException;
import com.wonderful.onlineshop.configcode.entity.ProductConfigCode;
import com.wonderful.onlineshop.configcode.mapper.ProductConfigCodeMapper;
import com.wonderful.onlineshop.product.entity.Product;
import com.wonderful.onlineshop.product.service.ProductService;
import com.wonderful.onlineshop.user.entity.User;
import com.wonderful.onlineshop.user.mapper.UserMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Set;

@Service
public class ProductConfigCodeService {

    private static final Set<Integer> ALLOWED_EXPIRE_DAYS = Set.of(0, 1, 7, 14, 30, 90);
    private static final int RATE_LIMIT_PER_MINUTE = 10;
    private static final int CODE_GENERATE_MAX_RETRY = 30;

    private final ProductConfigCodeMapper productConfigCodeMapper;
    private final ProductService productService;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SecureRandom secureRandom = new SecureRandom();

    public ProductConfigCodeService(ProductConfigCodeMapper productConfigCodeMapper,
                                    ProductService productService,
                                    UserMapper userMapper) {
        this.productConfigCodeMapper = productConfigCodeMapper;
        this.productService = productService;
        this.userMapper = userMapper;
    }

    public CreateResponse create(Long userId, CreateRequest req) {
        if (req == null) {
            throw new BusinessException("请求参数不能为空");
        }
        if (req.productId() == null || req.productId() <= 0) {
            throw new BusinessException("productId 无效");
        }
        if (req.expireDays() == null || !ALLOWED_EXPIRE_DAYS.contains(req.expireDays())) {
            throw new BusinessException("有效期选项无效");
        }

        Product product = productService.getActiveRequired(req.productId());
        String normalizedSnapshot = normalizeSnapshot(req.paramsSnapshot());
        checkRateLimit(userId);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expireAt = req.expireDays() == 0 ? null : now.plusDays(req.expireDays());
        String creatorName = resolveCreatorName(userId);

        ProductConfigCode entity = new ProductConfigCode();
        entity.setCreatorUserId(userId);
        entity.setProductId(req.productId());
        entity.setParamsSnapshot(normalizedSnapshot);
        entity.setExpireAt(expireAt);
        entity.setCreatedAt(now);

        for (int i = 0; i < CODE_GENERATE_MAX_RETRY; i++) {
            entity.setCode(generate8DigitCode());
            try {
                productConfigCodeMapper.insert(entity);
                return new CreateResponse(entity.getCode(), req.productId(), product.getName(), creatorName, now,
                        expireAt, req.expireDays());
            } catch (DuplicateKeyException ignore) {
                // Retry with a new random numeric code when collision happens.
            }
        }
        throw new BusinessException("生成配置码失败，请重试");
    }

    public ResolveResponse resolve(String code) {
        String safeCode = sanitizeCode(code);
        ProductConfigCode entity = productConfigCodeMapper.selectOne(
                new LambdaQueryWrapper<ProductConfigCode>()
                        .eq(ProductConfigCode::getCode, safeCode)
                        .last("LIMIT 1"));
        if (entity == null) {
            throw new BusinessException("配置码不存在/已删除");
        }
        if (isExpired(entity.getExpireAt())) {
            throw new BusinessException("配置码已过期");
        }

        Product product = productService.getActiveRequired(entity.getProductId());
        String creatorName = resolveCreatorName(entity.getCreatorUserId());
        return new ResolveResponse(
                entity.getCode(),
                entity.getProductId(),
                product.getName(),
                creatorName,
                entity.getParamsSnapshot(),
                entity.getCreatedAt(),
                entity.getExpireAt(),
                false);
    }

    private void checkRateLimit(Long userId) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(1);
        Long count = productConfigCodeMapper.selectCount(
                new LambdaQueryWrapper<ProductConfigCode>()
                        .eq(ProductConfigCode::getCreatorUserId, userId)
                        .ge(ProductConfigCode::getCreatedAt, since));
        if (count != null && count >= RATE_LIMIT_PER_MINUTE) {
            throw new BusinessException("生成过于频繁，请稍后再试");
        }
    }

    private String sanitizeCode(String rawCode) {
        String code = rawCode == null ? "" : rawCode.trim();
        if (!code.matches("\\d{8}")) {
            throw new BusinessException("配置码格式无效");
        }
        return code;
    }

    private boolean isExpired(LocalDateTime expireAt) {
        if (expireAt == null) return false;
        return LocalDateTime.now().isAfter(expireAt);
    }

    private String generate8DigitCode() {
        int n = 10000000 + secureRandom.nextInt(90000000);
        return String.valueOf(n);
    }

    private String normalizeSnapshot(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BusinessException("参数快照不能为空");
        }
        try {
            JsonNode root = objectMapper.readTree(raw);
            JsonNode paramsNode = root.get("params");
            if (paramsNode == null || !paramsNode.isArray()) {
                throw new BusinessException("参数快照格式无效");
            }
            return objectMapper.writeValueAsString(root);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("参数快照格式无效");
        }
    }

    private String resolveCreatorName(Long userId) {
        if (userId == null) return "用户";
        User user = userMapper.selectById(userId);
        if (user == null) return "用户";
        if (user.getNickname() != null && !user.getNickname().isBlank()) {
            return user.getNickname().trim();
        }
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return user.getUsername().trim();
        }
        return "用户";
    }

    public record CreateRequest(Long productId, String paramsSnapshot, Integer expireDays) {
    }

    public record CreateResponse(
            String code,
            Long productId,
            String productName,
            String creatorName,
            LocalDateTime createdAt,
            LocalDateTime expireAt,
            Integer expireDays) {
    }

    public record ResolveResponse(
            String code,
            Long productId,
            String productName,
            String creatorName,
            String paramsSnapshot,
            LocalDateTime createdAt,
            LocalDateTime expireAt,
            boolean expired) {
    }
}
