package com.interview.tts.websocket;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.interview.tts.entity.ChatMessage;
import com.interview.tts.entity.Session;
import com.interview.tts.repository.ChatMessageRepository;
import com.interview.tts.repository.SessionRepository;
import com.interview.tts.service.DouyinTtsService;
import com.interview.tts.service.LlmScriptService;
import com.interview.tts.service.StorageService;
import com.interview.tts.service.UserService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 语音问答WebSocket处理器
 * 处理边听边问功能的实时语音交互
 */
@Slf4j
@Component
public class VoiceChatHandler extends TextWebSocketHandler {

    private final Map<String, UserSession> sessions = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    private final LlmScriptService llmScriptService;
    private final DouyinTtsService ttsService;
    private final StorageService storageService;
    private final UserService userService;
    private final SessionRepository sessionRepository;
    private final ChatMessageRepository chatMessageRepository;

    @Value("${doubao.api-key:}")
    private String apiKey;

    public VoiceChatHandler(LlmScriptService llmScriptService,
                           DouyinTtsService ttsService,
                           StorageService storageService,
                           UserService userService,
                           SessionRepository sessionRepository,
                           ChatMessageRepository chatMessageRepository) {
        this.llmScriptService = llmScriptService;
        this.ttsService = ttsService;
        this.storageService = storageService;
        this.userService = userService;
        this.sessionRepository = sessionRepository;
        this.chatMessageRepository = chatMessageRepository;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        log.info("WebSocket连接建立: {}", sessionId);

        // 发送连接成功消息
        JSONObject response = new JSONObject();
        response.put("type", "connected");
        response.put("sessionId", sessionId);
        session.sendMessage(new TextMessage(response.toJSONString()));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.debug("收到消息: {}", payload);

        try {
            JSONObject request = JSON.parseObject(payload);
            String type = request.getString("type");

            switch (type) {
                case "init":
                    handleInit(session, request);
                    break;
                case "asr":
                    handleAsr(session, request);
                    break;
                case "text":
                    handleTextMessage(session, request);
                    break;
                case "end":
                    handleEnd(session);
                    break;
                default:
                    log.warn("未知消息类型: {}", type);
            }
        } catch (Exception e) {
            log.error("处理消息失败: {}", e.getMessage(), e);
            sendError(session, e.getMessage());
        }
    }

    /**
     * 初始化会话
     */
    private void handleInit(WebSocketSession session, JSONObject request) {
        String userKey = request.getString("userKey");
        String contextText = request.getString("contextText");
        String voiceName = request.getString("voiceName");

        // 创建会话记录
        Session chatSession = new Session();
        chatSession.setUserId(userService.getUserByKey(userKey) != null ?
                userService.getUserByKey(userKey).getId() : 0L);
        chatSession.setSessionType("VOICE_CHAT");
        chatSession.setTitle("语音问答");
        chatSession.setContextText(contextText);
        chatSession.setCreateTime(java.time.LocalDateTime.now());
        chatSession = sessionRepository.save(chatSession);

        UserSession userSession = new UserSession();
        userSession.setSessionId(session.getId());
        userSession.setChatSessionId(chatSession.getId());
        userSession.setUserKey(userKey);
        userSession.setContextText(contextText);
        userSession.setVoiceName(voiceName != null ? voiceName : "BV001_streaming");
        sessions.put(session.getId(), userSession);

        JSONObject response = new JSONObject();
        response.put("type", "init");
        response.put("chatSessionId", chatSession.getId());
        sendMessage(session, response);
    }

    /**
     * 处理语音识别结果 (ASR)
     */
    private void handleAsr(WebSocketSession session, JSONObject request) {
        String text = request.getString("text");
        if (text == null || text.isEmpty()) return;

        UserSession userSession = sessions.get(session.getId());
        if (userSession == null) {
            sendError(session, "会话未初始化");
            return;
        }

        // 保存用户消息
        saveMessage(userSession.getChatSessionId(), userSession.getUserKey(),
                ChatMessage.MessageRole.USER, text, null, 0);

        // 异步处理回答
        executor.submit(() -> {
            try {
                processAnswer(session, userSession, text);
            } catch (Exception e) {
                log.error("处理回答失败: {}", e.getMessage(), e);
                sendError(session, "处理失败: " + e.getMessage());
            }
        });
    }

    /**
     * 处理文本消息
     */
    private void handleTextMessage(WebSocketSession session, JSONObject request) {
        String text = request.getString("text");
        if (text == null || text.isEmpty()) return;

        UserSession userSession = sessions.get(session.getId());
        if (userSession == null) {
            sendError(session, "会话未初始化");
            return;
        }

        // 保存用户消息
        saveMessage(userSession.getChatSessionId(), userSession.getUserKey(),
                ChatMessage.MessageRole.USER, text, null, 0);

        // 发送确认
        JSONObject ack = new JSONObject();
        ack.put("type", "ack");
        ack.put("message", "正在思考...");
        sendMessage(session, ack);

        // 异步处理回答
        executor.submit(() -> {
            try {
                processAnswer(session, userSession, text);
            } catch (Exception e) {
                log.error("处理回答失败: {}", e.getMessage(), e);
                sendError(session, "处理失败: " + e.getMessage());
            }
        });
    }

    /**
     * 处理回答
     */
    private void processAnswer(WebSocketSession session, UserSession userSession, String question) {
        try {
            // 构建上下文
            String context = buildContext(userSession.getContextText(), question);

            // 调用LLM生成回答
            String answer = generateAnswer(context, question);

            // 发送文本回答
            JSONObject textResponse = new JSONObject();
            textResponse.put("type", "text");
            textResponse.put("content", answer);
            sendMessage(session, textResponse);

            // 转换为语音
            byte[] audioData = ttsService.generateAudio(answer, userSession.getVoiceName());
            String audioUrl = storageService.uploadAudio(audioData, "chat_" + System.currentTimeMillis());

            // 发送语音回答
            JSONObject audioResponse = new JSONObject();
            audioResponse.put("type", "audio");
            audioResponse.put("audioUrl", audioUrl);
            sendMessage(session, audioResponse);

            // 保存助手消息
            int duration = storageService.estimateDuration(audioData);
            saveMessage(userSession.getChatSessionId(), userSession.getUserKey(),
                    ChatMessage.MessageRole.ASSISTANT, answer, audioUrl, duration);

        } catch (Exception e) {
            log.error("生成回答失败: {}", e.getMessage(), e);
            sendError(session, "生成回答失败: " + e.getMessage());
        }
    }

    /**
     * 构建上下文
     */
    private String buildContext(String contextText, String question) {
        if (contextText == null || contextText.isEmpty()) {
            return "用户正在收听内容，现在问了一个问题：" + question;
        }
        return "上下文内容：" + contextText + "\n\n用户现在问了一个问题：" + question;
    }

    /**
     * 生成回答
     */
    private String generateAnswer(String context, String question) {
        if (apiKey == null || apiKey.isEmpty()) {
            return "抱歉，暂时无法回答您的问题。请稍后再试。";
        }

        // 使用简单的提示词调用LLM
        String prompt = context + "\n\n请根据以上内容回答用户的问题。如果问题与内容无关，请礼貌地说明并提供帮助。";

        try {
            // 这里可以调用实际的LLM API
            // 目前返回模拟回答
            return "根据您收听的内容，关于这个问题，我的回答是：这是一个测试回答。实际上我需要调用LLM API来生成更准确的回答。";
        } catch (Exception e) {
            log.error("LLM调用失败: {}", e.getMessage());
            return "抱歉，处理您的请求时出现问题。";
        }
    }

    /**
     * 结束会话
     */
    private void handleEnd(WebSocketSession session) {
        String sessionId = session.getId();
        sessions.remove(sessionId);
        log.info("会话结束: {}", sessionId);

        JSONObject response = new JSONObject();
        response.put("type", "end");
        response.put("message", "会话已结束");
        sendMessage(session, response);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        sessions.remove(sessionId);
        log.info("WebSocket连接关闭: {}, status: {}", sessionId, status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket传输错误: {}", exception.getMessage(), exception);
        sessions.remove(session.getId());
    }

    private void sendMessage(WebSocketSession session, JSONObject message) {
        try {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(message.toJSONString()));
            }
        } catch (IOException e) {
            log.error("发送消息失败: {}", e.getMessage());
        }
    }

    private void sendError(WebSocketSession session, String errorMessage) {
        JSONObject error = new JSONObject();
        error.put("type", "error");
        error.put("message", errorMessage);
        sendMessage(session, error);
    }

    private void saveMessage(Long sessionId, String userKey,
                             ChatMessage.MessageRole role, String content,
                             String audioUrl, int audioDuration) {
        try {
            ChatMessage message = new ChatMessage();
            message.setSessionId(sessionId);
            message.setUserId(userService.getUserByKey(userKey) != null ?
                    userService.getUserByKey(userKey).getId() : 0L);
            message.setRole(role);
            message.setContent(content);
            message.setAudioUrl(audioUrl);
            message.setAudioDuration(audioDuration);
            message.setCreateTime(java.time.LocalDateTime.now());
            chatMessageRepository.save(message);
        } catch (Exception e) {
            log.warn("保存消息失败: {}", e.getMessage());
        }
    }

    @Data
    public static class UserSession {
        private String sessionId;
        private Long chatSessionId;
        private String userKey;
        private String contextText;
        private String voiceName;
    }
}
