package com.interview.tts.repository;

import com.interview.tts.entity.VoiceSample;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VoiceSampleRepository extends JpaRepository<VoiceSample, Long> {

    List<VoiceSample> findByClonedVoiceId(Long clonedVoiceId);

    List<VoiceSample> findByUserId(Long userId);

    void deleteByClonedVoiceId(Long clonedVoiceId);
}
