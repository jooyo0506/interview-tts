package com.interview.tts.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PodcastGenerateResponse {
    private Long podcastId;
    private String status;
    private String audioUrl;
    private Integer duration;
    private String scriptContent;
    private String message;
}
