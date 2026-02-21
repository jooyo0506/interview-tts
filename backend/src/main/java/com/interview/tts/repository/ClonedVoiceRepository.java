package com.interview.tts.repository;

import com.interview.tts.entity.ClonedVoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClonedVoiceRepository extends JpaRepository<ClonedVoice, Long> {

    List<ClonedVoice> findByUserIdOrderByCreateTimeDesc(Long userId);

    List<ClonedVoice> findByUserIdAndStatus(Long userId, ClonedVoice.CloneStatus status);
}
