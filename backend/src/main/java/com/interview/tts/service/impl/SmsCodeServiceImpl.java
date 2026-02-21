package com.interview.tts.service.impl;

import com.interview.tts.entity.SmsCode;
import com.interview.tts.repository.SmsCodeRepository;
import com.interview.tts.service.SmsCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 短信验证码服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsCodeServiceImpl implements SmsCodeService {

    private final SmsCodeRepository smsCodeRepository;

    // 内存限流（生产环境应使用 Redis）
    private static final ConcurrentHashMap<String, Long> PHONE_LIMIT = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Long> IP_LIMIT = new ConcurrentHashMap<>();

    private static final int PHONE_LIMIT_MIN = 60;  // 1分钟
    private static final int PHONE_LIMIT_DAY = 5;   // 1天5次
    private static final int IP_LIMIT_DAY = 20;     // 1天20次

    private static final int CODE_EXPIRE_MINUTES = 5;

    @Override
    public void sendCode(String phone, String ip, String type) {
        // 检查手机号限流
        checkPhoneLimit(phone);
        // 检查IP限流
        checkIpLimit(ip);

        // 生成6位验证码
        String code = generateCode();

        // 保存验证码
        SmsCode smsCode = new SmsCode();
        smsCode.setPhone(phone);
        smsCode.setCode(code);
        smsCode.setIp(ip);
        smsCode.setType(type);
        smsCode.setExpiresAt(LocalDateTime.now().plusMinutes(CODE_EXPIRE_MINUTES));
        smsCodeRepository.save(smsCode);

        // TODO: 调用短信网关发送验证码
        log.info("发送短信验证码: phone={}, code={}", phone, code);

        // 更新限流记录
        updateLimit(phone, ip);
    }

    @Override
    public boolean verifyCode(String phone, String code) {
        SmsCode smsCode = smsCodeRepository.findTopByPhoneOrderByCreatedAtDesc(phone)
                .orElse(null);

        if (smsCode == null) {
            return false;
        }

        // 检查是否过期
        if (LocalDateTime.now().isAfter(smsCode.getExpiresAt())) {
            return false;
        }

        // 验证码匹配
        return smsCode.getCode().equals(code);
    }

    private void checkPhoneLimit(String phone) {
        String key = phone + "_day";
        Long count = PHONE_LIMIT.get(key);
        if (count != null && count >= PHONE_LIMIT_DAY) {
            throw new RuntimeException("今日发送次数已达上限");
        }

        String minKey = phone + "_min";
        Long minCount = PHONE_LIMIT.get(minKey);
        if (minCount != null && minCount >= 1) {
            throw new RuntimeException("发送过于频繁，请稍后再试");
        }
    }

    private void checkIpLimit(String ip) {
        String key = ip + "_day";
        Long count = IP_LIMIT.get(key);
        if (count != null && count >= IP_LIMIT_DAY) {
            throw new RuntimeException("IP发送次数已达上限");
        }
    }

    private void updateLimit(String phone, String ip) {
        // 更新手机号分钟限流
        String minKey = phone + "_min";
        PHONE_LIMIT.put(minKey, System.currentTimeMillis());

        // 更新手机号天限流
        String dayKey = phone + "_day";
        PHONE_LIMIT.merge(dayKey, 1L, Long::sum);

        // 更新IP天限流
        String ipKey = ip + "_day";
        IP_LIMIT.merge(ipKey, 1L, Long::sum);
    }

    private String generateCode() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(1000000));
    }
}
