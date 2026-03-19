package com.wonderful.onlineshop.favorite.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wonderful.onlineshop.common.BusinessException;
import com.wonderful.onlineshop.configcode.entity.ProductConfigCode;
import com.wonderful.onlineshop.configcode.mapper.ProductConfigCodeMapper;
import com.wonderful.onlineshop.favorite.entity.UserFavoriteConfigCode;
import com.wonderful.onlineshop.favorite.entity.UserFavoriteProduct;
import com.wonderful.onlineshop.favorite.mapper.UserFavoriteConfigCodeMapper;
import com.wonderful.onlineshop.favorite.mapper.UserFavoriteProductMapper;
import com.wonderful.onlineshop.product.entity.Product;
import com.wonderful.onlineshop.product.mapper.ProductMapper;
import com.wonderful.onlineshop.product.service.ProductService;
import com.wonderful.onlineshop.user.entity.User;
import com.wonderful.onlineshop.user.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FavoriteService {

    private final ProductService productService;
    private final ProductMapper productMapper;
    private final ProductConfigCodeMapper productConfigCodeMapper;
    private final UserFavoriteProductMapper userFavoriteProductMapper;
    private final UserFavoriteConfigCodeMapper userFavoriteConfigCodeMapper;
    private final UserMapper userMapper;

    public FavoriteService(ProductService productService,
                           ProductMapper productMapper,
                           ProductConfigCodeMapper productConfigCodeMapper,
                           UserFavoriteProductMapper userFavoriteProductMapper,
                           UserFavoriteConfigCodeMapper userFavoriteConfigCodeMapper,
                           UserMapper userMapper) {
        this.productService = productService;
        this.productMapper = productMapper;
        this.productConfigCodeMapper = productConfigCodeMapper;
        this.userFavoriteProductMapper = userFavoriteProductMapper;
        this.userFavoriteConfigCodeMapper = userFavoriteConfigCodeMapper;
        this.userMapper = userMapper;
    }

    public void favoriteProduct(Long userId, Long productId) {
        if (productId == null || productId <= 0) {
            throw new BusinessException("productId 无效");
        }
        productService.getById(productId);
        UserFavoriteProduct exists = userFavoriteProductMapper.selectOne(
                new LambdaQueryWrapper<UserFavoriteProduct>()
                        .eq(UserFavoriteProduct::getUserId, userId)
                        .eq(UserFavoriteProduct::getProductId, productId)
                        .last("LIMIT 1"));
        if (exists != null) {
            return;
        }
        UserFavoriteProduct fav = new UserFavoriteProduct();
        fav.setUserId(userId);
        fav.setProductId(productId);
        fav.setCreatedAt(LocalDateTime.now());
        userFavoriteProductMapper.insert(fav);
    }

    public void unfavoriteProduct(Long userId, Long productId) {
        unfavoriteProduct(userId, productId, false);
    }

    public void unfavoriteProduct(Long userId, Long productId, boolean removeConfigCodes) {
        Long configCodeCount = countFavoriteConfigCodesByProduct(userId, productId);
        int count = configCodeCount == null ? 0 : configCodeCount.intValue();
        if (count > 0 && !removeConfigCodes) {
            throw new BusinessException("此商品收藏有 " + count + " 个配置码，请选择全部删除");
        }
        userFavoriteProductMapper.delete(
                new LambdaQueryWrapper<UserFavoriteProduct>()
                        .eq(UserFavoriteProduct::getUserId, userId)
                        .eq(UserFavoriteProduct::getProductId, productId));
        if (removeConfigCodes) {
            userFavoriteConfigCodeMapper.delete(
                    new LambdaQueryWrapper<UserFavoriteConfigCode>()
                            .eq(UserFavoriteConfigCode::getUserId, userId)
                            .eq(UserFavoriteConfigCode::getProductId, productId));
        }
    }

    public ProductFavoriteStatus getProductFavoriteStatus(Long userId, Long productId) {
        if (productId == null || productId <= 0) {
            throw new BusinessException("productId 无效");
        }
        Long directCount = userFavoriteProductMapper.selectCount(
                new LambdaQueryWrapper<UserFavoriteProduct>()
                        .eq(UserFavoriteProduct::getUserId, userId)
                        .eq(UserFavoriteProduct::getProductId, productId));
        Long codeCount = countFavoriteConfigCodesByProduct(userId, productId);
        boolean directFavorited = directCount != null && directCount > 0;
        int favoriteConfigCodeCount = codeCount == null ? 0 : codeCount.intValue();
        return new ProductFavoriteStatus(directFavorited || favoriteConfigCodeCount > 0, directFavorited, favoriteConfigCodeCount);
    }

    public FavoriteConfigCodeResponse favoriteConfigCode(Long userId, String code) {
        ProductConfigCode target = findAvailableConfigCodeByCode(code);
        favoriteProduct(userId, target.getProductId());
        UserFavoriteConfigCode exists = userFavoriteConfigCodeMapper.selectOne(
                new LambdaQueryWrapper<UserFavoriteConfigCode>()
                        .eq(UserFavoriteConfigCode::getUserId, userId)
                        .eq(UserFavoriteConfigCode::getConfigCodeId, target.getId())
                        .last("LIMIT 1"));
        if (exists == null) {
            UserFavoriteConfigCode fav = new UserFavoriteConfigCode();
            fav.setUserId(userId);
            fav.setConfigCodeId(target.getId());
            fav.setProductId(target.getProductId());
            fav.setCreatedAt(LocalDateTime.now());
            userFavoriteConfigCodeMapper.insert(fav);
        }
        return new FavoriteConfigCodeResponse(target.getCode(), target.getProductId());
    }

    public void unfavoriteConfigCode(Long userId, String code) {
        ProductConfigCode target = productConfigCodeMapper.selectOne(
                new LambdaQueryWrapper<ProductConfigCode>()
                        .eq(ProductConfigCode::getCode, sanitizeCode(code))
                        .last("LIMIT 1"));
        if (target == null) {
            return;
        }
        // 删除配置码时，彻底清理配置码记录和所有收藏关联，避免无效数据累积。
        userFavoriteConfigCodeMapper.delete(
                new LambdaQueryWrapper<UserFavoriteConfigCode>()
                        .eq(UserFavoriteConfigCode::getConfigCodeId, target.getId()));
        productConfigCodeMapper.deleteById(target.getId());
    }

    public List<FavoriteProductItem> listFavoriteProducts(Long userId) {
        List<UserFavoriteProduct> productFavs = userFavoriteProductMapper.selectList(
                new LambdaQueryWrapper<UserFavoriteProduct>()
                        .eq(UserFavoriteProduct::getUserId, userId));
        List<UserFavoriteConfigCode> codeFavs = userFavoriteConfigCodeMapper.selectList(
                new LambdaQueryWrapper<UserFavoriteConfigCode>()
                        .eq(UserFavoriteConfigCode::getUserId, userId));

        Map<Long, Long> configCountByProduct = codeFavs.stream()
                .collect(Collectors.groupingBy(UserFavoriteConfigCode::getProductId, Collectors.counting()));

        Map<Long, Boolean> productDirectFavorite = productFavs.stream()
                .collect(Collectors.toMap(UserFavoriteProduct::getProductId, v -> true, (a, b) -> true));

        Map<Long, LocalDateTime> latestAtByProduct = new HashMap<>();
        for (UserFavoriteProduct p : productFavs) {
            latestAtByProduct.merge(p.getProductId(), p.getCreatedAt(), (a, b) -> a.isAfter(b) ? a : b);
        }
        for (UserFavoriteConfigCode c : codeFavs) {
            latestAtByProduct.merge(c.getProductId(), c.getCreatedAt(), (a, b) -> a.isAfter(b) ? a : b);
        }

        Set<Long> allProductIds = new HashSet<>();
        allProductIds.addAll(productDirectFavorite.keySet());
        allProductIds.addAll(configCountByProduct.keySet());
        if (allProductIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Product> products = productMapper.selectBatchIds(allProductIds);
        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        List<FavoriteProductItem> result = new ArrayList<>();
        for (Long productId : allProductIds) {
            Product product = productMap.get(productId);
            if (product == null) {
                continue;
            }
            result.add(new FavoriteProductItem(
                    product.getId(),
                    product.getName(),
                    product.getDescription(),
                    product.getImageUrl(),
                    product.getImageUrls(),
                    productDirectFavorite.getOrDefault(productId, false),
                    configCountByProduct.getOrDefault(productId, 0L).intValue(),
                    latestAtByProduct.get(productId)
            ));
        }
        result.sort((a, b) -> {
            LocalDateTime ta = a.lastFavoritedAt();
            LocalDateTime tb = b.lastFavoritedAt();
            if (ta == null && tb == null) return 0;
            if (ta == null) return 1;
            if (tb == null) return -1;
            return tb.compareTo(ta);
        });
        return result;
    }

    public List<FavoriteConfigCodeItem> listFavoriteConfigCodes(Long userId, Long productId) {
        if (productId == null || productId <= 0) {
            throw new BusinessException("productId 无效");
        }
        productService.getById(productId);

        List<UserFavoriteConfigCode> favoriteCodes = userFavoriteConfigCodeMapper.selectList(
                new LambdaQueryWrapper<UserFavoriteConfigCode>()
                        .eq(UserFavoriteConfigCode::getUserId, userId)
                        .eq(UserFavoriteConfigCode::getProductId, productId)
                        .orderByDesc(UserFavoriteConfigCode::getCreatedAt));
        if (favoriteCodes.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> configCodeIds = favoriteCodes.stream()
                .map(UserFavoriteConfigCode::getConfigCodeId)
                .collect(Collectors.toSet());
        if (configCodeIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<ProductConfigCode> configCodes = productConfigCodeMapper.selectBatchIds(configCodeIds);
        Map<Long, ProductConfigCode> configMap = configCodes.stream()
                .collect(Collectors.toMap(ProductConfigCode::getId, c -> c));

        List<FavoriteConfigCodeItem> result = new ArrayList<>();
        for (UserFavoriteConfigCode fav : favoriteCodes) {
            ProductConfigCode code = configMap.get(fav.getConfigCodeId());
            if (code == null) continue;
            result.add(new FavoriteConfigCodeItem(
                    code.getCode(),
                    code.getProductId(),
                    resolveCreatorName(code.getCreatorUserId()),
                    code.getCreatedAt(),
                    code.getExpireAt(),
                    isExpired(code.getExpireAt())
            ));
        }
        return result;
    }

    private ProductConfigCode findAvailableConfigCodeByCode(String rawCode) {
        ProductConfigCode target = productConfigCodeMapper.selectOne(
                new LambdaQueryWrapper<ProductConfigCode>()
                        .eq(ProductConfigCode::getCode, sanitizeCode(rawCode))
                        .last("LIMIT 1"));
        if (target == null) {
            throw new BusinessException("配置码不存在");
        }
        if (isExpired(target.getExpireAt())) {
            throw new BusinessException("配置码已过期");
        }
        return target;
    }

    private String sanitizeCode(String rawCode) {
        String code = rawCode == null ? "" : rawCode.trim();
        if (!code.matches("\\d{8}")) {
            throw new BusinessException("配置码格式无效");
        }
        return code;
    }

    private boolean isExpired(LocalDateTime expireAt) {
        return expireAt != null && LocalDateTime.now().isAfter(expireAt);
    }

    private Long countFavoriteConfigCodesByProduct(Long userId, Long productId) {
        return userFavoriteConfigCodeMapper.selectCount(
                new LambdaQueryWrapper<UserFavoriteConfigCode>()
                        .eq(UserFavoriteConfigCode::getUserId, userId)
                        .eq(UserFavoriteConfigCode::getProductId, productId));
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

    public record ProductFavoriteStatus(boolean favorited, boolean directFavorited, int favoriteConfigCodeCount) {
    }

    public record FavoriteConfigCodeResponse(String code, Long productId) {
    }

    public record FavoriteProductItem(
            Long productId,
            String productName,
            String productDescription,
            String productImageUrl,
            List<String> productImageUrls,
            boolean favoritedProduct,
            int favoriteConfigCodeCount,
            LocalDateTime lastFavoritedAt
    ) {
    }

    public record FavoriteConfigCodeItem(
            String code,
            Long productId,
            String creatorName,
            LocalDateTime createdAt,
            LocalDateTime expireAt,
            boolean expired
    ) {
    }
}
