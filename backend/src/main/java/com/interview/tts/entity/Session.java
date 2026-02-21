package com.interview.tts.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "session")
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "session_type", length = 50)
    private String sessionType;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "context_text", columnDefinition = "TEXT")
    private String contextText;

    @Column(name = "audio_url", length = 255)
    private String audioUrl;

    @Column(name = "last_message_time")
    private LocalDateTime lastMessageTime;

    @Column(name = "create_time")
    private LocalDateTime createTime = LocalDateTime.now();

    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
