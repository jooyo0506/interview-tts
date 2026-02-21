# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**声读 (SoundRead)** - A voice content platform with TTS, AI podcasts, voice cloning, and real-time translation.

- **Backend**: Spring Boot 3.3.5 + Java 21 + MySQL
- **Frontend**: uni-app + Vue 3 + H5
- **Storage**: Cloudflare R2
- **TTS Engine**: ByteDance Volcano Engine (豆包TTS)

## Common Commands

### Backend
```bash
cd backend
mvn spring-boot:run        # Start backend on port 8080
mvn clean package           # Build JAR
```

### Frontend
```bash
cd frontend
npm install                 # Install dependencies
npm run dev                 # Start dev server (H5)
npm run dev:h5              # Same as above
npm run build               # Build for production
```

## Architecture

### Backend Structure
```
backend/src/main/java/com/interview/tts/
├── controller/           # REST endpoints
│   ├── TtsV2Controller.java      # TTSv2.0流式合成
│   ├── AudioController.java       # 音频管理
│   ├── PodcastController.java     # AI播客
│   ├── ChatController.java       # 语音问答
│   └── ...
├── service/             # Business logic
│   ├── ttsv2/
│   │   ├── TtsV2Service.java            # TTSv2核心服务
│   │   ├── TtsV2WebSocketClient.java    # WebSocket客户端
│   │   └── VoiceInfo.java               # 音色配置
│   ├── StorageService.java    # R2/本地存储
│   └── ...
└── config/              # Configuration
    └── TtsV2Properties.java   # TTSv2配置
```

### Frontend Structure
```
frontend/src/
├── api/                 # API wrappers
│   ├── index.js         # Base API + TTS
│   └── ...
├── pages/
│   ├── tts/
│   │   ├── index.vue    # TTS v1.0
│   │   └── v2.vue      # TTS v2.0 (语音指令/标签)
│   └── ...
└── App.vue
```

### Key Files
- `backend/src/main/resources/application.yml` - Backend config (R2, API keys)
- `frontend/src/api/index.js` - API base URL
- `frontend/src/pages/tts/v2.vue` - TTSv2.0 UI component

## TTSv2.0 Features

TTSv2.0 uses bidirectional WebSocket streaming with ByteDance's seed-tts-2.0 model.

### Supported Modes
- **default**: Use emotion tags like 【开心】, 【悲伤】, 【撒娇】
- **voice_command**: Use commands like #开心, #四川话, #语速快
- **context**: Reference previous text for context-aware emotion

### Supported Voices (情感音色)
- `zh_female_cancan_mars_bigtts` - 灿灿(可爱女生)
- `zh_female_xiaoyuan_mars_bigtts` - 调皮公主
- `zh_male_shengyang_mars_bigtts` - 爽朗少年
- `zh_male_tiancai_mars_bigtts` - 天才同桌

### API Endpoints
| Method | Path | Description |
|--------|------|-------------|
| POST | /api/tts/v2/synthesize | TTSv2.0流式合成 |
| GET | /api/tts/v2/voices | 获取支持音色列表 |

## Configuration

### R2 Storage (Critical)
In `application.yml`, ensure `r2.public-domain` includes the full path:
```yaml
r2:
  public-domain: ${R2_PUBLIC_DOMAIN:https://r2.joyoai.xyz/aiyou}
```
Missing `//` or `/aiyou` will cause invalid audio URLs.

### Environment Variables
```bash
# Backend
VOLCENGINE_API_KEY=your_key
VOLCENGINE_SECRET_KEY=your_secret
R2_PUBLIC_DOMAIN=https://your-domain.com
```

## Development Notes

- TTSv2.0 uses WebSocket at `wss://openspeech.bytedance.com/api/v3/tts/bidirection`
- Audio files are uploaded to Cloudflare R2 after synthesis
- Frontend uses `uni.createInnerAudioContext()` for audio playback (uni-app API)
- User identification via `X-User-Key` header (generated and stored in localStorage)
