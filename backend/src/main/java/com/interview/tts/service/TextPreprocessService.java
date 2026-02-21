package com.interview.tts.service;

import cn.hutool.crypto.digest.DigestUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class TextPreprocessService {

    @Value("${doubao.api-key:}")
    private String doubaoApiKey;

    @Value("${doubao.model-id:doubao-seed-2-0-lite-260215}")
    private String doubaoModelId;

    private static final String API_ENDPOINT = "https://ark.cn-beijing.volces.com/api/v3/responses";

    private static final String DEFAULT_PROMPT = """
            你是一位专业的AI语音合成文本优化师，专门把面试八股文转化为适合豆包TTS朗读的SSML格式，禁止改变原文核心技术内容。
            请严格按照以下规则处理输入的文本：
            1. 考点标记：超高频面试必考点前加<break time="2000ms"/>，并说「核心考点」；面试易踩坑内容前加<break time="1500ms"/>，并说「易错点提醒」；逻辑转折前加<break time="1000ms"/>，并说「注意逻辑」。
            2. 代码处理：代码块前加<break time="3000ms"/>，并说「准备听代码逻辑」，代码块本身无需修改，直接保留。
            3. 口语化优化：把生硬书面语转化为专业且口语化的表达，适当加入「这个考点超高频，面试必背」「这里一定要记牢」等记忆强化提示语。
            4. 停顿规则：每个独立段落结束后加<break time="500ms"/>，标题结束后加<break time="1000ms"/>。
            5. 输出格式要求：直接输出原始SSML标签文本（如<speak>、</speak>、<break time="500ms"/>等），不要使用任何转义字符（如\u003c、\u003e、\"等）。只输出SSML内容，不要输出任何解释、备注或其他内容。
            现在请处理以下面试八股文文本：
            """;

    public String preprocess(String rawText) {
        // 直接返回原始文本，不需要任何处理
        return rawText;
    }

    public String getPromptHash() {
        // 简单返回默认prompt的MD5
        return DigestUtil.md5Hex(DEFAULT_PROMPT);
    }

    private String preprocessWithDoubao(String rawText) throws Exception {
        String prompt = DEFAULT_PROMPT + rawText;

        // 使用 v3/responses API (新格式)
        String requestBody = String.format("""
            {
                "model": "%s",
                "input": [
                    {
                        "role": "user",
                        "content": [
                            {
                                "type": "input_text",
                                "text": "%s"
                            }
                        ]
                    }
                ]
            }
            """, doubaoModelId, prompt.replace("\"", "\\\"").replace("\n", "\\n"));

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_ENDPOINT))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + doubaoApiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        log.debug("豆包响应: {}", response.body());

        // 解析 v3/responses 响应格式
        String body = response.body();
        String content = null;

        // 优先尝试从 summary 中提取 (reasoning类型的summary.text)
        int summaryStart = body.indexOf("\"summary\":[");
        if (summaryStart != -1) {
            int textStart = body.indexOf("\"text\":\"", summaryStart);
            if (textStart != -1) {
                textStart += 8;
                int textEnd = body.indexOf("\"", textStart);
                if (textEnd != -1) {
                    content = body.substring(textStart, textEnd);
                    content = content.replace("\\n", "\n").replace("\\\"", "\"");
                    log.info("从summary中提取文本成功");
                }
            }
        }

        // 如果summary中没有，尝试从message中提取
        if (content == null || content.isEmpty()) {
            int messageStart = body.indexOf("\"type\":\"message\"");
            if (messageStart != -1) {
                int textSearchStart = body.indexOf("\"text\":\"", messageStart);
                if (textSearchStart != -1) {
                    textSearchStart += 8;
                    int textEnd = body.indexOf("\"", textSearchStart);
                    if (textEnd != -1) {
                        content = body.substring(textSearchStart, textEnd);
                        content = content.replace("\\n", "\n").replace("\\\"", "\"");
                        log.info("从message中提取文本成功");
                    }
                }
            }
        }

        if (content == null || content.isEmpty()) {
            log.warn("响应中无法提取文本: {}", body.substring(0, Math.min(500, body.length())));
            return null;
        }

        // 清理转义字符
        content = cleanEscapeChars(content);

        // 确保有 <speak> 标签
        if (!content.contains("<speak>")) {
            content = "<speak>" + content;
        }
        if (!content.contains("</speak>")) {
            content = content + "</speak>";
        }

        return content;
    }

    /**
     * 清理转义字符
     */
    private String cleanEscapeChars(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        // 替换常见的转义序列
        text = text.replace("\\n", "\n")
                   .replace("\\\"", "\"")
                   .replace("\\/", "/")
                   .replace("\\\\", "\\")
                   // 替换Unicode转义字符 \u003c -> <, \u003e -> >
                   .replace("\\u003c", "<")
                   .replace("\\u003e", ">")
                   .replace("\\u0026", "&")
                   .replace("\\u0027", "'")
                   .replace("\\u003d", "=");
        return text;
    }

    private String preprocessLocal(String rawText) {
        log.info("使用本地规则预处理");
        StringBuilder sb = new StringBuilder();
        sb.append("<speak>");

        // 转义XML特殊字符
        String escaped = escapeXml(rawText);

        // 按段落分割处理
        String[] paragraphs = escaped.split("\n");
        for (int i = 0; i < paragraphs.length; i++) {
            String para = paragraphs[i].trim();
            if (para.isEmpty()) {
                continue;
            }

            // 检测标题 (短行或以#开头)
            if (para.length() < 50 || para.startsWith("#")) {
                sb.append("<break time=\"1000ms\"/>");
            }

            // 检测代码块
            if (para.contains("```") || para.contains("public ") ||
                para.contains("private ") || para.contains("class ")) {
                sb.append("<break time=\"3000ms\"/>");
                sb.append("准备听代码逻辑");
            }

            sb.append(para);
            sb.append("<break time=\"500ms\"/>");
        }

        sb.append("</speak>");
        return sb.toString();
    }

    private String escapeXml(String text) {
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&apos;");
    }
}
