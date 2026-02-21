<template>
  <view class="tts-page">
    <!-- 动态声波背景 -->
    <view class="bg-visualizer">
      <view class="wave-bg" v-for="i in 5" :key="i" :class="'wave-' + i"></view>
      <view class="noise-overlay"></view>
    </view>

    <!-- 顶部导航 -->
    <view class="nav-bar">
      <view class="nav-left">
        <view class="back-btn" @click="goBack">
          <text class="back-icon">←</text>
        </view>
      </view>
      <view class="nav-center">
        <text class="nav-title">文字转语音</text>
      </view>
      <view class="nav-right"></view>
    </view>

    <!-- 模式切换 - 胶囊设计 -->
    <view class="mode-selector">
      <view
        class="mode-pill"
        :class="{ active: textMode === 'short' }"
        @click="switchMode('short')"
      >
        <text class="mode-label">短文本</text>
        <text class="mode-limit">300字</text>
        <view class="pill-indicator" v-if="textMode === 'short'"></view>
      </view>
      <view
        class="mode-pill"
        :class="{ active: textMode === 'long' }"
        @click="switchMode('long')"
      >
        <text class="mode-label">长文本</text>
        <text class="mode-limit">万字</text>
        <view class="pill-indicator" v-if="textMode === 'long'"></view>
      </view>
    </view>

    <!-- 主内容区 -->
    <view class="main-content">
      <!-- 文本输入卡片 -->
      <view class="input-card">
        <!-- 情感预测开关（长文本模式） -->
        <view v-if="textMode === 'long'" class="emotion-bar">
          <view class="emotion-info">
            <text class="emotion-icon">✨</text>
            <text class="emotion-text">情感预测</text>
          </view>
          <switch
            :checked="useEmotion"
            @change="useEmotion = !useEmotion"
            class="emotion-switch"
            color="#f59e0b"
          />
        </view>

        <!-- 输入区域 -->
        <view class="textarea-wrapper">
          <textarea
            v-model="rawText"
            class="text-input"
            :placeholder="placeholderText"
            :maxlength="maxLength"
            cursor-spacing="120"
          />
          <view class="char-indicator">
            <text :class="['char-count', { warning: rawText.length > maxLength * 0.9 }]">
              {{ rawText.length }}
            </text>
            <text class="char-separator">/</text>
            <text class="char-total">{{ maxLength }}</text>
          </view>
        </view>

        <!-- 快捷标签 -->
        <view class="quick-bar" v-if="textMode === 'short'">
          <view
            v-for="tag in quickTags"
            :key="tag"
            class="quick-chip"
            @click="addQuickText(tag)"
          >
            <text>{{ tag }}</text>
          </view>
        </view>

        <!-- 长文本提示 -->
        <view v-if="textMode === 'long'" class="tips-row">
          <view class="tip-badge">
            <text class="tip-icon">📚</text>
            <text>支持万字长文</text>
          </view>
          <view class="tip-badge">
            <text class="tip-icon">⏱</text>
            <text>后台处理</text>
          </view>
        </view>
      </view>

      <!-- 音色选择 -->
      <view class="voice-card">
        <view class="card-header">
          <text class="card-title">选择音色</text>
          <text class="card-subtitle">共 {{ voices.length }} 种</text>
        </view>

        <scroll-view class="voice-scroll" scroll-x enhanced :show-scrollbar="false">
          <view class="voice-grid">
            <view
              v-for="(voice, index) in voices"
              :key="voice.name"
              class="voice-chip"
              :class="{ active: selectedVoice === voice.name }"
              @click="selectVoice(voice.name)"
              :style="{ animationDelay: index * 0.05 + 's' }"
            >
              <view class="voice-avatar" :class="voice.gender === 'Female' ? 'female' : 'male'">
                <text>{{ voice.shortName?.charAt(0) || '声' }}</text>
              </view>
              <view class="voice-info">
                <text class="voice-name">{{ voice.shortName }}</text>
                <text class="voice-gender">{{ voice.gender === 'Female' ? '女声' : '男声' }}</text>
              </view>
              <view class="voice-check" v-if="selectedVoice === voice.name">✓</view>
            </view>
          </view>
        </scroll-view>
      </view>

      <!-- 我的声音 -->
      <view v-if="clonedVoices.length > 0" class="voice-card cloned">
        <view class="card-header">
          <text class="card-title">我的声音</text>
          <text class="card-subtitle">克隆音色</text>
        </view>

        <scroll-view class="voice-scroll" scroll-x :show-scrollbar="false">
          <view class="voice-grid">
            <view
              v-for="voice in clonedVoices"
              :key="voice.id"
              class="voice-chip cloned"
              :class="{ active: selectedVoice === voice.voiceId }"
              @click="selectVoice(voice.voiceId)"
            >
              <view class="voice-avatar custom">
                <text>我的</text>
              </view>
              <view class="voice-info">
                <text class="voice-name">{{ voice.name }}</text>
                <text class="voice-gender">自定义</text>
              </view>
              <view class="voice-check" v-if="selectedVoice === voice.voiceId">✓</view>
            </view>
          </view>
        </scroll-view>
      </view>
    </view>

    <!-- 底部生成按钮 -->
    <view class="bottom-bar">
      <view class="generate-btn" :class="{ disabled: !canGenerate || loading }" @click="handleGenerate">
        <view class="btn-content">
          <text class="btn-icon">{{ loading ? '⏳' : '🔊' }}</text>
          <text class="btn-text">{{ btnText }}</text>
        </view>
        <view class="btn-glow" v-if="canGenerate && !loading"></view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getUserKey, initUser, getVoices, generateAudio, generateLongAudio } from '@/api'
import { getVoiceCloneList } from '@/api/voice'

const textMode = ref('short')
const rawText = ref('')
const selectedVoice = ref('BV001_streaming')
const voices = ref([])
const clonedVoices = ref([])
const loading = ref(false)
const taskId = ref('')
const textRecordId = ref('')
const useEmotion = ref(false)

const maxLength = computed(() => textMode.value === 'short' ? 300 : 10000)

const quickTags = ['文章', '新闻', '小说', '教程', '故事']

const placeholderText = computed(() => {
  if (textMode.value === 'short') {
    return '输入想转语音的文字...\n支持故事、新闻、文章等多种类型'
  }
  return '粘贴或输入长文本内容...\n支持小说、文档、文章等万字长文'
})

const btnText = computed(() => {
  if (loading.value) {
    return textMode.value === 'long' ? 'AI正在合成中...' : '生成中...'
  }
  return '立即生成语音'
})

const canGenerate = computed(() => {
  if (!selectedVoice.value) return false
  if (textMode.value === 'short') {
    return rawText.value.trim().length > 0 && rawText.value.length <= 300
  }
  return rawText.value.trim().length > 0
})

onMounted(async () => {
  const userKey = getUserKey()
  await initUser(userKey)

  try {
    const res = await getVoices()
    voices.value = res.data || []
    if (voices.value.length > 0 && !selectedVoice.value) {
      selectedVoice.value = voices.value[0].name
    }
  } catch (e) {
    console.error('获取音色失败:', e)
  }

  try {
    const res = await getVoiceCloneList()
    clonedVoices.value = (res.data || []).filter(v => v.status === 'COMPLETED')
  } catch (e) {
    console.error('获取克隆声音失败:', e)
  }
})

function switchMode(mode) {
  textMode.value = mode
  rawText.value = ''
}

function addQuickText(tag) {
  const sampleTexts = {
    '文章': '今天分享一篇关于产品设计的文章。好的产品设计应该以用户为中心，关注用户需求，提供简洁易用的体验。',
    '新闻': '为您播报最新新闻。科技领域传来重大消息，人工智能技术正在快速发展，为各行各业带来新的机遇。',
    '小说': '开始为您讲述一段小说故事。在一个遥远的星球上，有一个神秘的文明，他们拥有超乎寻常的科技力量。',
    '教程': '欢迎学习本次教程。本课程将帮助您掌握核心技能，提升专业能力。',
    '故事': '分享一个温暖的小故事。从前有一只小鸟，它梦想着能飞上天空，看看更广阔的世界。'
  }
  const newText = sampleTexts[tag] || ''
  if (textMode.value === 'short' && rawText.value.length + newText.length > 300) {
    uni.showToast({ title: '已超过300字限制', icon: 'none' })
    return
  }
  rawText.value += newText
}

function selectVoice(name) {
  selectedVoice.value = name
}

function goBack() {
  uni.navigateBack()
}

async function handleGenerate() {
  if (!canGenerate.value || loading.value) return

  if (textMode.value === 'short' && rawText.value.length > 300) {
    uni.showToast({ title: '短文本不能超过300字', icon: 'none' })
    return
  }

  loading.value = true

  try {
    let res

    if (textMode.value === 'long') {
      res = await generateLongAudio(rawText.value, selectedVoice.value, useEmotion.value)

      if (!res.data || !res.data.taskId) {
        uni.showToast({ title: '任务创建失败', icon: 'none' })
        loading.value = false
        return
      }

      taskId.value = res.data.taskId
      textRecordId.value = res.data.audioId || ''

      setTimeout(() => {
        uni.navigateTo({
          url: `/pages/tts/task?id=${textRecordId.value}&taskId=${taskId.value}&text=${encodeURIComponent(rawText.value.substring(0, 500))}&emotion=${useEmotion.value}`
        })
      }, 300)
      loading.value = false
      return
    } else {
      res = await generateAudio(rawText.value, selectedVoice.value)
    }

    if (!res.data || !res.data.audioId) {
      uni.showToast({ title: '生成失败', icon: 'none' })
      loading.value = false
      return
    }

    if (!res.data.r2Url) {
      uni.showToast({ title: '音频生成失败', icon: 'none' })
      loading.value = false
      return
    }

    uni.showToast({ title: '生成成功', icon: 'success' })

    setTimeout(() => {
      const title = rawText.value.substring(0, 20) + (rawText.value.length > 20 ? '...' : '')
      uni.navigateTo({
        url: `/pages/play/play?id=${res.data.audioId}&url=${encodeURIComponent(res.data.r2Url)}&text=${encodeURIComponent(rawText.value.substring(0, 1000))}&title=${encodeURIComponent(title)}`
      })
    }, 500)
  } catch (e) {
    console.error('生成失败:', e)
    uni.showToast({
      title: textMode.value === 'long' ? '服务暂未开放' : '生成失败',
      icon: 'none'
    })
  } finally {
    if (textMode.value !== 'long') {
      loading.value = false
    }
  }
}
</script>

<style lang="scss" scoped>
// 配色系统
$bg-primary: #0a0a0f;
$bg-secondary: #14141a;
$bg-card: rgba(255, 255, 255, 0.03);
$bg-card-hover: rgba(255, 255, 255, 0.06);

$accent-primary: #f59e0b;
$accent-secondary: #fbbf24;
$accent-glow: rgba(245, 158, 11, 0.4);

$text-primary: #ffffff;
$text-secondary: rgba(255, 255, 255, 0.6);
$text-tertiary: rgba(255, 255, 255, 0.4);

$border-subtle: rgba(255, 255, 255, 0.08);
$border-active: rgba(245, 158, 11, 0.5);

$female-color: #ec4899;
$male-color: #3b82f6;

.tts-page {
  min-height: 100vh;
  background: $bg-primary;
  position: relative;
  overflow: hidden;
}

// 动态声波背景
.bg-visualizer {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 0;
  overflow: hidden;
}

.wave-bg {
  position: absolute;
  bottom: -50%;
  left: 50%;
  transform: translateX(-50%);
  width: 200vw;
  height: 100%;
  border-radius: 50%;
  opacity: 0.15;
  animation: waveFloat 20s ease-in-out infinite;

  &.wave-1 {
    background: radial-gradient(ellipse at center, $accent-primary 0%, transparent 70%);
    animation-delay: 0s;
  }
  &.wave-2 {
    background: radial-gradient(ellipse at center, $accent-secondary 0%, transparent 70%);
    animation-delay: -5s;
    opacity: 0.1;
  }
  &.wave-3 {
    background: radial-gradient(ellipse at center, $accent-glow 0%, transparent 70%);
    animation-delay: -10s;
  }
  &.wave-4, &.wave-5 {
    background: radial-gradient(ellipse at center, rgba(255,255,255,0.1) 0%, transparent 70%);
    animation-delay: -15s;
  }
}

@keyframes waveFloat {
  0%, 100% {
    transform: translateX(-50%) scale(1) translateY(0);
  }
  50% {
    transform: translateX(-50%) scale(1.1) translateY(-5%);
  }
}

.noise-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 256 256' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noise'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='4' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noise)'/%3E%3C/svg%3E");
  opacity: 0.03;
  pointer-events: none;
}

// 导航栏
.nav-bar {
  position: relative;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 50px 20px 20px;
}

.nav-left, .nav-right {
  width: 60px;
}

.nav-center {
  flex: 1;
  text-align: center;
}

.back-btn {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  background: $bg-card;
  border: 1px solid $border-subtle;
  transition: all 0.3s ease;

  &:active {
    background: $bg-card-hover;
  }
}

.back-icon {
  font-size: 20px;
  color: $text-primary;
}

.nav-title {
  font-size: 20px;
  font-weight: 600;
  color: $text-primary;
  letter-spacing: 2px;
}

// 模式选择器
.mode-selector {
  position: relative;
  z-index: 10;
  display: flex;
  justify-content: center;
  gap: 8px;
  padding: 0 20px 20px;
}

.mode-pill {
  position: relative;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 12px 24px;
  border-radius: 24px;
  background: $bg-card;
  border: 1px solid $border-subtle;
  transition: all 0.3s ease;
  overflow: hidden;

  &.active {
    background: rgba(245, 158, 11, 0.15);
    border-color: $border-active;

    .mode-label {
      color: $accent-primary;
    }
    .mode-limit {
      color: $accent-primary;
      opacity: 0.7;
    }
  }
}

.mode-label {
  font-size: 15px;
  font-weight: 500;
  color: $text-secondary;
  transition: color 0.3s ease;
}

.mode-limit {
  font-size: 11px;
  color: $text-tertiary;
  transition: color 0.3s ease;
}

.pill-indicator {
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 30px;
  height: 3px;
  background: $accent-primary;
  border-radius: 2px;
  animation: slideUp 0.3s ease;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateX(-50%) scaleX(0);
  }
  to {
    opacity: 1;
    transform: translateX(-50%) scaleX(1);
  }
}

// 主内容区
.main-content {
  position: relative;
  z-index: 10;
  flex: 1;
  padding: 0 16px 140px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

// 输入卡片
.input-card {
  background: $bg-card;
  border: 1px solid $border-subtle;
  border-radius: 20px;
  padding: 20px;
  backdrop-filter: blur(20px);
}

.emotion-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 16px;
  margin-bottom: 16px;
  border-bottom: 1px solid $border-subtle;
}

.emotion-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.emotion-icon {
  font-size: 18px;
}

.emotion-text {
  font-size: 14px;
  color: $text-secondary;
}

.emotion-switch {
  transform: scale(0.8);
  transform-origin: right center;
}

.textarea-wrapper {
  position: relative;
}

.text-input {
  width: 100%;
  min-height: 160px;
  font-size: 16px;
  line-height: 1.8;
  color: $text-primary;
  background: transparent;
  border: none;
  resize: none;

  &::placeholder {
    color: $text-tertiary;
  }
}

.char-indicator {
  position: absolute;
  bottom: 8px;
  right: 0;
  display: flex;
  align-items: baseline;
  gap: 2px;
}

.char-count {
  font-size: 14px;
  font-weight: 600;
  color: $accent-primary;

  &.warning {
    color: #ef4444;
    animation: pulse 1s ease infinite;
  }
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.char-separator, .char-total {
  font-size: 12px;
  color: $text-tertiary;
}

.quick-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid $border-subtle;
}

.quick-chip {
  padding: 8px 16px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid $border-subtle;
  transition: all 0.2s ease;

  text {
    font-size: 13px;
    color: $text-secondary;
  }

  &:active {
    background: rgba(245, 158, 11, 0.2);
    border-color: $border-active;
    transform: scale(0.95);
  }
}

.tips-row {
  display: flex;
  gap: 12px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid $border-subtle;
}

.tip-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid $border-subtle;

  text {
    font-size: 12px;
    color: $text-tertiary;
  }
}

.tip-icon {
  font-size: 12px;
}

// 音色卡片
.voice-card {
  background: $bg-card;
  border: 1px solid $border-subtle;
  border-radius: 20px;
  padding: 20px;
  backdrop-filter: blur(20px);

  &.cloned {
    border-color: rgba(236, 72, 153, 0.3);
  }
}

.card-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 16px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: $text-primary;
}

.card-subtitle {
  font-size: 12px;
  color: $text-tertiary;
}

.voice-scroll {
  margin: 0 -20px;
  padding: 0 20px;
}

.voice-grid {
  display: flex;
  gap: 12px;
}

.voice-chip {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid $border-subtle;
  min-width: 160px;
  transition: all 0.3s ease;
  animation: fadeInUp 0.4s ease backwards;

  &:active {
    transform: scale(0.98);
  }

  &.active {
    background: rgba(245, 158, 11, 0.12);
    border-color: $border-active;

    .voice-name {
      color: $accent-primary;
    }
  }

  &.cloned {
    border-color: rgba(236, 72, 153, 0.3);

    &.active {
      background: rgba(236, 72, 153, 0.12);
      border-color: rgba(236, 72, 153, 0.5);
    }
  }
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.voice-avatar {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 600;
  color: #fff;

  &.female {
    background: linear-gradient(135deg, $female-color 0%, #be185d 100%);
  }

  &.male {
    background: linear-gradient(135deg, $male-color 0%, #1d4ed8 100%);
  }

  &.custom {
    background: linear-gradient(135deg, #8b5cf6 0%, #6d28d9 100%);
  }
}

.voice-info {
  flex: 1;
  min-width: 0;
}

.voice-name {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: $text-primary;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.voice-gender {
  display: block;
  font-size: 11px;
  color: $text-tertiary;
  margin-top: 2px;
}

.voice-check {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: $accent-primary;
  color: #000;
  font-size: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
}

// 底部生成按钮
.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 20px;
  padding-bottom: calc(20px + env(safe-area-inset-bottom));
  background: linear-gradient(to top, $bg-primary 60%, transparent);
  z-index: 100;
}

.generate-btn {
  position: relative;
  height: 56px;
  border-radius: 28px;
  background: linear-gradient(135deg, $accent-primary 0%, $accent-secondary 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  transition: all 0.3s ease;

  &.disabled {
    background: rgba(255, 255, 255, 0.1);
    opacity: 0.6;
  }

  &:not(.disabled):active {
    transform: scale(0.98);
  }
}

.btn-content {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  gap: 10px;
}

.btn-icon {
  font-size: 20px;
}

.btn-text {
  font-size: 17px;
  font-weight: 600;
  color: #000;
  letter-spacing: 1px;
}

.btn-glow {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(90deg, transparent 0%, rgba(255,255,255,0.3) 50%, transparent 100%);
  animation: shimmer 2s ease infinite;
}

@keyframes shimmer {
  0% {
    transform: translateX(-100%);
  }
  100% {
    transform: translateX(100%);
  }
}
</style>
