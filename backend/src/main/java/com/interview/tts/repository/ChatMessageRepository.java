package com.interview.tts.repository;

import com.interview.tts.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findBySessionIdOrderByCreateTimeAsc(Long sessionId);

    List<ChatMessage> findByUserIdOrderByCreateTimeDesc(Long userId);

    void deleteBySessionId(Long sessionId);
}
