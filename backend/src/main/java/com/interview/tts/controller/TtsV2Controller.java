package com.interview.tts.controller;

import com.interview.tts.annotation.RequirePermission;
import com.interview.tts.dto.ApiResponse;
import com.interview.tts.entity.UserType;
import com.interview.tts.interceptor.UserKeyInterceptor;
import com.interview.tts.service.ttsv2.TtsV2Request;
import com.interview.tts.service.ttsv2.TtsV2Response;
import com.interview.tts.service.ttsv2.TtsV2Service;
import com.interview.tts.service.ttsv2.VoiceInfo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * TTS v2.0 语音合成控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/tts/v2")
@RequiredArgsConstructor
public class TtsV2Controller {

    private final TtsV2Service ttsV2Service;

    /**
     * 合成语音
     */
    @RequirePermission(UserType.USER)
    @PostMapping("/synthesize")
    public ApiResponse<TtsV2Response> synthesize(
            @RequestBody TtsV2Request request,
            HttpServletRequest httpRequest) {
        try {
            String userKey = (String) httpRequest.getAttribute(UserKeyInterceptor.USER_KEY_ATTR);
            request.setUserKey(userKey);

            log.info("收到TTSv2合成请求, voiceType={}, textLength={}, mode={}",
                request.getVoiceType(), request.getText().length(), request.getMode());

            TtsV2Response response = ttsV2Service.synthesize(request);
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("TTSv2合成失败", e);
            return ApiResponse.error("合成失败: " + e.getMessage());
        }
    }

    /**
     * 获取支持的音色列表
     */
    @GetMapping("/voices")
    public ApiResponse<List<VoiceInfo>> getVoices() {
        return ApiResponse.success(VoiceInfo.getSupportedVoices());
    }
}
