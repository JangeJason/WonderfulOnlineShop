package com.wonderful.onlineshop.storage;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wonderful.onlineshop.cart.entity.CartItem;
import com.wonderful.onlineshop.cart.mapper.CartItemMapper;
import com.wonderful.onlineshop.order.entity.OrderItem;
import com.wonderful.onlineshop.order.mapper.OrderItemMapper;
import com.wonderful.onlineshop.product.entity.ParameterOption;
import com.wonderful.onlineshop.product.entity.Product;
import com.wonderful.onlineshop.product.mapper.ParameterOptionMapper;
import com.wonderful.onlineshop.product.mapper.ProductMapper;
import com.wonderful.onlineshop.user.entity.UserCertificate;
import com.wonderful.onlineshop.user.mapper.UserCertificateMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class UploadFileCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(UploadFileCleanupScheduler.class);
    private static final String UPLOADS_PREFIX = "/uploads/";

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    @Value("${app.upload-cleanup.enabled:true}")
    private boolean cleanupEnabled;

    @Value("${app.upload-cleanup.delete-older-than-hours:48}")
    private long deleteOlderThanHours;

    @Value("${app.upload-cleanup.dry-run:false}")
    private boolean dryRun;

    private final ProductMapper productMapper;
    private final ParameterOptionMapper parameterOptionMapper;
    private final CartItemMapper cartItemMapper;
    private final OrderItemMapper orderItemMapper;
    private final UserCertificateMapper userCertificateMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public UploadFileCleanupScheduler(ProductMapper productMapper,
                                      ParameterOptionMapper parameterOptionMapper,
                                      CartItemMapper cartItemMapper,
                                      OrderItemMapper orderItemMapper,
                                      UserCertificateMapper userCertificateMapper) {
        this.productMapper = productMapper;
        this.parameterOptionMapper = parameterOptionMapper;
        this.cartItemMapper = cartItemMapper;
        this.orderItemMapper = orderItemMapper;
        this.userCertificateMapper = userCertificateMapper;
    }

    @Scheduled(cron = "${app.upload-cleanup.cron:0 30 3 * * ?}")
    public void cleanupOrphanUploads() {
        if (!cleanupEnabled) {
            return;
        }
        Path uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        if (!Files.exists(uploadRoot) || !Files.isDirectory(uploadRoot)) {
            log.info("Upload cleanup skipped: upload dir not found: {}", uploadRoot);
            return;
        }

        Set<String> referencedUrls = collectReferencedUploadUrls();
        Instant olderThan = Instant.now().minus(Duration.ofHours(Math.max(1, deleteOlderThanHours)));

        int scanned = 0;
        int kept = 0;
        int deleted = 0;
        int skippedRecent = 0;
        int failed = 0;

        try (Stream<Path> stream = Files.list(uploadRoot)) {
            List<Path> files = stream.filter(Files::isRegularFile).collect(Collectors.toList());
            for (Path file : files) {
                scanned++;
                String name = file.getFileName().toString();
                if (name.startsWith(".")) {
                    kept++;
                    continue;
                }

                String uploadUrl = UPLOADS_PREFIX + name;
                if (referencedUrls.contains(uploadUrl)) {
                    kept++;
                    continue;
                }

                Instant modifiedAt = Files.getLastModifiedTime(file).toInstant();
                if (modifiedAt.isAfter(olderThan)) {
                    skippedRecent++;
                    continue;
                }

                if (dryRun) {
                    deleted++;
                    log.info("[dry-run] orphan upload candidate: {}", file);
                    continue;
                }

                try {
                    Files.deleteIfExists(file);
                    deleted++;
                    log.info("Deleted orphan upload file: {}", file);
                } catch (IOException ex) {
                    failed++;
                    log.warn("Failed to delete orphan upload file: {}", file, ex);
                }
            }
        } catch (IOException ex) {
            log.error("Upload cleanup failed when scanning directory: {}", uploadRoot, ex);
            return;
        }

        log.info(
                "Upload cleanup finished: scanned={}, referenced_kept={}, recent_skipped={}, deleted={}, failed={}, referenced_url_count={}, dry_run={}",
                scanned, kept, skippedRecent, deleted, failed, referencedUrls.size(), dryRun);
    }

    private Set<String> collectReferencedUploadUrls() {
        Set<String> urls = new HashSet<>();

        List<Product> products = productMapper.selectList(new LambdaQueryWrapper<>());
        for (Product p : products) {
            addIfUploadUrl(urls, p.getImageUrl());
            if (p.getImageUrls() != null) {
                p.getImageUrls().forEach(u -> addIfUploadUrl(urls, u));
            }
        }

        List<ParameterOption> options = parameterOptionMapper.selectList(new LambdaQueryWrapper<>());
        for (ParameterOption o : options) {
            addIfUploadUrl(urls, o.getImageUrl());
        }

        List<CartItem> cartItems = cartItemMapper.selectList(new LambdaQueryWrapper<>());
        for (CartItem item : cartItems) {
            addIfUploadUrl(urls, item.getPrintFileUrl());
            addIfUploadUrl(urls, item.getProofFileUrl());
            addIfUploadUrl(urls, item.getCopyrightFileUrl());
        }

        List<OrderItem> orderItems = orderItemMapper.selectList(new LambdaQueryWrapper<>());
        for (OrderItem item : orderItems) {
            addIfUploadUrl(urls, item.getPrintFileUrl());
            addIfUploadUrl(urls, item.getProofFileUrl());
            addIfUploadUrl(urls, item.getCopyrightFileUrl());
        }

        List<UserCertificate> certificates = userCertificateMapper.selectList(new LambdaQueryWrapper<>());
        for (UserCertificate cert : certificates) {
            addCertificateFileUrls(urls, cert.getFileUrls());
        }

        return urls;
    }

    private void addCertificateFileUrls(Set<String> urls, String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return;
        }
        try {
            JsonNode node = objectMapper.readTree(rawJson);
            if (!node.isArray()) {
                return;
            }
            for (JsonNode n : node) {
                if (n.isTextual()) {
                    addIfUploadUrl(urls, n.asText());
                    continue;
                }
                if (n.isObject()) {
                    JsonNode urlNode = n.get("url");
                    if (urlNode != null && urlNode.isTextual()) {
                        addIfUploadUrl(urls, urlNode.asText());
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("Failed to parse certificate file_urls JSON, skip this row");
        }
    }

    private void addIfUploadUrl(Set<String> urls, String raw) {
        String normalized = normalizeUploadUrl(raw);
        if (normalized != null) {
            urls.add(normalized);
        }
    }

    private String normalizeUploadUrl(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String v = raw.trim().replace('\\', '/');
        if (v.startsWith("http://") || v.startsWith("https://")) {
            try {
                URI uri = URI.create(v);
                v = uri.getPath();
            } catch (Exception ignored) {
                return null;
            }
        }
        try {
            v = URLDecoder.decode(v, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            // ignore decode issues and keep raw value
        }
        int idx = v.indexOf(UPLOADS_PREFIX);
        if (idx >= 0) {
            v = v.substring(idx);
        } else if (v.startsWith("uploads/")) {
            v = "/" + v;
        } else if (!v.startsWith(UPLOADS_PREFIX)) {
            return null;
        }
        while (v.contains("//")) {
            v = v.replace("//", "/");
        }
        if (!v.startsWith(UPLOADS_PREFIX)) {
            return null;
        }
        return v;
    }
}
