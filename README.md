# 声读 (SoundRead) - 技术文档

## 项目概述

**声读**是一个泛用型音频内容平台，支持文本转语音、AI播客生成、声音复刻、同声传译等功能。

### 技术栈
- **后端**: Spring Boot 3.3.5 + Java 17 + MySQL
- **前端**: uni-app + Vue 3 + H5
- **实时通信**: WebSocket (STOMP)
- **存储**: Cloudflare R2

---

## 功能模块

### 1. 文本转语音 (TTS)
将文字转换为语音输出

### 2. AI播客
将文本转换为双人对谈节目（主播A + 主播B）

### 3. 声音复刻
克隆用户声音，用于语音合成

### 4. 同声传译
实时翻译音频/文本内容

### 5. 边听边问
播放音频时实时语音问答

---

## 后端接口

### 基础接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/user/init | 初始化用户 |
| POST | /api/tts/generate | 生成TTS音频 |
| GET | /api/tts/voices | 获取音色列表 |
| GET | /api/audio/my-list | 获取我的音频列表 |
| POST | /api/audio/collect | 收藏/取消收藏 |
| GET | /api/audio/collect-list | 获取收藏列表 |

### 播客接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/podcast/generate | 生成播客 |
| GET | /api/podcast/list | 获取播客列表 |
| GET | /api/podcast/{id} | 获取播客详情 |

### 语音问答接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/chat/session | 创建问答会话 |
| GET | /api/chat/sessions | 获取会话列表 |
| GET | /api/chat/session/{id}/messages | 获取会话消息 |
| POST | /api/chat/session/{id}/message | 发送消息 |
| DELETE | /api/chat/session/{id} | 删除会话 |

### 声音复刻接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/voice-clone/create | 创建复刻任务 |
| GET | /api/voice-clone/status/{id} | 获取复刻状态 |
| GET | /api/voice-clone/list | 获取复刻列表 |
| GET | /api/voice-clone/{id}/samples | 获取样本列表 |
| DELETE | /api/voice-clone/{id} | 删除复刻 |

### 翻译接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/translate/text | 文本翻译 |
| POST | /api/translate/speech | 语音翻译 |
| GET | /api/translate/list | 获取翻译历史 |
| GET | /api/translate/{id} | 获取翻译详情 |

### WebSocket

| 端点 | 说明 |
|------|------|
| /ws | STOMP WebSocket端点 |

---

## 前端页面

### 页面路由 (pages.json)

| 路径 | 页面 | 说明 |
|------|------|------|
| /pages/index/index | 首页 | 功能入口 |
| /pages/tts/index | 文本转语音 | TTS生成 |
| /pages/podcast/create | AI播客 | 播客创建 |
| /pages/voice/clone | 声音复刻 | 复刻管理 |
| /pages/translate/live | 同声传译 | 翻译功能 |
| /pages/play/play | 播放页 | 音频播放+字幕+问答 |
| /pages/my/my | 我的 | 用户记录 |

### API模块

| 文件 | 功能 |
|------|------|
| api/index.js | 基础API (用户、TTS、收藏) |
| api/podcast.js | 播客API |
| api/voice.js | 声音复刻API |
| api/translate.js | 翻译API |
| api/chat.js | 问答API |

---

## 数据库表

### 现有表

| 表名 | 说明 |
|------|------|
| sys_user | 用户表 |
| text_record | 文本记录 |
| audio_file | 音频文件 |
| user_collect | 收藏记录 |
| audio_cache | 音频缓存 |

### 新增表

| 表名 | 说明 |
|------|------|
| podcast | 播客记录 |
| cloned_voice | 克隆声音 |
| voice_sample | 声音样本 |
| session | 问答会话 |
| chat_message | 聊天消息 |
| translation | 翻译记录 |

---

## 音色列表

| 音色ID | 语言 | 性别 | 名称 |
|--------|------|------|------|
| BV001_streaming | zh-CN | Female | 通用女声 |
| BV002_streaming | zh-CN | Male | 通用男声 |
| BV700_streaming | zh-CN | Female | 灿灿 |
| BV102_streaming | zh-CN | Male | 儒雅青年 |
| BV113_streaming | zh-CN | Female | 甜宠少御 |
| BV033_streaming | zh-CN | Male | 温柔小哥 |
| BV034_streaming | zh-CN | Female | 知性姐姐 |
| BV524_streaming | ja-JP | Male | 日语男声 |
| BV503_streaming | en-US | Female | Ariana |
| BV504_streaming | en-US | Male | Jackson |

---

## 第三方API

### 豆包API
- **TTS**: https://openspeech.bytedance.com/api/v1/tts
- **语音识别**: WebSocket流式
- **声音复刻**: https://openspeech.bytedance.com/api/v2/voice_clone
- **同声传译**: https://openspeech.bytedance.com/api/v2/translate

### 火山引擎
- App ID: 配置在 application.yml
- Access Token: 配置在 application.yml

---

## 项目结构

```
interview-tts/
├── backend/                    # Spring Boot后端
│   └── src/main/java/com/interview/tts/
│       ├── config/            # 配置类
│       ├── controller/       # 控制器
│       ├── dto/             # 数据传输对象
│       ├── entity/           # 实体类
│       ├── exception/        # 异常处理
│       ├── interceptor/      # 拦截器
│       ├── repository/       # 数据仓库
│       ├── service/         # 业务逻辑
│       └── websocket/       # WebSocket处理
│
├── frontend/                  # Uni-app前端
│   └── src/
│       ├── api/             # API封装
│       ├── pages/           # 页面
│       │   ├── index/       # 首页
│       │   ├── tts/         # TTS页面
│       │   ├── podcast/     # 播客页面
│       │   ├── voice/      # 声音复刻
│       │   ├── translate/  # 翻译页面
│       │   ├── play/       # 播放页面
│       │   └── my/         # 我的页面
│       └── static/         # 静态资源
│
└── data/                     # 本地音频存储
```

---

## 配置说明

### 后端配置 (application.yml)

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/interview_tts
    username: root
    password: 123456

doubao:
  api-key: your_api_key
  model-id: doubao-seed-2-0-lite-260215

volcengine:
  tts:
    app-id: your_app_id
    access-token: your_token
```

### 前端配置 (api/index.js)

```javascript
const API_BASE_URL = 'http://localhost:8080'
```

---

## 待实现功能

1. [ ] 真实的音频文件上传
2. [ ] 真实的语音录音和ASR
3. [ ] LLM API集成（目前是模拟回答）
4. [ ] WebSocket实时语音对话
5. [ ] 进度状态展示优化

---

## 常见问题

### Q: 如何启动后端?
```bash
cd backend
mvn spring-boot:run
```

### Q: 如何启动前端?
```bash
cd frontend
npm run dev:h5
```

### Q: 如何使用克隆声音?
1. 进入"声音复刻"页面
2. 上传3-5个语音样本（10-30秒）
3. 等待复刻完成（状态轮询）
4. 复刻成功后可在TTS页面使用
