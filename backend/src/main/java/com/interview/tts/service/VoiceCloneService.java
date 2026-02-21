package com.interview.tts.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.interview.tts.entity.ClonedVoice;
import com.interview.tts.entity.VoiceSample;
import com.interview.tts.repository.ClonedVoiceRepository;
import com.interview.tts.repository.VoiceSampleRepository;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 声音复刻服务
 * 对接豆包声音复刻2.0 API
 */
@Slf4j
@Service
public class VoiceCloneService {

    private static final String CLONE_API_URL = "https://openspeech.bytedance.com/api/v2/voice_clone";
    private static final String TASK_STATUS_URL = "https://openspeech.bytedance.com/api/v2/voice_clone/task";

    @Value("${doubao.api-key:}")
    private String apiKey;

    @Value("${volcengine.tts.app-id:}")
    private String appId;

    private final ClonedVoiceRepository clonedVoiceRepository;
    private final VoiceSampleRepository voiceSampleRepository;
    private final StorageService storageService;

    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Map<Long, CloneTask> pendingTasks = new ConcurrentHashMap<>();

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

    public VoiceCloneService(ClonedVoiceRepository clonedVoiceRepository,
                             VoiceSampleRepository voiceSampleRepository,
                             StorageService storageService) {
        this.clonedVoiceRepository = clonedVoiceRepository;
        this.voiceSampleRepository = voiceSampleRepository;
        this.storageService = storageService;
    }

    /**
     * 创建声音复刻任务
     */
    @Transactional
    public ClonedVoice createCloneTask(Long userId, String name, List<String> sampleUrls) {
        // 1. 创建克隆记录
        ClonedVoice clonedVoice = new ClonedVoice();
        clonedVoice.setUserId(userId);
        clonedVoice.setName(name);
        clonedVoice.setStatus(ClonedVoice.CloneStatus.PENDING);
        clonedVoice.setSampleCount(sampleUrls.size());
        clonedVoice.setCreateTime(LocalDateTime.now());
        clonedVoice = clonedVoiceRepository.save(clonedVoice);

        // 2. 保存样本记录
        for (String url : sampleUrls) {
            VoiceSample sample = new VoiceSample();
            sample.setClonedVoiceId(clonedVoice.getId());
            sample.setUserId(userId);
            sample.setAudioUrl(url);
            sample.setStatus(VoiceSample.SampleStatus.PENDING);
            sample.setCreateTime(LocalDateTime.now());
            voiceSampleRepository.save(sample);
        }

        // 3. 提交异步任务
        ClonedVoice finalClonedVoice = clonedVoice;
        executor.submit(() -> submitCloneTask(finalClonedVoice.getId(), sampleUrls));

        return clonedVoice;
    }

    /**
     * 提交克隆任务到豆包API
     */
    private void submitCloneTask(Long clonedVoiceId, List<String> sampleUrls) {
        ClonedVoice clonedVoice = clonedVoiceRepository.findById(clonedVoiceId).orElse(null);
        if (clonedVoice == null) return;

        try {
            clonedVoice.setStatus(ClonedVoice.CloneStatus.PROCESSING);
            clonedVoice.setUpdateTime(LocalDateTime.now());
            clonedVoiceRepository.save(clonedVoice);

            if (apiKey == null || apiKey.isEmpty()) {
                log.warn("豆包API未配置，使用模拟结果");
                // 模拟成功
                clonedVoice.setVoiceId("mock_voice_" + clonedVoiceId);
                clonedVoice.setStatus(ClonedVoice.CloneStatus.COMPLETED);
                clonedVoice.setUpdateTime(LocalDateTime.now());
                clonedVoiceRepository.save(clonedVoice);
                return;
            }

            // 构建请求
            JSONObject requestBody = new JSONObject();
            requestBody.put("app", new JSONObject() {{
                put("appid", appId);
            }});
            requestBody.put("audio", new JSONObject() {{
                put("format", "mp3");
                put("rate", 16000);
                put("bits", 16);
                put("channel", 1);
            }});
            requestBody.put("voice", new JSONObject() {{
                put("clone_type", "instant");
                put("voice_id", "voice_" + System.currentTimeMillis());
            }});
            requestBody.put("samples", sampleUrls);

            RequestBody body = RequestBody.create(
                    requestBody.toJSONString(),
                    MediaType.get("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(CLONE_API_URL)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .post(body)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                log.info("声音复刻API响应: {}", responseBody);

                if (response.isSuccessful()) {
                    JSONObject json = JSON.parseObject(responseBody);
                    String taskId = json.getString("task_id");
                    if (taskId != null) {
                        // 保存任务ID，开始轮询
                        clonedVoice.setVoiceId(taskId);
                        clonedVoiceRepository.save(clonedVoice);
                        pollTaskStatus(clonedVoiceId, taskId);
                    }
                } else {
                    clonedVoice.setStatus(ClonedVoice.CloneStatus.FAILED);
                    clonedVoice.setErrorMessage("API请求失败: " + response.code());
                    clonedVoice.setUpdateTime(LocalDateTime.now());
                    clonedVoiceRepository.save(clonedVoice);
                }
            }

        } catch (Exception e) {
            log.error("提交克隆任务失败: {}", e.getMessage(), e);
            clonedVoice.setStatus(ClonedVoice.CloneStatus.FAILED);
            clonedVoice.setErrorMessage(e.getMessage());
            clonedVoice.setUpdateTime(LocalDateTime.now());
            clonedVoiceRepository.save(clonedVoice);
        }
    }

    /**
     * 轮询任务状态
     */
    private void pollTaskStatus(Long clonedVoiceId, String taskId) {
        int maxRetries = 30;
        int retryCount = 0;

        while (retryCount < maxRetries) {
            try {
                Thread.sleep(5000); // 5秒轮询一次
            } catch (InterruptedException e) {
                break;
            }

            try {
                Request request = new Request.Builder()
                        .url(TASK_STATUS_URL + "?task_id=" + taskId)
                        .header("Authorization", "Bearer " + apiKey)
                        .get()
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    JSONObject json = JSON.parseObject(responseBody);
                    String status = json.getString("status");

                    ClonedVoice clonedVoice = clonedVoiceRepository.findById(clonedVoiceId).orElse(null);
                    if (clonedVoice == null) break;

                    if ("completed".equals(status)) {
                        String voiceId = json.getString("voice_id");
                        clonedVoice.setVoiceId(voiceId);
                        clonedVoice.setStatus(ClonedVoice.CloneStatus.COMPLETED);
                        clonedVoice.setUpdateTime(LocalDateTime.now());
                        clonedVoiceRepository.save(clonedVoice);
                        log.info("声音复刻完成: clonedVoiceId={}, voiceId={}", clonedVoiceId, voiceId);
                        break;
                    } else if ("failed".equals(status)) {
                        String errorMsg = json.getString("error_message");
                        clonedVoice.setStatus(ClonedVoice.CloneStatus.FAILED);
                        clonedVoice.setErrorMessage(errorMsg);
                        clonedVoice.setUpdateTime(LocalDateTime.now());
                        clonedVoiceRepository.save(clonedVoice);
                        log.error("声音复刻失败: {}", errorMsg);
                        break;
                    } else {
                        log.info("声音复刻进行中: taskId={}, status={}", taskId, status);
                    }
                }

                retryCount++;
            } catch (Exception e) {
                log.error("轮询任务状态失败: {}", e.getMessage());
                retryCount++;
            }
        }

        if (retryCount >= maxRetries) {
            ClonedVoice clonedVoice = clonedVoiceRepository.findById(clonedVoiceId).orElse(null);
            if (clonedVoice != null && clonedVoice.getStatus() == ClonedVoice.CloneStatus.PROCESSING) {
                clonedVoice.setStatus(ClonedVoice.CloneStatus.FAILED);
                clonedVoice.setErrorMessage("任务超时");
                clonedVoice.setUpdateTime(LocalDateTime.now());
                clonedVoiceRepository.save(clonedVoice);
            }
        }
    }

    /**
     * 获取克隆状态
     */
    public ClonedVoice getCloneStatus(Long clonedVoiceId) {
        return clonedVoiceRepository.findById(clonedVoiceId).orElse(null);
    }

    /**
     * 获取用户所有克隆声音
     */
    public List<ClonedVoice> getUserClonedVoices(Long userId) {
        return clonedVoiceRepository.findByUserIdOrderByCreateTimeDesc(userId);
    }

    /**
     * 删除克隆声音
     */
    @Transactional
    public void deleteClonedVoice(Long clonedVoiceId) {
        voiceSampleRepository.deleteByClonedVoiceId(clonedVoiceId);
        clonedVoiceRepository.deleteById(clonedVoiceId);
    }

    /**
     * 获取克隆声音的样本
     */
    public List<VoiceSample> getVoiceSamples(Long clonedVoiceId) {
        return voiceSampleRepository.findByClonedVoiceId(clonedVoiceId);
    }

    @Data
    public static class CloneTask {
        private Long clonedVoiceId;
        private String taskId;
        private int retryCount;
    }
}
