package com.interview.tts.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 短信验证码
 */
@Data
@Entity
@Table(name = "sms_code")
public class SmsCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "code", nullable = false, length = 10)
    private String code;

    @Column(name = "ip", length = 50)
    private String ip;

    @Column(name = "type", length = 20)
    private String type = "LOGIN";

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
}
