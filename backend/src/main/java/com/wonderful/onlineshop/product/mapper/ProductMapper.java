package com.wonderful.onlineshop.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wonderful.onlineshop.product.entity.Product;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {
}
