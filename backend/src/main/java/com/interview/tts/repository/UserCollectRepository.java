package com.interview.tts.repository;

import com.interview.tts.entity.UserCollect;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserCollectRepository extends JpaRepository<UserCollect, Long> {
    List<UserCollect> findByUserIdOrderByCreateTimeDesc(Long userId);
    Optional<UserCollect> findByUserIdAndAudioFileId(Long userId, Long audioFileId);
    boolean existsByUserIdAndAudioFileId(Long userId, Long audioFileId);
    void deleteByUserIdAndAudioFileId(Long userId, Long audioFileId);
}
