package com.interview.tts.service.ttsv2;

import com.interview.tts.config.TtsV2Properties;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * TTS v2.0 双向流式WebSocket客户端 (来自官方示例)
 */
@Slf4j
@Component
public class TtsV2WebSocketClient {

    @Autowired
    private TtsV2Properties properties;

    private BlockingQueue<TtsV2Message> messageQueue = new LinkedBlockingQueue<>();
    private WebSocketClient webSocketClient;
    private String connectId;
    private boolean connected = false;
    private String appId;
    private String accessToken;

    @PostConstruct
    public void init() {
        this.appId = properties.getAppId();
        this.accessToken = properties.getAccessToken();
    }

    /**
     * 获取Resource ID - 优先使用配置文件的值
     * 配置文件: volcengine.tts.v2.resource-id
     */
    private String getResourceIdForVoice(String voiceType) {
        // 打印配置文件中的resourceId
        String configuredResourceId = properties.getResourceId();
        log.info("配置文件中的resourceId: [{}]", configuredResourceId);

        if (configuredResourceId != null && !configuredResourceId.isEmpty()) {
            return configuredResourceId;
        }
        // 备用：根据音色选择
        if (voiceType == null) {
            return "seed-tts-2.0";
        }
        if (voiceType.startsWith("S_")) {
            return "volc.megatts.default";
        }
        return "seed-tts-2.0";
    }

    /**
     * 创建并连接WebSocket
     * @param voiceType 音色类型，用于选择正确的resource ID
     */
    public void connect(String voiceType) throws Exception {
        this.connectId = UUID.randomUUID().toString();

        // 优先使用配置文件的resourceId
        String resourceId = getResourceIdForVoice(voiceType);

        Map<String, String> headers = Map.of(
            "X-Api-App-Key", appId != null ? appId : "",
            "X-Api-Access-Key", accessToken != null ? accessToken : "",
            "X-Api-Resource-Id", resourceId,
            "X-Api-Connect-Id", connectId
        );

        log.info("=== WebSocket请求头 ===");
        log.info("X-Api-App-Key: [{}]", appId);
        log.info("X-Api-Access-Key: [{}]", accessToken);
        log.info("X-Api-Resource-Id: [{}]", resourceId);
        log.info("X-Api-Connect-Id: [{}]", connectId);
        log.info("voiceType: [{}]", voiceType);

        webSocketClient = new WebSocketClient(new URI(properties.getWsUrl()), headers) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                connected = true;
                log.info("TTSv2 WebSocket连接建立成功, ConnectId: {}", connectId);
            }

            @Override
            public void onMessage(String message) {
                log.warn("收到文本消息: {}", message);
            }

            @Override
            public void onMessage(ByteBuffer bytes) {
                try {
                    TtsV2Message message = TtsV2Message.unmarshal(bytes.array());
                    log.debug("收到消息: {}", message);
                    messageQueue.offer(message);
                } catch (Exception e) {
                    log.error("解析消息失败", e);
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                connected = false;
                log.info("TTSv2 WebSocket连接关闭: code={}, reason={}", code, reason);
            }

            @Override
            public void onError(Exception ex) {
                log.error("TTSv2 WebSocket错误", ex);
            }
        };

        webSocketClient.connectBlocking(10, TimeUnit.SECONDS);
        if (!connected) {
            throw new RuntimeException("WebSocket连接超时");
        }
    }

    /**
     * 关闭连接
     */
    public void close() {
        if (webSocketClient != null) {
            webSocketClient.close();
            connected = false;
        }
    }

    /**
     * 发送连接开始消息
     */
    public void sendStartConnection() throws Exception {
        TtsV2Message message = new TtsV2Message(TtsV2Message.MsgType.FULL_CLIENT_REQUEST, TtsV2Message.MsgFlag.WITH_EVENT);
        message.setEvent(TtsV2EventType.START_CONNECTION);
        message.setPayload("{}".getBytes());
        sendMessage(message);
        log.info("发送: START_CONNECTION");
    }

    /**
     * 发送连接关闭消息
     */
    public void sendFinishConnection() throws Exception {
        TtsV2Message message = new TtsV2Message(TtsV2Message.MsgType.FULL_CLIENT_REQUEST, TtsV2Message.MsgFlag.WITH_EVENT);
        message.setEvent(TtsV2EventType.FINISH_CONNECTION);
        message.setPayload("{}".getBytes());
        sendMessage(message);
        log.info("发送: FINISH_CONNECTION");
    }

    /**
     * 发送会话开始消息
     */
    public void sendStartSession(String sessionId, byte[] payload) throws Exception {
        TtsV2Message message = new TtsV2Message(TtsV2Message.MsgType.FULL_CLIENT_REQUEST, TtsV2Message.MsgFlag.WITH_EVENT);
        message.setEvent(TtsV2EventType.START_SESSION);
        message.setSessionId(sessionId);
        message.setPayload(payload);
        sendMessage(message);
        log.info("发送: START_SESSION, sessionId={}", sessionId);
    }

    /**
     * 发送会话结束消息
     */
    public void sendFinishSession(String sessionId) throws Exception {
        TtsV2Message message = new TtsV2Message(TtsV2Message.MsgType.FULL_CLIENT_REQUEST, TtsV2Message.MsgFlag.WITH_EVENT);
        message.setEvent(TtsV2EventType.FINISH_SESSION);
        message.setSessionId(sessionId);
        message.setPayload("{}".getBytes());
        sendMessage(message);
        log.info("发送: FINISH_SESSION, sessionId={}", sessionId);
    }

    /**
     * 发送文本请求消息
     */
    public void sendTaskRequest(String sessionId, byte[] payload) throws Exception {
        TtsV2Message message = new TtsV2Message(TtsV2Message.MsgType.FULL_CLIENT_REQUEST, TtsV2Message.MsgFlag.WITH_EVENT);
        message.setEvent(TtsV2EventType.TASK_REQUEST);
        message.setSessionId(sessionId);
        message.setPayload(payload);
        sendMessage(message);
    }

    /**
     * 发送消息
     */
    private void sendMessage(TtsV2Message message) throws Exception {
        log.debug("发送消息: {}", message);
        webSocketClient.send(message.marshal());
    }

    /**
     * 接收消息(阻塞)
     */
    public TtsV2Message receiveMessage() throws InterruptedException {
        return messageQueue.take();
    }

    /**
     * 等待指定事件
     */
    public TtsV2Message waitForEvent(int expectedEvent) throws InterruptedException {
        while (true) {
            TtsV2Message message = messageQueue.poll(30, TimeUnit.SECONDS);
            if (message == null) {
                throw new RuntimeException("等待消息超时");
            }

            log.debug("收到消息事件: {} (期望: {})", message.getEvent(), expectedEvent);

            if (message.getEvent() != null && message.getEvent().getValue() == expectedEvent) {
                return message;
            } else if (message.getEvent() != null && (
                       message.getEvent() == TtsV2EventType.CONNECTION_FAILED ||
                       message.getEvent() == TtsV2EventType.SESSION_FAILED)) {
                throw new RuntimeException("服务返回错误: " + message.getEvent());
            }

            // 其他事件继续等待
        }
    }

    /**
     * 等待任意消息(带超时)
     */
    public TtsV2Message waitForAnyMessage(long timeoutMs) throws InterruptedException {
        return messageQueue.poll(timeoutMs, TimeUnit.MILLISECONDS);
    }

    public boolean isConnected() {
        return connected;
    }

    @Data
    public static class AudioSegment {
        private byte[] audioData;
        private long timestamp;
        private String text;
    }
}
