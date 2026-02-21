package com.interview.tts.controller;

import com.interview.tts.dto.ApiResponse;
import com.interview.tts.dto.AudioGenerateRequest;
import com.interview.tts.dto.AudioGenerateResponse;
import com.interview.tts.interceptor.UserKeyInterceptor;
import com.interview.tts.service.AudioService;
import com.interview.tts.service.DouyinTtsService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tts")
@RequiredArgsConstructor
public class TtsController {

    private final DouyinTtsService ttsService;
    private final AudioService audioService;

    @PostMapping("/generate")
    public ApiResponse<AudioGenerateResponse> generate(
            @RequestBody AudioGenerateRequest request,
            HttpServletRequest httpRequest) {
        String userKey = (String) httpRequest.getAttribute(UserKeyInterceptor.USER_KEY_ATTR);
        AudioGenerateResponse response = audioService.generate(userKey, request.getRawText(), request.getVoiceName());
        return ApiResponse.success(response);
    }

    @GetMapping("/voices")
    public ApiResponse<List<VoiceResponse>> listVoices() {
        List<DouyinTtsService.VoiceInfo> voices = ttsService.listChineseVoices();
        List<VoiceResponse> response = voices.stream()
                .map(v -> new VoiceResponse(v.getName(), v.getLocale(), v.getGender(), v.getShortName()))
                .collect(Collectors.toList());
        return ApiResponse.success(response);
    }

    public static class VoiceResponse {
        private String name;
        private String locale;
        private String gender;
        private String shortName;

        public VoiceResponse(String name, String locale, String gender, String shortName) {
            this.name = name;
            this.locale = locale;
            this.gender = gender;
            this.shortName = shortName;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getLocale() { return locale; }
        public void setLocale(String locale) { this.locale = locale; }
        public String getGender() { return gender; }
        public void setGender(String gender) { this.gender = gender; }
        public String getShortName() { return shortName; }
        public void setShortName(String shortName) { this.shortName = shortName; }
    }
}
