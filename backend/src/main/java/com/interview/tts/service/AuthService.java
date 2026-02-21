package com.interview.tts.service;

import com.interview.tts.dto.AuthRequest;
import com.interview.tts.dto.AuthResponse;

/**
 * 认证服务
 */
public interface AuthService {

    /**
     * 注册
     * @param request 注册请求
     * @param ip IP地址
     * @return 认证响应
     */
    AuthResponse register(AuthRequest request, String ip);

    /**
     * 密码登录
     * @param phone 手机号
     * @param password 密码
     * @return 认证响应
     */
    AuthResponse loginByPassword(String phone, String password);

    /**
     * 验证码登录
     * @param phone 手机号
     * @param code 验证码
     * @param ip IP地址
     * @return 认证响应
     */
    AuthResponse loginByCode(String phone, String code, String ip);

    /**
     * 发送验证码
     * @param phone 手机号
     * @param ip IP地址
     * @param type 类型
     */
    void sendCode(String phone, String ip, String type);
}
