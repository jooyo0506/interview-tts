package com.interview.tts.service;

import com.interview.tts.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@Service
public class StorageService {

    @Value("${storage.mode:local}")
    private String mode;

    @Value("${storage.local-dir:./data/audio}")
    private String localDir;

    @Value("${storage.public-base-url:http://localhost:8080}")
    private String publicBaseUrl;

    @Value("${storage.estimated-bitrate-kbps:64}")
    private int estimatedBitrateKbps;

    @Value("${r2.access-key-id:}")
    private String r2AccessKeyId;

    @Value("${r2.secret-access-key:}")
    private String r2SecretAccessKey;

    @Value("${r2.bucket-name:}")
    private String r2BucketName;

    @Value("${r2.endpoint:}")
    private String r2Endpoint;

    @Value("${r2.public-domain:}")
    private String r2PublicDomain;

    private S3Client r2Client;

    public void init() {
        if ("r2".equals(mode) && r2AccessKeyId != null && !r2AccessKeyId.isEmpty()) {
            AwsBasicCredentials credentials = AwsBasicCredentials.create(r2AccessKeyId, r2SecretAccessKey);
            r2Client = S3Client.builder()
                    .endpointOverride(URI.create(r2Endpoint))
                    .region(Region.US_EAST_1)
                    .credentialsProvider(StaticCredentialsProvider.create(credentials))
                    .forcePathStyle(true)
                    .build();
            log.info("R2 存储初始化成功");
        } else {
            log.info("本地存储模式初始化成功");
        }
    }

    public String uploadAudio(byte[] audioData, String originalFileName) {
        if ("r2".equals(mode) && r2Client != null) {
            return uploadToR2(audioData);
        } else {
            return uploadToLocal(audioData);
        }
    }

    private String uploadToR2(byte[] audioData) {
        try {
            String key = "audio/" + UUID.randomUUID().toString() + ".mp3";

            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(r2BucketName)
                    .key(key)
                    .contentType("audio/mpeg")
                    .contentLength((long) audioData.length)
                    .build();

            r2Client.putObject(putRequest, RequestBody.fromBytes(audioData));

            String url = r2PublicDomain + "/" + key;
            log.info("R2 上传成功: {}", url);
            return url;
        } catch (Exception e) {
            log.error("R2 上传失败: {}", e.getMessage(), e);
            throw BusinessException.r2UploadFailed();
        }
    }

    private String uploadToLocal(byte[] audioData) {
        try {
            Path dirPath = Paths.get(localDir);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            String fileName = UUID.randomUUID().toString() + ".mp3";
            Path filePath = dirPath.resolve(fileName);
            Files.write(filePath, audioData);

            String url = publicBaseUrl + "/" + fileName;
            log.info("本地存储成功: {}", url);
            return url;
        } catch (Exception e) {
            log.error("本地存储失败: {}", e.getMessage(), e);
            throw BusinessException.r2UploadFailed();
        }
    }

    public int estimateDuration(byte[] audioData) {
        // 估算音频时长 (秒)
        long bits = (long) audioData.length * 8;
        int duration = (int) (bits / (estimatedBitrateKbps * 1000L));
        return Math.max(duration, 1);
    }
}
