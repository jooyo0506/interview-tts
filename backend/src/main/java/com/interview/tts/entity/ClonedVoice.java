package com.interview.tts.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "cloned_voice")
public class ClonedVoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "voice_id", length = 100)
    private String voiceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private CloneStatus status = CloneStatus.PENDING;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "sample_count")
    private Integer sampleCount = 0;

    @Column(name = "create_time")
    private LocalDateTime createTime = LocalDateTime.now();

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    public enum CloneStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED
    }
}
