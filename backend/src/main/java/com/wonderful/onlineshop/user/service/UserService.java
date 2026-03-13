package com.wonderful.onlineshop.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wonderful.onlineshop.common.BusinessException;
import com.wonderful.onlineshop.user.entity.User;
import com.wonderful.onlineshop.user.mapper.UserMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class UserService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public User register(String username, String password, String nickname, String phone, String email) {
        // Check duplicate username
        User existing = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (existing != null) {
            throw new BusinessException("用户名已存在");
        }
        ensurePhoneAndEmailUnique(phone, email, null);

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(nickname != null && !nickname.isBlank() ? nickname : username);
        user.setPhone(phone);
        user.setEmail(email);
        user.setRole("USER");
        user.setCreatedAt(LocalDateTime.now());
        userMapper.insert(user);
        return user;
    }

    public User login(String phone, String email, String username, String password) {
        User user;
        if (phone != null && !phone.isBlank()) {
            user = findUniqueByField(User::getPhone, phone, "手机号");
        } else if (email != null && !email.isBlank()) {
            user = findUniqueByField(User::getEmail, email, "邮箱");
        } else if (username != null && !username.isBlank()) {
            user = findUniqueByField(User::getUsername, username, "用户名");
        } else {
            throw new BusinessException("请输入手机号或邮箱");
        }
        if (user == null) {
            throw new BusinessException("账号或密码错误");
        }
        if (!isPasswordSet(user)) {
            throw new BusinessException("该账号尚未设置密码，请先在小程序中设置密码");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException("账号或密码错误");
        }
        return user;
    }

    public User loginByPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new BusinessException("请输入手机号");
        }
        User user = findUniqueByField(User::getPhone, phone, "手机号");
        if (user == null) {
            throw new BusinessException("该手机号未绑定账号");
        }
        return user;
    }

    public User loginOrCreateWechatUserByPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new BusinessException("微信手机号为空");
        }
        User user = findUniqueByField(User::getPhone, phone, "手机号");
        if (user != null) {
            return user;
        }

        User newUser = new User();
        newUser.setUsername(generateWechatUsername(phone));
        // Empty means password has not been set yet.
        newUser.setPassword("");
        String suffix = phone.length() >= 4 ? phone.substring(phone.length() - 4) : phone;
        newUser.setNickname("微信用户" + suffix);
        newUser.setPhone(phone);
        newUser.setRole("USER");
        newUser.setCreatedAt(LocalDateTime.now());
        userMapper.insert(newUser);
        return newUser;
    }

    public User getById(Long id) {
        return userMapper.selectById(id);
    }

    public void updateProfile(Long userId, String nickname, String companyName, String avatarUrl, String phone,
            String email) {
        User user = userMapper.selectById(userId);
        if (user == null)
            throw new BusinessException("用户不存在");
        user.setNickname(nickname);
        user.setCompanyName(companyName);
        user.setAvatarUrl(avatarUrl);
        ensurePhoneAndEmailUnique(phone, email, userId);
        user.setPhone(phone);
        user.setEmail(email);
        userMapper.updateById(user);
    }

    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null)
            throw new BusinessException("用户不存在");
        if (!isPasswordSet(user)) {
            throw new BusinessException("当前账号尚未设置密码，请先设置登录密码");
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("原密码错误");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
    }

    public void initPassword(Long userId, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (isPasswordSet(user)) {
            throw new BusinessException("该账号已设置密码，请使用修改密码");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
    }

    public boolean isPasswordSet(User user) {
        if (user == null) {
            return false;
        }
        String password = user.getPassword();
        return password != null && !password.isBlank();
    }

    public void applyWechatProfile(Long userId, String nickname, String avatarUrl) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return;
        }
        boolean changed = false;
        if (nickname != null && !nickname.isBlank()) {
            user.setNickname(nickname.trim());
            changed = true;
        }
        if (avatarUrl != null && !avatarUrl.isBlank()) {
            user.setAvatarUrl(avatarUrl.trim());
            changed = true;
        }
        if (changed) {
            userMapper.updateById(user);
        }
    }

    private <T> User findUniqueByField(com.baomidou.mybatisplus.core.toolkit.support.SFunction<User, T> field, T value,
            String fieldLabel) {
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>().eq(field, value));
        if (users == null || users.isEmpty()) {
            return null;
        }
        if (users.size() > 1) {
            throw new BusinessException(fieldLabel + "存在重复账号，请联系管理员处理");
        }
        return users.get(0);
    }

    private void ensurePhoneAndEmailUnique(String phone, String email, Long currentUserId) {
        if (phone != null && !phone.isBlank()) {
            List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
            for (User u : users) {
                if (currentUserId == null || !u.getId().equals(currentUserId)) {
                    throw new BusinessException("手机号已被占用");
                }
            }
        }
        if (email != null && !email.isBlank()) {
            List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
            for (User u : users) {
                if (currentUserId == null || !u.getId().equals(currentUserId)) {
                    throw new BusinessException("邮箱已被占用");
                }
            }
        }
    }

    private String generateWechatUsername(String phone) {
        String normalized = phone == null ? "" : phone.replaceAll("\\D", "");
        if (normalized.isBlank()) {
            normalized = String.valueOf(System.currentTimeMillis());
        }
        String base = "wx_" + normalized.toLowerCase(Locale.ROOT);
        User existing = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, base));
        if (existing == null) {
            return base;
        }
        return base + "_" + System.currentTimeMillis();
    }
}
