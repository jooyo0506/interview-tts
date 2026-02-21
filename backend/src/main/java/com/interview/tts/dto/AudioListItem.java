package com.interview.tts.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AudioListItem {
    private Long id;
    private String r2Url;
    private String rawText;
    private String voiceName;
    private Integer duration;
    private String createTime;
    private String title;
}
