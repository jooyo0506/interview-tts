package com.interview.tts.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "voice_sample")
public class VoiceSample {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cloned_voice_id", nullable = false)
    private Long clonedVoiceId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "audio_url", nullable = false, length = 255)
    private String audioUrl;

    @Column(name = "duration")
    private Integer duration;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private SampleStatus status = SampleStatus.PENDING;

    @Column(name = "create_time")
    private LocalDateTime createTime = LocalDateTime.now();

    public enum SampleStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED
    }
}
