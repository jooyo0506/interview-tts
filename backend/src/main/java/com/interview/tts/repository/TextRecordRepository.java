package com.interview.tts.repository;

import com.interview.tts.entity.TextRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TextRecordRepository extends JpaRepository<TextRecord, Long> {
    List<TextRecord> findByUserIdOrderByCreateTimeDesc(Long userId);
}
