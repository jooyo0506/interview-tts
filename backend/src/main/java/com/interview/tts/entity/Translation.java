package com.interview.tts.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "translation")
public class Translation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "source_text", columnDefinition = "TEXT")
    private String sourceText;

    @Column(name = "translated_text", columnDefinition = "TEXT")
    private String translatedText;

    @Column(name = "source_lang", length = 10)
    private String sourceLang = "zh";

    @Column(name = "target_lang", length = 10)
    private String targetLang = "en";

    @Column(name = "source_audio_url", length = 255)
    private String sourceAudioUrl;

    @Column(name = "translated_audio_url", length = 255)
    private String translatedAudioUrl;

    @Column(name = "translated_audio_duration")
    private Integer translatedAudioDuration;

    @Enumerated(EnumType.STRING)
    @Column(name = "translation_type")
    private TranslationType translationType = TranslationType.TEXT;

    @Column(name = "create_time")
    private LocalDateTime createTime = LocalDateTime.now();

    public enum TranslationType {
        TEXT,
        SPEECH
    }
}
