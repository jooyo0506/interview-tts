package com.interview.tts.interceptor;

import com.interview.tts.annotation.RequirePermission;
import com.interview.tts.entity.SysUser;
import com.interview.tts.entity.UserType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDate;

/**
 * 权限拦截器
 */
@Slf4j
@Component
public class PermissionInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        RequirePermission annotation = handlerMethod.getMethodAnnotation(RequirePermission.class);

        if (annotation == null) {
            return true;
        }

        SysUser user = (SysUser) request.getAttribute("currentUser");
        if (user == null) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"message\":\"请先登录\"}");
            return false;
        }

        // 检查用户类型
        UserType requiredType = annotation.value();
        if (requiredType == UserType.VIP && user.getUserType() != UserType.VIP) {
            response.setStatus(403);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"message\":\"VIP权限不足\"}");
            return false;
        }

        // 检查字数限额
        if (user.getUserType() == UserType.USER) {
            // 重置月度字数
            resetMonthlyCharIfNeeded(user);
            if (user.getMonthlyCharUsed() >= user.getMonthlyCharLimit()) {
                response.setStatus(403);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"success\":false,\"message\":\"本月字数限额已用完\"}");
                return false;
            }
        }

        return true;
    }

    private void resetMonthlyCharIfNeeded(SysUser user) {
        LocalDate today = LocalDate.now();
        if (user.getLastCharResetDate() == null ||
            user.getLastCharResetDate().getMonth() != today.getMonth()) {
            user.setMonthlyCharUsed(0);
            user.setLastCharResetDate(today);
        }
    }
}
