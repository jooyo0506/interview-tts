<template>
  <view class="translate-page">
    <!-- 顶部导航 -->
    <view class="nav-bar">
      <view class="back-btn" @click="goBack">
        <text class="back-icon">←</text>
      </view>
      <view class="nav-center">
        <text class="nav-title">同声传译</text>
        <text class="nav-subtitle">实时翻译您听到的内容</text>
      </view>
      <view class="placeholder"></view>
    </view>

    <!-- 主要内容 -->
    <view class="main-content">
      <!-- 翻译模式选择 -->
      <view class="mode-section">
        <view
          class="mode-item"
          :class="{ active: mode === 'text' }"
          @click="mode = 'text'"
        >
          <text class="mode-name">文本翻译</text>
        </view>
        <view
          class="mode-item"
          :class="{ active: mode === 'speech' }"
          @click="mode = 'speech'"
        >
          <text class="mode-name">语音翻译</text>
        </view>
      </view>

      <!-- 语言选择 -->
      <view class="lang-section">
        <view class="lang-item" @click="showSourceLangPicker = true">
          <text class="lang-label">源语言</text>
          <text class="lang-value">{{ getLangText(sourceLang) }}</text>
        </view>
        <view class="lang-arrow">→</view>
        <view class="lang-item" @click="showTargetLangPicker = true">
          <text class="lang-label">目标语言</text>
          <text class="lang-value">{{ getLangText(targetLang) }}</text>
        </view>
      </view>

      <!-- 文本输入模式 -->
      <view v-if="mode === 'text'" class="input-section">
        <textarea
          v-model="inputText"
          class="text-input"
          placeholder="请输入要翻译的文本..."
        ></textarea>
        <button class="translate-btn" @click="handleTranslate" :disabled="!inputText.trim() || translating">
          {{ translating ? '翻译中...' : '翻译' }}
        </button>
      </view>

      <!-- 语音输入模式 -->
      <view v-else class="speech-section">
        <view class="record-btn" @touchstart="startRecord" @touchend="stopRecord">
          <view class="record-icon" :class="{ recording: isRecording }"></view>
          <text class="record-text">{{ isRecording ? '松开结束' : '按住说话' }}</text>
        </view>

        <view v-if="recordedText" class="recorded-text">
          <text class="label">识别内容：</text>
          <text class="content">{{ recordedText }}</text>
        </view>

        <button
          v-if="recordedText"
          class="translate-btn"
          @click="handleTranslate"
          :disabled="translating"
        >
          {{ translating ? '翻译中...' : '翻译' }}
        </button>
      </view>

      <!-- 翻译结果 -->
      <view v-if="result" class="result-section">
        <view class="result-title">翻译结果</view>
        <view class="result-text">{{ result.translatedText }}</view>

        <view v-if="result.translatedAudioUrl" class="result-audio">
          <button class="play-btn" @click="playAudio(result.translatedAudioUrl)">
            播放翻译语音
          </button>
        </view>
      </view>

      <!-- 历史记录 -->
      <view v-if="history.length > 0" class="history-section">
        <view class="history-title">翻译历史</view>
        <scroll-view class="history-list" scroll-y>
          <view
            v-for="item in history"
            :key="item.id"
            class="history-item"
            @click="selectHistory(item)"
          >
            <view class="history-source">{{ item.sourceText }}</view>
            <view class="history-target">{{ item.translatedText }}</view>
          </view>
        </scroll-view>
      </view>
    </view>

    <!-- 语言选择器 -->
    <view v-if="showSourceLangPicker" class="picker-mask" @click="showSourceLangPicker = false">
      <view class="picker-content" @click.stop>
        <view class="picker-title">选择源语言</view>
        <view
          v-for="lang in langs"
          :key="lang.value"
          class="picker-item"
          :class="{ active: sourceLang === lang.value }"
          @click="selectSourceLang(lang.value)"
        >
          {{ lang.label }}
        </view>
      </view>
    </view>

    <view v-if="showTargetLangPicker" class="picker-mask" @click="showTargetLangPicker = false">
      <view class="picker-content" @click.stop>
        <view class="picker-title">选择目标语言</view>
        <view
          v-for="lang in langs"
          :key="lang.value"
          class="picker-item"
          :class="{ active: targetLang === lang.value }"
          @click="selectTargetLang(lang.value)"
        >
          {{ lang.label }}
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getUserKey, initUser } from '@/api'
import { translateText, translateSpeech, getTranslationList } from '@/api/translate'

const mode = ref('text')
const sourceLang = ref('zh')
const targetLang = ref('en')
const inputText = ref('')
const recordedText = ref('')
const translating = ref(false)
const isRecording = ref(false)
const result = ref(null)
const history = ref([])
const showSourceLangPicker = ref(false)
const showTargetLangPicker = ref(false)

const langs = [
  { value: 'zh', label: '中文' },
  { value: 'en', label: '英语' },
  { value: 'ja', label: '日语' },
  { value: 'ko', label: '韩语' },
  { value: 'fr', label: '法语' },
  { value: 'de', label: '德语' },
  { value: 'es', label: '西班牙语' }
]

onMounted(async () => {
  const userKey = getUserKey()
  await initUser(userKey)
  await loadHistory()
})

async function loadHistory() {
  try {
    const res = await getTranslationList()
    history.value = res.data || []
  } catch (e) {
    console.error('获取历史失败:', e)
  }
}

function getLangText(value) {
  const lang = langs.find(l => l.value === value)
  return lang ? lang.label : value
}

function selectSourceLang(value) {
  sourceLang.value = value
  showSourceLangPicker.value = false
}

function selectTargetLang(value) {
  targetLang.value = value
  showTargetLangPicker.value = false
}

function startRecord() {
  isRecording.value = true
  // 模拟开始录音
  uni.showToast({ title: '开始录音', icon: 'none' })
}

function stopRecord() {
  isRecording.value = false
  // 模拟识别结果
  recordedText.value = '这是模拟的语音识别结果'
  uni.showToast({ title: '录音结束', icon: 'none' })
}

async function handleTranslate() {
  const text = mode.value === 'text' ? inputText.value : recordedText.value
  if (!text.trim()) return

  translating.value = true
  try {
    if (mode.value === 'text') {
      const res = await translateText(text, sourceLang.value, targetLang.value)
      result.value = res.data
    } else {
      const res = await translateSpeech(text, sourceLang.value, targetLang.value)
      result.value = res.data
    }

    // 刷新历史
    await loadHistory()
  } catch (e) {
    console.error('翻译失败:', e)
    uni.showToast({ title: '翻译失败', icon: 'none' })
  } finally {
    translating.value = false
  }
}

function playAudio(url) {
  const audioContext = uni.createInnerAudioContext()
  audioContext.src = url
  audioContext.play()
  uni.showToast({ title: '播放中', icon: 'none' })
}

function selectHistory(item) {
  result.value = item
}

function goBack() {
  uni.navigateBack()
}
</script>

<style lang="scss" scoped>
.translate-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #fa709a 0%, #fee140 100%);
  padding-bottom: 50px;
}

.nav-bar {
  display: flex;
  align-items: center;
  padding: 60px 20px 30px;
  background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);
}

.back-btn {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.back-icon {
  font-size: 24px;
  color: #fff;
}

.nav-center {
  flex: 1;
  text-align: center;
}

.nav-title {
  display: block;
  font-size: 22px;
  font-weight: bold;
  color: #fff;
}

.nav-subtitle {
  display: block;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.8);
  margin-top: 4px;
}

.placeholder {
  width: 40px;
}

.main-content {
  padding: 20px;
}

.mode-section {
  display: flex;
  background: #fff;
  border-radius: 12px;
  padding: 6px;
  margin-bottom: 16px;
}

.mode-item {
  flex: 1;
  text-align: center;
  padding: 12px;
  border-radius: 8px;
  &.active {
    background: #fa709a;
    .mode-name {
      color: #fff;
    }
  }
}

.mode-name {
  font-size: 15px;
  color: #666;
}

.lang-section {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 16px;
}

.lang-item {
  flex: 1;
  text-align: center;
}

.lang-label {
  display: block;
  font-size: 12px;
  color: #999;
  margin-bottom: 4px;
}

.lang-value {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.lang-arrow {
  font-size: 20px;
  color: #fa709a;
  padding: 0 16px;
}

.input-section, .speech-section, .result-section, .history-section {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 16px;
}

.text-input {
  width: 100%;
  height: 120px;
  font-size: 14px;
  line-height: 1.6;
  border: 1px solid #eee;
  border-radius: 8px;
  padding: 12px;
  box-sizing: border-box;
  margin-bottom: 16px;
}

.translate-btn {
  width: 100%;
  height: 44px;
  line-height: 44px;
  background: #fa709a;
  color: #fff;
  border-radius: 22px;
  font-size: 16px;
  font-weight: 600;
  border: none;
  &:disabled {
    background: #ccc;
  }
}

.record-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px;
}

.record-icon {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: #fa709a;
  margin-bottom: 16px;
  &.recording {
    background: #f44336;
    animation: pulse 1s infinite;
  }
}

@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.1); }
}

.record-text {
  font-size: 16px;
  color: #666;
}

.recorded-text {
  margin: 20px 0;
}

.recorded-text .label {
  font-size: 14px;
  color: #999;
}

.recorded-text .content {
  font-size: 15px;
  color: #333;
  margin-top: 8px;
  display: block;
}

.result-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 12px;
}

.result-text {
  font-size: 15px;
  line-height: 1.8;
  color: #333;
}

.result-audio {
  margin-top: 16px;
}

.play-btn {
  width: 100%;
  height: 40px;
  line-height: 40px;
  background: #fee140;
  color: #333;
  border-radius: 20px;
  font-size: 14px;
  border: none;
}

.history-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 12px;
}

.history-list {
  max-height: 200px;
}

.history-item {
  padding: 12px;
  background: #f9f9f9;
  border-radius: 8px;
  margin-bottom: 10px;
}

.history-source {
  font-size: 13px;
  color: #999;
  margin-bottom: 6px;
}

.history-target {
  font-size: 14px;
  color: #333;
}

.picker-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: flex-end;
  justify-content: center;
}

.picker-content {
  background: #fff;
  border-radius: 16px 16px 0 0;
  width: 100%;
  padding: 20px;
  max-height: 60vh;
  overflow-y: auto;
}

.picker-title {
  font-size: 18px;
  font-weight: 600;
  text-align: center;
  margin-bottom: 20px;
}

.picker-item {
  padding: 16px;
  text-align: center;
  font-size: 16px;
  border-bottom: 1px solid #f0f0f0;
  &.active {
    color: #fa709a;
  }
}
</style>
