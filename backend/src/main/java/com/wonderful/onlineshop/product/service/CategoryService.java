package com.wonderful.onlineshop.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wonderful.onlineshop.common.BusinessException;
import com.wonderful.onlineshop.product.entity.Category;
import com.wonderful.onlineshop.product.mapper.CategoryMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryMapper categoryMapper;

    public CategoryService(CategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    public List<Category> listAll() {
        return categoryMapper.selectList(
                new LambdaQueryWrapper<Category>().orderByAsc(Category::getSortOrder));
    }

    public Category create(Category category) {
        categoryMapper.insert(category);
        return category;
    }

    public Category update(Long id, Category patch) {
        Category existing = categoryMapper.selectById(id);
        if (existing == null)
            throw new BusinessException("分类不存在");
        if (patch.getName() != null)
            existing.setName(patch.getName());
        if (patch.getSortOrder() != null)
            existing.setSortOrder(patch.getSortOrder());
        if (patch.getParentId() != null)
            existing.setParentId(patch.getParentId());
        categoryMapper.updateById(existing);
        return existing;
    }

    public void delete(Long id) {
        categoryMapper.deleteById(id);
    }

    public Category getById(Long id) {
        return categoryMapper.selectById(id);
    }
}
