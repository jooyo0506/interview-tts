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
     * 根据音色列表.md文档
     */
    public static final List<VoiceInfo> EMOTION_VOICES = Arrays.asList(
        // ===== 通用场景 =====
        VoiceInfo.builder()
            .id("zh_female_vv_uranus_bigtts")
            .name("vivi 2.0")
            .language("zh-CN")
            .gender("女")
            .description("通用女声")
            .supportsEmotion(true)
            .supportsContext(true)
            .build(),
        VoiceInfo.builder()
            .id("zh_female_xiaohe_uranus_bigtts")
            .name("小何")
            .language("zh-CN")
            .gender("女")
            .description("温柔女声")
            .supportsEmotion(true)
            .supportsContext(true)
            .build(),
        VoiceInfo.builder()
            .id("zh_male_m191_uranus_bigtts")
            .name("云舟")
            .language("zh-CN")
            .gender("男")
            .description("成熟男声")
            .supportsEmotion(true)
            .supportsContext(true)
            .build(),
        VoiceInfo.builder()
            .id("zh_male_taocheng_uranus_bigtts")
            .name("小天")
            .language("zh-CN")
            .gender("男")
            .description("青年男声")
            .supportsEmotion(true)
            .supportsContext(true)
            .build(),
        // ===== 视频配音 =====
        VoiceInfo.builder()
            .id("zh_male_dayi_saturn_bigtts")
            .name("大壹")
            .language("zh-CN")
            .gender("男")
            .description("视频配音")
            .supportsEmotion(true)
            .supportsContext(true)
            .build(),
        VoiceInfo.builder()
            .id("zh_female_meilinvyou_saturn_bigtts")
            .name("魅力女友")
            .language("zh-CN")
            .gender("女")
            .description("视频配音")
            .supportsEmotion(true)
            .supportsContext(true)
            .build(),
        // ===== 角色扮演 =====
        VoiceInfo.builder()
            .id("saturn_zh_female_cancan_tob")
            .name("知性灿灿")
            .language("zh-CN")
            .gender("女")
            .description("角色扮演")
            .supportsEmotion(true)
            .supportsContext(true)
            .build(),
        VoiceInfo.builder()
            .id("saturn_zh_female_keainvsheng_tob")
            .name("可爱女生")
            .language("zh-CN")
            .gender("女")
            .description("角色扮演")
            .supportsEmotion(true)
            .supportsContext(true)
            .build(),
        VoiceInfo.builder()
            .id("saturn_zh_female_tiaopigongzhu_tob")
            .name("调皮公主")
            .language("zh-CN")
            .gender("女")
            .description("角色扮演")
            .supportsEmotion(true)
            .supportsContext(true)
            .build(),
        VoiceInfo.builder()
            .id("saturn_zh_male_shuanglangshaonian_tob")
            .name("爽朗少年")
            .language("zh-CN")
            .gender("男")
            .description("角色扮演")
            .supportsEmotion(true)
            .supportsContext(true)
            .build(),
        VoiceInfo.builder()
            .id("saturn_zh_male_tiancaitongzhuo_tob")
            .name("天才同桌")
            .language("zh-CN")
            .gender("男")
            .description("角色扮演")
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
