package com.interview.tts.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.interview.tts.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 火山引擎认证服务
 *
 * 配置说明 (application.yml):
 * volcengine:
 *   auth:
 *     api-key: your_api_key (从控制台获取)
 *     secret-key: your_secret_key (从控制台获取)
 */
@Slf4j
@Service
public class VolcanoAuthService {

    private static final String AUTH_URL = "https://open.volcengineapi.com/";

    @Value("${volcengine.auth.api-key:}")
    private String apiKey;

    @Value("${volcengine.auth.secret-key:}")
    private String secretKey;

    // Token缓存
    private String cachedToken;
    private long tokenExpireTime;

    private final OkHttpClient httpClient;

    public VolcanoAuthService() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    @PostConstruct
    public void init() {
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("火山引擎未配置api-key，请检查配置!");
        } else if (secretKey == null || secretKey.isEmpty()) {
            log.warn("火山引擎未配置secret-key，请检查配置!");
        } else {
            log.info("火山引擎认证服务初始化完成");
        }
    }

    /**
     * 获取access_token（带缓存，24小时有效）
     */
    public String getAccessToken() {
        // 检查缓存
        if (cachedToken != null && System.currentTimeMillis() < tokenExpireTime) {
            log.debug("使用缓存的access_token");
            return cachedToken;
        }

        // 重新获取
        return refreshAccessToken();
    }

    /**
     * 刷新access_token
     */
    public String refreshAccessToken() {
        if (apiKey == null || apiKey.isEmpty() || secretKey == null || secretKey.isEmpty()) {
            log.error("火山引擎认证信息不完整");
            throw BusinessException.ttsFailed();
        }

        try {
            // 火山引擎API需要用URL参数
            String url = AUTH_URL + "?Action=GetAppAccessToken&Version=2021-11-01&GrantType=client_credentials&ClientKey=" + apiKey + "&ClientSecret=" + secretKey;

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "empty";
                    log.error("获取access_token失败: status={}, body={}", response.code(), errorBody);
                    throw BusinessException.ttsFailed();
                }

                String responseBody = response.body().string();
                log.debug("获取token响应: {}", responseBody);

                JSONObject json = JSON.parseObject(responseBody);

                // 检查是否有错误
                if (json.containsKey("ResponseMetadata")) {
                    JSONObject metadata = json.getJSONObject("ResponseMetadata");
                    if (metadata.containsKey("Error")) {
                        JSONObject error = metadata.getJSONObject("Error");
                        String errorCode = error.getString("Code");
                        String errorMsg = error.getString("Message");
                        log.error("获取token错误: {} - {}", errorCode, errorMsg);
                        throw BusinessException.ttsFailed();
                    }
                }

                // 提取token
                if (!json.containsKey("Result")) {
                    log.error("响应中没有Result: {}", responseBody);
                    throw BusinessException.ttsFailed();
                }
                JSONObject result = json.getJSONObject("Result");
                String accessToken = result.getString("AccessToken");
                long expiresIn = result.getLongValue("ExpiresIn");

                // 缓存token，留一点缓冲时间
                cachedToken = accessToken;
                tokenExpireTime = System.currentTimeMillis() + (expiresIn - 300) * 1000;

                log.info("成功获取access_token，有效期: {}秒", expiresIn);
                return accessToken;
            }

        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            log.error("获取access_token失败: {}", e.getMessage(), e);
            throw BusinessException.ttsFailed();
        }
    }

    /**
     * 强制刷新token
     */
    public String forceRefreshToken() {
        cachedToken = null;
        tokenExpireTime = 0;
        return refreshAccessToken();
    }

    /**
     * 获取secret key（用于TTS签名）
     */
    public String getSecretKey() {
        return secretKey;
    }
}
