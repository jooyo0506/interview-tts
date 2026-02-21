# Interview TTS 项目更新日志

## v1.0.0 - 基础功能

### 已完成功能

#### 1. TTS语音合成
- ✅ 短文本合成 (≤300字)
- ✅ 长文本异步合成 (≤1万字)
  - 普通版
  - 情感预测版
- ✅ 音色选择 (7种内置音色)
- ✅ 声音克隆

#### 2. AI播客
- ✅ 双人对谈播客生成
- ✅ LLM脚本改写 (豆包API)
- ✅ 多种音色组合

#### 3. 翻译功能
- ✅ 文本翻译
- ✅ 语音翻译
- ✅ 多语言支持

#### 4. 聊天功能
- ✅ AI对话
- ✅ 语音回复
- ✅ 会话管理

#### 5. 文件管理
- ✅ 音频列表
- ✅ 收藏功能
- ✅ 播放列表
- ✅ 文件上传

#### 6. 用户体系
- ✅ 用户初始化
- ✅ 用户信息管理

### 技术栈

- **后端**: Spring Boot 3.x + JPA + MySQL
- **前端**: Vue 3 + uni-app
- **存储**: R2 Cloudflare / 本地存储
- **TTS**: 火山引擎豆包TTS

### 数据库表

| 表名 | 说明 |
|------|------|
| sys_user | 用户表 |
| text_record | 文本记录 |
| audio_file | 音频文件 |
| user_collect | 用户收藏 |
| audio_cache | 音频缓存 |
| podcast | 播客表 |
| cloned_voice | 克隆声音 |
| voice_sample | 声音样本 |
| translation | 翻译记录 |
| session | 会话表 |
| chat_message | 聊天消息 |
| playlist | 播放列表 |
| playlist_audio | 播放列表音频 |

### API接口统计

- 用户接口: 3个
- 语音合成: 10个
- 文件上传: 1个
- 播客: 3个
- 声音克隆: 4个
- 翻译: 3个
- 聊天: 4个
- 播放列表: 4个

**总计: 31个API接口**

### 配置说明

```yaml
# application.yml

volcengine:
  tts:
    app-id: your_app_id
    access-token: your_access_token

# R2存储配置
r2:
  access-key-id: your_access_key
  secret-access-key: your_secret_key
  bucket-name: your_bucket
  endpoint: https://xxx.r2.cloudflarestorage.com
  public-domain: https://your-domain.com
```

### 快速开始

1. 初始化数据库
```bash
mysql -u root -p < docs/sql/init.sql
```

2. 配置火山引擎API密钥

3. 启动后端
```bash
cd backend
mvn spring-boot:run
```

4. 启动前端
```bash
cd frontend
npm run dev
```

### 版本记录

- 2026-02-22: 前端设计优化 + 账户体系 (v1.2.0)
  - 前端设计改进
    - 首页顶部导航：Logo "ViVi 2.0" + 头像/登录组合
    - 头像添加金色皇冠标识VIP状态（亮显=VIP，灰显=普通用户）
    - 字数仪表盘：调整至主标题下方，点击弹出升级提示
    - VIP卡片：添加"Pro"角标 + 暗紫色流光特效
    - 登录页添加数学验证码防刷
    - VIP页面毛玻璃风格 + 套餐呼吸灯特效
    - 新增发现页面（热门作品展示）
  - 账户体系
    - 手机号+密码/验证码登录
    - 短信验证码服务（带限流）
    - JWT token认证
    - VIP会员系统（月度/年度/终身）
    - Mock支付接口
    - 权限拦截器
  - 安全修复
    - JWT Secret 改用环境变量
    - Random 改用 SecureRandom
    - 修复限流逻辑错误
    - 添加输入验证注解

- 2026-02-22: Bug修复 (v1.1.1)
  - 前端修复
    - 修复音频播放器显示 NaN:NaN 的问题
    - 修复音频错误处理 errMsg undefined 问题
    - 修复 play() 返回值 undefined 导致 catch 报错
    - 修复 JSON.parse(undefined) 错误
  - 后端修复
    - 修复 R2 public-domain 配置缺少 // 的问题
    - 导致生成的音频 URL 无效

- 2026-02-21: 语音合成v2.0 + 移动端优化 (v1.1.0)
  - 新增 TTSv2.0 双向流式语音合成
    - 语音指令功能 (#开心/#悲伤/#四川话等)
    - 引用上文功能 (让AI理解语境)
    - 语音标签功能 (【开心】/【撒娇】等)
    - WebSocket流式音频
  - 新增长文本任务页面 (pages/tts/task.vue)
  - 新增长短文本模式切换 (pages/tts/index.vue)
  - 移动端适配优化
    - index.vue 使用 safe-area-inset-bottom
    - my.vue 改为 flex 弹性布局
    - 修复 100vh vs 100dvh 问题
    - 添加 iOS 刘海屏适配
  - 降级处理机制
    - 新增 temp_audio_url 字段存储临时URL
    - 新增 download_failed 标记下载失败状态
    - AudioGenerateResponse 新增 audioUrl 字段
  - 移除硬编码API密钥，改用环境变量
  - 新增页面 pages/tts/v2.vue
  - 修复 AudioService 编译错误
  - GitHub 安全推送

- 2026-02-20: 初始版本
  - 新增 TTSv2.0 双向流式语音合成
    - 语音指令功能 (#开心/#悲伤/#四川话等)
    - 引用上文功能 (让AI理解语境)
    - 语音标签功能 (【开心】/【撒娇】等)
    - WebSocket流式音频
  - 移动端适配优化
    - index.vue 使用 safe-area-inset-bottom
    - my.vue 改为 flex 弹性布局
    - 修复 100vh vs 100dvh 问题
    - 添加 iOS 刘海屏适配
  - 移除硬编码API密钥，改用环境变量
  - 新增页面 pages/tts/v2.vue
  - 修复 AudioService 编译错误
  - GitHub 安全推送

- 2024-02-20: 初始版本
  - 长文本异步合成
  - 情感预测版支持
  - 文件上传
  - 首页最近使用修复
