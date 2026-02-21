package com.interview.tts.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "chat_message")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private MessageRole role;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "audio_url", length = 255)
    private String audioUrl;

    @Column(name = "audio_duration")
    private Integer audioDuration;

    @Column(name = "create_time")
    private LocalDateTime createTime = LocalDateTime.now();

    public enum MessageRole {
        USER,
        ASSISTANT,
        SYSTEM
    }
}
