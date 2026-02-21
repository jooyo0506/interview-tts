package com.interview.tts.service.impl;

import com.interview.tts.dto.AuthRequest;
import com.interview.tts.dto.AuthResponse;
import com.interview.tts.entity.SysUser;
import com.interview.tts.entity.UserType;
import com.interview.tts.repository.SysUserRepository;
import com.interview.tts.service.AuthService;
import com.interview.tts.service.SmsCodeService;
import com.interview.tts.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 认证服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserRepository userRepository;
    private final SmsCodeService smsCodeService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public AuthResponse register(AuthRequest request, String ip) {
        // 验证短信验证码
        if (!smsCodeService.verifyCode(request.getPhone(), request.getCode())) {
            throw new RuntimeException("验证码错误或已过期");
        }

        // 检查手机号是否已注册
        if (userRepository.findByPhone(request.getPhone()).isPresent()) {
            throw new RuntimeException("该手机号已注册");
        }

        // 创建用户
        SysUser user = new SysUser();
        user.setUserKey(UUID.randomUUID().toString());
        user.setPhone(request.getPhone());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setUserType(UserType.USER);
        user.setMonthlyCharLimit(5000);
        user.setMonthlyCharUsed(0);
        user.setLastCharResetDate(LocalDate.now());

        user = userRepository.save(user);

        // 生成Token
        String token = JwtUtil.generateToken(user.getId());

        return AuthResponse.builder()
                .token(token)
                .user(user)
                .build();
    }

    @Override
    public AuthResponse loginByPassword(String phone, String password) {
        SysUser user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("手机号或密码错误"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new RuntimeException("手机号或密码错误");
        }

        String token = JwtUtil.generateToken(user.getId());

        return AuthResponse.builder()
                .token(token)
                .user(user)
                .build();
    }

    @Override
    public AuthResponse loginByCode(String phone, String code, String ip) {
        // 验证短信验证码
        if (!smsCodeService.verifyCode(phone, code)) {
            throw new RuntimeException("验证码错误或已过期");
        }

        // 查找或创建用户
        SysUser user = userRepository.findByPhone(phone)
                .orElseGet(() -> {
                    SysUser newUser = new SysUser();
                    newUser.setUserKey(UUID.randomUUID().toString());
                    newUser.setPhone(phone);
                    newUser.setUserType(UserType.USER);
                    newUser.setMonthlyCharLimit(5000);
                    newUser.setMonthlyCharUsed(0);
                    newUser.setLastCharResetDate(LocalDate.now());
                    return userRepository.save(newUser);
                });

        String token = JwtUtil.generateToken(user.getId());

        return AuthResponse.builder()
                .token(token)
                .user(user)
                .build();
    }

    @Override
    public void sendCode(String phone, String ip, String type) {
        smsCodeService.sendCode(phone, ip, type);
    }
}
