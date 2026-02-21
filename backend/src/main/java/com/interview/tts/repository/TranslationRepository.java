package com.interview.tts.repository;

import com.interview.tts.entity.Translation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TranslationRepository extends JpaRepository<Translation, Long> {

    List<Translation> findByUserIdOrderByCreateTimeDesc(Long userId);

    List<Translation> findByUserIdAndSourceLangAndTargetLang(Long userId, String sourceLang, String targetLang);
}
