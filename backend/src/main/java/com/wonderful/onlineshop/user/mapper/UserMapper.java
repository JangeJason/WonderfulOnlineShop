package com.wonderful.onlineshop.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wonderful.onlineshop.user.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
