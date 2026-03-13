package com.wonderful.onlineshop.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wonderful.onlineshop.user.entity.User;
import com.wonderful.onlineshop.user.mapper.UserMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public DataInitializer(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public void run(String... args) {
        // Ensure admin user exists with correct BCrypt hash
        User existing = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, "admin"));
        if (existing == null) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(encoder.encode("admin"));
            admin.setNickname("管理员");
            admin.setRole("ADMIN");
            userMapper.insert(admin);
        }
    }
}
