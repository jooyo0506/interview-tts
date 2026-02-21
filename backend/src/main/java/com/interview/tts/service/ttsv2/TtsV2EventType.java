package com.interview.tts.service.ttsv2;

import lombok.Getter;

/**
 * 事件类型枚举 (来自官方示例)
 */
@Getter
public enum TtsV2EventType {
    // Default event
    NONE(0),

    // Upstream Connection events (1-49)
    START_CONNECTION(1),
    FINISH_CONNECTION(2),

    // Downstream Connection events (50-99)
    CONNECTION_STARTED(50),
    CONNECTION_FAILED(51),
    CONNECTION_FINISHED(52),

    // Upstream Session events (100-149)
    START_SESSION(100),
    CANCEL_SESSION(101),
    FINISH_SESSION(102),

    // Downstream Session events (150-199)
    SESSION_STARTED(150),
    SESSION_CANCELED(151),
    SESSION_FINISHED(152),
    SESSION_FAILED(153),

    // Upstream General events (200-249)
    TASK_REQUEST(200),

    // Downstream TTS events (350-399)
    TTS_SENTENCE_START(350),
    TTS_SENTENCE_END(351),
    TTS_RESPONSE(352),
    TTS_ENDED(359);

    private final int value;

    TtsV2EventType(int value) {
        this.value = value;
    }

    public static TtsV2EventType fromValue(int value) {
        for (TtsV2EventType type : values()) {
            if (type.value == value) {
                return type;
            }
        }
        return NONE;
    }
}
