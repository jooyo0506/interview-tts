package com.interview.tts.controller;

import com.interview.tts.dto.ApiResponse;
import com.interview.tts.entity.Translation;
import com.interview.tts.interceptor.UserKeyInterceptor;
import com.interview.tts.service.TranslateService;
import com.interview.tts.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/translate")
@RequiredArgsConstructor
public class TranslateController {

    private final TranslateService translateService;
    private final UserService userService;

    /**
     * 文本翻译
     */
    @PostMapping("/text")
    public ApiResponse<TranslationInfo> translateText(
            @RequestBody TranslateRequest request,
            HttpServletRequest httpRequest) {
        String userKey = (String) httpRequest.getAttribute(UserKeyInterceptor.USER_KEY_ATTR);
        var user = userService.getUserByKey(userKey);
        if (user == null) {
            return ApiResponse.error("用户不存在");
        }

        Translation translation = translateService.translateText(
                user.getId(),
                request.getText(),
                request.getSourceLang(),
                request.getTargetLang()
        );

        return ApiResponse.success(toInfo(translation));
    }

    /**
     * 语音翻译
     */
    @PostMapping("/speech")
    public ApiResponse<TranslationInfo> translateSpeech(
            @RequestBody TranslateRequest request,
            HttpServletRequest httpRequest) {
        String userKey = (String) httpRequest.getAttribute(UserKeyInterceptor.USER_KEY_ATTR);
        var user = userService.getUserByKey(userKey);
        if (user == null) {
            return ApiResponse.error("用户不存在");
        }

        Translation translation = translateService.translateSpeech(
                user.getId(),
                request.getAudioUrl(),
                request.getSourceLang(),
                request.getTargetLang(),
                request.getVoiceName()
        );

        return ApiResponse.success(toInfo(translation));
    }

    /**
     * 获取翻译历史
     */
    @GetMapping("/list")
    public ApiResponse<List<TranslationInfo>> listTranslations(HttpServletRequest httpRequest) {
        String userKey = (String) httpRequest.getAttribute(UserKeyInterceptor.USER_KEY_ATTR);
        var user = userService.getUserByKey(userKey);
        if (user == null) {
            return ApiResponse.success(List.of());
        }

        List<Translation> translations = translateService.getUserTranslations(user.getId());
        List<TranslationInfo> list = translations.stream()
                .map(this::toInfo)
                .collect(Collectors.toList());
        return ApiResponse.success(list);
    }

    /**
     * 获取翻译详情
     */
    @GetMapping("/{id}")
    public ApiResponse<TranslationInfo> getTranslation(@PathVariable Long id) {
        Translation translation = translateService.getTranslation(id);
        if (translation == null) {
            return ApiResponse.error("翻译记录不存在");
        }
        return ApiResponse.success(toInfo(translation));
    }

    private TranslationInfo toInfo(Translation translation) {
        TranslationInfo info = new TranslationInfo();
        info.setId(translation.getId());
        info.setSourceText(translation.getSourceText());
        info.setTranslatedText(translation.getTranslatedText());
        info.setSourceLang(translation.getSourceLang());
        info.setTargetLang(translation.getTargetLang());
        info.setSourceAudioUrl(translation.getSourceAudioUrl());
        info.setTranslatedAudioUrl(translation.getTranslatedAudioUrl());
        info.setTranslatedAudioDuration(translation.getTranslatedAudioDuration());
        info.setType(translation.getTranslationType().name());
        info.setCreateTime(translation.getCreateTime().toString());
        return info;
    }

    @Data
    public static class TranslateRequest {
        private String text;
        private String audioUrl;
        private String sourceLang = "zh";
        private String targetLang = "en";
        private String voiceName;
        private String type;
    }

    @Data
    public static class TranslationInfo {
        private Long id;
        private String sourceText;
        private String translatedText;
        private String sourceLang;
        private String targetLang;
        private String sourceAudioUrl;
        private String translatedAudioUrl;
        private Integer translatedAudioDuration;
        private String type;
        private String createTime;
    }
}
