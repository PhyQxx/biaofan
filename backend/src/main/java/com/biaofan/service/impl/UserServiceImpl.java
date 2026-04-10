package com.biaofan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.biaofan.entity.User;
import com.biaofan.dto.LoginRequest;
import com.biaofan.dto.RegisterRequest;
import com.biaofan.mapper.UserMapper;
import com.biaofan.service.UserService;
import com.biaofan.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 用户服务实现类
 * 提供用户登录、注册、资料管理、密码及手机号修改等功能
 * 密码使用BCrypt加密，短信验证码存储在Redis中
 *
 * @author biaofan
 */

/**
 * 用户服务实现
 * - 用户注册（手机号验证码）
 * - 用户信息查询/更新
 * - 管理员对用户角色的修改
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    // H-15 / M-08: 注入 BCryptPasswordEncoder Bean，而非重复创建
    private final BCryptPasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;

    private static final String SMS_CODE_PREFIX = "sms:code:";

    /**
     * 用户登录
     * @param req 包含手机号和密码的登录请求
     * @return JWT token字符串
     */
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

    /**
     * 用户注册
     * @param req 包含用户名、手机号、密码的注册请求
     */
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

    /**
     * 根据ID查询用户
     * @param id 用户ID
     * @return 用户实体，未找到返回null
     */
    @Override
    public User getUserById(Long id) {
        return userMapper.selectById(id);
    }

    /**
     * 根据手机号查询用户
     * @param phone 手机号
     * @return 用户实体，未找到返回null
     */
    @Override
    public User getUserByPhone(String phone) {
        return userMapper.selectOne(
            new LambdaQueryWrapper<User>().eq(User::getPhone, phone)
        );
    }

    /**
     * 更新用户昵称
     * @param userId 用户ID
     * @param username 新昵称，不能为空
     */
    @Override
    public void updateProfile(Long userId, String username) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new RuntimeException("用户不存在");
        if (username == null || username.isBlank()) throw new RuntimeException("用户名不能为空");
        user.setUsername(username);
        userMapper.updateById(user);
    }

    /**
     * 修改密码
     * @param userId 用户ID
     * @param oldPassword 旧密码，需校验正确
     * @param newPassword 新密码，至少6位
     */
    @Override
    public void updatePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new RuntimeException("用户不存在");
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) throw new RuntimeException("旧密码错误");
        if (newPassword == null || newPassword.length() < 6) throw new RuntimeException("新密码至少6位");
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
    }

    /**
     * 修改手机号（带验证码）
     * @param userId 用户ID
     * @param phone 新手机号
     * @param code 短信验证码，从Redis中校验
     */
    // H-13: updatePhone 增加验证码校验
    @Override
    public void updatePhone(Long userId, String phone, String code) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new RuntimeException("用户不存在");
        if (phone == null || !phone.matches("^1[3-9]\\d{9}$")) throw new RuntimeException("手机号格式不正确");
        // 验证码校验
        if (code == null || code.isBlank()) throw new RuntimeException("验证码不能为空");
        String key = SMS_CODE_PREFIX + phone;
        String cachedCode = redisTemplate.opsForValue().get(key);
        if (cachedCode == null || !cachedCode.equals(code)) {
            throw new RuntimeException("验证码错误或已过期");
        }
        redisTemplate.delete(key);
        user.setPhone(phone);
        userMapper.updateById(user);
    }

    /**
     * 修改手机号（无验证码，已过时）
     * @deprecated 使用带验证码的updatePhone方法
     */
    // 保持旧签名兼容（无验证码），标记为过时
    @Override
    public void updatePhone(Long userId, String phone) {
        // H-13: 旧的无验证码方法已不安全，重定向到带验证码的方法
        throw new RuntimeException("修改手机号需要验证码，请使用带验证码的接口");
    }
}
