package com.interview.tts.service.ttsv2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interview.tts.config.TtsV2Properties;
import com.interview.tts.service.StorageService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TTS v2.0 双向流式语音合成服务
 */
@Slf4j
@Service
public class TtsV2Service {

    @Autowired
    private TtsV2Properties properties;

    @Autowired
    private StorageService storageService;

    @Autowired
    private TtsV2WebSocketClient webSocketClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 合成语音
     */
    public TtsV2Response synthesize(TtsV2Request request) throws Exception {
        log.info("开始TTSv2合成, voiceType={}, textLength={}", request.getVoiceType(), request.getText().length());

        // 1. 解析指令和标签
        ParsedText parsed = parseText(request.getText(), request.getMode());

        // 2. 构建API请求参数
        Map<String, Object> reqParams = buildRequestParams(request, parsed);

        // 3. 创建WebSocket客户端并连接
        TtsV2WebSocketClient client = webSocketClient;
        try {
            client.connect();

            // 4. 发送连接开始
            client.sendStartConnection();
            client.waitForEvent(TtsV2EventType.CONNECTION_STARTED.getValue());

            // 5. 生成会话ID并发送会话开始
            String sessionId = UUID.randomUUID().toString();
            String sessionParamsJson = objectMapper.writeValueAsString(reqParams);
            client.sendStartSession(sessionId, sessionParamsJson.getBytes(StandardCharsets.UTF_8));
            client.waitForEvent(TtsV2EventType.SESSION_STARTED.getValue());

            // 6. 发送文本
            String textToSynthesize = parsed.getCleanText();
            Map<String, Object> taskRequest = new HashMap<>();
            taskRequest.put("text", textToSynthesize);

            String taskJson = objectMapper.writeValueAsString(taskRequest);
            client.sendTaskRequest(sessionId, taskJson.getBytes(StandardCharsets.UTF_8));

            // 7. 接收音频流和字幕
            ByteArrayOutputStream audioStream = new ByteArrayOutputStream();
            List<TtsV2Response.Subtitle> subtitles = new ArrayList<>();
            long startTime = System.currentTimeMillis();
            String currentSentence = "";
            long sentenceStartTime = 0;

            while (true) {
                TtsV2Message message = client.waitForAnyMessage(30000);
                if (message == null) {
                    break;
                }

                int event = message.getEvent();

                // 句子开始
                if (event == TtsV2EventType.TTS_SENTENCE_START.getValue()) {
                    if (message.getPayload() != null) {
                        currentSentence = new String(message.getPayload(), StandardCharsets.UTF_8);
                        sentenceStartTime = System.currentTimeMillis() - startTime;
                    }
                }
                // 句子结束
                else if (event == TtsV2EventType.TTS_SENTENCE_END.getValue()) {
                    long sentenceEndTime = System.currentTimeMillis() - startTime;
                    if (!currentSentence.isEmpty()) {
                        subtitles.add(TtsV2Response.Subtitle.builder()
                            .text(currentSentence)
                            .startTime(sentenceStartTime)
                            .endTime(sentenceEndTime)
                            .build());
                    }
                    currentSentence = "";
                }
                // 音频数据
                else if (event == TtsV2EventType.TTS_RESPONSE.getValue()) {
                    if (message.getPayload() != null) {
                        audioStream.write(message.getPayload());
                    }
                }
                // 会话结束
                else if (event == TtsV2EventType.SESSION_FINISHED.getValue()) {
                    break;
                }
            }

            // 8. 发送会话结束
            client.sendFinishSession(sessionId);
            client.waitForEvent(TtsV2EventType.SESSION_FINISHED.getValue());

            // 9. 计算时长
            int duration = (int) ((System.currentTimeMillis() - startTime) / 1000);

            // 10. 上传到R2
            byte[] audioData = audioStream.toByteArray();
            if (audioData.length == 0) {
                throw new RuntimeException("未收到音频数据");
            }

            String fileName = "ttsv2_" + System.currentTimeMillis() + ".mp3";
            String r2Url = storageService.uploadAudio(audioData, fileName);

            log.info("TTSv2合成完成, audioSize={}, duration={}", audioData.length, duration);

            return TtsV2Response.builder()
                .audioUrl(r2Url)
                .duration(duration)
                .subtitles(subtitles)
                .build();

        } finally {
            try {
                client.sendFinishConnection();
            } catch (Exception e) {
                log.warn("发送连接关闭失败", e);
            }
            client.close();
        }
    }

    /**
     * 解析文本中的指令和标签
     */
    private ParsedText parseText(String text, String mode) {
        ParsedText result = new ParsedText();
        result.setOriginalText(text);

        // 提取#语音指令
        List<String> commands = new ArrayList<>();
        Pattern cmdPattern = Pattern.compile("#([^#\\s]+)");
        Matcher cmdMatcher = cmdPattern.matcher(text);
        while (cmdMatcher.find()) {
            commands.add(cmdMatcher.group(1));
        }
        result.setCommands(commands);

        // 提取【语音标签】
        List<String> tags = new ArrayList<>();
        Pattern tagPattern = Pattern.compile("【([^】]+)】");
        Matcher tagMatcher = tagPattern.matcher(text);
        while (tagMatcher.find()) {
            tags.add(tagMatcher.group(1));
        }
        result.setTags(tags);

        // 移除#和【】标记，保留原文
        String cleanText = text
            .replaceAll("#[^#\\s]+", "")
            .replaceAll("【[^】]+】", "")
            .trim();
        result.setCleanText(cleanText);

        return result;
    }

    /**
     * 构建API请求参数
     */
    private Map<String, Object> buildRequestParams(TtsV2Request request, ParsedText parsed) {
        Map<String, Object> reqParams = new HashMap<>();

        // 用户信息
        Map<String, String> user = new HashMap<>();
        user.put("uid", request.getUserKey() != null ? request.getUserKey() : "anonymous");
        reqParams.put("user", user);
        reqParams.put("namespace", "BidirectionalTTS");

        // 音频参数
        Map<String, Object> audioParams = new HashMap<>();
        audioParams.put("format", "mp3");
        audioParams.put("sample_rate", 24000);
        audioParams.put("enable_timestamp", true);
        reqParams.put("audio_params", audioParams);

        // 音色
        reqParams.put("speaker", request.getVoiceType());

        // 扩展参数
        Map<String, Object> additions = new HashMap<>();
        additions.put("disable_markdown_filter", false);

        // 引用上文
        if (request.getContextText() != null && !request.getContextText().isEmpty()) {
            additions.put("context_texts", Collections.singletonList(request.getContextText()));
        }

        // 语音指令 - 转换为emotion参数
        if (!parsed.getCommands().isEmpty()) {
            // 取第一个指令作为情感
            String emotion = parsed.getCommands().get(0);
            additions.put("emotion", emotion);
        }

        // 语音标签 - 添加到文本
        if (!parsed.getTags().isEmpty()) {
            additions.put("voice_tags", parsed.getTags());
        }

        reqParams.put("additions", additions);

        return reqParams;
    }

    /**
     * 解析后的文本
     */
    @Data
    private static class ParsedText {
        private String originalText;
        private String cleanText;
        private List<String> commands;  // #指令
        private List<String> tags;      // 【标签】
    }
}
