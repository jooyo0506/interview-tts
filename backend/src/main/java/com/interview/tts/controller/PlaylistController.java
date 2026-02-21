package com.interview.tts.controller;

import com.interview.tts.dto.ApiResponse;
import com.interview.tts.entity.Playlist;
import com.interview.tts.interceptor.UserKeyInterceptor;
import com.interview.tts.service.PlaylistService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/playlist")
@RequiredArgsConstructor
public class PlaylistController {

    private final PlaylistService playlistService;

    // 获取播放列表
    @GetMapping("/list")
    public ApiResponse<List<PlaylistInfo>> list(HttpServletRequest httpRequest) {
        String userKey = (String) httpRequest.getAttribute(UserKeyInterceptor.USER_KEY_ATTR);
        List<Playlist> playlists = playlistService.getUserPlaylists(userKey);
        List<PlaylistInfo> list = playlists.stream()
                .map(this::toInfo)
                .collect(Collectors.toList());
        return ApiResponse.success(list);
    }

    // 创建播放列表
    @PostMapping("/create")
    public ApiResponse<PlaylistInfo> create(
            @RequestBody CreateRequest request,
            HttpServletRequest httpRequest) {
        String userKey = (String) httpRequest.getAttribute(UserKeyInterceptor.USER_KEY_ATTR);
        Playlist playlist = playlistService.createPlaylist(userKey, request.getName(), request.getDescription());
        return ApiResponse.success(toInfo(playlist));
    }

    // 删除播放列表
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        String userKey = (String) httpRequest.getAttribute(UserKeyInterceptor.USER_KEY_ATTR);
        playlistService.deletePlaylist(userKey, id);
        return ApiResponse.success(null);
    }

    // 重命名播放列表
    @PutMapping("/{id}/rename")
    public ApiResponse<Void> rename(
            @PathVariable Long id,
            @RequestBody RenameRequest request,
            HttpServletRequest httpRequest) {
        String userKey = (String) httpRequest.getAttribute(UserKeyInterceptor.USER_KEY_ATTR);
        playlistService.renamePlaylist(userKey, id, request.getName());
        return ApiResponse.success(null);
    }

    // 获取播放列表音频
    @GetMapping("/{id}/audios")
    public ApiResponse<List<AudioInfo>> getAudios(@PathVariable Long id) {
        List<com.interview.tts.dto.AudioListItem> audios = playlistService.getPlaylistAudios(id);
        List<AudioInfo> list = audios.stream()
                .map(a -> {
                    AudioInfo info = new AudioInfo();
                    info.setId(a.getId());
                    info.setR2Url(a.getR2Url());
                    info.setRawText(a.getRawText());
                    info.setVoiceName(a.getVoiceName());
                    info.setDuration(a.getDuration());
                    info.setCreateTime(a.getCreateTime());
                    info.setTitle(a.getTitle());
                    return info;
                })
                .collect(Collectors.toList());
        return ApiResponse.success(list);
    }

    // 添加音频到播放列表
    @PostMapping("/{id}/audio")
    public ApiResponse<Void> addAudio(
            @PathVariable Long id,
            @RequestBody AudioRequest request,
            HttpServletRequest httpRequest) {
        String userKey = (String) httpRequest.getAttribute(UserKeyInterceptor.USER_KEY_ATTR);
        playlistService.addAudioToPlaylist(userKey, id, request.getAudioFileId());
        return ApiResponse.success(null);
    }

    // 从播放列表移除音频
    @DeleteMapping("/{id}/audio/{audioId}")
    public ApiResponse<Void> removeAudio(
            @PathVariable Long id,
            @PathVariable Long audioId,
            HttpServletRequest httpRequest) {
        String userKey = (String) httpRequest.getAttribute(UserKeyInterceptor.USER_KEY_ATTR);
        playlistService.removeAudioFromPlaylist(userKey, id, audioId);
        return ApiResponse.success(null);
    }

    private PlaylistInfo toInfo(Playlist p) {
        PlaylistInfo info = new PlaylistInfo();
        info.setId(p.getId());
        info.setName(p.getName());
        info.setDescription(p.getDescription());
        info.setCoverUrl(p.getCoverUrl());
        info.setAudioCount(p.getAudioCount());
        info.setTotalDuration(p.getTotalDuration());
        info.setCreateTime(p.getCreateTime().toString());
        return info;
    }

    @Data
    public static class CreateRequest {
        private String name;
        private String description;
    }

    @Data
    public static class RenameRequest {
        private String name;
    }

    @Data
    public static class AudioRequest {
        private Long audioFileId;
    }

    @Data
    public static class PlaylistInfo {
        private Long id;
        private String name;
        private String description;
        private String coverUrl;
        private Integer audioCount;
        private Integer totalDuration;
        private String createTime;
    }

    @Data
    public static class AudioInfo {
        private Long id;
        private String r2Url;
        private String rawText;
        private String voiceName;
        private Integer duration;
        private String createTime;
        private String title;
    }
}
