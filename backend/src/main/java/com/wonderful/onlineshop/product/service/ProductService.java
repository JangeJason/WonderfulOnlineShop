package com.wonderful.onlineshop.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wonderful.onlineshop.common.BusinessException;
import com.wonderful.onlineshop.product.entity.Product;
import com.wonderful.onlineshop.product.mapper.ProductMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ProductService {

    private final ProductMapper productMapper;

    public ProductService(ProductMapper productMapper) {
        this.productMapper = productMapper;
    }

    public List<Product> listActive(String keyword, Long categoryId) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, 1);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(Product::getName, keyword.trim());
        }
        if (categoryId != null) {
            wrapper.eq(Product::getCategoryId, categoryId);
        }
        wrapper.orderByDesc(Product::getId);
        List<Product> products = productMapper.selectList(wrapper);
        products.forEach(this::hydrateImageFieldsForResponse);
        return products;
    }

    public IPage<Product> listActivePaged(String keyword, Long categoryId, int page, int size) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, 1);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(Product::getName, keyword.trim());
        }
        if (categoryId != null) {
            wrapper.eq(Product::getCategoryId, categoryId);
        }
        wrapper.orderByDesc(Product::getId);
        IPage<Product> pageData = productMapper.selectPage(new Page<>(page, size), wrapper);
        pageData.getRecords().forEach(this::hydrateImageFieldsForResponse);
        return pageData;
    }

    public IPage<Product> listAllPaged(int page, int size) {
        IPage<Product> pageData = productMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<Product>().orderByDesc(Product::getId));
        pageData.getRecords().forEach(this::hydrateImageFieldsForResponse);
        return pageData;
    }

    public Product getActiveRequired(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException("product not found");
        }
        if (product.getStatus() == null || product.getStatus() != 1) {
            throw new BusinessException("product is not available");
        }
        hydrateImageFieldsForResponse(product);
        return product;
    }

    public Product getById(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException("product not found");
        }
        hydrateImageFieldsForResponse(product);
        return product;
    }

    public List<Product> listAll() {
        List<Product> products = productMapper.selectList(new LambdaQueryWrapper<Product>().orderByDesc(Product::getId));
        products.forEach(this::hydrateImageFieldsForResponse);
        return products;
    }

    public Product create(Product product) {
        if (product.getStatus() == null) {
            product.setStatus(1);
        }
        normalizeImageFieldsBeforePersist(product);
        productMapper.insert(product);
        hydrateImageFieldsForResponse(product);
        return product;
    }

    public Product update(Long id, Product patch) {
        Product existing = productMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("product not found");
        }

        if (patch.getName() != null)
            existing.setName(patch.getName());
        if (patch.getBasePrice() != null)
            existing.setBasePrice(patch.getBasePrice());
        if (patch.getSetupFee() != null)
            existing.setSetupFee(patch.getSetupFee());
        if (patch.getFreeSetupQuantity() != null)
            existing.setFreeSetupQuantity(patch.getFreeSetupQuantity());
        if (patch.getStatus() != null)
            existing.setStatus(patch.getStatus());
        // Allow null to clear these fields
        existing.setDescription(patch.getDescription());
        existing.setImageUrl(patch.getImageUrl());
        existing.setImageUrls(patch.getImageUrls());
        existing.setPricingFormula(patch.getPricingFormula());
        existing.setCategoryId(patch.getCategoryId());
        normalizeImageFieldsBeforePersist(existing);

        productMapper.updateById(existing);
        hydrateImageFieldsForResponse(existing);
        return existing;
    }

    public void delete(Long id) {
        productMapper.deleteById(id);
    }

    private void normalizeImageFieldsBeforePersist(Product product) {
        List<String> normalized = normalizeImageUrls(product.getImageUrls());
        if (normalized.isEmpty() && product.getImageUrl() != null && !product.getImageUrl().isBlank()) {
            normalized.add(product.getImageUrl().trim());
        }
        if (normalized.isEmpty()) {
            product.setImageUrl(null);
            product.setImageUrls(null);
            return;
        }
        product.setImageUrls(normalized);
        product.setImageUrl(normalized.get(0));
    }

    private void hydrateImageFieldsForResponse(Product product) {
        if (product == null) {
            return;
        }
        List<String> normalized = normalizeImageUrls(product.getImageUrls());
        if (normalized.isEmpty() && product.getImageUrl() != null && !product.getImageUrl().isBlank()) {
            normalized.add(product.getImageUrl().trim());
        }
        if (normalized.isEmpty()) {
            product.setImageUrls(new ArrayList<>());
            product.setImageUrl(null);
            return;
        }
        product.setImageUrls(normalized);
        product.setImageUrl(normalized.get(0));
    }

    private List<String> normalizeImageUrls(List<String> imageUrls) {
        List<String> result = new ArrayList<>();
        if (imageUrls == null) {
            return result;
        }
        for (String url : imageUrls) {
            if (url == null) {
                continue;
            }
            String trimmed = url.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }
}
