package com.biaofan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.biaofan.entity.User;
import com.biaofan.dto.LoginRequest;
import com.biaofan.dto.RegisterRequest;
import com.biaofan.mapper.UserMapper;
import com.biaofan.service.UserService;
import com.biaofan.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public String login(LoginRequest req) {
        User user = userMapper.selectOne(
            new LambdaQueryWrapper<User>().eq(User::getPhone, req.getPhone())
        );
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new RuntimeException("密码错误");
        }
        return jwtUtil.generateToken(user.getId(), user.getUsername());
    }

    @Override
    public void register(RegisterRequest req) {
        User exist = userMapper.selectOne(
            new LambdaQueryWrapper<User>().eq(User::getPhone, req.getPhone())
        );
        if (exist != null) {
            throw new RuntimeException("手机号已被注册");
        }
        User user = new User();
        user.setUsername(req.getUsername());
        user.setPhone(req.getPhone());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        userMapper.insert(user);
    }

    @Override
    public User getUserById(Long id) {
        return userMapper.selectById(id);
    }

    @Override
    public User getUserByPhone(String phone) {
        return userMapper.selectOne(
            new LambdaQueryWrapper<User>().eq(User::getPhone, phone)
        );
    }

    @Override
    public void updateProfile(Long userId, String username) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new RuntimeException("用户不存在");
        if (username == null || username.isBlank()) throw new RuntimeException("用户名不能为空");
        user.setUsername(username);
        userMapper.updateById(user);
    }

    @Override
    public void updatePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new RuntimeException("用户不存在");
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) throw new RuntimeException("旧密码错误");
        if (newPassword == null || newPassword.length() < 6) throw new RuntimeException("新密码至少6位");
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
    }

    @Override
    public void updatePhone(Long userId, String phone) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new RuntimeException("用户不存在");
        if (phone == null || !phone.matches("^1[3-9]\\d{9}$")) throw new RuntimeException("手机号格式不正确");
        user.setPhone(phone);
        userMapper.updateById(user);
    }
}
