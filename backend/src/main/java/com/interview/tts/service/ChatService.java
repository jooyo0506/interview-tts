package com.interview.tts.service;

import com.interview.tts.entity.ChatMessage;
import com.interview.tts.entity.Session;
import com.interview.tts.repository.ChatMessageRepository;
import com.interview.tts.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 语音问答服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final SessionRepository sessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final DouyinTtsService ttsService;
    private final StorageService storageService;
    private final LlmScriptService llmScriptService;

    // 模拟LLM回答（实际应该调用LLM API）
    private static final String MOCK_ANSWER = "根据您收听的内容，我理解您的问题是关于文章中的某个部分。" +
            "文章主要讨论了这个话题的核心要点。" +
            "具体来说，它涵盖了以下几个方面：" +
            "第一，基础概念的解析；" +
            "第二，实践方法的说明；" +
            "第三，相关案例的分析。" +
            "希望这个回答对您有帮助。如果您想了解更多细节，欢迎继续提问。";

    /**
     * 创建新会话
     */
    @Transactional
    public Session createSession(Long userId, String title, String contextText) {
        Session session = new Session();
        session.setUserId(userId);
        session.setSessionType("VOICE_CHAT");
        session.setTitle(title);
        session.setContextText(contextText);
        session.setCreateTime(LocalDateTime.now());
        session.setLastMessageTime(LocalDateTime.now());
        return sessionRepository.save(session);
    }

    /**
     * 获取用户会话列表
     */
    public List<Session> getUserSessions(Long userId) {
        return sessionRepository.findByUserIdOrderByLastMessageTimeDesc(userId);
    }

    /**
     * 获取会话消息历史
     */
    public List<ChatMessage> getSessionMessages(Long sessionId) {
        return chatMessageRepository.findBySessionIdOrderByCreateTimeAsc(sessionId);
    }

    /**
     * 发送消息并获取回答
     */
    @Transactional
    public Map<String, Object> sendMessage(Long sessionId, Long userId, String content) {
        Map<String, Object> result = new HashMap<>();

        // 1. 保存用户消息
        ChatMessage userMessage = new ChatMessage();
        userMessage.setSessionId(sessionId);
        userMessage.setUserId(userId);
        userMessage.setRole(ChatMessage.MessageRole.USER);
        userMessage.setContent(content);
        userMessage.setCreateTime(LocalDateTime.now());
        userMessage = chatMessageRepository.save(userMessage);

        // 2. 获取会话上下文
        Session session = sessionRepository.findById(sessionId).orElse(null);
        String contextText = session != null ? session.getContextText() : "";

        // 3. 生成回答
        String answer = generateAnswer(contextText, content);

        // 4. 保存助手消息
        ChatMessage assistantMessage = new ChatMessage();
        assistantMessage.setSessionId(sessionId);
        assistantMessage.setUserId(userId);
        assistantMessage.setRole(ChatMessage.MessageRole.ASSISTANT);
        assistantMessage.setContent(answer);
        assistantMessage.setCreateTime(LocalDateTime.now());

        // 5. 生成语音
        try {
            String voiceName = "BV001_streaming";
            byte[] audioData = ttsService.generateAudio(answer, voiceName);
            String audioUrl = storageService.uploadAudio(audioData, "chat_" + System.currentTimeMillis());
            int duration = storageService.estimateDuration(audioData);

            assistantMessage.setAudioUrl(audioUrl);
            assistantMessage.setAudioDuration(duration);

            result.put("audioUrl", audioUrl);
            result.put("audioDuration", duration);
        } catch (Exception e) {
            log.error("语音生成失败: {}", e.getMessage());
        }

        assistantMessage = chatMessageRepository.save(assistantMessage);

        // 6. 更新会话最后消息时间
        if (session != null) {
            session.setLastMessageTime(LocalDateTime.now());
            sessionRepository.save(session);
        }

        // 7. 返回结果
        result.put("userMessageId", userMessage.getId());
        result.put("assistantMessageId", assistantMessage.getId());
        result.put("answer", answer);
        result.put("timestamp", assistantMessage.getCreateTime().toString());

        return result;
    }

    /**
     * 生成回答（实际应该调用LLM API）
     */
    private String generateAnswer(String contextText, String question) {
        // 这里应该调用实际的LLM API
        // 目前返回模拟回答
        if (contextText != null && !contextText.isEmpty()) {
            return "关于您的问题：「" + question + "」\n\n" +
                    "根据收听的内容，我的回答是：\n\n" +
                    MOCK_ANSWER;
        }
        return "您好！我正在听您的问题：「" + question + "」\n\n" +
                "为了更好地回答您的问题，请确保您正在收听相关的内容。" +
                "这样我可以根据内容为您提供更准确的答案。\n\n" +
                "如果您有关于当前收听内容的任何问题，欢迎随时提问！";
    }

    /**
     * 删除会话
     */
    @Transactional
    public void deleteSession(Long sessionId) {
        // 先删除所有消息
        chatMessageRepository.deleteById(sessionId);
        // 再删除会话
        sessionRepository.deleteById(sessionId);
    }
}
