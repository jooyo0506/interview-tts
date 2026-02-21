package com.interview.tts.controller;

import com.interview.tts.dto.ApiResponse;
import com.interview.tts.dto.PodcastGenerateRequest;
import com.interview.tts.dto.PodcastGenerateResponse;
import com.interview.tts.entity.Podcast;
import com.interview.tts.interceptor.UserKeyInterceptor;
import com.interview.tts.service.PodcastService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/podcast")
@RequiredArgsConstructor
public class PodcastController {

    private final PodcastService podcastService;

    @PostMapping("/generate")
    public ApiResponse<PodcastGenerateResponse> generate(
            @RequestBody PodcastGenerateRequest request,
            HttpServletRequest httpRequest) {
        String userKey = (String) httpRequest.getAttribute(UserKeyInterceptor.USER_KEY_ATTR);
        PodcastGenerateResponse response = podcastService.generate(userKey, request);
        return ApiResponse.success(response);
    }

    @GetMapping("/list")
    public ApiResponse<List<PodcastListItem>> list(HttpServletRequest httpRequest) {
        String userKey = (String) httpRequest.getAttribute(UserKeyInterceptor.USER_KEY_ATTR);
        List<Podcast> podcasts = podcastService.getUserPodcasts(userKey);
        List<PodcastListItem> items = podcasts.stream()
                .map(p -> new PodcastListItem(
                        p.getId(),
                        p.getTitle(),
                        p.getAudioUrl(),
                        p.getDuration(),
                        p.getStatus().name(),
                        p.getCreateTime().toString()
                ))
                .collect(Collectors.toList());
        return ApiResponse.success(items);
    }

    @GetMapping("/{id}")
    public ApiResponse<PodcastDetail> get(@PathVariable Long id) {
        Podcast podcast = podcastService.getPodcast(id);
        if (podcast == null) {
            return ApiResponse.error("播客不存在");
        }
        PodcastDetail detail = new PodcastDetail();
        detail.setId(podcast.getId());
        detail.setTitle(podcast.getTitle());
        detail.setSourceText(podcast.getSourceText());
        detail.setScriptContent(podcast.getScriptContent());
        detail.setVoiceA(podcast.getVoiceA());
        detail.setVoiceB(podcast.getVoiceB());
        detail.setAudioUrl(podcast.getAudioUrl());
        detail.setDuration(podcast.getDuration());
        detail.setStatus(podcast.getStatus().name());
        detail.setCreateTime(podcast.getCreateTime().toString());
        return ApiResponse.success(detail);
    }

    public static class PodcastListItem {
        private Long id;
        private String title;
        private String audioUrl;
        private Integer duration;
        private String status;
        private String createTime;

        public PodcastListItem(Long id, String title, String audioUrl, Integer duration, String status, String createTime) {
            this.id = id;
            this.title = title;
            this.audioUrl = audioUrl;
            this.duration = duration;
            this.status = status;
            this.createTime = createTime;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getAudioUrl() { return audioUrl; }
        public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }
        public Integer getDuration() { return duration; }
        public void setDuration(Integer duration) { this.duration = duration; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getCreateTime() { return createTime; }
        public void setCreateTime(String createTime) { this.createTime = createTime; }
    }

    public static class PodcastDetail {
        private Long id;
        private String title;
        private String sourceText;
        private String scriptContent;
        private String voiceA;
        private String voiceB;
        private String audioUrl;
        private Integer duration;
        private String status;
        private String createTime;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getSourceText() { return sourceText; }
        public void setSourceText(String sourceText) { this.sourceText = sourceText; }
        public String getScriptContent() { return scriptContent; }
        public void setScriptContent(String scriptContent) { this.scriptContent = scriptContent; }
        public String getVoiceA() { return voiceA; }
        public void setVoiceA(String voiceA) { this.voiceA = voiceA; }
        public String getVoiceB() { return voiceB; }
        public void setVoiceB(String voiceB) { this.voiceB = voiceB; }
        public String getAudioUrl() { return audioUrl; }
        public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }
        public Integer getDuration() { return duration; }
        public void setDuration(Integer duration) { this.duration = duration; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getCreateTime() { return createTime; }
        public void setCreateTime(String createTime) { this.createTime = createTime; }
    }
}
