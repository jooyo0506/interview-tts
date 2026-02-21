package com.interview.tts.dto;

import com.interview.tts.entity.SysUser;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 认证响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    /**
     * JWT Token
     */
    private String token;

    /**
     * 用户信息
     */
    private SysUser user;
}
