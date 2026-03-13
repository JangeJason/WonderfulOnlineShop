package com.wonderful.onlineshop.cart.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wonderful.onlineshop.cart.entity.CartItem;
import com.wonderful.onlineshop.cart.mapper.CartItemMapper;
import com.wonderful.onlineshop.common.BusinessException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CartService {

    private final CartItemMapper cartItemMapper;
    private final com.wonderful.onlineshop.product.mapper.ProductMapper productMapper;

    public CartService(CartItemMapper cartItemMapper,
            com.wonderful.onlineshop.product.mapper.ProductMapper productMapper) {
        this.cartItemMapper = cartItemMapper;
        this.productMapper = productMapper;
    }

    public List<CartItem> listByUser(Long userId) {
        List<CartItem> items = cartItemMapper.selectList(
                new LambdaQueryWrapper<CartItem>()
                        .eq(CartItem::getUserId, userId)
                        .orderByDesc(CartItem::getCreatedAt));

        for (CartItem item : items) {
            com.wonderful.onlineshop.product.entity.Product product = productMapper.selectById(item.getProductId());
            if (product != null) {
                item.setProductName(product.getName());
            }
        }
        return items;
    }

    public CartItem addItem(CartItem item) {
        cartItemMapper.insert(item);
        return item;
    }

    public CartItem updateQuantity(Long id, Long userId, Integer quantity) {
        CartItem item = cartItemMapper.selectById(id);
        if (item == null || !item.getUserId().equals(userId)) {
            throw new BusinessException("购物车条目不存在");
        }
        item.setQuantity(quantity);
        cartItemMapper.updateById(item);
        return item;
    }

    public void removeItem(Long id, Long userId) {
        CartItem item = cartItemMapper.selectById(id);
        if (item == null || !item.getUserId().equals(userId)) {
            throw new BusinessException("购物车条目不存在");
        }
        cartItemMapper.deleteById(id);
    }

    public void clearByUser(Long userId) {
        cartItemMapper.delete(
                new LambdaQueryWrapper<CartItem>()
                        .eq(CartItem::getUserId, userId));
    }

    public void removeBatch(List<Long> ids, Long userId) {
        if (ids == null || ids.isEmpty())
            return;
        cartItemMapper.delete(
                new LambdaQueryWrapper<CartItem>()
                        .in(CartItem::getId, ids)
                        .eq(CartItem::getUserId, userId));
    }
}
