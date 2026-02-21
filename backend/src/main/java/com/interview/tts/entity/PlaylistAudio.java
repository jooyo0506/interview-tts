package com.interview.tts.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "playlist_audio")
public class PlaylistAudio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "playlist_id", nullable = false)
    private Long playlistId;

    @Column(name = "audio_file_id", nullable = false)
    private Long audioFileId;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(name = "add_time")
    private LocalDateTime addTime = LocalDateTime.now();
}
