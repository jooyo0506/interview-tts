package com.interview.tts.service;

import cn.hutool.crypto.digest.DigestUtil;
import com.interview.tts.entity.AudioCache;
import com.interview.tts.repository.AudioCacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {

    private final AudioCacheRepository audioCacheRepository;

    public String getCachedUrl(String rawText, String voiceName, String promptHash) {
        String md5 = DigestUtil.md5Hex(rawText);
        Optional<AudioCache> cached = audioCacheRepository
                .findByRawTextMd5AndVoiceNameAndPromptHash(md5, voiceName, promptHash);

        if (cached.isPresent()) {
            log.info("缓存命中: md5={}, voice={}", md5, voiceName);
            return cached.get().getR2Url();
        }
        return null;
    }

    public void saveToCache(String rawText, String voiceName, String promptHash, String r2Url) {
        String md5 = DigestUtil.md5Hex(rawText);

        // 检查是否已存在
        Optional<AudioCache> existing = audioCacheRepository
                .findByRawTextMd5AndVoiceNameAndPromptHash(md5, voiceName, promptHash);

        if (existing.isPresent()) {
            log.info("缓存已存在，无需重复创建");
            return;
        }

        AudioCache cache = new AudioCache();
        cache.setRawTextMd5(md5);
        cache.setVoiceName(voiceName);
        cache.setPromptHash(promptHash);
        cache.setR2Url(r2Url);
        audioCacheRepository.save(cache);

        log.info("缓存保存成功: md5={}, voice={}", md5, voiceName);
    }

    public String getRawTextMd5(String rawText) {
        return DigestUtil.md5Hex(rawText);
    }
}
