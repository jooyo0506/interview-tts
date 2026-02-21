package com.interview.tts.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * LLM脚本生成服务 - 用于生成播客对话脚本
 */
@Slf4j
@Service
public class LlmScriptService {

    private static final String API_URL = "https://ark.cn-beijing.volces.com/api/v3/chat/completions";

    @Value("${doubao.api-key:}")
    private String apiKey;

    @Value("${doubao.model-id:doubao-seed-2-0-lite-260215}")
    private String modelId;

    private final OkHttpClient httpClient;

    public LlmScriptService() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 将文本改写为双人对谈播客脚本
     * @param sourceText 原始文本
     * @return 对话脚本内容
     */
    public String generatePodcastScript(String sourceText) {
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("豆包API未配置，使用默认脚本格式");
            return generateDefaultScript(sourceText);
        }

        String prompt = buildPodcastPrompt(sourceText);

        try {
            String response = callLlmApi(prompt);
            return parseScriptFromResponse(response);
        } catch (Exception e) {
            log.error("LLM脚本生成失败: {}", e.getMessage(), e);
            return generateDefaultScript(sourceText);
        }
    }

    private String buildPodcastPrompt(String sourceText) {
        return String.format("""
                请将下面的文章或内容改写为双人对谈播客脚本。

要求：
1. 两位主播：主播A（女声）和主播B（男声）
2. 口语化表达，自然对话
3. 包含开场白和结束语
4. 每人每句话前标注"主播A："或"主播B："
5. 内容完整保留原文要点

原文内容：
%s

请直接输出脚本内容，不要有其他说明。
                """, sourceText);
    }

    private String callLlmApi(String prompt) throws IOException {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", modelId);

        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", "你是一个播客脚本生成助手，擅长将文章改写成生动的双人对谈内容。");
        messages.add(systemMsg);

        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", prompt);
        messages.add(userMsg);

        requestBody.put("messages", messages);
        requestBody.put("temperature", 0.7);

        RequestBody body = RequestBody.create(
                JSON.toJSONString(requestBody),
                MediaType.get("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(API_URL)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("LLM API请求失败: " + response.code());
            }

            String responseBody = response.body() != null ? response.body().string() : "";
            return responseBody;
        }
    }

    private String parseScriptFromResponse(String responseBody) {
        try {
            JSONObject json = JSON.parseObject(responseBody);
            if (json.containsKey("choices")) {
                var choices = json.getJSONArray("choices");
                if (choices != null && !choices.isEmpty()) {
                    JSONObject choice = choices.getJSONObject(0);
                    JSONObject message = choice.getJSONObject("message");
                    if (message != null) {
                        return message.getString("content");
                    }
                }
            }
        } catch (Exception e) {
            log.error("解析LLM响应失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 生成默认脚本格式（当LLM不可用时）
     */
    private String generateDefaultScript(String sourceText) {
        // 将文本分段，每段轮流分配给主播A和主播B
        String[] paragraphs = sourceText.split("\\n+|。");
        StringBuilder script = new StringBuilder();

        script.append("主播A：大家好，欢迎收听今天的节目！\n\n");

        boolean isA = true;
        for (String para : paragraphs) {
            if (para.trim().length() < 5) continue;

            String speaker = isA ? "主播A：" : "主播B：";
            script.append(speaker).append(para.trim()).append("\n\n");
            isA = !isA;
        }

        script.append("主播A：感谢大家的收听，我们下期再见！\n");
        return script.toString();
    }

    @Data
    public static class PodcastScript {
        private String speakerA;
        private String speakerB;
        private List<DialogueLine> dialogues;

        @Data
        public static class DialogueLine {
            private String speaker;
            private String content;
        }
    }
}
