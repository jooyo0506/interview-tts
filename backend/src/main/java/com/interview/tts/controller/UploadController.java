package com.interview.tts.controller;

import com.interview.tts.dto.ApiResponse;
import com.interview.tts.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    private final StorageService storageService;

    /**
     * 音频文件上传
     */
    @PostMapping("/audio")
    public ApiResponse<Map<String, Object>> uploadAudio(
            @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ApiResponse.error("文件不能为空");
        }

        // 验证文件类型
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return ApiResponse.error("文件名无效");
        }

        String suffix = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        if (!suffix.matches("\\.(mp3|wav|m4a|ogg|aac)$")) {
            return ApiResponse.error("仅支持 mp3/wav/m4a/ogg/aac 格式");
        }

        // 验证文件大小 (最大50MB)
        if (file.getSize() > 50 * 1024 * 1024) {
            return ApiResponse.error("文件大小不能超过50MB");
        }

        try {
            byte[] audioData = file.getBytes();
            String audioUrl = storageService.uploadAudio(audioData, originalFilename);
            int duration = storageService.estimateDuration(audioData);

            Map<String, Object> result = new HashMap<>();
            result.put("url", audioUrl);
            result.put("duration", duration);
            result.put("fileName", originalFilename);

            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("上传音频失败: {}", e.getMessage(), e);
            return ApiResponse.error("上传失败: " + e.getMessage());
        }
    }

    /**
     * 通用文件上传
     */
    @PostMapping("/file")
    public ApiResponse<Map<String, Object>> uploadFile(
            @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ApiResponse.error("文件不能为空");
        }

        try {
            byte[] data = file.getBytes();
            String fileName = file.getOriginalFilename();
            String url = storageService.uploadAudio(data, fileName); // 复用音频上传方法

            Map<String, Object> result = new HashMap<>();
            result.put("url", url);
            result.put("fileName", fileName);

            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("上传文件失败: {}", e.getMessage(), e);
            return ApiResponse.error("上传失败: " + e.getMessage());
        }
    }
}
