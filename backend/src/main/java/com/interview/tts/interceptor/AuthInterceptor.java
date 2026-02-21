package com.interview.tts.interceptor;

import com.interview.tts.entity.SysUser;
import com.interview.tts.repository.SysUserRepository;
import com.interview.tts.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 认证拦截器
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final SysUserRepository userRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 放行 OPTIONS 请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = request.getHeader("X-Token");

        if (token != null && !token.isEmpty()) {
            try {
                Long userId = JwtUtil.parseToken(token);
                userRepository.findById(userId).ifPresent(user -> {
                    request.setAttribute("currentUser", user);
                });
            } catch (Exception e) {
                // Token 无效，不阻塞请求，只是没有用户信息
            }
        }

        return true;
    }
}
