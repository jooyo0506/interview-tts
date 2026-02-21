package com.interview.tts.service.ttsv2;

import lombok.Getter;

/**
 * TTS v2.0 双向流式API事件类型
 */
@Getter
public enum TtsV2EventType {
    // 上行连接事件 (1-49)
    START_CONNECTION(1, "建立连接"),
    FINISH_CONNECTION(2, "关闭连接"),

    // 下行连接事件 (50-99)
    CONNECTION_STARTED(50, "连接已建立"),
    CONNECTION_FAILED(51, "连接失败"),
    CONNECTION_FINISHED(52, "连接已关闭"),

    // 上行会话事件 (100-149)
    START_SESSION(100, "开始会话"),
    CANCEL_SESSION(101, "取消会话"),
    FINISH_SESSION(102, "结束会话"),

    // 下行会话事件 (150-199)
    SESSION_STARTED(150, "会话已开始"),
    SESSION_CANCELED(151, "会话已取消"),
    SESSION_FINISHED(152, "会话已结束"),
    SESSION_FAILED(153, "会话失败"),

    // 上行数据事件 (200-249)
    TASK_REQUEST(200, "发送文本"),
    UPDATE_CONFIG(201, "更新配置"),

    // 下行TTS事件 (350-399)
    TTS_SENTENCE_START(350, "句子开始"),
    TTS_SENTENCE_END(351, "句子结束"),
    TTS_RESPONSE(352, "音频数据"),
    TTS_ENDED(359, "TTS结束"),

    // 字幕事件 (650-699)
    SOURCE_SUBTITLE_START(650, "原文字幕开始"),
    SOURCE_SUBTITLE_RESPONSE(651, "原文字幕内容"),
    SOURCE_SUBTITLE_END(652, "原文字幕结束");

    private final int value;
    private final String description;

    TtsV2EventType(int value, String description) {
        this.value = value;
        this.description = description;
    }

    public static TtsV2EventType fromValue(int value) {
        for (TtsV2EventType type : values()) {
            if (type.value == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown TtsV2EventType value: " + value);
    }
}
