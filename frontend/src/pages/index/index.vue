<template>
  <view class="index-page">
    <!-- 动态声波背景 -->
    <view class="wave-bg">
      <view class="wave wave-1"></view>
      <view class="wave wave-2"></view>
      <view class="wave wave-3"></view>
      <view class="noise-overlay"></view>
    </view>

    <!-- 顶部品牌区 -->
    <view class="brand-section">
      <view class="brand-content">
        <view class="brand-logo">
          <view class="logo-ring"></view>
          <text class="logo-icon">🔊</text>
        </view>
        <text class="brand-name">声读</text>
        <text class="brand-slogan">让文字发声，随时听起来</text>
      </view>

      <!-- 装饰性声波 -->
      <view class="decorative-waves">
        <view v-for="i in 5" :key="i" class="d-wave" :style="{ '--i': i }"></view>
      </view>
    </view>

    <!-- 核心场景区 -->
    <view class="scenario-section">
      <view class="scenario-card primary" @click="goToTextToAudio">
        <view class="card-glow"></view>
        <view class="card-content">
          <view class="card-icon-wrap">
            <text class="card-icon">📝</text>
          </view>
          <view class="card-info">
            <text class="card-title">文本转语音</text>
            <text class="card-desc">输入文字，一键生成语音</text>
          </view>
        </view>
        <view class="card-arrow">
          <text>→</text>
        </view>
      </view>

      <view class="scenario-card purple" @click="goToTtsV2">
        <view class="card-glow"></view>
        <view class="card-content">
          <view class="card-icon-wrap">
            <text class="card-icon">✨</text>
          </view>
          <view class="card-info">
            <text class="card-title">情感合成 v2</text>
            <text class="card-desc">语音指令、情感标签、上下文理解</text>
          </view>
        </view>
        <view class="card-arrow">
          <text>→</text>
        </view>
      </view>

      <view class="scenario-card pink" @click="goToPodcast">
        <view class="card-glow"></view>
        <view class="card-content">
          <view class="card-icon-wrap">
            <text class="card-icon">🎙️</text>
          </view>
          <view class="card-info">
            <text class="card-title">AI播客</text>
            <text class="card-desc">双人对谈，沉浸式收听</text>
          </view>
        </view>
        <view class="card-arrow">
          <text>→</text>
        </view>
      </view>
    </view>

    <!-- 快捷工具区 -->
    <view class="tools-section">
      <view class="section-header">
        <text class="section-title">更多工具</text>
        <view class="title-underline"></view>
      </view>
      <view class="tools-grid">
        <view class="tool-item" @click="goToVoiceClone">
          <view class="tool-icon blue">
            <text>🎤</text>
          </view>
          <text class="tool-name">声音复刻</text>
        </view>
        <view class="tool-item" @click="goToTranslate">
          <view class="tool-icon amber">
            <text>🌐</text>
          </view>
          <text class="tool-name">同声传译</text>
        </view>
        <view class="tool-item" @click="goToPlaylist">
          <view class="tool-icon rose">
            <text>📋</text>
          </view>
          <text class="tool-name">播放列表</text>
        </view>
        <view class="tool-item" @click="goToHistory">
          <view class="tool-icon violet">
            <text>📚</text>
          </view>
          <text class="tool-name">我的记录</text>
        </view>
      </view>
    </view>

    <!-- 最近使用 -->
    <view class="recent-section">
      <view class="section-header">
        <text class="section-title">最近使用</text>
        <view class="title-underline"></view>
        <text v-if="recentList.length > 0" class="section-more" @click="goToHistory">查看全部</text>
      </view>

      <view v-if="recentList.length === 0" class="empty-state">
        <view class="empty-illustration">
          <view class="empty-wave"></view>
          <view class="empty-wave delay-1"></view>
          <view class="empty-wave delay-2"></view>
        </view>
        <text class="empty-title">暂无记录</text>
        <text class="empty-desc">开始创建你的第一个音频吧</text>
      </view>

      <scroll-view v-else class="recent-list" scroll-x :show-scrollbar="false">
        <view
          v-for="(item, index) in recentList"
          :key="item.id"
          class="recent-item"
          :style="{ '--delay': index * 0.1 + 's' }"
          @click="playItem(item)"
        >
          <view class="item-glow"></view>
          <view class="recent-cover">
            <text class="cover-icon">🔊</text>
          </view>
          <view class="recent-info">
            <text class="recent-title">{{ item.title }}</text>
            <text class="recent-duration">{{ formatDuration(item.duration) }}</text>
          </view>
          <view class="play-overlay">
            <text class="play-btn">▶</text>
          </view>
        </view>
      </scroll-view>
    </view>

    <!-- 底部提示 -->
    <view class="tip-section">
      <view class="tip-badge">
        <text class="tip-icon">💡</text>
        <text class="tip-text">不同的声音，不同的体验</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getUserKey, initUser, getMyList } from '@/api'

const recentList = ref([])
const loading = ref(false)

onMounted(async () => {
  const userKey = getUserKey()
  await initUser(userKey)
  loadRecentList()
})

async function loadRecentList() {
  loading.value = true
  try {
    const res = await getMyList()
    recentList.value = (res.data || []).slice(0, 5).map(item => ({
      id: item.id,
      title: item.title || item.rawText?.substring(0, 20) + '...' || '未命名',
      duration: item.duration || 0,
      r2Url: item.r2Url
    }))
  } catch (e) {
    console.error('加载最近记录失败:', e)
    recentList.value = []
  } finally {
    loading.value = false
  }
}

function goToTextToAudio() {
  uni.navigateTo({
    url: '/pages/tts/index'
  })
}

function goToTtsV2() {
  uni.navigateTo({
    url: '/pages/tts/v2'
  })
}

function goToPodcast() {
  uni.navigateTo({
    url: '/pages/podcast/create'
  })
}

function goToVoiceClone() {
  uni.navigateTo({
    url: '/pages/voice/clone'
  })
}

function goToTranslate() {
  uni.navigateTo({
    url: '/pages/translate/live'
  })
}

function goToHistory() {
  uni.switchTab({
    url: '/pages/my/my'
  })
}

function goToPlaylist() {
  uni.navigateTo({
    url: '/pages/playlist/index'
  })
}

function playItem(item) {
  if (!item.r2Url) {
    uni.showToast({
      title: '音频不可用，请重新生成',
      icon: 'none'
    })
    return
  }

  uni.navigateTo({
    url: `/pages/play/play?id=${item.id}&url=${encodeURIComponent(item.r2Url)}&title=${encodeURIComponent(item.title)}`
  })
}

function formatDuration(seconds) {
  const min = Math.floor(seconds / 60)
  return `${min}分钟`
}
</script>

<style lang="scss" scoped>
.index-page {
  min-height: 100vh;
  min-height: 100dvh;
  background: #0a0a0f;
  position: relative;
  overflow: hidden;
  padding-bottom: calc(20px + env(safe-area-inset-bottom, 0px));
}

// 动态声波背景
.wave-bg {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
  z-index: 0;
  overflow: hidden;
}

.wave {
  position: absolute;
  width: 200%;
  height: 200%;
  left: -50%;
  border-radius: 40%;
  animation: wave-rotate 20s linear infinite;
  opacity: 0.03;
}

.wave-1 {
  bottom: -30%;
  background: radial-gradient(ellipse at center, #f59e0b 0%, transparent 70%);
  animation-duration: 25s;
}

.wave-2 {
  bottom: -35%;
  background: radial-gradient(ellipse at center, #667eea 0%, transparent 70%);
  animation-duration: 30s;
  animation-direction: reverse;
}

.wave-3 {
  bottom: -40%;
  background: radial-gradient(ellipse at center, #764ba2 0%, transparent 70%);
  animation-duration: 35s;
}

@keyframes wave-rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.noise-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 256 256' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noise'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='4' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noise)'/%3E%3C/svg%3E");
  opacity: 0.03;
  mix-blend-mode: overlay;
}

// 品牌区
.brand-section {
  position: relative;
  padding: calc(60px + env(safe-area-inset-top, 0px)) 24px 40px;
  text-align: center;
  z-index: 1;
}

.brand-content {
  position: relative;
  z-index: 2;
}

.brand-logo {
  position: relative;
  width: 90px;
  height: 90px;
  margin: 0 auto 20px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.logo-ring {
  position: absolute;
  width: 100%;
  height: 100%;
  border-radius: 28px;
  border: 2px solid rgba(245, 158, 11, 0.3);
  animation: ring-pulse 3s ease-in-out infinite;
}

@keyframes ring-pulse {
  0%, 100% {
    transform: scale(1);
    opacity: 0.3;
  }
  50% {
    transform: scale(1.1);
    opacity: 0.1;
  }
}

.logo-icon {
  font-size: 42px;
  filter: drop-shadow(0 0 20px rgba(245, 158, 11, 0.5));
}

.brand-name {
  display: block;
  font-size: 36px;
  font-weight: 700;
  color: #fff;
  margin-bottom: 10px;
  letter-spacing: 2px;
  text-shadow: 0 4px 20px rgba(245, 158, 11, 0.3);
}

.brand-slogan {
  display: block;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.5);
  letter-spacing: 1px;
}

// 装饰性声波
.decorative-waves {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 60px;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  gap: 4px;
  opacity: 0.15;
}

.d-wave {
  width: 3px;
  height: 20px;
  background: linear-gradient(180deg, #f59e0b, transparent);
  border-radius: 2px;
  animation: d-wave-float 2s ease-in-out infinite;
  animation-delay: calc(var(--i) * 0.15s);

  &:nth-child(odd) {
    animation-duration: 2.3s;
  }
}

@keyframes d-wave-float {
  0%, 100% {
    transform: scaleY(0.6);
  }
  50% {
    transform: scaleY(1);
  }
}

// 核心场景区
.scenario-section {
  padding: 0 20px;
  position: relative;
  z-index: 1;
}

.scenario-card {
  position: relative;
  background: rgba(30, 30, 50, 0.6);
  backdrop-filter: blur(20px);
  border-radius: 20px;
  padding: 24px;
  margin-bottom: 16px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.05);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);

  &:active {
    transform: scale(0.98);
  }

  &.primary {
    .card-glow {
      background: radial-gradient(ellipse at 30% 50%, rgba(245, 158, 11, 0.15) 0%, transparent 60%);
    }
    .card-icon-wrap {
      background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
    }
  }

  &.pink {
    .card-glow {
      background: radial-gradient(ellipse at 30% 50%, rgba(236, 72, 153, 0.15) 0%, transparent 60%);
    }
    .card-icon-wrap {
      background: linear-gradient(135deg, #ec4899 0%, #db2777 100%);
    }
  }

  &.purple {
    .card-glow {
      background: radial-gradient(ellipse at 30% 50%, rgba(139, 92, 246, 0.2) 0%, transparent 60%);
    }
    .card-icon-wrap {
      background: linear-gradient(135deg, #8b5cf6 0%, #6d28d9 100%);
    }
  }
}

.card-glow {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
}

.card-content {
  display: flex;
  align-items: center;
  position: relative;
  z-index: 1;
}

.card-icon-wrap {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3);
}

.card-icon {
  font-size: 26px;
}

.card-info {
  flex: 1;
}

.card-title {
  display: block;
  font-size: 18px;
  font-weight: 600;
  color: #fff;
  margin-bottom: 6px;
}

.card-desc {
  display: block;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.5);
}

.card-arrow {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.05);
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.4);
  font-size: 16px;
  transition: all 0.3s ease;

  .scenario-card:active & {
    transform: translateX(4px);
    background: rgba(255, 255, 255, 0.1);
  }
}

// 工具区
.tools-section {
  padding: 32px 20px 0;
  position: relative;
  z-index: 1;
}

.section-header {
  margin-bottom: 20px;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #fff;
  display: block;
  margin-bottom: 8px;
}

.title-underline {
  width: 40px;
  height: 3px;
  background: linear-gradient(90deg, #f59e0b, transparent);
  border-radius: 2px;
}

.section-more {
  font-size: 13px;
  color: #f59e0b;
  float: right;
  margin-top: -24px;
}

.tools-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.tool-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px 8px;
  background: rgba(30, 30, 50, 0.4);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.03);
  transition: all 0.3s ease;

  &:active {
    transform: scale(0.95);
    background: rgba(30, 30, 50, 0.7);
  }
}

.tool-icon {
  width: 44px;
  height: 44px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 10px;
  font-size: 20px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);

  &.blue {
    background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
  }
  &.amber {
    background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
  }
  &.rose {
    background: linear-gradient(135deg, #f43f5e 0%, #e11d48 100%);
  }
  &.violet {
    background: linear-gradient(135deg, #8b5cf6 0%, #6d28d9 100%);
  }
}

.tool-name {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
  text-align: center;
}

// 最近使用
.recent-section {
  padding: 32px 20px;
  position: relative;
  z-index: 1;
}

.empty-state {
  background: rgba(30, 30, 50, 0.3);
  border-radius: 20px;
  padding: 40px 20px;
  text-align: center;
  border: 1px dashed rgba(255, 255, 255, 0.1);
}

.empty-illustration {
  height: 60px;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  gap: 6px;
  margin-bottom: 20px;
}

.empty-wave {
  width: 4px;
  height: 30px;
  background: linear-gradient(180deg, #667eea, transparent);
  border-radius: 2px;
  animation: empty-float 1.5s ease-in-out infinite;

  &.delay-1 {
    height: 20px;
    animation-delay: 0.2s;
  }
  &.delay-2 {
    height: 40px;
    animation-delay: 0.4s;
  }
}

@keyframes empty-float {
  0%, 100% {
    transform: scaleY(0.5);
    opacity: 0.3;
  }
  50% {
    transform: scaleY(1);
    opacity: 0.8;
  }
}

.empty-title {
  display: block;
  font-size: 16px;
  color: rgba(255, 255, 255, 0.6);
  margin-bottom: 8px;
}

.empty-desc {
  display: block;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.4);
}

.recent-list {
  white-space: nowrap;
  padding-bottom: 10px;
}

.recent-item {
  display: inline-block;
  width: 140px;
  margin-right: 14px;
  background: rgba(30, 30, 50, 0.5);
  border-radius: 16px;
  padding: 14px;
  position: relative;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.05);
  transition: all 0.3s ease;
  animation: item-fade-in 0.5s ease-out backwards;
  animation-delay: var(--delay, 0s);

  &:active {
    transform: scale(0.96);
  }
}

@keyframes item-fade-in {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.item-glow {
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle, rgba(245, 158, 11, 0.1) 0%, transparent 50%);
  pointer-events: none;
}

.recent-cover {
  width: 100%;
  height: 70px;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.3) 0%, rgba(118, 75, 162, 0.3) 100%);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
  position: relative;
  overflow: hidden;
}

.cover-icon {
  font-size: 28px;
  opacity: 0.8;
}

.recent-info {
  position: relative;
  z-index: 1;
}

.recent-title {
  display: block;
  font-size: 13px;
  color: #fff;
  font-weight: 500;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recent-duration {
  display: block;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.4);
}

.play-overlay {
  position: absolute;
  top: 14px;
  right: 14px;
  width: 28px;
  height: 28px;
  background: rgba(245, 158, 11, 0.9);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: all 0.3s ease;

  .recent-item:active & {
    opacity: 1;
  }
}

.play-btn {
  font-size: 10px;
  color: #fff;
  margin-left: 2px;
}

// 底部提示
.tip-section {
  position: relative;
  z-index: 1;
  padding: 0 20px;
  padding-bottom: env(safe-area-inset-bottom, 0px);
  text-align: center;
}

.tip-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  background: rgba(245, 158, 11, 0.1);
  border: 1px solid rgba(245, 158, 11, 0.2);
  border-radius: 20px;
}

.tip-icon {
  font-size: 14px;
}

.tip-text {
  font-size: 13px;
  color: rgba(245, 158, 11, 0.8);
}
</style>
