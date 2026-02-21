package com.interview.tts.service.ttsv2;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * TTS v2.0 合成响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TtsV2Response {

    /**
     * 音频URL (R2存储)
     */
    private String audioUrl;

    /**
     * 音频时长(秒)
     */
    private Integer duration;

    /**
     * 字幕列表
     */
    private List<Subtitle> subtitles;

    /**
     * 字幕
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Subtitle {
        /**
         * 句子文本
         */
        private String text;

        /**
         * 开始时间(毫秒)
         */
        private Long startTime;

        /**
         * 结束时间(毫秒)
         */
        private Long endTime;
    }
}
