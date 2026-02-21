package com.interview.tts.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final String code;

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }

    public static BusinessException userKeyMissing() {
        return new BusinessException("USER_KEY_MISSING", "用户标识缺失");
    }

    public static BusinessException textTooLong() {
        return new BusinessException("TEXT_TOO_LONG", "文本长度超出限制");
    }

    public static BusinessException ttsFailed() {
        return new BusinessException("TTS_FAILED", "语音合成失败");
    }

    public static BusinessException r2UploadFailed() {
        return new BusinessException("R2_UPLOAD_FAILED", "音频上传失败");
    }

    public static BusinessException rateLimited() {
        return new BusinessException("RATE_LIMITED", "今日生成次数已达上限");
    }
}
