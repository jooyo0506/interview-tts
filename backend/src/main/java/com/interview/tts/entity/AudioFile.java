package com.interview.tts.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "audio_file")
public class AudioFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "text_record_id", nullable = false)
    private Long textRecordId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "name", length = 255)
    private String name;

    @Column(name = "r2_url", nullable = true, length = 255)
    private String r2Url;

    // 火山引擎临时音频URL（下载失败时保存，供前端直接播放）
    @Column(name = "temp_audio_url", nullable = true, length = 500)
    private String tempAudioUrl;

    // 下载是否失败（失败后不再重试）
    @Column(name = "download_failed", nullable = true)
    private Boolean downloadFailed;

    @Column(name = "duration", nullable = true)
    private Integer duration;

    @Column(name = "create_time")
    private LocalDateTime createTime = LocalDateTime.now();
}
