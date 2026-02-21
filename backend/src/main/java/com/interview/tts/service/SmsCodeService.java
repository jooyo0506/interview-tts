package com.interview.tts.service;

/**
 * 短信验证码服务
 */
public interface SmsCodeService {

    /**
     * 发送验证码
     * @param phone 手机号
     * @param ip IP地址
     * @param type 验证码类型 LOGIN/REGISTER
     */
    void sendCode(String phone, String ip, String type);

    /**
     * 验证验证码
     * @param phone 手机号
     * @param code 验证码
     * @return 是否验证通过
     */
    boolean verifyCode(String phone, String code);
}
