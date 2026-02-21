package com.interview.tts.interceptor;

import com.interview.tts.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class UserKeyInterceptor implements HandlerInterceptor {

    public static final String USER_KEY_HEADER = "X-User-Key";
    public static final String USER_KEY_ATTR = "userKey";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();

        // 这些接口不需要用户验证
        if (path.startsWith("/api/user/init") || path.startsWith("/api/tts/voices")) {
            return true;
        }

        // 其他接口需要 X-User-Key
        String userKey = request.getHeader(USER_KEY_HEADER);
        if (userKey == null || userKey.trim().isEmpty()) {
            throw BusinessException.userKeyMissing();
        }

        // 将 userKey 存入请求属性，供后续使用
        request.setAttribute(USER_KEY_ATTR, userKey);
        return true;
    }
}
