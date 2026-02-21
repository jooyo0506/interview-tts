package com.interview.tts.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "text_record")
public class TextRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "raw_text", columnDefinition = "TEXT", nullable = false)
    private String rawText;

    @Column(name = "ssml_text", columnDefinition = "TEXT")
    private String ssmlText;

    @Column(name = "voice_name", nullable = false, length = 100)
    private String voiceName;

    @Column(name = "create_time")
    private LocalDateTime createTime = LocalDateTime.now();
}
