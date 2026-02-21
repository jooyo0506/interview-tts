package com.interview.tts.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "audio_cache", uniqueConstraints = {
    @UniqueConstraint(name = "uk_md5_voice_prompt", columnNames = {"raw_text_md5", "voice_name", "prompt_hash"})
})
public class AudioCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "raw_text_md5", nullable = false, length = 32)
    private String rawTextMd5;

    @Column(name = "voice_name", nullable = false, length = 100)
    private String voiceName;

    @Column(name = "prompt_hash", nullable = false, length = 32)
    private String promptHash;

    @Column(name = "r2_url", nullable = false, length = 255)
    private String r2Url;

    @Column(name = "create_time")
    private LocalDateTime createTime = LocalDateTime.now();
}
