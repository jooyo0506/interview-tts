package com.interview.tts.controller;

import com.interview.tts.dto.ApiResponse;
import com.interview.tts.dto.UserInitRequest;
import com.interview.tts.dto.UserInitResponse;
import com.interview.tts.entity.SysUser;
import com.interview.tts.interceptor.UserKeyInterceptor;
import com.interview.tts.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/init")
    public ApiResponse<UserInitResponse> initUser(@RequestBody UserInitRequest request) {
        String userKey = request.getUserKey();
        if (userKey == null || userKey.trim().isEmpty()) {
            return ApiResponse.error("USER_KEY_MISSING", "用户标识不能为空");
        }
        UserInitResponse response = userService.initUser(userKey);
        return ApiResponse.success(response);
    }

    @GetMapping("/profile")
    public ApiResponse<UserProfile> getProfile(HttpServletRequest httpRequest) {
        String userKey = (String) httpRequest.getAttribute(UserKeyInterceptor.USER_KEY_ATTR);
        SysUser user = userService.getUserByKey(userKey);
        if (user == null) {
            return ApiResponse.error("用户不存在");
        }
        UserProfile profile = new UserProfile();
        profile.setId(user.getId());
        profile.setNickname(user.getNickname());
        profile.setAvatar(user.getAvatar());
        profile.setCreateTime(user.getCreateTime().toString());
        return ApiResponse.success(profile);
    }

    @PutMapping("/profile")
    public ApiResponse<UserProfile> updateProfile(
            @RequestBody UpdateProfileRequest request,
            HttpServletRequest httpRequest) {
        String userKey = (String) httpRequest.getAttribute(UserKeyInterceptor.USER_KEY_ATTR);
        SysUser user = userService.updateProfile(userKey, request.getNickname(), request.getAvatar());
        if (user == null) {
            return ApiResponse.error("更新失败");
        }
        UserProfile profile = new UserProfile();
        profile.setId(user.getId());
        profile.setNickname(user.getNickname());
        profile.setAvatar(user.getAvatar());
        profile.setCreateTime(user.getCreateTime().toString());
        return ApiResponse.success(profile);
    }

    @Data
    public static class UpdateProfileRequest {
        private String nickname;
        private String avatar;
    }

    @Data
    public static class UserProfile {
        private Long id;
        private String nickname;
        private String avatar;
        private String createTime;
    }
}
