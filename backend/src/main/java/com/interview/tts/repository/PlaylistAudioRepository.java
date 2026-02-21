package com.interview.tts.repository;

import com.interview.tts.entity.PlaylistAudio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlaylistAudioRepository extends JpaRepository<PlaylistAudio, Long> {

    List<PlaylistAudio> findByPlaylistIdOrderBySortOrderAsc(Long playlistId);

    @Modifying
    void deleteByPlaylistId(Long playlistId);

    @Modifying
    void deleteByPlaylistIdAndAudioFileId(Long playlistId, Long audioFileId);

    boolean existsByPlaylistIdAndAudioFileId(Long playlistId, Long audioFileId);
}
