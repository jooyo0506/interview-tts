# 语音合成v2.0 实现计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 基于豆包双向流式TTS API实现新一代语音合成，支持语音指令、引用上文、语音标签三大核心功能

**Architecture:** WebSocket长连接+流式音频响应，前端解析#指令和【标签】转换为API参数

**Tech Stack:** Spring Boot后端(VOIP WebSocket) + Vue3/uni-app前端 + 豆包TTS2.0 API

---

## Phase 1: 后端基础服务

### Task 1: 创建TTSv2 WebSocket配置类

**Files:**
- Create: `backend/src/main/java/com/interview/tts/config/TtsV2WebSocketConfig.java`

**Step 1: 创建配置类**

```java
@Configuration
public class TtsV2WebSocketConfig {
    @Value("${volcengine.tts.v2.ws-url:wss://openspeech.bytedance.com/api/v3/tts/bidirection}")
    private String wsUrl;

    @Value("${volcengine.tts.app-id:}")
    private String appId;

    @Value("${volcengine.tts.access-token:}")
    private String accessToken;

    @Bean
    public TtsV2Properties ttsV2Properties() {
        return new TtsV2Properties(wsUrl, appId, accessToken);
    }
}
```

**Step 2: 创建属性类**

```java
@Data
@ConfigurationProperties(prefix = "volcengine.tts.v2")
public class TtsV2Properties {
    private String wsUrl;
    private String appId;
    private String accessToken;
    private String resourceId = "seed-tts-2.0";
}
```

**Step 3: 提交**

```bash
git add backend/src/main/java/com/interview/tts/config/
git commit -m "feat: 添加TTSv2 WebSocket配置类"
```

---

### Task 2: 创建TTSv2协议解析类

**Files:**
- Create: `backend/src/main/java/com/interview/tts/service/ttsv2/TtsV2Protocol.java`
- Create: `backend/src/main/java/com/interview/tts/service/ttsv2/TtsV2Message.java`

**Step 1: 创建消息类型枚举**

```java
public enum TtsV2EventType {
    START_CONNECTION(1),
    FINISH_CONNECTION(2),
    CONNECTION_STARTED(50),
    CONNECTION_FINISHED(52),
    START_SESSION(100),
    CANCEL_SESSION(101),
    FINISH_SESSION(102),
    SESSION_STARTED(150),
    SESSION_CANCELED(151),
    SESSION_FINISHED(152),
    TASK_REQUEST(200),
    TTS_SENTENCE_START(350),
    TTS_SENTENCE_END(351),
    TTS_RESPONSE(352);
}
```

**Step 2: 创建消息类**

```java
@Data
public class TtsV2Message {
    private int version;
    private int headerLength;
    private byte messageType;
    private byte serializationType;
    private byte compressionType;
    private int event;
    private String sessionId;
    private byte[] payload;

    public byte[] marshal() { ... }
    public static TtsV2Message unmarshal(byte[] data) { ... }
}
```

**Step 3: 提交**

```bash
git add backend/src/main/java/com/interview/tts/service/ttsv2/
git commit -m "feat: 添加TTSv2协议解析类"
```

---

### Task 3: 创建TTSv2 WebSocket客户端

**Files:**
- Create: `backend/src/main/java/com/interview/tts/service/ttsv2/TtsV2WebSocketClient.java`

**Step 1: 创建WebSocket客户端**

```java
@Service
public class TtsV2WebSocketClient extends WebSocketClient {
    private BlockingQueue<TtsV2Message> messageQueue = new LinkedBlockingQueue<>();
    private String sessionId;
    private boolean connected = false;

    @Override
    public void onOpen(ServerHandshake handshake) {
        this.connected = true;
        log.info("TTSv2 WebSocket连接建立");
    }

    @Override
    public void onMessage(ByteBuffer bytes) {
        TtsV2Message msg = TtsV2Message.unmarshal(bytes.array());
        messageQueue.offer(msg);
    }

    public void sendStartConnection() throws Exception { ... }
    public void sendStartSession(TtsV2Request request) throws Exception { ... }
    public void sendTaskRequest(String text) throws Exception { ... }
    public void sendFinishSession() throws Exception { ... }
    public TtsV2Message waitForEvent(int expectedEvent) throws Exception { ... }
}
```

**Step 2: 提交**

```bash
git add backend/src/main/java/com/interview/tts/service/ttsv2/TtsV2WebSocketClient.java
git commit -m "feat: 添加TTSv2 WebSocket客户端"
```

---

### Task 4: 创建TTSv2核心服务

**Files:**
- Create: `backend/src/main/java/com/interview/tts/service/ttsv2/TtsV2Service.java`
- Create: `backend/src/main/java/com/interview/tts/service/ttsv2/TtsV2Request.java`
- Create: `backend/src/main/java/com/interview/tts/service/ttsv2/TtsV2Response.java`

**Step 1: 创建请求/响应DTO**

```java
@Data
public class TtsV2Request {
    private String text;          // 合成文本
    private String contextText;   // 上文(可选)
    private String voiceType;    // 音色ID
    private String userKey;      // 用户标识
}

@Data
public class TtsV2Response {
    private String audioUrl;      // 音频URL(R2存储)
    private int duration;        // 音频时长
    private List<Subtitle> subtitles; // 字幕
}
```

**Step 2: 创建核心服务**

```java
@Service
public class TtsV2Service {
    @Autowired
    private TtsV2Properties properties;

    @Autowired
    private StorageService storageService;

    public TtsV2Response synthesize(TtsV2Request request) {
        // 1. 解析指令和标签
        String processedText = parseCommands(request.getText());

        // 2. 建立WebSocket连接
        TtsV2WebSocketClient client = createClient();

        // 3. 发送合成请求
        client.sendStartConnection();
        client.waitForEvent(TtsV2EventType.CONNECTION_STARTED);

        client.sendStartSession(buildSessionParams(request));
        client.waitForEvent(TtsV2EventType.SESSION_STARTED);

        // 4. 流式接收音频
        ByteArrayOutputStream audioStream = new ByteArrayOutputStream();
        List<Subtitle> subtitles = new ArrayList<>();

        while (true) {
            TtsV2Message msg = client.waitForAnyEvent();
            if (msg.getEvent() == TtsV2EventType.TTS_RESPONSE.getValue()) {
                audioStream.write(msg.getPayload());
            } else if (msg.getEvent() == TtsV2EventType.TTS_SENTENCE_START.getValue()) {
                // 解析字幕开始
            } else if (msg.getEvent() == TtsV2EventType.TTS_SENTENCE_END.getValue()) {
                // 解析字幕结束
            } else if (msg.getEvent() == TtsV2EventType.SESSION_FINISHED.getValue()) {
                break;
            }
        }

        // 5. 保存到R2
        String r2Url = storageService.uploadAudio(audioStream.toByteArray());

        return new TtsV2Response(r2Url, duration, subtitles);
    }

    private String parseCommands(String text) {
        // 解析#指令和【标签】
    }
}
```

**Step 3: 提交**

```bash
git add backend/src/main/java/com/interview/tts/service/ttsv2/
git commit -m "feat: 添加TTSv2核心合成服务"
```

---

### Task 5: 创建TTSv2控制器

**Files:**
- Create: `backend/src/main/java/com/interview/tts/controller/TtsV2Controller.java`

**Step 1: 创建控制器**

```java
@RestController
@RequestMapping("/api/tts/v2")
public class TtsV2Controller {

    @Autowired
    private TtsV2Service ttsV2Service;

    @PostMapping("/synthesize")
    public Result<TtsV2Response> synthesize(@RequestBody TtsV2Request request) {
        try {
            TtsV2Response response = ttsV2Service.synthesize(request);
            return Result.success(response);
        } catch (Exception e) {
            log.error("TTSv2合成失败", e);
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/voices")
    public Result<List<VoiceInfo>> getVoices() {
        return Result.success(TtsV2VoiceManager.getSupportedVoices());
    }
}
```

**Step 2: 提交**

```bash
git add backend/src/main/java/com/interview/tts/controller/TtsV2Controller.java
git commit -m "feat: 添加TTSv2控制器"
```

---

## Phase 2: 前端页面开发

### Task 6: 创建TTSv2页面

**Files:**
- Create: `frontend/src/pages/tts/v2.vue`

**Step 1: 创建页面模板**

```vue
<template>
  <view class="tts-v2-page">
    <!-- 顶部说话人选择 -->
    <view class="speaker-bar">
      <text class="speaker-label">说话人</text>
      <picker :value="currentSpeakerIndex" :range="speakers" @change="onSpeakerChange">
        <view class="speaker-select">
          {{ speakers[currentSpeakerIndex].name }} ▼
        </view>
      </picker>
    </view>

    <!-- 模式选择 -->
    <view class="mode-selector">
      <view class="mode-option" :class="{ active: mode === 'default' }" @click="mode = 'default'">
        <text>默认</text>
      </view>
      <view class="mode-option" :class="{ active: mode === 'voice_command' }" @click="mode = 'voice_command'">
        <text>语音指令</text>
      </view>
      <view class="mode-option" :class="{ active: mode === 'context' }" @click="mode = 'context'">
        <text>引用上文</text>
      </view>
    </view>

    <!-- 文本输入区 -->
    <view class="text-input-area">
      <textarea
        v-model="text"
        class="text-input"
        placeholder="请输入要合成的文本..."
        :maxlength="10000"
        @input="onTextInput"
      />
      <view class="char-count">{{ text.length }} 字符</view>
    </view>

    <!-- 引用上文(可选) -->
    <view v-if="showContext" class="context-area">
      <view class="context-header">
        <text>上文（可选）</text>
        <text class="context-hint">让AI理解语境</text>
      </view>
      <textarea
        v-model="contextText"
        class="context-input"
        placeholder="输入上文内容..."
      />
    </view>

    <!-- 示例提示 -->
    <view v-if="mode === 'voice_command'" class="examples">
      <text class="examples-title">💡 示例指令</text>
      <view class="example-tags">
        <text class="tag" @click="insertCommand('#开心')">#开心</text>
        <text class="tag" @click="insertCommand('#悲伤')">#悲伤</text>
        <text class="tag" @click="insertCommand('#撒娇')">#撒娇</text>
        <text class="tag" @click="insertCommand('#四川话')">#四川话</text>
      </view>
    </view>

    <!-- 合成按钮 -->
    <button class="synthesize-btn" @click="synthesize">
      {{ isGenerating ? '合成中...' : '🎵 合成试听' }}
    </button>
  </view>
</template>
```

**Step 2: 添加样式**

```scss
.tts-vts-page {
  min-height: 100vh;
  background: #0a0a0f;
  padding: 20px;
}

.speaker-bar {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}

.speaker-select {
  background: rgba(255,255,255,0.1);
  padding: 10px 20px;
  border-radius: 10px;
  color: #fff;
}

.mode-selector {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
}

.mode-option {
  flex: 1;
  padding: 12px;
  background: rgba(255,255,255,0.05);
  border-radius: 10px;
  text-align: center;
  color: rgba(255,255,255,0.6);

  &.active {
    background: linear-gradient(135deg, #667eea, #764ba2);
    color: #fff;
  }
}

.text-input {
  width: 100%;
  height: 200px;
  background: rgba(255,255,255,0.05);
  border-radius: 12px;
  padding: 16px;
  color: #fff;
}

.synthesize-btn {
  width: 100%;
  height: 50px;
  background: linear-gradient(135deg, #f59e0b, #d97706);
  border-radius: 25px;
  color: #fff;
  font-size: 16px;
  margin-top: 30px;
}
```

**Step 3: 提交**

```bash
git add frontend/src/pages/tts/v2.vue
git commit -m "feat: 添加TTSv2页面"
```

---

### Task 7: 添加标签选择面板

**Files:**
- Modify: `frontend/src/pages/tts/v2.vue`

**Step 1: 添加标签面板组件**

```vue
<!-- 标签选择面板 -->
<view v-if="showTagPanel" class="tag-panel-mask" @click="showTagPanel = false">
  <view class="tag-panel" @click.stop>
    <view class="tag-panel-header">
      <text>选择标签</text>
      <text @click="showTagPanel = false">✕</text>
    </view>

    <scroll-view scroll-y class="tag-list">
      <view class="tag-section">
        <text class="tag-section-title">情感</text>
        <view class="tag-items">
          <text class="tag-item" @click="insertTag('【开心】')">开心</text>
          <text class="tag-item" @click="insertTag('【悲伤】')">悲伤</text>
          <text class="tag-item" @click="insertTag('【生气】')">生气</text>
          <text class="tag-item" @click="insertTag('【惊讶】')">惊讶</text>
        </view>
      </view>

      <view class="tag-section">
        <text class="tag-section-title">语气</text>
        <view class="tag-items">
          <text class="tag-item" @click="insertTag('【撒娇】')">撒娇</text>
          <text class="tag-item" @click="insertTag('【严肃】')">严肃</text>
          <text class="tag-item" @click="insertTag('【温柔】')">温柔</text>
          <text class="tag-item" @click="insertTag('【俏皮】')">俏皮</text>
        </view>
      </view>
    </scroll-view>
  </view>
</view>
```

**Step 2: 添加标签输入监听**

```javascript
onTextInput(e) {
  const value = e.detail.value;
  // 监测【输入，显示标签面板
  if (value.endsWith('【')) {
    this.showTagPanel = true;
  }
}
```

**Step 3: 提交**

```bash
git add frontend/src/pages/tts/v2.vue
git commit -m "feat: 添加标签选择面板"
```

---

### Task 8: 添加API调用

**Files:**
- Create: `frontend/src/api/ttsV2.js`

**Step 1: 创建API**

```javascript
import request from './request'

export function getTtsV2Voices() {
  return request.get('/api/tts/v2/voices')
}

export function synthesizeTtsV2(data) {
  return request.post('/api/tts/v2/synthesize', data)
}
```

**Step 2: 在页面中调用**

```javascript
import { synthesizeTtsV2, getTtsV2Voices } from '@/api/ttsV2'

async function synthesize() {
  if (!text.value.trim()) {
    uni.showToast({ title: '请输入文本', icon: 'none' })
    return
  }

  isGenerating.value = true
  try {
    const res = await synthesizeTtsV2({
      text: text.value,
      contextText: contextText.value,
      voiceType: speakers.value[currentSpeakerIndex.value].id,
      mode: mode.value
    })

    if (res.data.audioUrl) {
      // 跳转到播放页面
      uni.navigateTo({
        url: `/pages/play/play?url=${encodeURIComponent(res.data.audioUrl)}&text=${encodeURIComponent(text.value)}`
      })
    }
  } catch (e) {
    uni.showToast({ title: e.message || '合成失败', icon: 'none' })
  } finally {
    isGenerating.value = false
  }
}
```

**Step 3: 提交**

```bash
git add frontend/src/api/ttsV2.js
git commit -m "feat: 添加TTSv2 API调用"
```

---

## Phase 3: 高级功能

### Task 9: 指令解析器

**Files:**
- Modify: `backend/src/main/java/com/interview/tts/service/ttsv2/TtsV2Service.java`

**Step 1: 添加指令解析方法**

```java
private Map<String, Object> parseVoiceCommands(String text) {
    Map<String, Object> params = new HashMap<>();

    // 提取#指令
    Pattern pattern = Pattern.compile("#([^#\\s]+)");
    Matcher matcher = pattern.matcher(text);

    while (matcher.find()) {
        String command = matcher.group(1);
        if (isEmotionCommand(command)) {
            params.put("emotion", command);
        } else if (isSpeedCommand(command)) {
            params.put("speech_rate", parseSpeed(command));
        } else if (isDialectCommand(command)) {
            params.put("dialect", command);
        }
    }

    // 移除#指令，保留原文
    String cleanText = text.replaceAll("#[^#\\s]+", "").trim();
    params.put("text", cleanText);

    return params;
}

private boolean isEmotionCommand(String cmd) {
    return cmd.contains("开心") || cmd.contains("悲伤") ||
           cmd.contains("生气") || cmd.contains("惊讶") ||
           cmd.contains("撒娇") || cmd.contains("暧昧");
}
```

**Step 2: 提交**

```bash
git add backend/src/main/java/com/interview/tts/service/ttsv2/TtsV2Service.java
git commit -m "feat: 添加语音指令解析器"
```

---

### Task 10: 语音标签解析

**Files:**
- Modify: `backend/src/main/java/com/interview/tts/service/ttsv2/TtsV2Service.java`

**Step 1: 添加标签解析**

```java
private List<VoiceTag> parseVoiceTags(String text) {
    List<VoiceTag> tags = new ArrayList<>();
    Pattern pattern = Pattern.compile("【([^】]+)】");
    Matcher matcher = pattern.matcher(text);

    while (matcher.find()) {
        String tagContent = matcher.group(1);
        int startIndex = matcher.start();

        VoiceTag tag = new VoiceTag();
        tag.setContent(tagContent);
        tag.setPosition(startIndex);
        tags.add(tag);
    }

    return tags;
}
```

**Step 2: 提交**

```bash
git add backend/src/main/java/com/interview/tts/service/ttsv2/TtsV2Service.java
git commit -m "feat: 添加语音标签解析"
```

---

## Phase 4: 测试与优化

### Task 11: 集成测试

**Files:**
- Create: `backend/src/test/java/com/interview/tts/TtsV2Test.java`

**Step 1: 编写测试**

```java
@SpringBootTest
public class TtsV2Test {

    @Autowired
    private TtsV2Service ttsV2Service;

    @Test
    public void testBasicSynthesize() {
        TtsV2Request request = new TtsV2Request();
        request.setText("你好，这是测试");
        request.setVoiceType("zh_female_cancan_mars_bigtts");

        TtsV2Response response = ttsV2Service.synthesize(request);
        Assert.notNull(response.getAudioUrl(), "音频URL不应为空");
    }

    @Test
    public void testVoiceCommand() {
        TtsV2Request request = new TtsV2Request();
        request.setText("#开心 今天天气真好");
        request.setVoiceType("zh_female_cancan_mars_bigtts");

        TtsV2Response response = ttsV2Service.synthesize(request);
        Assert.notNull(response.getAudioUrl(), "带指令的合成应成功");
    }

    @Test
    public void testContextText() {
        TtsV2Request request = new TtsV2Request();
        request.setText("我觉得北京是一个很美的城市");
        request.setContextText("你怎么评价北京这个城市？");
        request.setVoiceType("zh_female_cancan_mars_bigtts");

        TtsV2Response response = ttsV2Service.synthesize(request);
        Assert.notNull(response.getAudioUrl(), "带上文的合成应成功");
    }
}
```

**Step 2: 运行测试**

```bash
cd backend && mvn test -Dtest=TtsV2Test
```

**Step 3: 提交**

```bash
git add backend/src/test/java/com/interview/tts/TtsV2Test.java
git commit -m "test: 添加TTSv2集成测试"
```

---

### Task 12: 前端测试

**Files:**
- Create: `frontend/src/pages/tts/v2.spec.js` (如使用Vitest)

**Step 1: 编写页面测试**

```javascript
import { mount } from '@vue/test-utils'
import V2 from './v2.vue'

describe('TTSv2 Page', () => {
  it('should switch mode correctly', () => {
    const wrapper = mount(V2)
    wrapper.findAll('.mode-option')[1].trigger('click')
    expect(wrapper.vm.mode).toBe('voice_command')
  })

  it('should show examples in voice_command mode', () => {
    const wrapper = mount(V2)
    wrapper.setData({ mode: 'voice_command' })
    expect(wrapper.find('.examples').exists()).toBe(true)
  })

  it('should insert command at cursor', () => {
    const wrapper = mount(V2)
    wrapper.setData({ text: 'Hello ' })
    wrapper.vm.insertCommand('#开心')
    expect(wrapper.vm.text).toContain('#开心')
  })
})
```

**Step 2: 提交**

```bash
git add frontend/src/pages/tts/v2.spec.js
git commit -m "test: 添加TTSv2页面测试"
```

---

## 实施顺序

1. **Task 1-5**: 后端基础服务 (后端先行)
2. **Task 6-8**: 前端页面开发
3. **Task 9-10**: 高级功能(指令/标签解析)
4. **Task 11-12**: 测试

---

## 预期产出

- 后端: TTSv2Controller + TtsV2Service + TtsV2WebSocketClient
- 前端: pages/tts/v2.vue 页面
- API: /api/tts/v2/synthesize, /api/tts/v2/voices
