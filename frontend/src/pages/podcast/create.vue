<template>
  <view class="podcast-page">
    <!-- 顶部导航 -->
    <view class="nav-bar">
      <view class="back-btn" @click="goBack">
        <text class="back-icon">←</text>
      </view>
      <view class="nav-center">
        <text class="nav-title">AI播客</text>
        <text class="nav-subtitle">将文章转换为双人对谈节目</text>
      </view>
      <view class="placeholder"></view>
    </view>

    <!-- 主要内容 -->
    <view class="main-content">
      <!-- 标题输入 -->
      <view class="input-section">
        <view class="section-title">播客标题</view>
        <input
          v-model="title"
          class="title-input"
          placeholder="请输入播客标题..."
        />
      </view>

      <!-- 文本输入区 -->
      <view class="input-section">
        <view class="section-title">文章内容</view>
        <textarea
          v-model="sourceText"
          class="text-input"
          placeholder="请粘贴文章内容..."
          :maxlength="maxTextLength"
        ></textarea>
        <view class="text-count">{{ sourceText.length }} / {{ maxTextLength }}</view>
      </view>

      <!-- 双音色选择区 -->
      <view class="voice-section">
        <view class="section-title">选择两位主播音色</view>

        <view class="voice-row">
          <text class="voice-label">主播A（女声）</text>
          <scroll-view class="voice-list" scroll-x>
            <view
              v-for="voice in femaleVoices"
              :key="voice.name"
              class="voice-item"
              :class="{ active: voiceA === voice.name }"
              @click="selectVoiceA(voice.name)"
            >
              <text class="voice-name">{{ voice.shortName }}</text>
            </view>
          </scroll-view>
        </view>

        <view class="voice-row">
          <text class="voice-label">主播B（男声）</text>
          <scroll-view class="voice-list" scroll-x>
            <view
              v-for="voice in maleVoices"
              :key="voice.name"
              class="voice-item"
              :class="{ active: voiceB === voice.name }"
              @click="selectVoiceB(voice.name)"
            >
              <text class="voice-name">{{ voice.shortName }}</text>
            </view>
          </scroll-view>
        </view>
      </view>

      <!-- 生成按钮 -->
      <view class="action-section">
        <button
          class="generate-btn"
          :disabled="!canGenerate || loading"
          @click="handleGenerate"
        >
          {{ loading ? '生成中...' : '生成播客' }}
        </button>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getUserKey, initUser, getVoices } from '@/api'
import { generatePodcast } from '@/api/podcast'

const title = ref('')
const sourceText = ref('')
const voiceA = ref('BV001_streaming')
const voiceB = ref('BV002_streaming')
const voices = ref([])
const loading = ref(false)
const maxTextLength = ref(10000)

const femaleVoices = computed(() => {
  return voices.value.filter(v => v.gender === 'Female')
})

const maleVoices = computed(() => {
  return voices.value.filter(v => v.gender === 'Male')
})

const canGenerate = computed(() => {
  return sourceText.value.trim().length > 0
})

onMounted(async () => {
  // 初始化用户
  const userKey = getUserKey()
  await initUser(userKey)

  // 获取音色列表
  try {
    const res = await getVoices()
    voices.value = res.data
  } catch (e) {
    console.error('获取音色失败:', e)
  }
})

function selectVoiceA(name) {
  voiceA.value = name
}

function goBack() {
  uni.navigateBack()
}

function selectVoiceB(name) {
  voiceB.value = name
}

async function handleGenerate() {
  if (!canGenerate.value || loading.value) return

  loading.value = true
  try {
    const res = await generatePodcast(title.value || '未命名播客', sourceText.value, voiceA.value, voiceB.value)
    uni.showToast({
      title: '生成成功',
      icon: 'success'
    })

    // 跳转到播放页面
    setTimeout(() => {
      uni.navigateTo({
        url: `/pages/play/play?id=${res.data.podcastId}&type=podcast`
      })
    }, 500)
  } catch (e) {
    console.error('生成失败:', e)
    uni.showToast({
      title: '生成失败',
      icon: 'none'
    })
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.podcast-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #f093fb 0%, #f5576c 100%);
  padding-bottom: 50px;
}

.nav-bar {
  display: flex;
  align-items: center;
  padding: 60px 20px 30px;
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
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

.input-section {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 20px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 12px;
}

.title-input {
  width: 100%;
  height: 44px;
  font-size: 14px;
  border: 1px solid #eee;
  border-radius: 8px;
  padding: 0 12px;
  box-sizing: border-box;
}

.text-input {
  width: 100%;
  height: 150px;
  font-size: 14px;
  line-height: 1.6;
  border: 1px solid #eee;
  border-radius: 8px;
  padding: 12px;
  box-sizing: border-box;
}

.text-count {
  text-align: right;
  font-size: 12px;
  color: #999;
  margin-top: 8px;
}

.voice-section {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 20px;
}

.voice-row {
  margin-bottom: 16px;
}

.voice-label {
  display: block;
  font-size: 14px;
  color: #666;
  margin-bottom: 8px;
}

.voice-list {
  white-space: nowrap;
  width: 100%;
}

.voice-item {
  display: inline-block;
  padding: 10px 16px;
  margin-right: 10px;
  border-radius: 8px;
  background: #f5f5f5;
  text-align: center;
  &.active {
    background: #f5576c;
    .voice-name {
      color: #fff;
    }
  }
}

.voice-name {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: #333;
}

.action-section {
  padding: 20px;
}

.generate-btn {
  width: 100%;
  height: 50px;
  line-height: 50px;
  background: #fff;
  color: #f5576c;
  border-radius: 25px;
  font-size: 18px;
  font-weight: 600;
  border: none;
  &:disabled {
    background: #ccc;
    color: #fff;
  }
}
</style>
