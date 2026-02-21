package com.interview.tts.controller;

import com.interview.tts.websocket.VoiceChatHandler;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

/**
 * STOMP WebSocket控制器
 * 处理实时语音问答的STOMP消息
 */
@Controller
public class WebSocketController {

    @Autowired
    private VoiceChatHandler voiceChatHandler;

    /**
     * 处理语音消息
     */
    @MessageMapping("/voice")
    @SendTo("/topic/voice")
    public VoiceMessage handleVoiceMessage(@Payload VoiceMessage message,
                                            SimpMessageHeaderAccessor headerAccessor) {
        return message;
    }

    /**
     * 处理文本消息
     */
    @MessageMapping("/chat")
    @SendTo("/topic/chat")
    public ChatMessage handleChatMessage(@Payload ChatMessage message,
                                          SimpMessageHeaderAccessor headerAccessor) {
        return message;
    }

    @Data
    public static class VoiceMessage {
        private String type;
        private String sessionId;
        private String userKey;
        private String audioData; // Base64编码的音频
        private String text;
        private Long timestamp;
    }

    @Data
    public static class ChatMessage {
        private String type;
        private String sessionId;
        private String role; // user, assistant
        private String content;
        private String audioUrl;
        private Integer audioDuration;
        private Long timestamp;
    }
}
