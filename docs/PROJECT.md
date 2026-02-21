# 项目结构

```
interview-tts/
├── backend/                    # Spring Boot后端
│   └── src/main/java/com/interview/tts/
│       ├── controller/         # 控制器
│       │   ├── AudioController.java
│       │   ├── PodcastController.java
│       │   ├── TranslateController.java
│       │   ├── ChatController.java
│       │   ├── VoiceCloneController.java
│       │   ├── PlaylistController.java
│       │   ├── UserController.java
│       │   ├── UploadController.java
│       │   └── TtsV2Controller.java      # TTSv2.0控制器
│       ├── service/            # 业务逻辑
│       │   ├── AudioService.java
│       │   ├── DouyinTtsService.java
│       │   ├── StorageService.java
│       │   ├── LlmScriptService.java
│       │   ├── ttsv2/          # TTSv2.0服务
│       │   │   ├── TtsV2Service.java
│       │   │   ├── TtsV2WebSocketClient.java
│       │   │   ├── TtsV2Message.java
│       │   │   ├── TtsV2EventType.java
│       │   │   ├── TtsV2Request.java
│       │   │   ├── TtsV2Response.java
│       │   │   └── VoiceInfo.java
│       │   └── ...
│       ├── entity/             # 数据实体
│       ├── repository/         # 数据访问
│       ├── dto/               # 数据传输对象
│       ├── config/            # 配置
│       │   └── TtsV2Properties.java
│       └── interceptor/       # 拦截器
│
├── frontend/                  # Vue3 + uni-app前端
│   └── src/
│       ├── api/               # API接口
│       ├── pages/             # 页面
│       │   ├── index/         # 首页
│       │   ├── tts/           # TTS
│       │   │   ├── index.vue  # TTS首页(短文本)
│       │   │   ├── task.vue   # 长文本任务
│       │   │   └── v2.vue     # TTSv2流式合成
│       │   ├── podcast/       # 播客
│       │   ├── voice/         # 声音克隆
│       │   │   └── clone.vue  # 声音克隆
│       │   ├── translate/     # 翻译
│       │   ├── chat/          # 聊天
│       │   ├── playlist/      # 播放列表
│       │   ├── my/            # 我的
│       │   └── play/          # 播放
│       └── components/        # 组件
│
├── docs/                     # 文档
│   ├── sql/
│   │   └── init.sql          # 数据库初始化
│   ├── API.md                # API接口文档
│   └── CHANGELOG.md          # 更新日志
│
└── 精品长文本文档说明.md       # 火山引擎API文档
```

## 数据库ER图

```
┌─────────────┐       ┌─────────────┐
│   sys_user  │       │ text_record │
├─────────────┤       ├─────────────┤
│ id          │◄──────│ user_id     │
│ user_key    │       │ id          │
│ nickname    │       │ raw_text    │
│ avatar      │       │ ssml_text   │
└─────────────┘       │ voice_name  │
                     └──────┬──────┘
                            │
                     ┌──────▼──────┐
                     │  audio_file │
                     ├─────────────┤
                     │ id          │
                     │ text_record_id
                     │ user_id     │
                     │ name        │
                     │ r2_url      │
                     │ temp_audio_url
                     │ download_failed
                     │ duration    │
                     └──────┬──────┘
                            │
              ┌─────────────┴─────────────┐
              │                           │
        ┌─────▼─────┐             ┌───────▼──────┐
        │user_collect│            │ playlist_audio│
        ├────────────┤            ├──────────────┤
        │ id         │            │ id           │
        │ user_id    │            │ playlist_id  │
        │ audio_file_id           │ audio_file_id│
        └────────────┘            └──────────────┘

┌─────────────┐       ┌─────────────┐
│   podcast   │       │cloned_voice │
├─────────────┤       ├─────────────┤
│ id          │       │ id          │
│ user_id     │       │ user_id     │
│ title       │       │ name        │
│ source_text │       │ voice_id    │
│ script_content         │ status     │
│ voice_a │       │ sample_count │
│ voice_b │       └──────┬──────┘
│ audio_url │            │
└─────────────┘     ┌─────▼─────┐
                    │voice_sample│
                    ├────────────┤
                    │ id         │
                    │cloned_voice_id
                    │ user_id    │
                    │ audio_url  │
                    │ duration   │
                    └────────────┘

┌─────────────┐       ┌─────────────┐
│ translation │       │   session   │
├─────────────┤       ├─────────────┤
│ id          │       │ id          │
│ user_id     │       │ user_id     │
│ source_text │       │ session_type│
│ translated_text      │ title       │
│ source_lang│       │ context_text│
│ target_lang│       │ audio_url   │
└─────────────┘       └──────┬──────┘
                            │
                     ┌──────▼──────┐
                     │ chat_message │
                     ├──────────────┤
                     │ id           │
                     │ session_id   │
                     │ user_id      │
                     │ role         │
                     │ content      │
                     │ audio_url    │
                     └──────────────┘

┌─────────────┐       ┌─────────────┐
│  playlist   │       │ audio_cache │
├─────────────┤       ├─────────────┤
│ id          │       │ id          │
│ user_id     │       │raw_text_md5 │
│ name        │       │ voice_name  │
│ description │       │ prompt_hash │
│ cover_url  │       │ r2_url      │
│ audio_count │       └─────────────┘
│ total_duration
└─────────────┘
```
