package com.interview.tts.repository;

import com.interview.tts.entity.Podcast;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PodcastRepository extends JpaRepository<Podcast, Long> {

    List<Podcast> findByUserIdOrderByCreateTimeDesc(Long userId);

    List<Podcast> findByUserIdAndStatusOrderByCreateTimeDesc(Long userId, Podcast.PodcastStatus status);
}
