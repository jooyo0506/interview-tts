package com.interview.tts.repository;

import com.interview.tts.entity.AudioCache;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AudioCacheRepository extends JpaRepository<AudioCache, Long> {
    Optional<AudioCache> findByRawTextMd5AndVoiceNameAndPromptHash(String rawTextMd5, String voiceName, String promptHash);
}
