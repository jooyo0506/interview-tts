<template>
  <view class="tts-v2-page">
    <!-- 顶部说话人选择 -->
    <view class="speaker-bar">
      <view class="speaker-info">
        <text class="speaker-label">说话人</text>
        <picker :value="currentSpeakerIndex" :range="speakers" range-key="name" @change="onSpeakerChange">
          <view class="speaker-select">
            <text>{{ speakers[currentSpeakerIndex]?.name || '请选择' }}</text>
            <text class="arrow">▼</text>
          </view>
        </picker>
      </view>
      <view class="speaker-hint" v-if="!speakers[currentSpeakerIndex]?.supportsEmotion">
        <text>该音色不支持情感功能</text>
      </view>
    </view>

    <!-- 模式选择 -->
    <view class="mode-selector">
      <view
        class="mode-option"
        :class="{ active: mode === 'default' }"
        @click="mode = 'default'"
      >
        <text class="mode-icon">🎭</text>
        <text class="mode-text">默认</text>
      </view>
      <view
        class="mode-option"
        :class="{ active: mode === 'voice_command' }"
        @click="mode = 'voice_command'"
      >
        <text class="mode-icon">🎤</text>
        <text class="mode-text">语音指令</text>
      </view>
      <view
        class="mode-option"
        :class="{ active: mode === 'context' }"
        @click="mode = 'context'"
      >
        <text class="mode-icon">📜</text>
        <text class="mode-text">引用上文</text>
      </view>
    </view>

    <!-- 模式说明 -->
    <view class="mode-desc" v-if="modeDescriptions[mode]">
      <text>{{ modeDescriptions[mode] }}</text>
    </view>

    <!-- 文本输入区 -->
    <view class="text-area">
      <view class="text-header">
        <text class="text-label">合成文本</text>
        <text class="text-required">*</text>
      </view>
      <view class="text-input-wrapper">
        <textarea
          v-model="text"
          class="text-input"
          placeholder="请输入要合成的文本..."
          :maxlength="10000"
          @input="onTextInput"
          @focus="onTextFocus"
        />
        <!-- 标签按钮 -->
        <view class="tag-trigger" @click="showTagPanel = true" v-if="mode === 'default'">
          <text>【】</text>
        </view>
      </view>
      <view class="char-count">
        <text>{{ text.length }} 字符</text>
        <text class="max-length">/ 10000</text>
      </view>
    </view>

    <!-- 引用上文区域 -->
    <view class="context-area" v-if="mode === 'context' || (mode === 'default' && showContext)">
      <view class="context-header">
        <text class="context-label">上文（可选）</text>
        <text class="context-hint">让AI理解语境，承接情绪</text>
      </view>
      <textarea
        v-model="contextText"
        class="context-input"
        placeholder="输入上文内容，让模型更好地理解语境..."
        :maxlength="500"
      />
    </view>

    <!-- 示例提示 -->
    <view class="examples" v-if="mode === 'voice_command'">
      <view class="examples-header">
        <text class="examples-icon">💡</text>
        <text class="examples-title">示例指令</text>
      </view>
      <view class="example-tags">
        <view class="example-tag" @click="insertCommand('#开心')">
          <text>#开心</text>
        </view>
        <view class="example-tag" @click="insertCommand('#悲伤')">
          <text>#悲伤</text>
        </view>
        <view class="example-tag" @click="insertCommand('#撒娇')">
          <text>#撒娇</text>
        </view>
        <view class="example-tag" @click="insertCommand('#四川话')">
          <text>#四川话</text>
        </view>
        <view class="example-tag" @click="insertCommand('#语速慢')">
          <text>#语速慢</text>
        </view>
        <view class="example-tag" @click="insertCommand('#温柔')">
          <text>#温柔</text>
        </view>
      </view>
    </view>

    <!-- 合成按钮 -->
    <view class="action-area">
      <button
        class="synthesize-btn"
        :class="{ loading: isGenerating }"
        :disabled="isGenerating || !text.trim()"
        @click="synthesize"
      >
        <text v-if="!isGenerating">🎵 合成试听</text>
        <text v-else>合成中...</text>
      </button>
    </view>

    <!-- 标签选择面板 -->
    <view class="tag-panel-mask" v-if="showTagPanel" @click="showTagPanel = false">
      <view class="tag-panel" @click.stop>
        <view class="tag-panel-header">
          <text class="tag-panel-title">选择标签</text>
          <view class="tag-panel-close" @click="showTagPanel = false">✕</view>
        </view>

        <scroll-view scroll-y class="tag-list">
          <!-- 情感 -->
          <view class="tag-section">
            <view class="tag-section-title">情感</view>
            <view class="tag-items">
              <view class="tag-item" @click="insertTag('【开心】')">
                <text>😊 开心</text>
              </view>
              <view class="tag-item" @click="insertTag('【悲伤】')">
                <text>😢 悲伤</text>
              </view>
              <view class="tag-item" @click="insertTag('【生气】')">
                <text>😠 生气</text>
              </view>
              <view class="tag-item" @click="insertTag('【惊讶】')">
                <text>😲 惊讶</text>
              </view>
              <view class="tag-item" @click="insertTag('【暧昧】')">
                <text>🥰 暧昧</text>
              </view>
            </view>
          </view>

          <!-- 语气 -->
          <view class="tag-section">
            <view class="tag-section-title">语气</view>
            <view class="tag-items">
              <view class="tag-item" @click="insertTag('【撒娇】')">
                <text>撒娇</text>
              </view>
              <view class="tag-item" @click="insertTag('【严肃】')">
                <text>严肃</text>
              </view>
              <view class="tag-item" @click="insertTag('【温柔】')">
                <text>温柔</text>
              </view>
              <view class="tag-item" @click="insertTag('【俏皮】')">
                <text>俏皮</text>
              </view>
              <view class="tag-item" @click="insertTag('【大声】')">
                <text>大声</text>
              </view>
              <view class="tag-item" @click="insertTag('【悄悄话】')">
                <text>悄悄话</text>
              </view>
            </view>
          </view>

          <!-- 方言 -->
          <view class="tag-section">
            <view class="tag-section-title">方言</view>
            <view class="tag-items">
              <view class="tag-item" @click="insertTag('【北京话】')">
                <text>北京话</text>
              </view>
              <view class="tag-item" @click="insertTag('【四川话】')">
                <text>四川话</text>
              </view>
              <view class="tag-item" @click="insertTag('【东北话】')">
                <text>东北话</text>
              </view>
              <view class="tag-item" @click="insertTag('【粤语】')">
                <text>粤语</text>
              </view>
            </view>
          </view>

          <!-- 动作 -->
          <view class="tag-section">
            <view class="tag-section-title">动作/状态</view>
            <view class="tag-items">
              <view class="tag-item" @click="insertTag('【站起来说】')">
                <text>站起来说</text>
              </view>
              <view class="tag-item" @click="insertTag('【轻声私语】')">
                <text>轻声私语</text>
              </view>
              <view class="tag-item" @click="insertTag('【结巴】')">
                <text>结巴</text>
              </view>
              <view class="tag-item" @click="insertTag('【哭腔】')">
                <text>哭腔</text>
              </view>
            </view>
          </view>

          <!-- 自定义输入 -->
          <view class="tag-section">
            <view class="tag-section-title">自定义</view>
            <view class="custom-input-wrapper">
              <input
                v-model="customTag"
                class="custom-input"
                placeholder="输入自定义标签..."
                @confirm="insertTag('【' + customTag + '】')"
              />
              <view class="custom-btn" @click="insertTag('【' + customTag + '】')">
                <text>添加</text>
              </view>
            </view>
          </view>
        </scroll-view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { getTtsV2Voices, synthesizeTtsV2 } from '@/api'

const speakers = ref([])
const currentSpeakerIndex = ref(0)
const mode = ref('default')
const text = ref('')
const contextText = ref('')
const isGenerating = ref(false)
const showTagPanel = ref(false)
const showContext = ref(false)
const customTag = ref('')
const cursorPosition = ref(0)

const modeDescriptions = {
  default: '在句子前添加【标签】可增强语音效果',
  voice_command: '使用#指令控制情绪、方言、语气、语速等',
  context: '输入上文内容，让AI理解语境承接情绪'
}

const currentSpeaker = computed(() => speakers.value[currentSpeakerIndex.value])

onMounted(async () => {
  await loadVoices()
})

async function loadVoices() {
  try {
    const res = await getTtsV2Voices()
    speakers.value = res.data || []
    // 默认选择第一个支持情感的音色
    const emotionIndex = speakers.value.findIndex(s => s.supportsEmotion)
    if (emotionIndex >= 0) {
      currentSpeakerIndex.value = emotionIndex
    }
  } catch (e) {
    console.error('加载音色失败:', e)
    // 使用默认音色
    speakers.value = [
      { id: 'zh_female_cancan_mars_bigtts', name: '灿灿', supportsEmotion: true }
    ]
  }
}

function onSpeakerChange(e) {
  currentSpeakerIndex.value = e.detail.value
}

function onTextInput(e) {
  const value = e.detail.value
  cursorPosition.value = e.detail.cursor
}

function onTextFocus(e) {
  cursorPosition.value = e.detail.cursor
}

function insertCommand(cmd) {
  const before = text.value.substring(0, cursorPosition.value)
  const after = text.value.substring(cursorPosition.value)
  text.value = before + cmd + after
  // 更新光标位置
  cursorPosition.value = before.length + cmd.length
}

function insertTag(tag) {
  if (!customTag.value && !tag) return

  const finalTag = tag || '【' + customTag.value + '】'
  const before = text.value.substring(0, cursorPosition.value)
  const after = text.value.substring(cursorPosition.value)
  text.value = before + finalTag + after
  cursorPosition.value = before.length + finalTag.length

  showTagPanel.value = false
  customTag.value = ''
}

async function synthesize() {
  if (!text.value.trim()) {
    uni.showToast({ title: '请输入文本', icon: 'none' })
    return
  }

  if (!currentSpeaker.value) {
    uni.showToast({ title: '请选择音色', icon: 'none' })
    return
  }

  isGenerating.value = true

  try {
    const res = await synthesizeTtsV2({
      text: text.value,
      contextText: contextText.value || null,
      voiceType: currentSpeaker.value.id,
      mode: mode.value
    })

    if (res.data && res.data.audioUrl) {
      // 跳转到播放页面
      const title = text.value.substring(0, 20) + (text.value.length > 20 ? '...' : '')
      uni.navigateTo({
        url: `/pages/play/play?url=${encodeURIComponent(res.data.audioUrl)}&text=${encodeURIComponent(text.value)}&title=${encodeURIComponent(title)}`
      })
    }
  } catch (e) {
    console.error('合成失败:', e)
    uni.showToast({
      title: e.message || '合成失败，请稍后重试',
      icon: 'none'
    })
  } finally {
    isGenerating.value = false
  }
}
</script>

<style lang="scss" scoped>
.tts-v2-page {
  min-height: 100vh;
  min-height: 100dvh;
  background: #0a0a0f;
  padding: 20px;
  padding-bottom: calc(20px + env(safe-area-inset-bottom));
}

// 说话人选择
.speaker-bar {
  margin-bottom: 20px;
}

.speaker-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.speaker-label {
  font-size: 15px;
  color: rgba(255, 255, 255, 0.7);
}

.speaker-select {
  display: flex;
  align-items: center;
  gap: 8px;
  background: rgba(255, 255, 255, 0.1);
  padding: 10px 16px;
  border-radius: 12px;
  color: #fff;
  font-size: 15px;

  .arrow {
    font-size: 10px;
    color: rgba(255, 255, 255, 0.5);
  }
}

.speaker-hint {
  margin-top: 8px;
  font-size: 12px;
  color: #f59e0b;
}

// 模式选择
.mode-selector {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
}

.mode-option {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 14px 10px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.05);
  transition: all 0.3s ease;

  &.active {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border-color: transparent;
    box-shadow: 0 4px 20px rgba(102, 126, 234, 0.4);
  }
}

.mode-icon {
  font-size: 20px;
}

.mode-text {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);

  .mode-option.active & {
    color: #fff;
  }
}

// 模式说明
.mode-desc {
  padding: 12px 16px;
  background: rgba(102, 126, 234, 0.1);
  border-radius: 10px;
  margin-bottom: 20px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.6);
}

// 文本输入区
.text-area {
  margin-bottom: 20px;
}

.text-header {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 10px;
}

.text-label {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.7);
}

.text-required {
  color: #ef4444;
  font-size: 14px;
}

.text-input-wrapper {
  position: relative;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  overflow: hidden;

  &:focus-within {
    border-color: #667eea;
  }
}

.text-input {
  width: 100%;
  min-height: 180px;
  padding: 16px;
  font-size: 15px;
  color: #fff;
  line-height: 1.6;
}

.tag-trigger {
  position: absolute;
  right: 12px;
  bottom: 12px;
  padding: 6px 12px;
  background: rgba(102, 126, 234, 0.2);
  border-radius: 8px;
  font-size: 13px;
  color: #667eea;
}

.char-count {
  display: flex;
  justify-content: flex-end;
  gap: 4px;
  margin-top: 8px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.4);

  .max-length {
    color: rgba(255, 255, 255, 0.2);
  }
}

// 引用上文
.context-area {
  margin-bottom: 20px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.03);
  border-radius: 14px;
}

.context-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.context-label {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.7);
}

.context-hint {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.4);
}

.context-input {
  width: 100%;
  min-height: 80px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 10px;
  font-size: 14px;
  color: #fff;
  line-height: 1.5;
}

// 示例
.examples {
  margin-bottom: 20px;
  padding: 16px;
  background: rgba(245, 158, 11, 0.1);
  border-radius: 14px;
}

.examples-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.examples-icon {
  font-size: 14px;
}

.examples-title {
  font-size: 13px;
  color: #f59e0b;
}

.example-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.example-tag {
  padding: 6px 12px;
  background: rgba(245, 158, 11, 0.15);
  border-radius: 16px;
  font-size: 12px;
  color: #f59e0b;
}

// 合成按钮
.action-area {
  margin-top: 30px;
}

.synthesize-btn {
  width: 100%;
  height: 54px;
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
  border-radius: 27px;
  border: none;
  font-size: 16px;
  font-weight: 600;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;

  &:disabled {
    opacity: 0.6;
  }

  &.loading {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  }
}

// 标签面板
.tag-panel-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: flex-end;
  z-index: 999;
}

.tag-panel {
  width: 100%;
  max-height: 70vh;
  background: #1a1a2e;
  border-radius: 24px 24px 0 0;
  overflow: hidden;
}

.tag-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.tag-panel-title {
  font-size: 17px;
  font-weight: 600;
  color: #fff;
}

.tag-panel-close {
  width: 28px;
  height: 28px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.6);
  font-size: 14px;
}

.tag-list {
  max-height: calc(70vh - 70px);
  padding: 16px 24px;
}

.tag-section {
  margin-bottom: 20px;
}

.tag-section-title {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.5);
  margin-bottom: 12px;
}

.tag-items {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.tag-item {
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 20px;
  font-size: 14px;
  color: #fff;

  &:active {
    background: rgba(102, 126, 234, 0.3);
  }
}

.custom-input-wrapper {
  display: flex;
  gap: 10px;
}

.custom-input {
  flex: 1;
  height: 40px;
  padding: 0 14px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 20px;
  font-size: 14px;
  color: #fff;

  &::placeholder {
    color: rgba(255, 255, 255, 0.3);
  }
}

.custom-btn {
  padding: 0 20px;
  background: #667eea;
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  color: #fff;
}
</style>
