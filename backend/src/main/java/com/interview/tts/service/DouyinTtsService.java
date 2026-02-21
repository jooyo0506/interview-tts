package com.interview.tts.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import com.interview.tts.exception.BusinessException;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 豆包TTS服务 (火山引擎)
 *
 * 配置说明 (application.yml):
 * volcengine:
 *   tts:
 *     app-id: your_app_id
 *     access-token: your_access_token
 *     cluster: volcano_tts
 *
 * 音色列表 (voice_type):
 * - BV001_streaming: 豆包原生
 * - BV701_streaming: 擎苍
 * - BV123_streaming: 阳光青年
 * 等
 */
@Slf4j
@Service
public class DouyinTtsService {

    private static final String API_URL = "https://openspeech.bytedance.com/api/v1/tts";
    private static final String HOST = "openspeech.bytedance.com";
    // 长文本异步合成接口 - 普通版
    private static final String ASYNC_SUBMIT_URL = "https://openspeech.bytedance.com/api/v1/tts_async/submit";
    private static final String ASYNC_QUERY_URL = "https://openspeech.bytedance.com/api/v1/tts_async/query";
    private static final String RESOURCE_ID_DEFAULT = "volc.tts_async.default";
    // 长文本异步合成接口 - 情感预测版
    private static final String ASYNC_SUBMIT_URL_EMOTION = "https://openspeech.bytedance.com/api/v1/tts_async_with_emotion/submit";
    private static final String ASYNC_QUERY_URL_EMOTION = "https://openspeech.bytedance.com/api/v1/tts_async_with_emotion/query";
    private static final String RESOURCE_ID_EMOTION = "volc.tts_async.emotion";

    @Value("${volcengine.tts.app-id:}")
    private String appId;

    @Value("${volcengine.tts.access-token:}")
    private String accessToken;

    @Value("${volcengine.tts.cluster:volcano_tts}")
    private String cluster;

    private final OkHttpClient httpClient;

    private static final List<VoiceInfo> CHINESE_VOICES = new ArrayList<>();
    private static final Map<String, String> VOICE_MAPPING = new HashMap<>();

    static {
        // 豆包TTS音色列表
        CHINESE_VOICES.add(new VoiceInfo("BV001_streaming", "zh-CN", "Female", "通用女声"));
        CHINESE_VOICES.add(new VoiceInfo("BV002_streaming", "zh-CN", "Male", "通用男声"));
        CHINESE_VOICES.add(new VoiceInfo("BV700_streaming", "zh-CN", "Female", "灿灿"));
        CHINESE_VOICES.add(new VoiceInfo("BV102_streaming", "zh-CN", "Male", "儒雅青年"));
        CHINESE_VOICES.add(new VoiceInfo("BV113_streaming", "zh-CN", "Female", "甜宠少御"));
        CHINESE_VOICES.add(new VoiceInfo("BV033_streaming", "zh-CN", "Male", "温柔小哥"));
        CHINESE_VOICES.add(new VoiceInfo("BV034_streaming", "zh-CN", "Female", "知性姐姐-双语"));
        CHINESE_VOICES.add(new VoiceInfo("BV524_streaming", "ja-JP", "Male", "日语男声"));
        CHINESE_VOICES.add(new VoiceInfo("BV503_streaming", "en-US", "Female", "活力女声-Ariana"));
        CHINESE_VOICES.add(new VoiceInfo("BV504_streaming", "en-US", "Male", "活力男声-Jackson"));
    }

    public DouyinTtsService() {
        this.httpClient = new OkHttpClient();
    }

    @PostConstruct
    public void init() {
        if (appId == null || appId.isEmpty()) {
            log.warn("豆包TTS未配置app-id，请检查配置!");
        } else if (accessToken == null || accessToken.isEmpty()) {
            log.warn("豆包TTS未配置access-token，请检查配置!");
        } else {
            log.info("豆包TTS 服务初始化完成，中文音色数量: {}", CHINESE_VOICES.size());
        }
    }

    public List<VoiceInfo> listChineseVoices() {
        return new ArrayList<>(CHINESE_VOICES);
    }

    /**
     * 生成语音
     * @param text 文本
     * @param voiceName 音色名称
     * @return 音频数据 (MP3格式)
     */
    public byte[] generateAudio(String text, String voiceName) {
        if (appId == null || appId.isEmpty()) {
            log.error("火山引擎TTS未配置app-id");
            throw BusinessException.ttsFailed();
        }

        if (accessToken == null || accessToken.isEmpty()) {
            log.error("火山引擎TTS未配置access-token");
            throw BusinessException.ttsFailed();
        }

        // 音色映射
        String mappedVoice = VOICE_MAPPING.getOrDefault(voiceName, voiceName);
        if (!mappedVoice.equals(voiceName)) {
            log.info("音色映射: {} -> {}", voiceName, mappedVoice);
        }
        final String voiceToCheck = mappedVoice;
        if (!CHINESE_VOICES.stream().anyMatch(v -> v.getName().equals(voiceToCheck))) {
            mappedVoice = "BV001_streaming";
            log.warn("未知音色 {}，使用默认音色 BV001_streaming", voiceName);
        }

        try {
            // 构造请求体（参考demo格式）
            TtsRequest ttsRequest = new TtsRequest();
            ttsRequest.setApp(new TtsRequest.App());
            ttsRequest.getApp().setAppid(appId);
            ttsRequest.getApp().setCluster(cluster);
            ttsRequest.getApp().setToken("access_token");

            ttsRequest.setUser(new TtsRequest.User());
            ttsRequest.getUser().setUid(UUID.randomUUID().toString());

            ttsRequest.setAudio(new TtsRequest.Audio());
            ttsRequest.getAudio().setVoiceType(mappedVoice);
            ttsRequest.getAudio().setEncoding("mp3");
            ttsRequest.getAudio().setSpeedRatio(1.0f);
            ttsRequest.getAudio().setVolumeRatio(1.0f);
            ttsRequest.getAudio().setPitchRatio(1.0f);

            ttsRequest.setRequest(new TtsRequest.Request());
            ttsRequest.getRequest().setReqID(UUID.randomUUID().toString());
            ttsRequest.getRequest().setText(text);
            ttsRequest.getRequest().setTextType("plain");
            ttsRequest.getRequest().setOperation("query");

            String reqBody = JSON.toJSONString(ttsRequest);
            log.info("豆包TTS请求: voice={}, text={}", mappedVoice, text.substring(0, Math.min(50, text.length())));

            // 使用Bearer Token鉴权
            String authorization = "Bearer; " + accessToken;

            RequestBody body = RequestBody.create(reqBody, MediaType.get("application/json; charset=utf-8"));
            Request request = new Request.Builder()
                    .url(API_URL)
                    .header("Host", HOST)
                    .header("Authorization", authorization)
                    .post(body)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "empty";
                    log.error("豆包TTS请求失败: status={}, body={}", response.code(), errorBody);
                    throw BusinessException.ttsFailed();
                }

                String responseBodyStr = response.body() != null ? response.body().string() : "";
                log.debug("豆包TTS原始响应: {}", responseBodyStr);

                if (responseBodyStr == null || responseBodyStr.isEmpty()) {
                    log.error("豆包TTS响应为空");
                    throw BusinessException.ttsFailed();
                }

                // 解析JSON响应，提取base64编码的音频
                JSONObject json = JSON.parseObject(responseBodyStr);

                // 检查是否有错误 (code=0或3000都是成功)
                int code = json.containsKey("code") ? json.getIntValue("code") : -1;
                if (code != 0 && code != 3000) {
                    String message = json.containsKey("message") ? json.getString("message") : "未知错误";
                    log.error("豆包TTS返回错误: code={}, message={}", json.getIntValue("code"), message);
                    throw BusinessException.ttsFailed();
                }

                // 提取base64音频数据
                String audioBase64 = null;
                if (json.containsKey("data")) {
                    audioBase64 = json.getString("data");
                } else if (json.containsKey("result")) {
                    JSONObject result = json.getJSONObject("result");
                    if (result != null && result.containsKey("data")) {
                        audioBase64 = result.getString("data");
                    }
                }

                if (audioBase64 == null || audioBase64.isEmpty()) {
                    log.error("豆包TTS响应中没有音频数据: {}", responseBodyStr);
                    throw BusinessException.ttsFailed();
                }

                // Base64解码
                byte[] audioData = Base64.getDecoder().decode(audioBase64);
                if (audioData == null || audioData.length == 0) {
                    log.error("豆包TTS解码后为空");
                    throw BusinessException.ttsFailed();
                }

                log.info("豆包TTS 合成成功: voice={}, size={}", mappedVoice, audioData.length);
                return audioData;
            }

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("豆包TTS 合成失败: {}", e.getMessage(), e);
            throw BusinessException.ttsFailed();
        }
    }

    // ========== 请求对象（参考demo）==========

    @Data
    public static class TtsRequest {
        private App app;
        private User user;
        private Audio audio;
        private Request request;

        @Data
        public static class App {
            @JSONField(name = "appid")
            private String appid;
            private String token = "access_token";
            private String cluster;
        }

        @Data
        public static class User {
            private String uid;
        }

        @Data
        public static class Audio {
            @JSONField(name = "voice_type")
            private String voiceType;
            private String encoding;
            @JSONField(name = "speed_ratio")
            private Float speedRatio = 1.0f;
            @JSONField(name = "volume_ratio")
            private Float volumeRatio = 1.0f;
            @JSONField(name = "pitch_ratio")
            private Float pitchRatio = 1.0f;
            private String emotion;
        }

        @Data
        public static class Request {
            @JSONField(name = "reqid")
            private String reqID;
            private String text;
            @JSONField(name = "text_type")
            private String textType;
            private String operation;
        }
    }

    public static class VoiceInfo {
        private final String name;
        private final String locale;
        private final String gender;
        private final String shortName;

        public VoiceInfo(String name, String locale, String gender, String shortName) {
            this.name = name;
            this.locale = locale;
            this.gender = gender;
            this.shortName = shortName;
        }

        public String getName() { return name; }
        public String getLocale() { return locale; }
        public String getGender() { return gender; }
        public String getShortName() { return shortName; }
    }

    // ========== 长文本异步合成 ==========

    /**
     * 长文本异步合成 - 创建任务
     * @param text 文本（≤10万字符）
     * @param voiceName 音色名称
     * @return 任务ID
     */
    public String createLongTextTask(String text, String voiceName) {
        return createLongTextTask(text, voiceName, false);
    }

    /**
     * 长文本异步合成 - 创建任务
     * @param text 文本（≤10万字符）
     * @param voiceName 音色名称
     * @param useEmotion 是否使用情感预测版
     * @return 任务ID
     */
    public String createLongTextTask(String text, String voiceName, boolean useEmotion) {
        log.info("========== 长文本TTS开始创建 ==========");
        log.info("参数: voice={}, textLength={}, emotion={}", voiceName, text.length(), useEmotion);

        if (appId == null || appId.isEmpty()) {
            log.error("❌ 火山引擎TTS未配置app-id");
            throw BusinessException.ttsFailed();
        }

        if (accessToken == null || accessToken.isEmpty()) {
            log.error("❌ 火山引擎TTS未配置access-token");
            throw BusinessException.ttsFailed();
        }

        log.info("✓ 火山引擎配置检查通过");
        log.info("  - appId: {}", appId.substring(0, Math.min(8, appId.length())) + "...");
        log.info("  - token: {}", accessToken.substring(0, Math.min(8, accessToken.length())) + "...");

        // 根据版本选择接口
        String submitUrl = useEmotion ? ASYNC_SUBMIT_URL_EMOTION : ASYNC_SUBMIT_URL;
        String resourceId = useEmotion ? RESOURCE_ID_EMOTION : RESOURCE_ID_DEFAULT;
        log.info("✓ 使用接口: {}", submitUrl);

        // 音色映射
        String mappedVoice = VOICE_MAPPING.getOrDefault(voiceName, voiceName);
        String finalMappedVoice = mappedVoice;
        if (!CHINESE_VOICES.stream().anyMatch(v -> v.getName().equals(finalMappedVoice))) {
            mappedVoice = "BV001_streaming";
            log.warn("⚠️ 未知音色 {}，使用默认音色 BV001_streaming", voiceName);
        }
        log.info("✓ 音色映射: {} -> {}", voiceName, mappedVoice);

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("appid", appId);
            requestBody.put("reqid", UUID.randomUUID().toString());
            requestBody.put("text", text);
            requestBody.put("format", "mp3");
            requestBody.put("voice_type", mappedVoice);
            requestBody.put("sample_rate", 24000);
            requestBody.put("enable_subtitle", 1); // 句级别字幕

            // 情感预测版可以指定情感类型
            if (useEmotion) {
                requestBody.put("voice", "");
            }

            String reqBody = JSON.toJSONString(requestBody);
            log.info("========== 发送请求到火山引擎 ==========");
            log.info("请求参数: {}", reqBody);

            // 使用Bearer Token鉴权
            String authorization = "Bearer; " + accessToken;

            RequestBody body = RequestBody.create(reqBody, MediaType.get("application/json; charset=utf-8"));
            Request request = new Request.Builder()
                    .url(submitUrl)
                    .header("Host", HOST)
                    .header("Authorization", authorization)
                    .header("Resource-Id", resourceId)
                    .post(body)
                    .build();

            log.info("发送请求中...");

            try (Response response = httpClient.newCall(request).execute()) {
                log.info("========== 收到火山引擎响应 ==========");
                log.info("HTTP状态码: {}", response.code());

                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "empty";
                    log.error("❌ 长文本TTS任务创建失败! HTTP状态码: {}", response.code());
                    log.error("错误响应: {}", errorBody);
                    throw BusinessException.ttsFailed();
                }

                String responseBody = response.body() != null ? response.body().string() : "";
                log.info("响应内容: {}", responseBody);

                JSONObject json = JSON.parseObject(responseBody);

                if (json.containsKey("task_id")) {
                    String taskId = json.getString("task_id");
                    log.info("✅ 长文本TTS任务创建成功!");
                    log.info("========== 任务ID: {} ==========", taskId);
                    return taskId;
                } else {
                    log.error("❌ 响应中无task_id: {}", responseBody);
                    throw BusinessException.ttsFailed();
                }
            }

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("长文本TTS任务创建异常: {}", e.getMessage(), e);
            throw BusinessException.ttsFailed();
        }
    }

    /**
     * 长文本异步合成 - 查询任务状态
     * @param taskId 任务ID
     * @return 任务结果
     */
    public AsyncTaskResult queryLongTextTask(String taskId) {
        return queryLongTextTask(taskId, false);
    }

    /**
     * 长文本异步合成 - 查询任务状态
     * @param taskId 任务ID
     * @param useEmotion 是否使用情感预测版
     * @return 任务结果
     */
    public AsyncTaskResult queryLongTextTask(String taskId, boolean useEmotion) {
        log.info("========== 查询长文本任务状态 ==========");
        log.info("任务ID: {}", taskId);

        if (appId == null || appId.isEmpty() || accessToken == null || accessToken.isEmpty()) {
            log.error("❌ 火山引擎未配置");
            throw BusinessException.ttsFailed();
        }

        // 根据版本选择接口
        String queryUrl = useEmotion ? ASYNC_QUERY_URL_EMOTION : ASYNC_QUERY_URL;
        String resourceId = useEmotion ? RESOURCE_ID_EMOTION : RESOURCE_ID_DEFAULT;

        try {
            String url = queryUrl + "?appid=" + appId + "&task_id=" + taskId;
            log.info("查询URL: {}", url);

            String authorization = "Bearer; " + accessToken;

            Request request = new Request.Builder()
                    .url(url)
                    .header("Host", HOST)
                    .header("Authorization", authorization)
                    .header("Resource-Id", resourceId)
                    .get()
                    .build();

            log.info("发送查询请求...");
            long startTime = System.currentTimeMillis();

            try (Response response = httpClient.newCall(request).execute()) {
                long costTime = System.currentTimeMillis() - startTime;
                log.info("========== 收到查询响应 ==========");
                log.info("HTTP状态码: {}, 耗时: {}ms", response.code(), costTime);

                String responseBody = response.body() != null ? response.body().string() : "";
                log.info("响应内容: {}", responseBody);

                if (!response.isSuccessful()) {
                    log.error("❌ 查询失败: HTTP {}", response.code());
                    throw BusinessException.ttsFailed();
                }

                JSONObject json = JSON.parseObject(responseBody);

                AsyncTaskResult result = new AsyncTaskResult();
                result.setTaskId(json.getString("task_id"));
                result.setTaskStatus(json.getIntValue("task_status")); // 0=合成中, 1=成功, 2=失败

                // 状态说明: 0=合成中, 1=成功, 2=失败
                String statusDesc = result.getTaskStatus() == 0 ? "⏳ 处理中" : (result.getTaskStatus() == 1 ? "✅ 成功" : "❌ 失败");
                log.info("任务状态: {} (status={})", statusDesc, result.getTaskStatus());

                if (result.getTaskStatus() == 1) {
                    result.setAudioUrl(json.getString("audio_url"));
                    result.setTextLength(json.getIntValue("text_length"));
                    log.info("✅ 音频URL: {}", result.getAudioUrl());
                    log.info("📝 文本长度: {} 字", result.getTextLength());

                    // 解析字幕
                    if (json.containsKey("sentences")) {
                        result.setSentences(json.getJSONArray("sentences"));
                        log.info("📋 字幕句子数: {}", json.getJSONArray("sentences").size());
                    }
                } else if (result.getTaskStatus() == 2) {
                    result.setErrorMessage(json.getString("message"));
                    log.error("❌ 错误信息: {}", result.getErrorMessage());
                }

                log.info("========== 查询任务状态结束 ==========");
                return result;
            }

        } catch (Exception e) {
            log.error("❌ 查询异常: {}", e.getMessage(), e);
            throw BusinessException.ttsFailed();
        }
    }

    /**
     * 长文本异步任务结果
     */
    @Data
    public static class AsyncTaskResult {
        private String taskId;
        private int taskStatus; // 0=合成中, 1=成功, 2=失败
        private String audioUrl;
        private int textLength;
        private String errorMessage;
        private Object sentences;
    }
}
