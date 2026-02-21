package com.interview.tts.dto;

import lombok.Data;

@Data
public class PodcastGenerateRequest {
    private String title;
    private String sourceText;
    private String voiceA = "BV001_streaming";
    private String voiceB = "BV002_streaming";
}
