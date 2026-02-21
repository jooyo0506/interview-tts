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
 * TTS v2.0 双向流式WebSocket客户端
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
     * 创建并连接WebSocket
     */
    public void connect() throws Exception {
        this.connectId = UUID.randomUUID().toString();

        Map<String, String> headers = Map.of(
            "X-Api-App-Key", appId != null ? appId : "",
            "X-Api-Access-Key", accessToken != null ? accessToken : "",
            "X-Api-Resource-Id", properties.getResourceId(),
            "X-Api-Connect-Id", connectId
        );

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
        TtsV2Message message = TtsV2Message.createStartConnection();
        webSocketClient.send(message.marshal());
        log.info("发送: START_CONNECTION");
    }

    /**
     * 发送连接关闭消息
     */
    public void sendFinishConnection() throws Exception {
        TtsV2Message message = TtsV2Message.createFinishConnection();
        webSocketClient.send(message.marshal());
        log.info("发送: FINISH_CONNECTION");
    }

    /**
     * 发送会话开始消息
     */
    public void sendStartSession(String sessionId, byte[] payload) throws Exception {
        TtsV2Message message = TtsV2Message.createStartSession(sessionId, payload);
        webSocketClient.send(message.marshal());
        log.info("发送: START_SESSION, sessionId={}", sessionId);
    }

    /**
     * 发送会话结束消息
     */
    public void sendFinishSession(String sessionId) throws Exception {
        TtsV2Message message = TtsV2Message.createFinishSession(sessionId);
        webSocketClient.send(message.marshal());
        log.info("发送: FINISH_SESSION, sessionId={}", sessionId);
    }

    /**
     * 发送文本请求消息
     */
    public void sendTaskRequest(String sessionId, byte[] payload) throws Exception {
        TtsV2Message message = TtsV2Message.createTaskRequest(sessionId, payload);
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

            if (message.getEvent() == expectedEvent) {
                return message;
            } else if (message.getEvent() == TtsV2EventType.CONNECTION_FAILED.getValue() ||
                       message.getEvent() == TtsV2EventType.SESSION_FAILED.getValue()) {
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
