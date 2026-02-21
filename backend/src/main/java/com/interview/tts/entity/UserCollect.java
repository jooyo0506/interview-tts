package com.interview.tts.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "user_collect", uniqueConstraints = {
    @UniqueConstraint(name = "uk_user_audio", columnNames = {"user_id", "audio_file_id"})
})
public class UserCollect {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "audio_file_id", nullable = false)
    private Long audioFileId;

    @Column(name = "create_time")
    private LocalDateTime createTime = LocalDateTime.now();
}
