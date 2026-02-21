package com.interview.tts.service.ttsv2;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * TTS v2.0 双向流式API消息格式
 *
 * 二进制协议格式:
 * - 固定Header(4字节): 版本(4bit) + Header长度(4bit) + 消息类型(4bit) + 序列化方式(4bit) + 压缩方式(4bit) + 保留(8bit)
 * - 事件编号(4字节)
 * - 连接ID(可选, 16字节)
 * - 会话ID(可选, 16字节)
 * - 负载数据长度(4字节)
 * - 负载数据
 */
@Slf4j
@Data
public class TtsV2Message {

    // 协议版本
    private static final byte PROTOCOL_VERSION = 0b0001;

    // Header长度(固定为1, 表示1个32位字 = 4字节)
    private static final byte HEADER_LENGTH = 0b0001;

    // 消息类型
    private static final byte MSG_TYPE_FULL_REQUEST = 0b0001;    // 客户端完整请求
    private static final byte MSG_TYPE_FULL_RESPONSE = 0b1001;   // 服务端完整响应
    private static final byte MSG_TYPE_AUDIO_ONLY = 0b1011;      // 仅音频响应
    private static final byte MSG_TYPE_ERROR = 0b1111;           // 错误信息

    // 序列化方式
    private static final byte SERIALIZATION_JSON = 0b0001;
    private static final byte SERIALIZATION_RAW = 0b0010;

    // 压缩方式
    private static final byte COMPRESSION_NONE = 0b0000;

    // Header字段
    private byte version;
    private byte headerLength;
    private byte messageType;
    private byte serializationType;
    private byte compressionType;

    // 事件编号
    private int event;

    // 会话ID
    private String sessionId;

    // 负载数据
    private byte[] payload;

    /**
     * 解码消息
     */
    public static TtsV2Message unmarshal(byte[] data) {
        if (data == null || data.length < 8) {
            throw new IllegalArgumentException("消息数据过短");
        }

        ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN);

        // 解析Header第一个字节
        byte header = buffer.get();
        byte version = (byte) ((header >> 4) & 0x0F);
        byte headerLength = (byte) (header & 0x0F);

        // 解析Header第二个字节
        header = buffer.get();
        byte messageType = (byte) ((header >> 4) & 0x0F);
        byte serializationType = (byte) ((header >> 4) & 0x0F);
        byte compressionType = (byte) (header & 0x0F);

        // 事件编号(4字节)
        int event = buffer.getInt();

        // 会话ID(16字节, 如果有)
        String sessionId = null;
        if (data.length >= 24) {
            byte[] sessionBytes = new byte[16];
            buffer.get(sessionBytes);
            sessionId = new String(sessionBytes).trim();
        }

        // 负载数据长度(4字节)
        int payloadLength = buffer.getInt();

        // 负载数据
        byte[] payload = null;
        if (payloadLength > 0 && buffer.hasRemaining()) {
            payload = new byte[payloadLength];
            buffer.get(payload);
        }

        TtsV2Message message = new TtsV2Message();
        message.setVersion(version);
        message.setHeaderLength(headerLength);
        message.setMessageType(messageType);
        message.setSerializationType(serializationType);
        message.setCompressionType(compressionType);
        message.setEvent(event);
        message.setSessionId(sessionId);
        message.setPayload(payload);

        return message;
    }

    /**
     * 编码消息
     */
    public byte[] marshal() {
        // 计算消息长度
        int headerSize = 4;  // 2字节header + 4字节event
        int sessionIdSize = (sessionId != null && !sessionId.isEmpty()) ? 16 : 0;
        int payloadSize = (payload != null) ? payload.length : 0;

        int totalSize = headerSize + sessionIdSize + 4 + payloadSize; // +4 for payload length

        ByteBuffer buffer = ByteBuffer.allocate(totalSize).order(ByteOrder.BIG_ENDIAN);

        // Header第一个字节: 版本 + Header长度
        byte header1 = (byte) ((version << 4) | headerLength);
        buffer.put(header1);

        // Header第二个字节: 消息类型 + 序列化方式 + 压缩方式
        byte header2 = (byte) ((messageType << 4) | (serializationType << 4) | compressionType);
        buffer.put(header2);

        // 事件编号
        buffer.putInt(event);

        // 会话ID
        if (sessionIdSize > 0) {
            byte[] sessionBytes = new byte[16];
            byte[] src = sessionId.getBytes();
            System.arraycopy(src, 0, sessionBytes, 0, Math.min(src.length, 16));
            buffer.put(sessionBytes);
        }

        // 负载数据长度
        buffer.putInt(payloadSize);

        // 负载数据
        if (payload != null && payload.length > 0) {
            buffer.put(payload);
        }

        return buffer.array();
    }

    /**
     * 创建连接开始消息
     */
    public static TtsV2Message createStartConnection() {
        TtsV2Message msg = new TtsV2Message();
        msg.setVersion(PROTOCOL_VERSION);
        msg.setHeaderLength(HEADER_LENGTH);
        msg.setMessageType(MSG_TYPE_FULL_REQUEST);
        msg.setSerializationType(SERIALIZATION_JSON);
        msg.setCompressionType(COMPRESSION_NONE);
        msg.setEvent(TtsV2EventType.START_CONNECTION.getValue());
        msg.setPayload("{}".getBytes());
        return msg;
    }

    /**
     * 创建连接关闭消息
     */
    public static TtsV2Message createFinishConnection() {
        TtsV2Message msg = new TtsV2Message();
        msg.setVersion(PROTOCOL_VERSION);
        msg.setHeaderLength(HEADER_LENGTH);
        msg.setMessageType(MSG_TYPE_FULL_REQUEST);
        msg.setSerializationType(SERIALIZATION_JSON);
        msg.setCompressionType(COMPRESSION_NONE);
        msg.setEvent(TtsV2EventType.FINISH_CONNECTION.getValue());
        msg.setPayload("{}".getBytes());
        return msg;
    }

    /**
     * 创建会话开始消息
     */
    public static TtsV2Message createStartSession(String sessionId, byte[] payload) {
        TtsV2Message msg = new TtsV2Message();
        msg.setVersion(PROTOCOL_VERSION);
        msg.setHeaderLength(HEADER_LENGTH);
        msg.setMessageType(MSG_TYPE_FULL_REQUEST);
        msg.setSerializationType(SERIALIZATION_JSON);
        msg.setCompressionType(COMPRESSION_NONE);
        msg.setEvent(TtsV2EventType.START_SESSION.getValue());
        msg.setSessionId(sessionId);
        msg.setPayload(payload);
        return msg;
    }

    /**
     * 创建会话结束消息
     */
    public static TtsV2Message createFinishSession(String sessionId) {
        TtsV2Message msg = new TtsV2Message();
        msg.setVersion(PROTOCOL_VERSION);
        msg.setHeaderLength(HEADER_LENGTH);
        msg.setMessageType(MSG_TYPE_FULL_REQUEST);
        msg.setSerializationType(SERIALIZATION_JSON);
        msg.setCompressionType(COMPRESSION_NONE);
        msg.setEvent(TtsV2EventType.FINISH_SESSION.getValue());
        msg.setSessionId(sessionId);
        msg.setPayload("{}".getBytes());
        return msg;
    }

    /**
     * 创建文本请求消息
     */
    public static TtsV2Message createTaskRequest(String sessionId, byte[] payload) {
        TtsV2Message msg = new TtsV2Message();
        msg.setVersion(PROTOCOL_VERSION);
        msg.setHeaderLength(HEADER_LENGTH);
        msg.setMessageType(MSG_TYPE_FULL_REQUEST);
        msg.setSerializationType(SERIALIZATION_JSON);
        msg.setCompressionType(COMPRESSION_NONE);
        msg.setEvent(TtsV2EventType.TASK_REQUEST.getValue());
        msg.setSessionId(sessionId);
        msg.setPayload(payload);
        return msg;
    }

    @Override
    public String toString() {
        return String.format("TtsV2Message{event=%d, sessionId=%s, payloadSize=%d}",
            event, sessionId, payload != null ? payload.length : 0);
    }
}
