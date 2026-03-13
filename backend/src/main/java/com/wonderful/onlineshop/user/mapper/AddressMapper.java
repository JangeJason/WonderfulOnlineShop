package com.wonderful.onlineshop.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wonderful.onlineshop.user.entity.Address;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AddressMapper extends BaseMapper<Address> {

    @Update("UPDATE address SET is_default = FALSE WHERE user_id = #{userId}")
    void clearDefaultAddress(Long userId);
}
