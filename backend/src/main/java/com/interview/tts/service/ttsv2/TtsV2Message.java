package com.interview.tts.service.ttsv2;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * TTS v2.0 消息格式 (来自官方示例 volcengine_bidirection_demo)
 */
@Slf4j
@Data
public class TtsV2Message {
    // 协议版本 (1 = Version1)
    private static final byte VERSION = 1;
    // Header大小 (1 = 4字节)
    private static final byte HEADER_SIZE = 1;

    // 消息类型
    public enum MsgType {
        INVALID((byte) 0),
        FULL_CLIENT_REQUEST((byte) 0b1),
        AUDIO_ONLY_CLIENT((byte) 0b10),
        FULL_SERVER_RESPONSE((byte) 0b1001),
        AUDIO_ONLY_SERVER((byte) 0b1011),
        ERROR((byte) 0b1111);

        private final byte value;
        MsgType(byte value) { this.value = value; }
        public byte getValue() { return value; }
        public static MsgType fromValue(int value) {
            for (MsgType t : values()) { if (t.value == value) return t; }
            return INVALID;
        }
    }

    // 消息标志
    public enum MsgFlag {
        NO_SEQ((byte) 0),
        POSITIVE_SEQ((byte) 0b1),
        LAST_NO_SEQ((byte) 0b10),
        NEGATIVE_SEQ((byte) 0b11),
        WITH_EVENT((byte) 0b100);

        private final byte value;
        MsgFlag(byte value) { this.value = value; }
        public byte getValue() { return value; }
        public static MsgFlag fromValue(int value) {
            for (MsgFlag t : values()) { if (t.value == value) return t; }
            return NO_SEQ;
        }
    }

    // 序列化方式
    private static final byte SERIALIZATION_JSON = 1;

    // 字段
    private byte version = VERSION;
    private byte headerSize = HEADER_SIZE;
    private MsgType type;
    private MsgFlag flag;
    private byte serialization = SERIALIZATION_JSON;
    private byte compression = 0;

    private TtsV2EventType event;
    private String sessionId;
    private String connectId;
    private int sequence;
    private int errorCode;
    private byte[] payload;

    public TtsV2Message(MsgType type, MsgFlag flag) {
        this.type = type;
        this.flag = flag;
    }

    /**
     * 解码消息
     */
    public static TtsV2Message unmarshal(byte[] data) {
        if (data == null || data.length < 4) {
            throw new IllegalArgumentException("消息数据过短: " + (data == null ? 0 : data.length));
        }

        // Debug: 打印原始字节
        StringBuilder hex = new StringBuilder();
        for (int i = 0; i < Math.min(data.length, 20); i++) {
            hex.append(String.format("%02X ", data[i]));
        }
        log.debug("原始消息: {}", hex);

        ByteBuffer buffer = ByteBuffer.wrap(data);

        // 解析第一个字节: version + headerSize
        int versionAndHeaderSize = buffer.get() & 0xFF;
        byte version = (byte) ((versionAndHeaderSize >> 4) & 0x0F);
        byte headerSize = (byte) (versionAndHeaderSize & 0x0F);

        // 解析第二个字节: type + flag
        int typeAndFlag = data[1] & 0xFF;
        MsgType type = MsgType.fromValue((typeAndFlag >> 4) & 0x0F);
        MsgFlag flag = MsgFlag.fromValue(typeAndFlag & 0x0F);

        // 第三个字节: serialization + compression (跳过)
        // buffer.get();

        // 跳过padding字节到第4字节位置
        int headerSizeInt = 4 * headerSize;  // 4
        int paddingSize = headerSizeInt - 3;  // 1
        for (int i = 0; i < paddingSize; i++) {
            buffer.get();
        }

        TtsV2Message message = new TtsV2Message(type, flag);
        message.setVersion(version);
        message.setHeaderSize(headerSize);

        log.debug("解析: type={}, flag={}, remaining={}", type, flag, buffer.remaining());

        // ERROR类型消息的特殊处理
        if (type == MsgType.ERROR) {
            // 读取错误码 (4字节)
            if (data.length >= 8) {
                int errorCode = ((data[4] & 0xFF) << 24)
                             | ((data[5] & 0xFF) << 16)
                             | ((data[6] & 0xFF) << 8)
                             | (data[7] & 0xFF);
                message.setErrorCode(errorCode);
                log.debug("错误码: {}", errorCode);
            }

            // 读取payload
            if (data.length > 12) {
                int payloadLength = ((data[8] & 0xFF) << 24)
                                 | ((data[9] & 0xFF) << 16)
                                 | ((data[10] & 0xFF) << 8)
                                 | (data[11] & 0xFF);
                log.debug("ERROR payloadLength: {}", payloadLength);
                if (payloadLength > 0 && data.length >= 12 + payloadLength) {
                    byte[] payload = new byte[payloadLength];
                    System.arraycopy(data, 12, payload, 0, payloadLength);
                    message.setPayload(payload);
                }
            }
            return message;
        }

        // 读取event (如果flag包含WITH_EVENT) - event在byte[4-7]
        if (flag == MsgFlag.WITH_EVENT) {
            // 直接从byte[4]读取event
            if (data.length >= 8) {
                int eventValue = ((data[4] & 0xFF) << 24)
                                | ((data[5] & 0xFF) << 16)
                                | ((data[6] & 0xFF) << 8)
                                | (data[7] & 0xFF);
                log.debug("event值: {}", eventValue);
                message.setEvent(TtsV2EventType.fromValue(eventValue));
            }

            // 如果不是连接相关事件，读取sessionId - 从byte[8]开始是sessionId length
            if (message.getEvent() != TtsV2EventType.CONNECTION_STARTED &&
                message.getEvent() != TtsV2EventType.CONNECTION_FAILED &&
                message.getEvent() != TtsV2EventType.CONNECTION_FINISHED) {
                if (data.length >= 12) {
                    int sessionIdLength = ((data[8] & 0xFF) << 24)
                                        | ((data[9] & 0xFF) << 16)
                                        | ((data[10] & 0xFF) << 8)
                                        | (data[11] & 0xFF);
                    log.debug("sessionIdLength: {}", sessionIdLength);
                    if (sessionIdLength > 0 && data.length >= 12 + sessionIdLength) {
                        byte[] sessionIdBytes = new byte[sessionIdLength];
                        System.arraycopy(data, 12, sessionIdBytes, 0, sessionIdLength);
                        message.setSessionId(new String(sessionIdBytes, StandardCharsets.UTF_8));
                    }
                }
            } else {
                // 连接相关事件 - 读取connectId
                if (data.length >= 12) {
                    int connectIdLength = ((data[8] & 0xFF) << 24)
                                        | ((data[9] & 0xFF) << 16)
                                        | ((data[10] & 0xFF) << 8)
                                        | (data[11] & 0xFF);
                    log.debug("connectIdLength: {}", connectIdLength);
                    if (connectIdLength > 0 && data.length >= 12 + connectIdLength) {
                        byte[] connectIdBytes = new byte[connectIdLength];
                        System.arraycopy(data, 12, connectIdBytes, 0, connectIdLength);
                        message.setConnectId(new String(connectIdBytes, StandardCharsets.UTF_8));
                    }
                }
            }

            // 读取payload - 找payload length的位置
            int payloadStart = 12;
            if (message.getSessionId() != null) {
                payloadStart = 12 + message.getSessionId().length();
            } else if (message.getConnectId() != null) {
                payloadStart = 12 + message.getConnectId().length();
            }
            // 对齐到4字节
            while (payloadStart % 4 != 0) payloadStart++;

            if (data.length > payloadStart + 4) {
                int payloadLength = ((data[payloadStart] & 0xFF) << 24)
                                  | ((data[payloadStart + 1] & 0xFF) << 16)
                                  | ((data[payloadStart + 2] & 0xFF) << 8)
                                  | (data[payloadStart + 3] & 0xFF);
                log.debug("payloadLength: {}", payloadLength);
                if (payloadLength > 0 && data.length >= payloadStart + 4 + payloadLength) {
                    byte[] payload = new byte[payloadLength];
                    System.arraycopy(data, payloadStart + 4, payload, 0, payloadLength);
                    message.setPayload(payload);
                }
            }

            return message;
        }

        // 不带event的消息
        return message;
    }

    /**
     * 编码消息
     */
    public byte[] marshal() throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        // Header: version + headerSize
        buffer.write(((version & 0x0F) << 4) | (headerSize & 0x0F));
        // Header: type + flag
        buffer.write(((type.getValue() & 0x0F) << 4) | (flag.getValue() & 0x0F));
        // Header: serialization + compression
        buffer.write(((serialization & 0x0F) << 4) | (compression & 0x0F));

        // Padding to headerSize * 4 bytes
        int headerSizeInt = 4 * headerSize;
        int padding = headerSizeInt - buffer.size();
        while (padding > 0) {
            buffer.write(0);
            padding--;
        }

        // Event (4 bytes)
        if (event != null) {
            byte[] eventBytes = ByteBuffer.allocate(4).putInt(event.getValue()).array();
            buffer.write(eventBytes);
        }

        // SessionId (length + data)
        if (sessionId != null) {
            byte[] sessionIdBytes = sessionId.getBytes(StandardCharsets.UTF_8);
            buffer.write(ByteBuffer.allocate(4).putInt(sessionIdBytes.length).array());
            buffer.write(sessionIdBytes);
        }

        // ConnectId (length + data)
        if (connectId != null) {
            byte[] connectIdBytes = connectId.getBytes(StandardCharsets.UTF_8);
            buffer.write(ByteBuffer.allocate(4).putInt(connectIdBytes.length).array());
            buffer.write(connectIdBytes);
        }

        // Sequence (4 bytes)
        if (sequence != 0) {
            buffer.write(ByteBuffer.allocate(4).putInt(sequence).array());
        }

        // ErrorCode (4 bytes)
        if (errorCode != 0) {
            buffer.write(ByteBuffer.allocate(4).putInt(errorCode).array());
        }

        // Payload (length + data)
        if (payload != null && payload.length > 0) {
            buffer.write(ByteBuffer.allocate(4).putInt(payload.length).array());
            buffer.write(payload);
        }

        return buffer.toByteArray();
    }

    @Override
    public String toString() {
        return String.format("TtsV2Message{type=%s, event=%s, sessionId=%s, payloadSize=%d}",
            type, event, sessionId, payload != null ? payload.length : 0);
    }
}
