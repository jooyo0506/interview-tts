package com.interview.tts.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * TTS v2.0 双向流式语音合成配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "volcengine.tts.v2")
public class TtsV2Properties {

    /**
     * WebSocket连接地址
     */
    private String wsUrl = "wss://openspeech.bytedance.com/api/v3/tts/bidirection";

    /**
     * 资源ID - TTS2.0字符版
     */
    private String resourceId = "seed-tts-2.0";

    /**
     * 是否启用TTSv2
     */
    private boolean enabled = true;

    /**
     * APP ID (从主配置继承)
     */
    @Value("${volcengine.tts.app-id:}")
    private String appId;

    /**
     * Access Token (从主配置继承)
     */
    @Value("${volcengine.tts.access-token:}")
    private String accessToken;
}
