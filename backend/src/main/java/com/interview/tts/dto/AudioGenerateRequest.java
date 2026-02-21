package com.interview.tts.dto;

import lombok.Data;

@Data
public class AudioGenerateRequest {
    private String rawText;
    private String voiceName;
    private Boolean useEmotion; // 是否使用情感预测版
}
