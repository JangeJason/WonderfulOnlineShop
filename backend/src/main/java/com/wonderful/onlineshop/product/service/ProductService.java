package com.wonderful.onlineshop.product.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wonderful.onlineshop.common.BusinessException;
import com.wonderful.onlineshop.product.entity.Product;
import com.wonderful.onlineshop.product.mapper.ProductMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    private final ProductMapper productMapper;

    public ProductService(ProductMapper productMapper) {
        this.productMapper = productMapper;
    }

    public List<Product> listActive() {
        return productMapper.selectList(new QueryWrapper<Product>().eq("status", 1));
    }

    public Product getActiveRequired(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException("product not found");
        }
        if (product.getStatus() == null || product.getStatus() != 1) {
            throw new BusinessException("product is not available");
        }
        return product;
    }

    public List<Product> listAll() {
        return productMapper.selectList(null);
    }

    public Product create(Product product) {
        if (product.getStatus() == null) {
            product.setStatus(1);
        }
        productMapper.insert(product);
        return product;
    }

    public Product update(Long id, Product patch) {
        Product existing = productMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("product not found");
        }

        if (patch.getName() != null) {
            existing.setName(patch.getName());
        }
        if (patch.getBasePrice() != null) {
            existing.setBasePrice(patch.getBasePrice());
        }
        if (patch.getStatus() != null) {
            existing.setStatus(patch.getStatus());
        }

        productMapper.updateById(existing);
        return existing;
    }

    public void delete(Long id) {
        productMapper.deleteById(id);
    }
}
