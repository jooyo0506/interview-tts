package com.interview.tts.controller;

import com.interview.tts.dto.ApiResponse;
import com.interview.tts.entity.ChatMessage;
import com.interview.tts.entity.Session;
import com.interview.tts.interceptor.UserKeyInterceptor;
import com.interview.tts.service.ChatService;
import com.interview.tts.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final UserService userService;

    /**
     * 创建新会话
     */
    @PostMapping("/session")
    public ApiResponse<SessionInfo> createSession(
            @RequestBody CreateSessionRequest request,
            HttpServletRequest httpRequest) {
        String userKey = (String) httpRequest.getAttribute(UserKeyInterceptor.USER_KEY_ATTR);
        var user = userService.getUserByKey(userKey);
        if (user == null) {
            return ApiResponse.error("用户不存在");
        }

        Session session = chatService.createSession(user.getId(), request.getTitle(), request.getContextText());
        return ApiResponse.success(toSessionInfo(session));
    }

    /**
     * 获取会话列表
     */
    @GetMapping("/sessions")
    public ApiResponse<List<SessionInfo>> listSessions(HttpServletRequest httpRequest) {
        String userKey = (String) httpRequest.getAttribute(UserKeyInterceptor.USER_KEY_ATTR);
        var user = userService.getUserByKey(userKey);
        if (user == null) {
            return ApiResponse.success(List.of());
        }

        List<Session> sessions = chatService.getUserSessions(user.getId());
        List<SessionInfo> list = sessions.stream()
                .map(this::toSessionInfo)
                .collect(Collectors.toList());
        return ApiResponse.success(list);
    }

    /**
     * 获取会话消息历史
     */
    @GetMapping("/session/{id}/messages")
    public ApiResponse<List<MessageInfo>> getMessages(@PathVariable Long id) {
        List<ChatMessage> messages = chatService.getSessionMessages(id);
        List<MessageInfo> list = messages.stream()
                .map(this::toMessageInfo)
                .collect(Collectors.toList());
        return ApiResponse.success(list);
    }

    /**
     * 发送消息
     */
    @PostMapping("/session/{id}/message")
    public ApiResponse<Map<String, Object>> sendMessage(
            @PathVariable Long id,
            @RequestBody SendMessageRequest request,
            HttpServletRequest httpRequest) {
        String userKey = (String) httpRequest.getAttribute(UserKeyInterceptor.USER_KEY_ATTR);
        var user = userService.getUserByKey(userKey);
        if (user == null) {
            return ApiResponse.error("用户不存在");
        }

        Map<String, Object> result = chatService.sendMessage(id, user.getId(), request.getContent());
        return ApiResponse.success(result);
    }

    /**
     * 删除会话
     */
    @DeleteMapping("/session/{id}")
    public ApiResponse<Void> deleteSession(@PathVariable Long id) {
        chatService.deleteSession(id);
        return ApiResponse.success(null);
    }

    private SessionInfo toSessionInfo(Session session) {
        SessionInfo info = new SessionInfo();
        info.setId(session.getId());
        info.setTitle(session.getTitle());
        info.setSessionType(session.getSessionType());
        info.setContextText(session.getContextText());
        info.setLastMessageTime(session.getLastMessageTime() != null ?
                session.getLastMessageTime().toString() : null);
        info.setCreateTime(session.getCreateTime().toString());
        return info;
    }

    private MessageInfo toMessageInfo(ChatMessage message) {
        MessageInfo info = new MessageInfo();
        info.setId(message.getId());
        info.setRole(message.getRole().name().toLowerCase());
        info.setContent(message.getContent());
        info.setAudioUrl(message.getAudioUrl());
        info.setAudioDuration(message.getAudioDuration());
        info.setCreateTime(message.getCreateTime().toString());
        return info;
    }

    @Data
    public static class CreateSessionRequest {
        private String title;
        private String contextText;
    }

    @Data
    public static class SendMessageRequest {
        private String content;
    }

    @Data
    public static class SessionInfo {
        private Long id;
        private String title;
        private String sessionType;
        private String contextText;
        private String lastMessageTime;
        private String createTime;
    }

    @Data
    public static class MessageInfo {
        private Long id;
        private String role;
        private String content;
        private String audioUrl;
        private Integer audioDuration;
        private String createTime;
    }
}
