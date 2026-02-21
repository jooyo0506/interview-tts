package com.interview.tts.service;

import com.interview.tts.dto.AudioListItem;
import com.interview.tts.entity.AudioFile;
import com.interview.tts.entity.Playlist;
import com.interview.tts.entity.PlaylistAudio;
import com.interview.tts.entity.SysUser;
import com.interview.tts.entity.TextRecord;
import com.interview.tts.repository.AudioFileRepository;
import com.interview.tts.repository.PlaylistAudioRepository;
import com.interview.tts.repository.PlaylistRepository;
import com.interview.tts.repository.TextRecordRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlaylistService {

    private final PlaylistRepository playlistRepository;
    private final PlaylistAudioRepository playlistAudioRepository;
    private final AudioFileRepository audioFileRepository;
    private final TextRecordRepository textRecordRepository;
    private final UserService userService;

    // 获取用户播放列表
    public List<Playlist> getUserPlaylists(String userKey) {
        SysUser user = userService.getUserByKey(userKey);
        if (user == null) {
            return List.of();
        }
        return playlistRepository.findByUserIdOrderByCreateTimeDesc(user.getId());
    }

    // 创建播放列表
    @Transactional
    public Playlist createPlaylist(String userKey, String name, String description) {
        SysUser user = userService.getUserByKey(userKey);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        Playlist playlist = new Playlist();
        playlist.setUserId(user.getId());
        playlist.setName(name);
        playlist.setDescription(description);
        playlist.setCreateTime(LocalDateTime.now());
        return playlistRepository.save(playlist);
    }

    // 删除播放列表
    @Transactional
    public void deletePlaylist(String userKey, Long playlistId) {
        SysUser user = userService.getUserByKey(userKey);
        if (user == null) {
            return;
        }

        Playlist playlist = playlistRepository.findById(playlistId).orElse(null);
        if (playlist == null || !playlist.getUserId().equals(user.getId())) {
            throw new RuntimeException("无权限删除");
        }

        // 删除播放列表中的音频关联
        playlistAudioRepository.deleteByPlaylistId(playlistId);
        playlistRepository.deleteById(playlistId);
    }

    // 重命名播放列表
    @Transactional
    public void renamePlaylist(String userKey, Long playlistId, String name) {
        SysUser user = userService.getUserByKey(userKey);
        if (user == null) {
            return;
        }

        Playlist playlist = playlistRepository.findById(playlistId).orElse(null);
        if (playlist == null || !playlist.getUserId().equals(user.getId())) {
            throw new RuntimeException("无权限修改");
        }

        playlist.setName(name);
        playlist.setUpdateTime(LocalDateTime.now());
        playlistRepository.save(playlist);
    }

    // 添加音频到播放列表
    @Transactional
    public void addAudioToPlaylist(String userKey, Long playlistId, Long audioFileId) {
        SysUser user = userService.getUserByKey(userKey);
        if (user == null) {
            return;
        }

        Playlist playlist = playlistRepository.findById(playlistId).orElse(null);
        if (playlist == null || !playlist.getUserId().equals(user.getId())) {
            throw new RuntimeException("无权限操作");
        }

        // 检查是否已存在
        if (playlistAudioRepository.existsByPlaylistIdAndAudioFileId(playlistId, audioFileId)) {
            return;
        }

        // 添加关联
        PlaylistAudio pa = new PlaylistAudio();
        pa.setPlaylistId(playlistId);
        pa.setAudioFileId(audioFileId);
        pa.setAddTime(LocalDateTime.now());
        playlistAudioRepository.save(pa);

        // 更新播放列表统计
        updatePlaylistStats(playlistId);
    }

    // 从播放列表移除音频
    @Transactional
    public void removeAudioFromPlaylist(String userKey, Long playlistId, Long audioFileId) {
        SysUser user = userService.getUserByKey(userKey);
        if (user == null) {
            return;
        }

        Playlist playlist = playlistRepository.findById(playlistId).orElse(null);
        if (playlist == null || !playlist.getUserId().equals(user.getId())) {
            throw new RuntimeException("无权限操作");
        }

        playlistAudioRepository.deleteByPlaylistIdAndAudioFileId(playlistId, audioFileId);

        // 更新播放列表统计
        updatePlaylistStats(playlistId);
    }

    // 获取播放列表中的音频
    public List<AudioListItem> getPlaylistAudios(Long playlistId) {
        List<PlaylistAudio> pas = playlistAudioRepository.findByPlaylistIdOrderBySortOrderAsc(playlistId);

        return pas.stream()
                .map(pa -> {
                    AudioFile af = audioFileRepository.findById(pa.getAudioFileId()).orElse(null);
                    if (af == null) {
                        return null;
                    }
                    TextRecord tr = textRecordRepository.findById(af.getTextRecordId()).orElse(null);
                    String rawText = tr != null ? tr.getRawText() : "";
                    String voiceName = tr != null ? tr.getVoiceName() : "";
                    String title = af.getName() != null ? af.getName() :
                            (rawText.length() > 20 ? rawText.substring(0, 20) + "..." : rawText);
                    return new AudioListItem(af.getId(), af.getR2Url(), rawText, voiceName,
                            af.getDuration(), af.getCreateTime().toString(), title);
                })
                .filter(item -> item != null)
                .collect(Collectors.toList());
    }

    // 更新播放列表统计
    private void updatePlaylistStats(Long playlistId) {
        Playlist playlist = playlistRepository.findById(playlistId).orElse(null);
        if (playlist == null) return;

        List<PlaylistAudio> pas = playlistAudioRepository.findByPlaylistIdOrderBySortOrderAsc(playlistId);
        int totalDuration = 0;
        for (PlaylistAudio pa : pas) {
            AudioFile af = audioFileRepository.findById(pa.getAudioFileId()).orElse(null);
            if (af != null && af.getDuration() != null) {
                totalDuration += af.getDuration();
            }
        }

        playlist.setAudioCount(pas.size());
        playlist.setTotalDuration(totalDuration);
        playlist.setUpdateTime(LocalDateTime.now());
        playlistRepository.save(playlist);
    }

    @Data
    public static class PlaylistWithAudios {
        private Playlist playlist;
        private List<AudioListItem> audios;
    }
}
