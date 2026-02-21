package com.interview.tts.dto;

import lombok.Data;

/**
 * 认证请求
 */
@Data
public class AuthRequest {
    /**
     * 手机号
     */
    private String phone;

    /**
     * 短信验证码
     */
    private String code;

    /**
     * 密码
     */
    private String password;

    /**
     * 数学验证码答案
     */
    private String captcha;

    /**
     * 数学验证码ID
     */
    private String captchaId;
}
