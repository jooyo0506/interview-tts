<template>
  <view class="page">

    <!-- ── 顶部操作行 ── -->
    <view class="topbar">
      <!-- 发音人选择 -->
      <picker :value="currentSpeakerIndex" :range="speakers" range-key="name" @change="onSpeakerChange">
        <view class="chip chip--speaker">
          <view class="chip__avatar">
            <text class="chip__avatar-icon">👩</text>
          </view>
          <text class="chip__label">{{ speakers[currentSpeakerIndex]?.name || '请选择音色' }}</text>
          <text class="chip__caret">›</text>
        </view>
      </picker>

      <view class="spacer" />

      <!-- 发音风格按钮 -->
      <view class="chip chip--mode" @click="showModePopover = !showModePopover">
        <view class="chip__badge" :style="{ background: currentModeInfo.color }">
          <text class="chip__badge-icon">{{ currentModeInfo.icon }}</text>
        </view>
        <text class="chip__label chip__label--mode">{{ currentModeInfo.label }}</text>
        <text class="chip__caret" :class="{ 'chip__caret--open': showModePopover }">›</text>
      </view>
    </view>

    <!-- Popover 遮罩 -->
    <view class="overlay" v-if="showModePopover" @click="showModePopover = false" />

    <!-- Popover 菜单 -->
    <view class="popover" v-if="showModePopover">
      <view class="popover__arrow" />
      <view
        v-for="item in modeOptions"
        :key="item.value"
        class="popover__item"
        :class="{ 'popover__item--active': mode === item.value }"
        @click="selectMode(item.value)"
      >
        <view class="popover__icon-wrap" :style="{ background: item.color }">
          <text class="popover__icon">{{ item.icon }}</text>
        </view>
        <view class="popover__copy">
          <text class="popover__title">{{ item.label }}</text>
          <text class="popover__desc">{{ item.desc }}</text>
        </view>
        <view class="popover__check" v-if="mode === item.value">
          <text class="popover__check-icon">✓</text>
        </view>
      </view>
    </view>

    <!-- ── 行内指令输入条 ── -->
    <view class="command-bar" v-if="mode !== 'default'">
      <view class="command-bar__indicator" :style="{ background: currentModeInfo.color }" />
      <text class="command-bar__bracket">&lt;</text>
      <input
        v-if="mode === 'voice_command'"
        class="command-bar__input"
        v-model="voiceCommand"
        placeholder="输入本次说话的情绪、方言、语气、语速等"
        placeholder-style="color: #C4B5FD; font-size: 13px;"
      />
      <input
        v-if="mode === 'context'"
        class="command-bar__input"
        v-model="contextText"
        placeholder="输入上文（只引用不合成），模型会承接语境情绪"
        placeholder-style="color: #C4B5FD; font-size: 13px;"
      />
      <text class="command-bar__bracket">&gt;</text>
    </view>

    <!-- ── 主文本区 ── -->
    <view class="editor-card">
      <textarea
        v-model="text"
        class="editor-card__textarea"
        placeholder="在这里输入要合成的文字..."
        placeholder-style="color: #D1D5DB; font-size: 15px; line-height: 1.8;"
        :maxlength="500"
        @input="onTextInput"
        @focus="onTextFocus"
      />

      <!-- 工具栏 -->
      <view class="editor-card__toolbar">
        <view class="spacer" />
        <text class="char-hint">{{ text.length }}<text class="char-hint__max"> / 500</text></text>
      </view>
    </view>

    <!-- ── 音频播放器 ── -->
    <view class="player" v-if="audioUrl">
      <view class="player__play" @click="togglePlay">
        <text class="player__play-icon">{{ isPlaying ? '⏸' : '▶' }}</text>
      </view>
      <view class="player__body">
        <text class="player__title">{{ audioTitle }}</text>
        <view class="player__track" @click="seekAudio">
          <view class="player__fill" :style="{ width: progressPercent + '%' }" />
          <view class="player__thumb" :style="{ left: progressPercent + '%' }" />
        </view>
        <text class="player__time">{{ formatTime(currentTime) }} · {{ formatTime(duration) }}</text>
      </view>
    </view>

    <!-- 音频加载失败提示 -->
    <view class="player player--error" v-if="audioError">
      <view class="player__error-icon">⚠️</view>
      <text class="player__error-text">{{ audioError }}</text>
      <view class="player__error-btn" @click="audioError = ''; audioUrl = ''">
        <text>关闭</text>
      </view>
    </view>

    <!-- ── 底部悬浮操作栏 ── -->
    <view class="fab-bar">
      <button
        class="fab-btn"
        :class="{ 'fab-btn--loading': isGenerating }"
        :disabled="isGenerating || !text.trim()"
        @click="synthesize"
      >
        <text class="fab-btn__icon">{{ isGenerating ? '⏳' : '▶' }}</text>
        <text class="fab-btn__label">{{ isGenerating ? '合成中…' : '合成试听' }}</text>
      </button>
      <view class="fab-bar__meta">
        <text class="fab-bar__count">{{ text.length }}</text>
        <text class="fab-bar__unit"> 字</text>
      </view>
    </view>

  </view>
</template>


<script setup>
import { ref, onMounted, computed, onUnmounted } from 'vue'
import { getTtsV2Voices, synthesizeTtsV2 } from '@/api'

const speakers = ref([])
const currentSpeakerIndex = ref(0)
const mode = ref('default')
const text = ref('')
const contextText = ref('')
const voiceCommand = ref('')
const isGenerating = ref(false)
const showTagPanel = ref(false)
const showModePopover = ref(false)
const customTag = ref('')
const cursorPosition = ref(0)

const audioUrl = ref('')
const audioTitle = ref('')
const isPlaying = ref(false)
const currentTime = ref(0)
const duration = ref(0)
const audioError = ref('')
let audioElement = null

const modeOptions = [
  {
    value: 'default',
    label: '默认',
    icon: '≋',
    color: '#7C3AED',
    desc: '支持在句子前插入情感标签增强效果'
  },
  {
    value: 'voice_command',
    label: '语音指令',
    icon: '◎',
    color: '#059669',
    desc: '自由控制情绪、方言、语气、语速'
  },
  {
    value: 'context',
    label: '引入上文',
    icon: '❝',
    color: '#2563EB',
    desc: '输入上文让模型理解语境并承接情绪'
  }
]

const tagGroups = [
  {
    title: '情感',
    tags: [
      { label: '开心', emoji: '😊', value: '【开心】' },
      { label: '悲伤', emoji: '😢', value: '【悲伤】' },
      { label: '生气', emoji: '😠', value: '【生气】' },
      { label: '惊讶', emoji: '😲', value: '【惊讶】' },
      { label: '暧昧', emoji: '🥰', value: '【暧昧】' }
    ]
  },
  {
    title: '语气',
    tags: [
      { label: '撒娇', value: '【撒娇】' },
      { label: '严肃', value: '【严肃】' },
      { label: '温柔', value: '【温柔】' },
      { label: '俏皮', value: '【俏皮】' },
      { label: '大声', value: '【大声】' },
      { label: '悄悄话', value: '【悄悄话】' }
    ]
  },
  {
    title: '方言',
    tags: [
      { label: '北京话', value: '【北京话】' },
      { label: '四川话', value: '【四川话】' },
      { label: '东北话', value: '【东北话】' },
      { label: '粤语', value: '【粤语】' }
    ]
  },
  {
    title: '动作',
    tags: [
      { label: '站起来说', value: '【站起来说】' },
      { label: '轻声私语', value: '【轻声私语】' },
      { label: '结巴', value: '【结巴】' },
      { label: '哭腔', value: '【哭腔】' }
    ]
  }
]

const currentModeInfo = computed(() => modeOptions.find(m => m.value === mode.value) || modeOptions[0])
const progressPercent = computed(() => duration.value ? (currentTime.value / duration.value) * 100 : 0)
const currentSpeaker = computed(() => speakers.value[currentSpeakerIndex.value])

onMounted(async () => { await loadVoices() })

onUnmounted(() => {
  if (audioElement) { audioElement.pause(); audioElement = null }
})

async function loadVoices() {
  try {
    const res = await getTtsV2Voices()
    speakers.value = res.data || []
    const idx = speakers.value.findIndex(s => s.supportsEmotion)
    if (idx >= 0) currentSpeakerIndex.value = idx
  } catch (e) {
    console.error('加载音色失败:', e)
    speakers.value = [{ id: 'zh_female_cancan_mars_bigtts', name: '灿灿', supportsEmotion: true }]
  }
}

function onSpeakerChange(e) { currentSpeakerIndex.value = e.detail.value }
function onTextInput(e) { cursorPosition.value = e.detail.cursor }
function onTextFocus(e) { cursorPosition.value = e.detail.cursor }
function selectMode(value) { mode.value = value; showModePopover.value = false }

function insertTag(tag) {
  if (!tag && !customTag.value) return
  const finalTag = tag || `【${customTag.value}】`
  const before = text.value.substring(0, cursorPosition.value)
  const after = text.value.substring(cursorPosition.value)
  const space = before && !before.endsWith(' ') ? ' ' : ''
  text.value = before + space + finalTag + ' ' + after
  cursorPosition.value = before.length + space.length + finalTag.length + 1
  showTagPanel.value = false
  customTag.value = ''
}

function formatTime(s) {
  if (isNaN(s) || s <= 0) return '0:00'
  const m = Math.floor(s / 60)
  return `${m}:${Math.floor(s % 60).toString().padStart(2, '0')}`
}

function initAudio() {
  if (audioElement) return
  audioElement = uni.createInnerAudioContext()
  audioElement.onPlay(() => { isPlaying.value = true })
  audioElement.onPause(() => { isPlaying.value = false })
  audioElement.onEnded(() => { isPlaying.value = false; currentTime.value = 0 })
  audioElement.onTimeUpdate(() => {
    currentTime.value = audioElement.currentTime
    const dur = audioElement.duration
    // 防止 NaN
    duration.value = isNaN(dur) ? 0 : dur
  })
  // 音频加载错误处理
  audioElement.onError((err) => {
    console.error('音频加载失败:', err)
    audioError.value = err.errMsg || '音频加载失败'
    isPlaying.value = false
  })
}

function togglePlay() {
  if (!audioElement || !audioUrl.value) return
  isPlaying.value ? audioElement.pause() : audioElement.play()
}

function seekAudio(e) {
  if (!audioElement || !duration.value) return
  const rect = e.currentTarget.getBoundingClientRect()
  audioElement.seek(((e.touches[0].clientX - rect.left) / rect.width) * duration.value)
}

async function synthesize() {
  if (!text.value.trim()) return uni.showToast({ title: '请输入文本', icon: 'none' })
  if (!currentSpeaker.value) return uni.showToast({ title: '请选择音色', icon: 'none' })

  // 重置状态
  audioError.value = ''
  isGenerating.value = true

  try {
    const finalText = mode.value === 'voice_command' && voiceCommand.value.trim()
      ? `#${voiceCommand.value.trim()} ${text.value}`
      : text.value

    const res = await synthesizeTtsV2({
      text: finalText,
      contextText: mode.value === 'context' ? (contextText.value || null) : null,
      voiceType: currentSpeaker.value.id,
      mode: mode.value
    })

    if (res.data?.audioUrl) {
      audioUrl.value = res.data.audioUrl
      audioTitle.value = text.value.substring(0, 20) + (text.value.length > 20 ? '…' : '')
      initAudio()
      audioElement.src = audioUrl.value
      // 尝试播放并处理可能的加载错误
      audioElement.play().catch(err => {
        console.error('播放失败:', err)
        audioError.value = '音频加载失败，请重试'
      })
      uni.showToast({ title: '合成成功', icon: 'success' })
    } else {
      audioError.value = '合成失败，未返回音频'
    }
  } catch (e) {
    console.error('合成失败:', e)
    audioError.value = e.message || '合成失败，请稍后重试'
    uni.showToast({ title: e.message || '合成失败，请稍后重试', icon: 'none' })
  } finally {
    isGenerating.value = false
  }
}
</script>


<style lang="scss" scoped>
// ─── 设计令牌 ────────────────────────────────────────
// 主色：饱和蓝紫
$primary:      #5B50F0;
$primary-dark: #4338CA;
$primary-glow: rgba(91, 80, 240, 0.32);

// 页面背景：深邃的蓝紫渐变 — 给整体注入"氛围感"
$bg-from: #1E1B4B;   // 深靛蓝
$bg-to:   #312E81;   // 深紫

// 卡片：半透明磨砂玻璃效果，浮在背景之上
$card-bg:     rgba(255, 255, 255, 0.10);
$card-border: rgba(255, 255, 255, 0.14);
$card-shadow: 0 8px 32px rgba(0, 0, 0, 0.28), inset 0 1px 0 rgba(255,255,255,0.15);

// 编辑器：更亮一点的磨砂，突出输入区域
$editor-bg:     rgba(255, 255, 255, 0.92);
$editor-border: rgba(255, 255, 255, 0.30);

// 文字
$text-on-dark:    #F1F0FF;   // 主要文字（亮）
$text-muted:      rgba(220, 218, 255, 0.60);  // 次要/提示文字
$text-on-light:   #1A1754;   // 亮色区域文字（编辑器内）
$text-light-muted:#6B65A0;   // 亮色区域次要文字

// 分割线 / 边框（暗色区）
$divider: rgba(255, 255, 255, 0.10);

$radius-pill: 999px;
$radius-card: 18px;
$radius-sm:   12px;

// ─── 页面容器 ────────────────────────────────────────
.page {
  min-height: 100vh;
  background: linear-gradient(160deg, $bg-from 0%, $bg-to 100%);
  padding: 20px 16px 110px;
  position: relative;

  // 顶部装饰光晕
  &::before {
    content: '';
    position: fixed;
    top: -80px;
    right: -60px;
    width: 260px;
    height: 260px;
    background: radial-gradient(circle, rgba(139, 92, 246, 0.35) 0%, transparent 70%);
    pointer-events: none;
    z-index: 0;
  }
}

.spacer { flex: 1; }

// ─── 顶部操作行 ─────────────────────────────────────
.topbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
  position: relative;
  z-index: 10;
}

.chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 7px 13px 7px 8px;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.20);
  border-radius: $radius-pill;
  backdrop-filter: blur(12px);
  box-shadow: 0 2px 8px rgba(0,0,0,0.20);

  &__avatar {
    width: 28px;
    height: 28px;
    background: linear-gradient(135deg, #A78BFA, #818CF8);
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 2px 6px rgba(139,92,246,0.50);
  }

  &__avatar-icon { font-size: 15px; }

  &__badge {
    width: 26px;
    height: 26px;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 2px 6px rgba(0,0,0,0.25);
  }

  &__badge-icon {
    font-size: 13px;
    color: #fff;
  }

  &__label {
    font-size: 13.5px;
    font-weight: 600;
    color: $text-on-dark;
    max-width: 90px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    letter-spacing: 0.1px;

    &--mode { max-width: 64px; }
  }

  &__caret {
    font-size: 15px;
    color: $text-muted;
    transform: rotate(90deg);
    display: inline-block;
    transition: transform 0.2s;

    &--open { transform: rotate(-90deg); }
  }
}

// ─── Popover ────────────────────────────────────────
.overlay {
  position: fixed;
  inset: 0;
  z-index: 98;
}

.popover {
  position: absolute;
  top: 58px;
  right: 0;
  width: 290px;
  background: #1F1D50;
  border: 1px solid rgba(255,255,255,0.15);
  border-radius: $radius-card;
  box-shadow: 0 16px 48px rgba(0,0,0,0.50), 0 4px 12px rgba(0,0,0,0.30);
  z-index: 99;
  overflow: hidden;

  &__arrow {
    position: absolute;
    top: -6px;
    right: 32px;
    width: 12px;
    height: 12px;
    background: #1F1D50;
    border-left: 1px solid rgba(255,255,255,0.15);
    border-top: 1px solid rgba(255,255,255,0.15);
    transform: rotate(45deg);
  }

  &__item {
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 14px 16px;
    border-bottom: 1px solid rgba(255,255,255,0.07);
    transition: background 0.15s;

    &:last-child { border-bottom: none; }
    &--active { background: rgba(91, 80, 240, 0.25); }
    &:active { background: rgba(255,255,255,0.06); }
  }

  &__icon-wrap {
    width: 42px;
    height: 42px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    box-shadow: 0 3px 8px rgba(0,0,0,0.30);
  }

  &__icon {
    font-size: 19px;
    color: #fff;
  }

  &__copy {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  &__title {
    font-size: 14.5px;
    font-weight: 600;
    color: $text-on-dark;
  }

  &__desc {
    font-size: 11px;
    color: $text-muted;
    line-height: 1.5;
  }

  &__check {
    width: 22px;
    height: 22px;
    background: $primary;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    box-shadow: 0 0 8px $primary-glow;
  }

  &__check-icon {
    font-size: 11px;
    color: #fff;
    font-weight: 700;
  }
}

// ─── 指令输入条 ─────────────────────────────────────
.command-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  margin-bottom: 12px;
  background: rgba(91, 80, 240, 0.20);
  border-radius: $radius-sm;
  border: 1px solid rgba(167, 139, 250, 0.35);
  backdrop-filter: blur(8px);
  position: relative;
  z-index: 1;

  &__indicator {
    width: 3px;
    height: 18px;
    border-radius: 2px;
    flex-shrink: 0;
    box-shadow: 0 0 6px currentColor;
  }

  &__bracket {
    font-size: 18px;
    font-weight: 800;
    color: #A5B4FC;
    flex-shrink: 0;
    line-height: 1;
  }

  &__input {
    flex: 1;
    border: none;
    background: transparent;
    font-size: 13.5px;
    color: #E0E7FF;
    font-weight: 500;
    outline: none;
  }
}

// ─── 编辑器卡片（亮色区域）────────────────────────
.editor-card {
  background: $editor-bg;
  border-radius: $radius-card;
  border: 1px solid $editor-border;
  overflow: hidden;
  box-shadow: $card-shadow;
  position: relative;
  z-index: 1;

  &__textarea {
    width: 100%;
    min-height: 220px;
    padding: 18px 16px 12px;
    font-size: 15.5px;
    color: $text-on-light;
    line-height: 1.90;
    border: none;
    background: transparent;
    box-sizing: border-box;
    font-weight: 400;
    letter-spacing: 0.15px;
  }

  &__toolbar {
    display: flex;
    align-items: center;
    padding: 10px 14px;
    border-top: 1px solid rgba(99, 88, 200, 0.12);
    background: rgba(238, 236, 255, 0.60);
    gap: 8px;
  }
}

.tag-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 5px 14px;
  background: rgba(91, 80, 240, 0.12);
  border: 1px solid rgba(91, 80, 240, 0.20);
  border-radius: $radius-pill;

  &__icon {
    font-size: 13px;
    color: $primary;
    font-weight: 700;
  }

  &__label {
    font-size: 12px;
    color: $primary;
    font-weight: 600;
  }
}

.char-hint {
  font-size: 12px;
  color: $text-light-muted;

  &__max { color: #B5B0D8; }
}

// ─── 播放器 ─────────────────────────────────────────
.player {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-top: 20px;
  padding: 16px;
  background: $card-bg;
  border-radius: $radius-card;
  border: 1px solid $card-border;
  box-shadow: $card-shadow;
  backdrop-filter: blur(16px);
  position: relative;
  z-index: 1;

  &__play {
    width: 48px;
    height: 48px;
    background: linear-gradient(135deg, $primary, $primary-dark);
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    box-shadow: 0 4px 16px $primary-glow;

    &-icon { font-size: 18px; color: #fff; }
  }

  &__body { flex: 1; }

  &__title {
    display: block;
    font-size: 13px;
    font-weight: 600;
    color: $text-on-dark;
    margin-bottom: 12px;
    opacity: 0.90;
  }

  &__track {
    position: relative;
    height: 4px;
    background: rgba(255,255,255,0.15);
    border-radius: 2px;
    margin-bottom: 8px;
    overflow: visible;
  }

  &__fill {
    position: absolute;
    left: 0;
    top: 0;
    height: 100%;
    background: linear-gradient(90deg, #818CF8, $primary);
    border-radius: 2px;
    transition: width 0.1s linear;
  }

  &__thumb {
    position: absolute;
    top: 50%;
    transform: translate(-50%, -50%);
    width: 13px;
    height: 13px;
    background: #fff;
    border: 2.5px solid $primary;
    border-radius: 50%;
    box-shadow: 0 0 8px $primary-glow;
    transition: left 0.1s linear;
  }

  &__time {
    font-size: 11px;
    color: $text-muted;
    letter-spacing: 0.3px;
  }

  // 错误状态样式
  &--error {
    flex-direction: column;
    gap: 12px;
    padding: 20px;
  }

  &__error-icon {
    font-size: 28px;
  }

  &__error-text {
    font-size: 13px;
    color: #FCA5A5;
    text-align: center;
  }

  &__error-btn {
    padding: 8px 24px;
    background: rgba(255,255,255,0.12);
    border-radius: 20px;
    border: 1px solid rgba(255,255,255,0.2);

    text {
      font-size: 13px;
      color: $text-on-dark;
    }
  }
}

// ─── 标签面板 ────────────────────────────────────────
.sheet-mask {
  position: fixed;
  inset: 0;
  background: rgba(10, 8, 40, 0.65);
  display: flex;
  align-items: flex-end;
  z-index: 999;
  backdrop-filter: blur(4px);
}

.sheet {
  width: 100%;
  max-height: 72vh;
  background: #16144A;
  border: 1px solid rgba(255,255,255,0.12);
  border-bottom: none;
  border-radius: 24px 24px 0 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;

  &__handle {
    width: 40px;
    height: 4px;
    background: rgba(255,255,255,0.20);
    border-radius: 2px;
    margin: 12px auto 0;
    flex-shrink: 0;
  }

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 14px 20px;
    flex-shrink: 0;
    border-bottom: 1px solid $divider;
  }

  &__title {
    font-size: 17px;
    font-weight: 700;
    color: $text-on-dark;
  }

  &__close {
    width: 30px;
    height: 30px;
    background: rgba(255,255,255,0.10);
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 13px;
    color: $text-muted;
  }

  &__body {
    flex: 1;
    padding: 8px 20px 20px;
    overflow-y: auto;
  }
}

.tag-group {
  margin-bottom: 22px;

  &__label {
    display: block;
    font-size: 10.5px;
    font-weight: 700;
    color: $text-muted;
    letter-spacing: 1.2px;
    text-transform: uppercase;
    margin-bottom: 10px;
  }

  &__row {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }
}

.tag-chip {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 7px 14px;
  background: rgba(255,255,255,0.08);
  border-radius: $radius-pill;
  border: 1px solid rgba(255,255,255,0.10);

  &:active {
    background: rgba(91, 80, 240, 0.35);
    border-color: rgba(129,140,248,0.50);
  }

  &__emoji { font-size: 14px; }

  &__text {
    font-size: 13.5px;
    color: $text-on-dark;
    font-weight: 500;
  }
}

.custom-row {
  display: flex;
  gap: 10px;
  align-items: center;

  &__input {
    flex: 1;
    height: 42px;
    padding: 0 14px;
    background: rgba(255,255,255,0.08);
    border-radius: $radius-pill;
    font-size: 14px;
    color: $text-on-dark;
    border: 1px solid rgba(255,255,255,0.12);
  }

  &__btn {
    height: 42px;
    padding: 0 22px;
    background: $primary;
    border-radius: $radius-pill;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 14px;
    font-weight: 600;
    color: #fff;
    white-space: nowrap;
    box-shadow: 0 4px 12px $primary-glow;
  }
}

// ─── 悬浮操作栏 ─────────────────────────────────────
.fab-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 20px calc(14px + env(safe-area-inset-bottom));
  background: rgba(20, 18, 58, 0.88);
  border-top: 1px solid rgba(255,255,255,0.10);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
}

.fab-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 50px;
  padding: 0 30px;
  background: linear-gradient(135deg, #6D5EF7, $primary-dark);
  border-radius: $radius-pill;
  border: none;
  box-shadow: 0 4px 20px $primary-glow, 0 1px 0 rgba(255,255,255,0.20) inset;
  transition: opacity 0.2s, transform 0.15s;

  &:active { transform: scale(0.96); }
  &:disabled { opacity: 0.40; box-shadow: none; }
  &--loading { opacity: 0.72; }

  &__icon { font-size: 14px; color: #fff; }
  &__label { font-size: 15px; font-weight: 700; color: #fff; letter-spacing: 0.3px; }
}

.fab-bar__meta { display: flex; align-items: baseline; }

.fab-bar__count {
  font-size: 22px;
  font-weight: 700;
  color: $text-on-dark;
  line-height: 1;
}

.fab-bar__unit {
  font-size: 12px;
  color: $text-muted;
  margin-left: 2px;
}
</style>
