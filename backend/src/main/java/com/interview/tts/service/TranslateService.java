package com.interview.tts.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.interview.tts.entity.Translation;
import com.interview.tts.repository.TranslationRepository;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 同声传译服务
 * 对接豆包同传2.0 API
 */
@Slf4j
@Service
public class TranslateService {

    private static final String TRANSLATE_API_URL = "https://openspeech.bytedance.com/api/v2/translate";

    @Value("${doubao.api-key:}")
    private String apiKey;

    @Value("${volcengine.tts.app-id:}")
    private String appId;

    private final TranslationRepository translationRepository;
    private final DouyinTtsService ttsService;
    private final StorageService storageService;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

    public TranslateService(TranslationRepository translationRepository,
                            DouyinTtsService ttsService,
                            StorageService storageService) {
        this.translationRepository = translationRepository;
        this.ttsService = ttsService;
        this.storageService = storageService;
    }

    /**
     * 文本翻译 (S2T - Speech to Text)
     */
    @Transactional
    public Translation translateText(Long userId, String text, String sourceLang, String targetLang) {
        Translation translation = new Translation();
        translation.setUserId(userId);
        translation.setSourceText(text);
        translation.setSourceLang(sourceLang);
        translation.setTargetLang(targetLang);
        translation.setTranslationType(Translation.TranslationType.TEXT);
        translation.setCreateTime(LocalDateTime.now());

        try {
            String translatedText = callTranslateApi(text, sourceLang, targetLang);
            translation.setTranslatedText(translatedText);
        } catch (Exception e) {
            log.error("翻译失败: {}", e.getMessage());
            translation.setTranslatedText("翻译失败: " + e.getMessage());
        }

        return translationRepository.save(translation);
    }

    /**
     * 语音翻译 (S2S - Speech to Speech)
     */
    @Transactional
    public Translation translateSpeech(Long userId, String audioUrl, String sourceLang, String targetLang, String voiceName) {
        Translation translation = new Translation();
        translation.setUserId(userId);
        translation.setSourceAudioUrl(audioUrl);
        translation.setSourceLang(sourceLang);
        translation.setTargetLang(targetLang);
        translation.setTranslationType(Translation.TranslationType.SPEECH);
        translation.setCreateTime(LocalDateTime.now());

        try {
            // 1. 语音识别 (ASR)
            String sourceText = "语音识别内容"; // 实际应该调用ASR API
            translation.setSourceText(sourceText);

            // 2. 翻译文本
            String translatedText = callTranslateApi(sourceText, sourceLang, targetLang);
            translation.setTranslatedText(translatedText);

            // 3. 生成目标语言语音
            if (voiceName == null) {
                voiceName = getVoiceForLang(targetLang);
            }
            byte[] audioData = ttsService.generateAudio(translatedText, voiceName);
            String translatedAudioUrl = storageService.uploadAudio(audioData, "translate_" + System.currentTimeMillis());

            translation.setTranslatedAudioUrl(translatedAudioUrl);
            translation.setTranslatedAudioDuration(storageService.estimateDuration(audioData));

        } catch (Exception e) {
            log.error("语音翻译失败: {}", e.getMessage());
        }

        return translationRepository.save(translation);
    }

    /**
     * 调用翻译API
     */
    private String callTranslateApi(String text, String sourceLang, String targetLang) throws IOException {
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("豆包API未配置，使用模拟翻译");
            return simulateTranslate(text, sourceLang, targetLang);
        }

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
        requestBody.put("trans", new JSONObject() {{
            put("from", sourceLang);
            put("to", targetLang);
            put("text", text);
        }});

        RequestBody body = RequestBody.create(
                requestBody.toJSONString(),
                MediaType.get("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(TRANSLATE_API_URL)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            log.info("翻译API响应: {}", responseBody);

            if (response.isSuccessful()) {
                JSONObject json = JSON.parseObject(responseBody);
                return json.getString("translated_text");
            } else {
                throw new IOException("翻译API请求失败: " + response.code());
            }
        }
    }

    /**
     * 模拟翻译
     */
    private String simulateTranslate(String text, String sourceLang, String targetLang) {
        // 简单的模拟翻译
        if ("zh".equals(sourceLang) && "en".equals(targetLang)) {
            return "[English Translation] " + text;
        } else if ("en".equals(sourceLang) && "zh".equals(targetLang)) {
            return "[中文翻译] " + text;
        }
        return text;
    }

    /**
     * 根据语言获取对应音色
     */
    private String getVoiceForLang(String lang) {
        switch (lang) {
            case "zh":
                return "BV001_streaming";
            case "en":
                return "BV503_streaming";
            case "ja":
                return "BV524_streaming";
            default:
                return "BV001_streaming";
        }
    }

    /**
     * 获取用户翻译历史
     */
    public List<Translation> getUserTranslations(Long userId) {
        return translationRepository.findByUserIdOrderByCreateTimeDesc(userId);
    }

    /**
     * 获取翻译详情
     */
    public Translation getTranslation(Long translationId) {
        return translationRepository.findById(translationId).orElse(null);
    }

    @Data
    public static class TranslateRequest {
        private String text;
        private String audioUrl;
        private String sourceLang = "zh";
        private String targetLang = "en";
        private String voiceName;
        private String type; // TEXT or SPEECH
    }
}
