# Interview TTS API 接口文档

## 一、基础信息

- 基础URL: `http://localhost:8080`
- 认证方式: Header `X-User-Key`

## 二、通用响应格式

```json
{
  "success": true,
  "data": {},
  "message": "success"
}
```

## 三、用户接口

### 1. 初始化用户
```
POST /api/user/init
Header: X-User-Key: {userKey}
```

### 2. 获取用户信息
```
GET /api/user/profile
Header: X-User-Key: {userKey}
```

### 3. 更新用户信息
```
PUT /api/user/profile
Header: X-User-Key: {userKey}
Body: { "nickname": "xxx", "avatar": "xxx" }
```

## 四、语音合成接口

### 1. 短文本合成
```
POST /api/audio/generate
Header: X-User-Key: {userKey}
Body: {
  "rawText": "需要转换的文本",
  "voiceName": "BV001_streaming"
}
Response: {
  "audioId": 123,
  "r2Url": "https://xxx.mp3",
  "duration": 30
}
```

### 2. 长文本异步合成
```
POST /api/audio/generate-long
Header: X-User-Key: {userKey}
Body: {
  "rawText": "长文本内容(≤1万字)",
  "voiceName": "BV001_streaming",
  "useEmotion": false  // 是否使用情感预测版
}
Response: {
  "audioId": 123,
  "taskId": "xxx-xxx-xxx",
  "r2Url": null,
  "duration": 0
}
```

### 3. 查询长文本任务状态
```
GET /api/audio/task-status?audioFileId=123&taskId=xxx&useEmotion=false
Header: X-User-Key: {userKey}
Response: {
  "audioId": 123,
  "taskId": "xxx",
  "r2Url": "https://xxx.mp3",
  "audioUrl": "https://xxx.mp3",  // 降级时返回临时URL
  "duration": 3600
}
```

### 4. 获取音频详情
```
GET /api/audio/detail/{audioId}
Header: X-User-Key: {userKey}
Response: {
  "id": 123,
  "name": "音频名称",
  "r2Url": "https://xxx.mp3",
  "tempAudioUrl": "https://xxx.tmp3",
  "downloadFailed": false,
  "duration": 60,
  "createTime": "2026-02-21 10:00:00"
}
```

### 4. 获取音色列表
```
GET /api/tts/voices
```

### 5. 获取我的音频列表
```
GET /api/audio/my-list
Header: X-User-Key: {userKey}
```

### 6. 收藏音频
```
POST /api/audio/collect/{audioId}
Header: X-User-Key: {userKey}
```

### 7. 获取收藏列表
```
GET /api/audio/collect/list
Header: X-User-Key: {userKey}
```

### 8. 删除音频
```
DELETE /api/audio/{audioId}
Header: X-User-Key: {userKey}
```

### 9. 批量删除音频
```
DELETE /api/audio/batch
Header: X-User-Key: {userKey}
Body: { "ids": [1, 2, 3] }
```

### 10. 重命名音频
```
PUT /api/audio/rename/{audioId}
Header: X-User-Key: {userKey}
Body: { "name": "新名称" }
```

## 五、文件上传接口

### 1. 上传音频文件
```
POST /api/upload/audio
Header: X-User-Key: {userKey}
Content-Type: multipart/form-data
Body: file (音频文件)
Response: {
  "url": "https://xxx.mp3",
  "duration": 120,
  "fileName": "audio.mp3"
}
```

## 六、播客接口

### 1. 创建播客
```
POST /api/podcast/create
Header: X-User-Key: {userKey}
Body: {
  "title": "播客标题",
  "sourceText": "文章内容",
  "voiceA": "BV001_streaming",
  "voiceB": "BV002_streaming"
}
```

### 2. 获取播客列表
```
GET /api/podcast/list
Header: X-User-Key: {userKey}
```

### 3. 获取播客详情
```
GET /api/podcast/{id}
Header: X-User-Key: {userKey}
```

## 七、声音克隆接口

### 1. 获取克隆声音列表
```
GET /api/voice-clone/list
Header: X-User-Key: {userKey}
```

### 2. 创建克隆声音
```
POST /api/voice-clone/create
Header: X-User-Key: {userKey}
Body: {
  "name": "我的声音",
  "sampleUrls": ["https://xxx1.mp3", "https://xxx2.mp3"]
}
```

### 3. 获取克隆状态
```
GET /api/voice-clone/status/{id}
Header: X-User-Key: {userKey}
```

### 4. 删除克隆声音
```
DELETE /api/voice-clone/{id}
Header: X-User-Key: {userKey}
```

## 八、翻译接口

### 1. 文本翻译
```
POST /api/translate/text
Header: X-User-Key: {userKey}
Body: {
  "text": "要翻译的文本",
  "sourceLang": "zh",
  "targetLang": "en"
}
```

### 2. 语音翻译
```
POST /api/translate/speech
Header: X-User-Key: {userKey}
Body: {
  "text": "识别的语音文本",
  "sourceLang": "zh",
  "targetLang": "en"
}
```

### 3. 获取翻译历史
```
GET /api/translate/list
Header: X-User-Key: {userKey}
```

## 九、聊天接口

### 1. 创建会话
```
POST /api/chat/session
Header: X-User-Key: {userKey}
Body: {
  "sessionType": "chat",
  "title": "会话标题"
}
```

### 2. 发送消息
```
POST /api/chat/message
Header: X-User-Key: {userKey}
Body: {
  "sessionId": 123,
  "content": "用户消息",
  "voiceName": "BV001_streaming"
}
```

### 3. 获取会话列表
```
GET /api/chat/sessions
Header: X-User-Key: {userKey}
```

### 4. 获取会话消息
```
GET /api/chat/messages/{sessionId}
Header: X-User-Key: {userKey}
```

## 十、播放列表接口

### 1. 创建播放列表
```
POST /api/playlist/create
Header: X-User-Key: {userKey}
Body: {
  "name": "列表名称",
  "description": "描述"
}
```

### 2. 获取播放列表
```
GET /api/playlist/list
Header: X-User-Key: {userKey}
```

### 3. 添加音频到播放列表
```
POST /api/playlist/{playlistId}/add
Header: X-User-Key: {userKey}
Body: { "audioFileId": 123 }
```

### 4. 从播放列表移除
```
DELETE /api/playlist/{playlistId}/remove/{audioFileId}
Header: X-User-Key: {userKey}
```

## 十一、音色列表

| 音色ID | 名称 | 性别 |
|--------|------|------|
| BV001_streaming | 通用女声 | Female |
| BV002_streaming | 通用男声 | Male |
| BV700_streaming | 灿灿 | Female |
| BV102_streaming | 儒雅青年 | Male |
| BV113_streaming | 甜宠少御 | Female |
| BV033_streaming | 温柔小哥 | Male |
| BV034_streaming | 知性姐姐 | Female |

## 十二、TTSv2 双向流式语音合成

### 1. 获取TTSv2音色列表
```
GET /api/tts/v2/voices
Header: X-User-Key: {userKey}
Response: {
  "voices": [
    {
      "voiceId": "BV001_streaming",
      "name": "通用女声",
      "gender": "female"
    }
  ]
}
```

### 2. WebSocket流式合成
```
ws://localhost:8080/api/tts/v2/stream
Header: X-User-Key: {userKey}
Query: text=文本内容&voiceName=BV001_streaming&referenceText=参考文本&voiceInstruction=语音指令
```

消息格式：
- 客户端发送:
```json
{
  "event": "start",
  "text": "需要转换的文本",
  "voiceName": "BV001_streaming",
  "referenceText": "参考文本(可选)",
  "voiceInstruction": "#开心 (可选，语音指令)"
}
```

- 服务端返回音频流:
```json
{
  "event": "audio",
  "audio": "base64编码的音频数据"
}
```

- 结束消息:
```json
{
  "event": "finish",
  "duration": 30
}
```

### 3. 语音指令列表
| 指令 | 效果 |
|------|------|
| #开心 | 开心情感 |
| #悲伤 | 悲伤情感 |
| #中性 | 中性情感 |
| #严肃 | 严肃情感 |
| #兴奋 | 兴奋情感 |
| #温柔 | 温柔情感 |
| #礼貌 | 礼貌语气 |
| #新闻 | 新闻播报 |
| #客服 | 客服语气 |
| #四川话 | 四川方言 |
| #东北话 | 东北方言 |
| #粤语 | 粤语方言 |

### 4. 语音标签（可在文本中使用）
| 标签 | 效果 |
|------|------|
| 【开心】 | 开心情感 |
| 【悲伤】 | 悲伤情感 |
| 【撒娇】 | 撒娇情感 |
| 【严肃】 | 严肃情感 |
| 【兴奋】 | 兴奋情感 |
| 【温柔】 | 温柔情感 |

## 十三、语言代码

| 代码 | 语言 |
|------|------|
| zh | 中文 |
| en | 英语 |
| ja | 日语 |
| ko | 韩语 |
| fr | 法语 |
| de | 德语 |
| es | 西班牙语 |
