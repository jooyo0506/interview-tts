package com.interview.tts.repository;

import com.interview.tts.entity.AudioFile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AudioFileRepository extends JpaRepository<AudioFile, Long> {
    List<AudioFile> findByUserIdOrderByCreateTimeDesc(Long userId);
    Optional<AudioFile> findByTextRecordId(Long textRecordId);
}
