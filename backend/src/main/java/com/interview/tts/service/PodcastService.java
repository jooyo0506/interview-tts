package com.interview.tts.service;

import com.interview.tts.dto.PodcastGenerateRequest;
import com.interview.tts.dto.PodcastGenerateResponse;
import com.interview.tts.entity.Podcast;
import com.interview.tts.entity.SysUser;
import com.interview.tts.repository.PodcastRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 播客生成服务
 * 1. 调用LLM将文本改写为双人对谈脚本
 * 2. 分别用两个不同音色合成语音
 * 3. 混合双声道音频
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PodcastService {

    private final LlmScriptService llmScriptService;
    private final DouyinTtsService ttsService;
    private final StorageService storageService;
    private final UserService userService;
    private final PodcastRepository podcastRepository;

    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    @Transactional
    public PodcastGenerateResponse generate(String userKey, PodcastGenerateRequest request) {
        // 1. 校验用户
        SysUser user = userService.getUserByKey(userKey);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 2. 创建播客记录
        Podcast podcast = new Podcast();
        podcast.setUserId(user.getId());
        podcast.setTitle(request.getTitle() != null ? request.getTitle() : "未命名播客");
        podcast.setSourceText(request.getSourceText());
        podcast.setVoiceA(request.getVoiceA());
        podcast.setVoiceB(request.getVoiceB());
        podcast.setStatus(Podcast.PodcastStatus.PROCESSING);
        podcast.setCreateTime(LocalDateTime.now());
        podcast = podcastRepository.save(podcast);

        try {
            // 3. 生成对话脚本
            log.info("开始生成播客脚本，podcastId={}", podcast.getId());
            String scriptContent = llmScriptService.generatePodcastScript(request.getSourceText());
            podcast.setScriptContent(scriptContent);

            // 4. 解析脚本，分别合成语音
            List<byte[]> audioParts = synthesizeDialogues(scriptContent, request.getVoiceA(), request.getVoiceB());

            // 5. 混合音频
            byte[] mixedAudio = mixAudio(audioParts);

            // 6. 上传存储
            String audioUrl = storageService.uploadAudio(mixedAudio, "podcast_" + podcast.getId());

            // 7. 估算时长
            int duration = storageService.estimateDuration(mixedAudio);

            // 8. 更新记录
            podcast.setAudioUrl(audioUrl);
            podcast.setDuration(duration);
            podcast.setStatus(Podcast.PodcastStatus.COMPLETED);
            podcast.setUpdateTime(LocalDateTime.now());
            podcastRepository.save(podcast);

            log.info("播客生成完成，podcastId={}, duration={}", podcast.getId(), duration);

            return new PodcastGenerateResponse(
                    podcast.getId(),
                    "COMPLETED",
                    audioUrl,
                    duration,
                    scriptContent,
                    "播客生成成功"
            );

        } catch (Exception e) {
            log.error("播客生成失败: {}", e.getMessage(), e);
            podcast.setStatus(Podcast.PodcastStatus.FAILED);
            podcast.setErrorMessage(e.getMessage());
            podcast.setUpdateTime(LocalDateTime.now());
            podcastRepository.save(podcast);

            return new PodcastGenerateResponse(
                    podcast.getId(),
                    "FAILED",
                    null,
                    null,
                    null,
                    "播客生成失败: " + e.getMessage()
            );
        }
    }

    /**
     * 解析脚本并分别合成语音
     */
    private List<byte[]> synthesizeDialogues(String script, String voiceA, String voiceB) {
        List<byte[]> audioParts = new ArrayList<>();

        String[] lines = script.split("\n");
        boolean useVoiceA = true;

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            // 提取说话内容
            String content = line;
            if (line.startsWith("主播A：") || line.startsWith("主播A:")) {
                content = line.replaceFirst("主播A[：:]", "").trim();
                useVoiceA = true;
            } else if (line.startsWith("主播B：") || line.startsWith("主播B:")) {
                content = line.replaceFirst("主播B[：:]", "").trim();
                useVoiceA = false;
            }

            if (content.isEmpty()) continue;

            try {
                String voice = useVoiceA ? voiceA : voiceB;
                byte[] audio = ttsService.generateAudio(content, voice);
                audioParts.add(audio);
            } catch (Exception e) {
                log.warn("语音合成失败: line={}, error={}", line, e.getMessage());
            }
        }

        return audioParts;
    }

    /**
     * 混合多段音频（简单拼接）
     * 注意：实际生产中应该使用更专业的音频处理库进行混音
     */
    private byte[] mixAudio(List<byte[]> audioParts) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        for (byte[] part : audioParts) {
            output.write(part);
        }
        return output.toByteArray();
    }

    public List<Podcast> getUserPodcasts(String userKey) {
        SysUser user = userService.getUserByKey(userKey);
        if (user == null) {
            return List.of();
        }
        return podcastRepository.findByUserIdOrderByCreateTimeDesc(user.getId());
    }

    public Podcast getPodcast(Long podcastId) {
        return podcastRepository.findById(podcastId).orElse(null);
    }
}
