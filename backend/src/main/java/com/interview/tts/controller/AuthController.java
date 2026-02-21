package com.interview.tts.controller;

import com.interview.tts.dto.ApiResponse;
import com.interview.tts.dto.AuthRequest;
import com.interview.tts.dto.AuthResponse;
import com.interview.tts.entity.SysUser;
import com.interview.tts.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 发送验证码
     */
    @PostMapping("/send-code")
    public ApiResponse<Void> sendCode(
            @RequestParam String phone,
            @RequestParam(defaultValue = "LOGIN") String type,
            HttpServletRequest request) {
        String ip = getClientIp(request);
        authService.sendCode(phone, ip, type);
        return ApiResponse.success(null);
    }

    /**
     * 注册
     */
    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(
            @Valid @RequestBody AuthRequest request,
            HttpServletRequest httpRequest) {
        String ip = getClientIp(httpRequest);
        AuthResponse response = authService.register(request, ip);
        return ApiResponse.success(response);
    }

    /**
     * 密码登录
     */
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        AuthResponse response = authService.loginByPassword(request.getPhone(), request.getPassword());
        return ApiResponse.success(response);
    }

    /**
     * 验证码登录
     */
    @PostMapping("/login-by-code")
    public ApiResponse<AuthResponse> loginByCode(
            @Valid @RequestBody AuthRequest request,
            HttpServletRequest httpRequest) {
        String ip = getClientIp(httpRequest);
        AuthResponse response = authService.loginByCode(request.getPhone(), request.getCode(), ip);
        return ApiResponse.success(response);
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/info")
    public ApiResponse<SysUser> getUserInfo(HttpServletRequest request) {
        SysUser user = (SysUser) request.getAttribute("currentUser");
        if (user == null) {
            return ApiResponse.error("未登录");
        }
        return ApiResponse.success(user);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            // X-Forwarded-For 可能包含多个IP，取第一个（真实客户端）
            ip = ip.split(",")[0].trim();
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
