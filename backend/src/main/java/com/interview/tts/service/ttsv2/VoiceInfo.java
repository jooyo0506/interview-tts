package com.interview.tts.service.ttsv2;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.List;

/**
 * 音色信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceInfo {

    private String id;
    private String name;
    private String language;
    private String gender;
    private String description;
    private boolean supportsEmotion;  // 是否支持情感/指令
    private boolean supportsContext;    // 是否支持上文引用

    /**
     * TTSv2.0 支持情感/指令的音色列表
     */
    public static final List<VoiceInfo> EMOTION_VOICES = Arrays.asList(
        VoiceInfo.builder()
            .id("zh_female_cancan_mars_bigtts")
            .name("灿灿")
            .language("zh-CN")
            .gender("女")
            .description("可爱女生")
            .supportsEmotion(true)
            .supportsContext(true)
            .build(),
        VoiceInfo.builder()
            .id("zh_female_xiaoyuan_mars_bigtts")
            .name("调皮公主")
            .language("zh-CN")
            .gender("女")
            .description("活泼可爱")
            .supportsEmotion(true)
            .supportsContext(true)
            .build(),
        VoiceInfo.builder()
            .id("zh_male_shengyang_mars_bigtts")
            .name("爽朗少年")
            .language("zh-CN")
            .gender("男")
            .description("青春活力")
            .supportsEmotion(true)
            .supportsContext(true)
            .build(),
        VoiceInfo.builder()
            .id("zh_male_tiancai_mars_bigtts")
            .name("天才同桌")
            .language("zh-CN")
            .gender("男")
            .description("聪明少年")
            .supportsEmotion(true)
            .supportsContext(true)
            .build()
    );

    /**
     * 获取支持的音色列表
     */
    public static List<VoiceInfo> getSupportedVoices() {
        return EMOTION_VOICES;
    }

    /**
     * 检查音色是否支持情感/指令
     */
    public static boolean supportsEmotion(String voiceId) {
        return EMOTION_VOICES.stream()
            .anyMatch(v -> v.getId().equals(voiceId));
    }
}
