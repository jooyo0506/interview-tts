package com.interview.tts.service;

import com.interview.tts.dto.UserInitResponse;
import com.interview.tts.entity.SysUser;
import com.interview.tts.repository.SysUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final SysUserRepository sysUserRepository;

    @Transactional
    public UserInitResponse initUser(String userKey) {
        // 幂等检查 - 如果用户已存在则直接返回
        SysUser existingUser = sysUserRepository.findByUserKey(userKey).orElse(null);
        if (existingUser != null) {
            log.info("用户已存在: userKey={}, userId={}", userKey, existingUser.getId());
            return new UserInitResponse(existingUser.getId(), existingUser.getUserKey());
        }

        // 创建新用户
        SysUser newUser = new SysUser();
        newUser.setUserKey(userKey);
        newUser.setNickname("用户" + System.currentTimeMillis() % 10000);
        newUser.setCreateTime(LocalDateTime.now());
        newUser = sysUserRepository.save(newUser);

        log.info("创建新用户: userKey={}, userId={}", userKey, newUser.getId());
        return new UserInitResponse(newUser.getId(), newUser.getUserKey());
    }

    public SysUser getUserByKey(String userKey) {
        return sysUserRepository.findByUserKey(userKey).orElse(null);
    }

    @Transactional
    public SysUser updateProfile(String userKey, String nickname, String avatar) {
        SysUser user = getUserByKey(userKey);
        if (user == null) {
            return null;
        }

        if (nickname != null && !nickname.trim().isEmpty()) {
            user.setNickname(nickname.trim());
        }
        if (avatar != null) {
            user.setAvatar(avatar);
        }

        return sysUserRepository.save(user);
    }
}
