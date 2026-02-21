package com.interview.tts.controller;

import com.interview.tts.dto.*;
import com.interview.tts.interceptor.UserKeyInterceptor;
import com.interview.tts.service.AudioService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audio")
@RequiredArgsConstructor
public class AudioController {

    private final AudioService audioService;

    @PostMapping("/generate")
    public ApiResponse<AudioGenerateResponse> generate(
            @RequestBody AudioGenerateRequest request,
            HttpServletRequest httpRequest) {
        String userKey = (String) httpRequest.getAttribute(UserKeyInterceptor.USER_KEY_ATTR);
        AudioGenerateResponse response = audioService.generate(userKey, request.getRawText(), request.getVoiceName());
        return ApiResponse.success(response);
    }

    /**
     * 长文本异步生成 - 创建任务
     * @param useEmotion 是否使用情感预测版（true=情感预测版，false=普通版）
     */
    @PostMapping("/generate-long")
    public ApiResponse<AudioGenerateResponse> generateLong(
            @RequestBody AudioGenerateRequest request,
            HttpServletRequest httpRequest) {
        String userKey = (String) httpRequest.getAttribute(UserKeyInterceptor.USER_KEY_ATTR);
        boolean useEmotion = request.getUseEmotion() != null && request.getUseEmotion();
        AudioGenerateResponse response = audioService.generateLongText(userKey, request.getRawText(), request.getVoiceName(), useEmotion);
        return ApiResponse.success(response);
    }

    /**
     * 长文本异步任务 - 查询状态
     */
    @GetMapping("/task-status")
    public ApiResponse<AudioGenerateResponse> queryTaskStatus(
            @RequestParam Long audioFileId,
            @RequestParam String taskId,
            @RequestParam(required = false) Boolean useEmotion,
            HttpServletRequest httpRequest) {
        String userKey = (String) httpRequest.getAttribute(UserKeyInterceptor.USER_KEY_ATTR);
        boolean emotion = useEmotion != null && useEmotion;
        AudioGenerateResponse response = audioService.queryLongTextTask(userKey, audioFileId, taskId, emotion);
        return ApiResponse.success(response);
    }

    @GetMapping("/my-list")
    public ApiResponse<List<AudioListItem>> myList(HttpServletRequest httpRequest) {
        String userKey = (String) httpRequest.getAttribute(UserKeyInterceptor.USER_KEY_ATTR);
        List<AudioListItem> list = audioService.getMyList(userKey);
        return ApiResponse.success(list);
    }

    @PostMapping("/collect")
    public ApiResponse<CollectResponse> collect(
            @RequestBody CollectRequest request,
            HttpServletRequest httpRequest) {
        String userKey = (String) httpRequest.getAttribute(UserKeyInterceptor.USER_KEY_ATTR);
        boolean isCollected = audioService.toggleCollect(userKey, request.getAudioId());
        return ApiResponse.success(new CollectResponse(isCollected));
    }

    @GetMapping("/collect-list")
    public ApiResponse<List<AudioListItem>> collectList(HttpServletRequest httpRequest) {
        String userKey = (String) httpRequest.getAttribute(UserKeyInterceptor.USER_KEY_ATTR);
        List<AudioListItem> list = audioService.getCollectList(userKey);
        return ApiResponse.success(list);
    }

    /**
     * 获取音频详情（包含完整文本）
     */
    @GetMapping("/detail")
    public ApiResponse<AudioDetailResponse> getDetail(
            @RequestParam Long id,
            HttpServletRequest httpRequest) {
        String userKey = (String) httpRequest.getAttribute(UserKeyInterceptor.USER_KEY_ATTR);
        AudioDetailResponse detail = audioService.getAudioDetail(userKey, id);
        return ApiResponse.success(detail);
    }

    // ========== 管理功能 ==========

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteAudio(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        String userKey = (String) httpRequest.getAttribute(UserKeyInterceptor.USER_KEY_ATTR);
        audioService.deleteAudio(userKey, id);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/batch")
    public ApiResponse<Void> deleteBatch(
            @RequestBody DeleteBatchRequest request,
            HttpServletRequest httpRequest) {
        String userKey = (String) httpRequest.getAttribute(UserKeyInterceptor.USER_KEY_ATTR);
        audioService.deleteBatch(userKey, request.getIds());
        return ApiResponse.success(null);
    }

    @PutMapping("/{id}/rename")
    public ApiResponse<Void> renameAudio(
            @PathVariable Long id,
            @RequestBody RenameRequest request,
            HttpServletRequest httpRequest) {
        String userKey = (String) httpRequest.getAttribute(UserKeyInterceptor.USER_KEY_ATTR);
        audioService.renameAudio(userKey, id, request.getName());
        return ApiResponse.success(null);
    }

    @DeleteMapping("/collect/{id}")
    public ApiResponse<Void> deleteCollect(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        String userKey = (String) httpRequest.getAttribute(UserKeyInterceptor.USER_KEY_ATTR);
        audioService.deleteCollect(userKey, id);
        return ApiResponse.success(null);
    }

    @Data
    public static class DeleteBatchRequest {
        private List<Long> ids;
    }

    @Data
    public static class RenameRequest {
        private String name;
    }
}
