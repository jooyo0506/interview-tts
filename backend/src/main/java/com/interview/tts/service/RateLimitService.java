package com.interview.tts.service;

import com.interview.tts.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class RateLimitService {

    @Value("${project.max-generate-count-per-day:20}")
    private int maxGenerateCountPerDay;

    @Value("${project.max-text-length:10000}")
    private int maxTextLength;

    // 用户今日生成次数缓存
    private final Map<String, UserDailyCount> dailyCountMap = new ConcurrentHashMap<>();

    public void checkTextLength(String text) {
        if (text != null && text.length() > maxTextLength) {
            throw BusinessException.textTooLong();
        }
    }

    public void checkAndIncrement(String userKey) {
        String today = LocalDate.now().toString();
        String cacheKey = userKey + ":" + today;

        UserDailyCount count = dailyCountMap.computeIfAbsent(cacheKey, k -> new UserDailyCount());

        if (count.count >= maxGenerateCountPerDay) {
            log.warn("用户 {} 今日生成次数已达上限: {}", userKey, count.count);
            throw BusinessException.rateLimited();
        }

        count.count++;
        log.info("用户 {} 今日生成次数: {}/{}", userKey, count.count, maxGenerateCountPerDay);
    }

    public int getRemainingCount(String userKey) {
        String today = LocalDate.now().toString();
        String cacheKey = userKey + ":" + today;
        UserDailyCount count = dailyCountMap.get(cacheKey);
        if (count == null) {
            return maxGenerateCountPerDay;
        }
        return Math.max(0, maxGenerateCountPerDay - count.count);
    }

    public int getMaxTextLength() {
        return maxTextLength;
    }

    private static class UserDailyCount {
        int count = 0;
    }
}
