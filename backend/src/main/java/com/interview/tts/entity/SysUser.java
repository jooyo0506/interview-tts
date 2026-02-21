package com.interview.tts.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "sys_user")
public class SysUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_key", unique = true, nullable = false, length = 64)
    private String userKey;

    @Column(name = "nickname", length = 50)
    private String nickname;

    @Column(name = "avatar", length = 255)
    private String avatar;

    @Column(name = "phone", unique = true, length = 20)
    private String phone;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", length = 20)
    private UserType userType = UserType.USER;

    @Column(name = "vip_expire_date")
    private LocalDateTime vipExpireDate;

    @Column(name = "monthly_char_limit")
    private Integer monthlyCharLimit = 5000;

    @Column(name = "monthly_char_used")
    private Integer monthlyCharUsed = 0;

    @Column(name = "last_char_reset_date")
    private LocalDate lastCharResetDate;

    @Column(name = "email", length = 100)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private UserStatus status = UserStatus.NORMAL;

    @Column(name = "create_time")
    private LocalDateTime createTime = LocalDateTime.now();

    public enum UserStatus {
        NORMAL,
        BANNED
    }
}
