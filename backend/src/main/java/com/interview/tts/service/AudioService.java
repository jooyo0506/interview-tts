package com.interview.tts.service;

import com.interview.tts.dto.AudioDetailResponse;
import com.interview.tts.dto.AudioGenerateResponse;
import com.interview.tts.dto.AudioListItem;
import com.interview.tts.entity.AudioFile;
import com.interview.tts.entity.SysUser;
import com.interview.tts.entity.TextRecord;
import com.interview.tts.entity.UserCollect;
import com.interview.tts.repository.AudioFileRepository;
import com.interview.tts.repository.TextRecordRepository;
import com.interview.tts.repository.UserCollectRepository;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AudioService {

    private final TextPreprocessService textPreprocessService;
    private final DouyinTtsService ttsService;
    private final StorageService storageService;
    private final CacheService cacheService;
    private final RateLimitService rateLimitService;
    private final UserService userService;

    private final TextRecordRepository textRecordRepository;
    private final AudioFileRepository audioFileRepository;
    private final UserCollectRepository userCollectRepository;

    // OkHttpClient用于下载音频
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();

    // 缓存已处理完成的任务，避免重复查询数据库
    private final ConcurrentHashMap<String, String> processedTaskCache = new ConcurrentHashMap<>();
    // 记录已完成任务的查询次数，实现限流
    private final ConcurrentHashMap<String, Integer> taskQueryCount = new ConcurrentHashMap<>();
    private static final int MAX_QUERY_COUNT_PER_TASK = 10; // 每个任务最多查询10次

    @PostConstruct
    public void init() {
        storageService.init();
    }

    /**
     * 长文本异步生成 - 创建任务（普通版）
     */
    @Transactional
    public AudioGenerateResponse generateLongText(String userKey, String rawText, String voiceName) {
        return generateLongText(userKey, rawText, voiceName, false);
    }

    /**
     * 长文本异步生成 - 创建任务
     * @param userKey 用户标识
     * @param rawText 原始文本
     * @param voiceName 音色名称
     * @param useEmotion 是否使用情感预测版
     */
    @Transactional
    public AudioGenerateResponse generateLongText(String userKey, String rawText, String voiceName, boolean useEmotion) {
        // 1. 校验用户
        SysUser user = userService.getUserByKey(userKey);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 2. 校验文本长度（长文本最大10万字符）
        if (rawText == null || rawText.isEmpty()) {
            throw new RuntimeException("文本不能为空");
        }
        if (rawText.length() > 10000) {
            throw new RuntimeException("文本长度不能超过1万字符");
        }

        // 3. 校验并增加限额
        rateLimitService.checkAndIncrement(userKey);

        // 4. 文本预处理
        String processedText = textPreprocessService.preprocess(rawText);

        // 5. 创建异步任务
        String taskId = ttsService.createLongTextTask(processedText, voiceName, useEmotion);

        // 6. 保存任务记录（状态为处理中）
        Long audioFileId = createRecord(user.getId(), rawText, processedText, voiceName, null, 0);
        saveTaskId(audioFileId, taskId);

        // 返回任务ID和audioFileId，前端可以轮询查询
        return new AudioGenerateResponse(audioFileId, (String) null, 0, taskId);
    }

    /**
     * 查询长文本任务状态
     */
    public AudioGenerateResponse queryLongTextTask(String userKey, Long audioFileId, String taskId) {
        return queryLongTextTask(userKey, audioFileId, taskId, false);
    }

    /**
     * 查询长文本任务状态
     */
    public AudioGenerateResponse queryLongTextTask(String userKey, Long audioFileId, String taskId, boolean useEmotion) {
        SysUser user = userService.getUserByKey(userKey);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 限流：检查查询次数，超过限制直接返回
        int queryCount = taskQueryCount.getOrDefault(taskId, 0);
        if (queryCount >= MAX_QUERY_COUNT_PER_TASK) {
            log.warn("⛔ 任务查询次数超限，不再查询: taskId={}, count={}", taskId, queryCount);
            // 尝试从缓存或数据库获取结果
            String cachedR2Url = processedTaskCache.get(taskId);
            if (cachedR2Url != null) {
                return new AudioGenerateResponse(audioFileId, cachedR2Url, 0, taskId);
            }
            // 尝试从数据库获取
            AudioFile dbFile = audioFileRepository.findById(audioFileId).orElse(null);
            if (dbFile != null && dbFile.getR2Url() != null) {
                processedTaskCache.put(taskId, dbFile.getR2Url());
                taskQueryCount.remove(taskId); // 清理查询计数
                return new AudioGenerateResponse(audioFileId, dbFile.getR2Url(),
                        dbFile.getDuration() != null ? dbFile.getDuration() : 0, taskId);
            }
            // 超过限制且没有结果，返回处理中状态
            return new AudioGenerateResponse((Long) null, (String) null, 0, taskId);
        }
        // 增加查询计数
        taskQueryCount.put(taskId, queryCount + 1);

        // 先检查内存缓存，避免重复处理
        String cachedR2Url = processedTaskCache.get(taskId);
        if (cachedR2Url != null) {
            log.info("⏭️ 任务已在缓存中，直接返回: {}", cachedR2Url);
            // 清理查询计数
            taskQueryCount.remove(taskId);
            // 从数据库获取正确的时长
            AudioFile cachedFile = audioFileRepository.findById(audioFileId).orElse(null);
            int duration = (cachedFile != null && cachedFile.getDuration() != null) ? cachedFile.getDuration() : 0;
            return new AudioGenerateResponse(audioFileId, cachedR2Url, duration, taskId);
        }

        // 检查是否已经下载失败过（只有失败才查数据库）
        AudioFile audioFileCheck = audioFileRepository.findById(audioFileId).orElse(null);
        if (audioFileCheck != null && Boolean.TRUE.equals(audioFileCheck.getDownloadFailed())) {
            log.info("⏭️ 下载之前已失败，直接返回临时URL: {}", audioFileCheck.getTempAudioUrl());
            AudioGenerateResponse response = new AudioGenerateResponse(audioFileId, (String) null,
                    audioFileCheck.getDuration() != null ? audioFileCheck.getDuration() : 0, taskId);
            response.setAudioUrl(audioFileCheck.getTempAudioUrl());
            return response;
        }

        // 查询任务状态
        DouyinTtsService.AsyncTaskResult result = ttsService.queryLongTextTask(taskId, useEmotion);
        log.info("任务状态: status={}", result.getTaskStatus());
        if (result.getTaskStatus() == 1) {
            log.info("✅ 任务完成! 临时音频URL: {}", result.getAudioUrl());
        }

        // 如果任务完成，保存音频
        if (result.getTaskStatus() == 1 && result.getAudioUrl() != null) {
            // 先检查缓存（优先，避免查数据库）
            String cachedUrl = processedTaskCache.get(taskId);
            if (cachedUrl != null) {
                log.info("⏭️ 任务已在缓存中，直接返回: {}", cachedUrl);
                return new AudioGenerateResponse(audioFileId, cachedUrl, 0, taskId);
            }

            // 再检查数据库
            AudioFile existingAudioFile = audioFileRepository.findById(audioFileId).orElse(null);
            if (existingAudioFile != null && existingAudioFile.getR2Url() != null && !existingAudioFile.getR2Url().isEmpty()) {
                log.info("⏭️ 音频已处理完成，直接返回已有URL: {}", existingAudioFile.getR2Url());
                // 存入缓存
                processedTaskCache.put(taskId, existingAudioFile.getR2Url());
                taskQueryCount.remove(taskId); // 清理查询计数
                return new AudioGenerateResponse(audioFileId, existingAudioFile.getR2Url(),
                        existingAudioFile.getDuration() != null ? existingAudioFile.getDuration() : 0, taskId);
            }

            try {
                // 1. 下载火山引擎的临时音频
                log.info("========== 开始持久化处理 ==========");
                log.info("📥 步骤1: 从火山引擎下载临时音频...");
                byte[] audioData = downloadAudio(result.getAudioUrl());
                log.info("✅ 音频下载成功! 大小: {} bytes ({} KB)", audioData.length, audioData.length / 1024);

                // 2. 上传到R2进行持久化存储
                log.info("📤 步骤2: 上传到R2持久化存储...");
                String r2Url = storageService.uploadAudio(audioData, "longtext_" + taskId);
                log.info("✅ 上传成功! R2 URL: {}", r2Url);

                // 3. 更新数据库记录
                log.info("💾 步骤3: 更新数据库...");
                AudioFile audioFile = audioFileRepository.findById(audioFileId).orElse(null);
                if (audioFile != null && audioFile.getTextRecordId() != null) {
                    TextRecord textRecord = textRecordRepository.findById(audioFile.getTextRecordId()).orElse(null);
                    if (textRecord != null) {
                        audioFile.setR2Url(r2Url);
                        audioFile.setDuration(estimateDurationFromText(textRecord.getRawText()));
                        audioFileRepository.save(audioFile);
                        log.info("✅ 数据库更新完成");
                    }
                }
                // 存入缓存，避免后续重复处理
                processedTaskCache.put(taskId, r2Url);
                // 清理查询计数
                taskQueryCount.remove(taskId);

                // 获取正确的音频时长
                int audioDuration = 0;
                AudioFile savedFile = audioFileRepository.findById(audioFileId).orElse(null);
                if (savedFile != null && savedFile.getDuration() != null) {
                    audioDuration = savedFile.getDuration();
                }

                log.info("========== 持久化处理完成! ==========");
                AudioGenerateResponse response = new AudioGenerateResponse(audioFileId, r2Url, audioDuration, taskId);
                return response;
            } catch (Exception e) {
                log.error("❌ 持久化处理失败: {}", e.getMessage(), e);
                // 保存失败状态，防止无限重试
                try {
                    AudioFile audioFile = audioFileRepository.findById(audioFileId).orElse(null);
                    if (audioFile != null) {
                        audioFile.setDownloadFailed(true);
                        audioFile.setTempAudioUrl(result.getAudioUrl());
                        audioFileRepository.save(audioFile);
                        log.info("💾 已记录下载失败状态，避免后续无限重试");
                    }
                } catch (Exception saveErr) {
                    log.error("保存失败状态失败: {}", saveErr.getMessage());
                }
                // 返回临时URL，让前端可以尝试播放
                log.warn("⚠️ 降级处理: 返回临时URL给前端");
                AudioGenerateResponse response = new AudioGenerateResponse(audioFileId, (String) null, result.getTextLength(), taskId);
                response.setAudioUrl(result.getAudioUrl());
                return response;
            }
        } else if (result.getTaskStatus() == 2) {
            log.error("❌ 任务合成失败: {}", result.getErrorMessage());
            throw new RuntimeException("合成失败: " + result.getErrorMessage());
        }

        // 任务还在处理中
        log.info("⏳ 任务仍在处理中...");
        return new AudioGenerateResponse((Long) null, (String) null, 0, taskId);
    }

    private void saveTaskId(Long audioFileId, String taskId) {
        AudioFile audioFile = audioFileRepository.findById(audioFileId).orElse(null);
        if (audioFile != null && audioFile.getTextRecordId() != null) {
            TextRecord textRecord = textRecordRepository.findById(audioFile.getTextRecordId()).orElse(null);
            if (textRecord != null) {
                textRecord.setSsmlText(textRecord.getSsmlText() + "\n<!--taskId:" + taskId + "-->");
                textRecordRepository.save(textRecord);
            }
        }
    }

    /**
     * 从URL下载音频文件
     */
    private byte[] downloadAudio(String audioUrl) throws Exception {
        Request request = new Request.Builder()
                .url(audioUrl)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new Exception("下载失败: " + response.code());
            }
            if (response.body() == null) {
                throw new Exception("响应体为空");
            }
            return response.body().bytes();
        }
    }

    private int estimateDurationFromText(String text) {
        // 粗略估算：假设平均每秒15个字符
        return text.length() / 15;
    }

    @Transactional
    public AudioGenerateResponse generate(String userKey, String rawText, String voiceName) {
        // 1. 校验用户
        SysUser user = userService.getUserByKey(userKey);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 2. 校验文本长度
        rateLimitService.checkTextLength(rawText);

        // 3. 校验并增加限额
        rateLimitService.checkAndIncrement(userKey);

        // 4. 检查缓存
        String promptHash = textPreprocessService.getPromptHash();
        String cachedUrl = cacheService.getCachedUrl(rawText, voiceName, promptHash);
        if (cachedUrl != null) {
            log.info("缓存命中，直接返回: {}", cachedUrl);
            // 即使缓存命中，也需要创建记录
            createRecord(user.getId(), rawText, null, voiceName, cachedUrl, 0);
            return new AudioGenerateResponse(null, cachedUrl, 0);
        }

        // 5. 文本预处理
        String ssmlText = textPreprocessService.preprocess(rawText);

        // 6. TTS 合成
        byte[] audioData = ttsService.generateAudio(ssmlText, voiceName);

        // 7. 存储
        String r2Url = storageService.uploadAudio(audioData, rawText);

        // 8. 估算时长
        int duration = storageService.estimateDuration(audioData);

        // 9. 保存到缓存
        cacheService.saveToCache(rawText, voiceName, promptHash, r2Url);

        // 10. 保存记录
        Long textRecordId = createRecord(user.getId(), rawText, ssmlText, voiceName, r2Url, duration);

        return new AudioGenerateResponse(textRecordId, r2Url, duration);
    }

    private Long createRecord(Long userId, String rawText, String ssmlText, String voiceName, String r2Url, int duration) {
        // 保存文本记录
        TextRecord textRecord = new TextRecord();
        textRecord.setUserId(userId);
        textRecord.setRawText(rawText);
        textRecord.setSsmlText(ssmlText);
        textRecord.setVoiceName(voiceName);
        textRecord.setCreateTime(LocalDateTime.now());
        textRecord = textRecordRepository.save(textRecord);

        // 保存音频文件记录
        AudioFile audioFile = new AudioFile();
        audioFile.setTextRecordId(textRecord.getId());
        audioFile.setUserId(userId);
        audioFile.setR2Url(r2Url);
        audioFile.setDuration(duration);
        audioFile.setCreateTime(LocalDateTime.now());
        audioFile = audioFileRepository.save(audioFile);

        return audioFile.getId();
    }

    public List<AudioListItem> getMyList(String userKey) {
        SysUser user = userService.getUserByKey(userKey);
        if (user == null) {
            return List.of();
        }

        List<AudioFile> audioFiles = audioFileRepository.findByUserIdOrderByCreateTimeDesc(user.getId());

        return audioFiles.stream()
                .map(af -> {
                    TextRecord tr = textRecordRepository.findById(af.getTextRecordId()).orElse(null);
                    String rawText = tr != null ? tr.getRawText() : "";
                    String voiceName = tr != null ? tr.getVoiceName() : "";
                    String title = af.getName() != null ? af.getName() : (rawText.length() > 20 ? rawText.substring(0, 20) + "..." : rawText);
                    return new AudioListItem(af.getId(), af.getR2Url(), rawText, voiceName, af.getDuration(), af.getCreateTime().toString(), title);
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public boolean toggleCollect(String userKey, Long audioFileId) {
        SysUser user = userService.getUserByKey(userKey);
        if (user == null) {
            return false;
        }

        boolean isCollected = userCollectRepository.existsByUserIdAndAudioFileId(user.getId(), audioFileId);

        if (isCollected) {
            // 取消收藏
            userCollectRepository.deleteByUserIdAndAudioFileId(user.getId(), audioFileId);
            return false;
        } else {
            // 添加收藏
            UserCollect collect = new UserCollect();
            collect.setUserId(user.getId());
            collect.setAudioFileId(audioFileId);
            collect.setCreateTime(LocalDateTime.now());
            userCollectRepository.save(collect);
            return true;
        }
    }

    public boolean isCollected(String userKey, Long audioFileId) {
        SysUser user = userService.getUserByKey(userKey);
        if (user == null) {
            return false;
        }
        return userCollectRepository.existsByUserIdAndAudioFileId(user.getId(), audioFileId);
    }

    public List<AudioListItem> getCollectList(String userKey) {
        SysUser user = userService.getUserByKey(userKey);
        if (user == null) {
            return List.of();
        }

        List<UserCollect> collects = userCollectRepository.findByUserIdOrderByCreateTimeDesc(user.getId());

        return collects.stream()
                .map(collect -> {
                    AudioFile af = audioFileRepository.findById(collect.getAudioFileId()).orElse(null);
                    if (af == null) {
                        return null;
                    }
                    TextRecord tr = textRecordRepository.findById(af.getTextRecordId()).orElse(null);
                    String rawText = tr != null ? tr.getRawText() : "";
                    String voiceName = tr != null ? tr.getVoiceName() : "";
                    String title = af.getName() != null ? af.getName() : rawText;
                    return new AudioListItem(af.getId(), af.getR2Url(), rawText, voiceName, af.getDuration(), af.getCreateTime().toString(), title);
                })
                .filter(item -> item != null)
                .collect(Collectors.toList());
    }

    /**
     * 获取音频详情（包含完整文本）
     */
    public AudioDetailResponse getAudioDetail(String userKey, Long audioId) {
        SysUser user = userService.getUserByKey(userKey);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        AudioFile audioFile = audioFileRepository.findById(audioId).orElse(null);
        if (audioFile == null) {
            throw new RuntimeException("音频不存在");
        }

        // 验证所有权
        if (!audioFile.getUserId().equals(user.getId())) {
            throw new RuntimeException("无权限访问");
        }

        // 获取文本记录
        TextRecord textRecord = null;
        if (audioFile.getTextRecordId() != null) {
            textRecord = textRecordRepository.findById(audioFile.getTextRecordId()).orElse(null);
        }

        AudioDetailResponse response = new AudioDetailResponse();
        response.setId(audioFile.getId());
        response.setR2Url(audioFile.getR2Url());
        response.setRawText(textRecord != null ? textRecord.getRawText() : "");
        response.setVoiceName(textRecord != null ? textRecord.getVoiceName() : "");
        response.setDuration(audioFile.getDuration());
        response.setTitle(audioFile.getName() != null ? audioFile.getName() :
                (textRecord != null && textRecord.getRawText() != null ?
                        (textRecord.getRawText().length() > 20 ?
                                textRecord.getRawText().substring(0, 20) + "..." :
                                textRecord.getRawText()) : "未命名"));
        if (audioFile.getCreateTime() != null) {
            long createTimeMs = audioFile.getCreateTime().atZone(java.time.ZoneOffset.ofHours(8)).toInstant().toEpochMilli();
            response.setCreateTime(createTimeMs);
        } else {
            response.setCreateTime(0L);
        }

        return response;
    }

    // ========== 管理功能 ==========

    @Transactional
    public void deleteAudio(String userKey, Long audioId) {
        SysUser user = userService.getUserByKey(userKey);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        AudioFile audioFile = audioFileRepository.findById(audioId).orElse(null);
        if (audioFile == null) {
            throw new RuntimeException("音频不存在");
        }

        // 验证所有权
        if (!audioFile.getUserId().equals(user.getId())) {
            throw new RuntimeException("无权限删除");
        }

        // 删除关联的收藏记录
        userCollectRepository.deleteByUserIdAndAudioFileId(user.getId(), audioId);

        // 获取textRecordId并删除
        Long textRecordId = audioFile.getTextRecordId();

        // 删除音频记录
        audioFileRepository.deleteById(audioId);

        // 删除文本记录
        if (textRecordId != null) {
            textRecordRepository.deleteById(textRecordId);
        }
    }

    @Transactional
    public void deleteBatch(String userKey, List<Long> audioIds) {
        for (Long id : audioIds) {
            try {
                deleteAudio(userKey, id);
            } catch (Exception e) {
                log.warn("删除音频失败: id={}, error={}", id, e.getMessage());
            }
        }
    }

    @Transactional
    public void renameAudio(String userKey, Long audioId, String name) {
        SysUser user = userService.getUserByKey(userKey);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        AudioFile audioFile = audioFileRepository.findById(audioId).orElse(null);
        if (audioFile == null) {
            throw new RuntimeException("音频不存在");
        }

        // 验证所有权
        if (!audioFile.getUserId().equals(user.getId())) {
            throw new RuntimeException("无权限修改");
        }

        audioFile.setName(name);
        audioFileRepository.save(audioFile);
    }

    @Transactional
    public void deleteCollect(String userKey, Long audioId) {
        SysUser user = userService.getUserByKey(userKey);
        if (user == null) {
            return;
        }
        userCollectRepository.deleteByUserIdAndAudioFileId(user.getId(), audioId);
    }
}
