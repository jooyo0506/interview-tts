package com.interview.tts.controller;

import com.interview.tts.dto.ApiResponse;
import com.interview.tts.entity.ClonedVoice;
import com.interview.tts.entity.VoiceSample;
import com.interview.tts.interceptor.UserKeyInterceptor;
import com.interview.tts.service.UserService;
import com.interview.tts.service.VoiceCloneService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/voice-clone")
@RequiredArgsConstructor
public class VoiceCloneController {

    private final VoiceCloneService voiceCloneService;
    private final UserService userService;

    /**
     * 创建声音复刻任务
     */
    @PostMapping("/create")
    public ApiResponse<ClonedVoiceInfo> createClone(
            @RequestBody CreateCloneRequest request,
            HttpServletRequest httpRequest) {
        String userKey = (String) httpRequest.getAttribute(UserKeyInterceptor.USER_KEY_ATTR);
        var user = userService.getUserByKey(userKey);
        if (user == null) {
            return ApiResponse.error("用户不存在");
        }

        if (request.getName() == null || request.getName().isEmpty()) {
            return ApiResponse.error("请输入声音名称");
        }

        if (request.getSampleUrls() == null || request.getSampleUrls().isEmpty()) {
            return ApiResponse.error("请上传至少一个声音样本");
        }

        if (request.getSampleUrls().size() < 3) {
            return ApiResponse.error("请至少上传3个声音样本以获得更好的效果");
        }

        ClonedVoice clonedVoice = voiceCloneService.createCloneTask(
                user.getId(),
                request.getName(),
                request.getSampleUrls()
        );

        return ApiResponse.success(toInfo(clonedVoice));
    }

    /**
     * 获取克隆状态
     */
    @GetMapping("/status/{id}")
    public ApiResponse<ClonedVoiceInfo> getStatus(@PathVariable Long id) {
        ClonedVoice clonedVoice = voiceCloneService.getCloneStatus(id);
        if (clonedVoice == null) {
            return ApiResponse.error("克隆任务不存在");
        }
        return ApiResponse.success(toInfo(clonedVoice));
    }

    /**
     * 获取用户所有克隆声音
     */
    @GetMapping("/list")
    public ApiResponse<List<ClonedVoiceInfo>> listClones(HttpServletRequest httpRequest) {
        String userKey = (String) httpRequest.getAttribute(UserKeyInterceptor.USER_KEY_ATTR);
        var user = userService.getUserByKey(userKey);
        if (user == null) {
            return ApiResponse.success(List.of());
        }

        List<ClonedVoice> clones = voiceCloneService.getUserClonedVoices(user.getId());
        List<ClonedVoiceInfo> list = clones.stream()
                .map(this::toInfo)
                .collect(Collectors.toList());
        return ApiResponse.success(list);
    }

    /**
     * 获取克隆声音的样本
     */
    @GetMapping("/{id}/samples")
    public ApiResponse<List<SampleInfo>> getSamples(@PathVariable Long id) {
        List<VoiceSample> samples = voiceCloneService.getVoiceSamples(id);
        List<SampleInfo> list = samples.stream()
                .map(s -> {
                    SampleInfo info = new SampleInfo();
                    info.setId(s.getId());
                    info.setAudioUrl(s.getAudioUrl());
                    info.setDuration(s.getDuration());
                    info.setStatus(s.getStatus().name());
                    return info;
                })
                .collect(Collectors.toList());
        return ApiResponse.success(list);
    }

    /**
     * 删除克隆声音
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteClone(@PathVariable Long id) {
        voiceCloneService.deleteClonedVoice(id);
        return ApiResponse.success(null);
    }

    private ClonedVoiceInfo toInfo(ClonedVoice clonedVoice) {
        ClonedVoiceInfo info = new ClonedVoiceInfo();
        info.setId(clonedVoice.getId());
        info.setName(clonedVoice.getName());
        info.setVoiceId(clonedVoice.getVoiceId());
        info.setStatus(clonedVoice.getStatus().name());
        info.setSampleCount(clonedVoice.getSampleCount());
        info.setErrorMessage(clonedVoice.getErrorMessage());
        info.setCreateTime(clonedVoice.getCreateTime().toString());
        return info;
    }

    @Data
    public static class CreateCloneRequest {
        private String name;
        private List<String> sampleUrls;
    }

    @Data
    public static class ClonedVoiceInfo {
        private Long id;
        private String name;
        private String voiceId;
        private String status;
        private Integer sampleCount;
        private String errorMessage;
        private String createTime;
    }

    @Data
    public static class SampleInfo {
        private Long id;
        private String audioUrl;
        private Integer duration;
        private String status;
    }
}
