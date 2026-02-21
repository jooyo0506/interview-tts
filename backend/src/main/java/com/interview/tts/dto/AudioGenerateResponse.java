package com.interview.tts.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AudioGenerateResponse {
    private Long audioId;
    private String r2Url;
    private String audioUrl; // 临时音频URL（降级时使用）
    private Integer duration;
    private String taskId; // 长文本异步任务ID

    // 短文本构造（3参数）
    public AudioGenerateResponse(Long audioId, String r2Url, Integer duration) {
        this.audioId = audioId;
        this.r2Url = r2Url;
        this.duration = duration;
    }

    // 长文本异步构造（4参数）- audioId, r2Url, duration, taskId
    public AudioGenerateResponse(Long audioId, String r2Url, Integer duration, String taskId) {
        this.audioId = audioId;
        this.r2Url = r2Url;
        this.duration = duration;
        this.taskId = taskId;
    }
}
