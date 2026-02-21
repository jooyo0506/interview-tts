package com.interview.tts.service.ttsv2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interview.tts.config.TtsV2Properties;
import com.interview.tts.service.StorageService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.interview.tts.service.ttsv2.TtsV2Message;
import com.interview.tts.service.ttsv2.TtsV2Message.MsgType;

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
     * 简化版合成 - 直接传入文本和音色
     */
    public byte[] synthesize(String text, String voiceId) throws Exception {
        TtsV2Request request = new TtsV2Request();
        request.setText(text);
        request.setVoiceType(voiceId);
        request.setUserKey("test-user");

        TtsV2Response response = synthesize(request);
        // 返回音频数据（不包含上传到R2的逻辑）
        return downloadAudio(response.getAudioUrl());
    }

    /**
     * 带上下文的合成
     */
    public byte[] synthesizeWithContext(String text, String voiceId, String contextText) throws Exception {
        TtsV2Request request = new TtsV2Request();
        request.setText(text);
        request.setVoiceType(voiceId);
        request.setUserKey("test-user");
        request.setContextText(contextText);

        TtsV2Response response = synthesize(request);
        return downloadAudio(response.getAudioUrl());
    }

    /**
     * 从URL下载音频数据
     */
    private byte[] downloadAudio(String url) throws Exception {
        if (url == null || url.isEmpty()) {
            throw new RuntimeException("音频URL为空");
        }
        // 如果是本地路径，直接读取
        if (url.startsWith("http://localhost") || url.startsWith("./data")) {
            String path = url.replace("http://localhost:8080/media/", "");
            path = path.replace("./data/", "data/");
            return java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(path));
        }
        // 远程URL需要下载
        java.net.URL urlObj = new java.net.URL(url);
        try (java.io.InputStream is = urlObj.openStream()) {
            return is.readAllBytes();
        }
    }

    /**
     * 合成语音
     */
    public TtsV2Response synthesize(TtsV2Request request) throws Exception {
        log.info("开始TTSv2合成, voiceType={}, textLength={}", request.getVoiceType(), request.getText().length());

        // 1. 解析指令和标签
        ParsedText parsed = parseText(request.getText(), request.getMode());

        // 2. 构建API请求参数
        Map<String, Object> reqParams = buildRequestParams(request, parsed);

        // 打印完整请求参数
        try {
            String reqParamsJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(reqParams);
            log.info("=== 完整请求参数 ===\n{}", reqParamsJson);
        } catch (Exception e) {
            log.info("请求参数: {}", reqParams);
        }

        // 3. 创建WebSocket客户端并连接
        TtsV2WebSocketClient client = webSocketClient;
        try {
            // 根据音色选择正确的resource ID
            client.connect(request.getVoiceType());

            // 4. 发送连接开始
            client.sendStartConnection();
            client.waitForEvent(TtsV2EventType.CONNECTION_STARTED.getValue());

            // 5. 生成会话ID并发送会话开始 (使用官方demo格式)
            String sessionId = UUID.randomUUID().toString();

            // 构建START_SESSION请求 (与官方demo一致)
            Map<String, Object> startSessionRequest = new HashMap<>();
            startSessionRequest.put("user", reqParams.get("user"));
            startSessionRequest.put("namespace", reqParams.get("namespace"));
            startSessionRequest.put("req_params", reqParams.get("req_params"));
            startSessionRequest.put("event", TtsV2EventType.START_SESSION.getValue());

            String startSessionJson = objectMapper.writeValueAsString(startSessionRequest);
            log.info("=== START_SESSION ===\n{}", startSessionJson);
            client.sendStartSession(sessionId, startSessionJson.getBytes(StandardCharsets.UTF_8));
            client.waitForEvent(TtsV2EventType.SESSION_STARTED.getValue());

            // 6. 发送文本 (使用官方示例格式 - 字符逐个发送)
            String textToSynthesize = parsed.getCleanText();

            // 构建TASK_REQUEST请求 - 逐字符发送
            for (char c : textToSynthesize.toCharArray()) {
                // 复制基础req_params并添加text
                @SuppressWarnings("unchecked")
                Map<String, Object> baseReqParams = (Map<String, Object>) reqParams.get("req_params");
                Map<String, Object> currentReqParams = new HashMap<>(baseReqParams);
                currentReqParams.put("text", String.valueOf(c));

                // 构建完整的TASK_REQUEST
                Map<String, Object> taskRequest = new HashMap<>();
                taskRequest.put("user", reqParams.get("user"));
                taskRequest.put("namespace", reqParams.get("namespace"));
                taskRequest.put("req_params", currentReqParams);
                taskRequest.put("event", TtsV2EventType.TASK_REQUEST.getValue());

                String taskJson = objectMapper.writeValueAsString(taskRequest);
                log.info("=== TASK_REQUEST (char: {}) ===\n{}", c, taskJson);
                client.sendTaskRequest(sessionId, taskJson.getBytes(StandardCharsets.UTF_8));
            }

            // 7. 发送FINISH_SESSION
            client.sendFinishSession(sessionId);

            // 8. 接收音频流和字幕
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

                // 检查是否是错误消息
                if (message.getType() == MsgType.ERROR) {
                    String errorMsg = "未知错误";
                    int errorCode = message.getErrorCode();
                    if (message.getPayload() != null) {
                        errorMsg = new String(message.getPayload(), StandardCharsets.UTF_8);
                    }
                    log.error("TTSv2服务返回错误: errorCode={}, errorMsg={}", errorCode, errorMsg);
                    throw new RuntimeException("TTSv2服务返回错误: " + errorCode + " - " + errorMsg);
                }

                TtsV2EventType event = message.getEvent();

                // 句子开始
                if (event == TtsV2EventType.TTS_SENTENCE_START) {
                    if (message.getPayload() != null) {
                        currentSentence = new String(message.getPayload(), StandardCharsets.UTF_8);
                        sentenceStartTime = System.currentTimeMillis() - startTime;
                    }
                }
                // 句子结束
                else if (event == TtsV2EventType.TTS_SENTENCE_END) {
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
                else if (event == TtsV2EventType.TTS_RESPONSE) {
                    if (message.getPayload() != null) {
                        audioStream.write(message.getPayload());
                    }
                }
                // 会话结束
                else if (event == TtsV2EventType.SESSION_FINISHED) {
                    break;
                }
            }

            // 注意：收到SESSION_FINISHED后，不需要再发送FINISH_SESSION
            // 服务端已经处理完session，直接发送FINISH_CONNECTION即可

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
        // #后面只匹配字母数字中文，不匹配后面的文本
        String cleanText = text
            .replaceAll("#[a-zA-Z0-9\\u4e00-\\u9fa5]+", "")  // 匹配 #指令
            .replaceAll("【[^】】]+】", "")  // 匹配 【标签】
            .replaceAll("\\s+", " ")       // 多空格变单空格
            .trim();
        result.setCleanText(cleanText);

        return result;
    }

    /**
     * 构建API请求参数 (与官方demo一致)
     */
    private Map<String, Object> buildRequestParams(TtsV2Request request, ParsedText parsed) {
        Map<String, Object> reqParams = new HashMap<>();

        // 用户信息 (top level)
        Map<String, String> user = new HashMap<>();
        user.put("uid", request.getUserKey() != null ? request.getUserKey() : "anonymous");
        reqParams.put("user", user);

        // namespace (top level)
        reqParams.put("namespace", "BidirectionalTTS");

        // 构建req_params (包含speaker, audio_params, additions)
        Map<String, Object> reqParamsInner = new HashMap<>();

        // 音色
        reqParamsInner.put("speaker", request.getVoiceType());

        // 音频参数
        Map<String, Object> audioParams = new HashMap<>();
        audioParams.put("format", "mp3");
        audioParams.put("sample_rate", 24000);
        audioParams.put("enable_timestamp", true);

        // 语音指令 - emotion放在audio_params中
        if (!parsed.getCommands().isEmpty()) {
            String emotion = parsed.getCommands().get(0);
            audioParams.put("emotion", emotion);
        }
        reqParamsInner.put("audio_params", audioParams);

        // 扩展参数 - 需要转成JSON字符串（与官方demo一致）
        Map<String, Object> additionsMap = new HashMap<>();
        additionsMap.put("disable_markdown_filter", false);

        // 引用上文
        if (request.getContextText() != null && !request.getContextText().isEmpty()) {
            additionsMap.put("context_texts", Collections.singletonList(request.getContextText()));
        }

        // 语音标签 - 使用voice_enroll参数（如果有的话）
        // 注意：根据火山引擎文档，voice_tags可能不是有效参数，暂时移除

        // 转成JSON字符串（官方demo格式）
        String additionsJson;
        try {
            additionsJson = objectMapper.writeValueAsString(additionsMap);
        } catch (Exception e) {
            additionsJson = "{\"disable_markdown_filter\":false}";
        }
        reqParamsInner.put("additions", additionsJson);

        // 将req_params放入顶层
        reqParams.put("req_params", reqParamsInner);

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
