package com.interview.tts.dto;

import lombok.Data;

@Data
public class AudioDetailResponse {
    private Long id;
    private String r2Url;
    private String rawText;
    private String voiceName;
    private Integer duration;
    private String title;
    private Long createTime;
}
