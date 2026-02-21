<template>
  <view class="index-page">
    <!-- 动态声波背景 -->
    <view class="wave-bg">
      <view class="wave wave-1"></view>
      <view class="wave wave-2"></view>
      <view class="wave wave-3"></view>
      <view class="noise-overlay"></view>
    </view>

    <!-- 顶部导航栏 -->
    <view class="top-nav">
      <view class="nav-left">
        <text class="logo-text">ViVi 2.0</text>
      </view>
      <view class="nav-right" @click="handleAvatarClick">
        <!-- 未登录：头像占位符+登录 -->
        <template v-if="!isLoggedIn">
          <view class="avatar-placeholder">
            <text>👤</text>
          </view>
          <text class="login-text">登录</text>
        </template>
        <!-- 已登录：头像+皇冠 -->
        <template v-else>
          <view class="avatar">
            <text>👤</text>
          </view>
          <view class="crown-icon" :class="{ vip: isVip }" @click.stop="goToVip">
            {{ isVip ? '👑' : '👑' }}
          </view>
        </template>
      </view>
    </view>

    <!-- 标题区 -->
    <view class="brand-section">
      <view class="brand-content">
        <text class="brand-name">声读</text>
        <text class="brand-slogan">让文字发声，随时听起来</text>
      </view>
    </view>

    <!-- 字数仪表盘 -->
    <view class="char-dashboard" v-if="isLoggedIn" @click="showUpgradePopup">
      <view class="dashboard-left">
        <text class="label" v-if="isVip">VIP会员</text>
        <text class="label" v-else>剩余字数</text>
        <text class="value" v-if="isVip">无限</text>
        <text class="value" v-else>{{ charLimit - usedChars }}</text>
        <text class="unit" v-if="!isVip">字</text>
      </view>
      <view class="dashboard-right" v-if="!isVip">
        <view class="progress-bar">
          <view class="progress-fill" :style="{ width: Math.min((usedChars / charLimit * 100), 100) + '%' }"></view>
        </view>
        <text class="plus-icon">+</text>
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

      <view class="scenario-card vip-card" @click="handleVipCardClick('ttsV2')">
        <view class="vip-badge-corner">Pro</view>
        <view class="card-glow purple-glow"></view>
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

      <view class="scenario-card vip-card" @click="handleVipCardClick('podcast')">
        <view class="vip-badge-corner">Pro</view>
        <view class="card-glow purple-glow"></view>
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
        <view class="tool-item vip-locked" :class="{ locked: !isVip }" @click="handleToolClick('voiceClone')">
          <view class="tool-icon blue">
            <text>🎤</text>
          </view>
          <text class="tool-name">声音复刻</text>
          <view class="tool-lock" v-if="!isVip">🔒</view>
        </view>
        <view class="tool-item vip-locked" :class="{ locked: !isVip }" @click="handleToolClick('translate')">
          <view class="tool-icon amber">
            <text>🌐</text>
          </view>
          <text class="tool-name">同声传译</text>
          <view class="tool-lock" v-if="!isVip">🔒</view>
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

      <!-- 存储提示 -->
      <view class="storage-hint" v-if="recentList.length > 0">
        <text>VIP 享永久云端存储，普通用户仅保留最近 7 天记录</text>
      </view>
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
import { ref, computed, onMounted } from 'vue'
import { getUserKey, initUser, getMyList } from '@/api'
import { getUserInfo } from '@/api/auth'

const recentList = ref([])
const loading = ref(false)
const userInfo = ref(null)
const token = ref('')

// 计算属性
const isLoggedIn = computed(() => !!token.value)
const isVip = computed(() => userInfo.value?.userType === 'VIP')
const usedChars = computed(() => userInfo.value?.monthlyCharUsed || 0)
const charLimit = computed(() => userInfo.value?.monthlyCharLimit || 5000)

onMounted(async () => {
  const userKey = getUserKey()
  await initUser(userKey)
  token.value = uni.getStorageSync('token') || ''
  if (token.value) {
    loadUserInfo()
  }
  loadRecentList()
})

async function loadUserInfo() {
  if (!token.value) return
  try {
    const res = await getUserInfo()
    if (res && res.data) {
      userInfo.value = res.data
    }
  } catch (e) {
    console.error('获取用户信息失败:', e)
  }
}

function goToLogin() {
  uni.navigateTo({ url: '/pages/auth/login' })
}

function goToVip() {
  uni.navigateTo({ url: '/pages/vip/index' })
}

function goToMy() {
  uni.switchTab({ url: '/pages/my/my' })
}

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

function handleToolClick(tool) {
  const vipTools = ['voiceClone', 'translate']
  if (vipTools.includes(tool) && !isVip.value) {
    showVipPopup()
    return
  }
  // 正常跳转
  if (tool === 'voiceClone') goToVoiceClone()
  if (tool === 'translate') goToTranslate()
}

// 处理头像点击
function handleAvatarClick() {
  if (isLoggedIn.value) {
    goToMy()
  } else {
    goToLogin()
  }
}

// 显示升级弹窗
function showUpgradePopup() {
  if (isVip.value) return
  uni.showModal({
    title: '升级VIP',
    content: '升级VIP尊享无限字数，解锁全功能',
    confirmText: '立即升级',
    success: (res) => {
      if (res.confirm) goToVip()
    }
  })
}

// 处理VIP卡片点击
function handleVipCardClick(card) {
  if (!isVip.value) {
    showVipPopup()
    return
  }
  // VIP用户正常跳转
  if (card === 'ttsV2') goToTtsV2()
  if (card === 'podcast') goToPodcast()
}

// 显示VIP权益弹窗
function showVipPopup() {
  uni.showModal({
    title: 'VIP专属权益',
    content: '解锁更多高级功能，尊享无限字数合成',
    confirmText: '立即升级',
    cancelText: '稍后再说',
    success: (res) => {
      if (res.confirm) goToVip()
    }
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

// 顶部导航栏
.top-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 30rpx;
  position: relative;
  z-index: 10;
}

.nav-left {
  .logo-text {
    font-size: 36rpx;
    font-weight: bold;
    background: linear-gradient(135deg, #FF6B00, #FFD700);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
  }
}

.nav-right {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.avatar-placeholder, .avatar {
  width: 56rpx;
  height: 56rpx;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2rpx solid rgba(255, 255, 255, 0.2);
}

.login-text {
  font-size: 28rpx;
  color: rgba(255, 255, 255, 0.6);
  transition: all 0.2s;

  &:active {
    color: #FF6B00;
  }
}

.crown-icon {
  font-size: 24rpx;
  opacity: 0.4;
  transition: all 0.2s;

  &.vip {
    opacity: 1;
    filter: drop-shadow(0 0 8rpx rgba(255, 215, 0, 0.6));
  }
}

// 字数仪表盘
.char-dashboard {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx 30rpx;
  margin: 0 30rpx 30rpx;
  background: rgba(255, 255, 255, 0.03);
  backdrop-filter: blur(10rpx);
  border-radius: 16rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.05);
}

.dashboard-left {
  display: flex;
  align-items: baseline;
  gap: 8rpx;

  .label {
    font-size: 24rpx;
    color: rgba(255, 255, 255, 0.5);
  }

  .value {
    font-size: 40rpx;
    font-weight: bold;
    color: #FF6B00;
  }

  .unit {
    font-size: 24rpx;
    color: rgba(255, 255, 255, 0.5);
  }
}

.dashboard-right {
  display: flex;
  align-items: center;
  gap: 12rpx;
  flex: 1;
  max-width: 40%;
  margin-left: 20rpx;
}

.progress-bar {
  flex: 1;
  height: 12rpx;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 6rpx;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #FF6B00, #FFD700);
  border-radius: 6rpx;
  transition: width 0.3s ease;
}

.plus-icon {
  font-size: 28rpx;
  color: #FFD700;
  font-weight: bold;
}

// VIP卡片角标
.vip-badge-corner {
  position: absolute;
  top: 16rpx;
  right: 16rpx;
  background: linear-gradient(135deg, #FFD700, #FFA500);
  color: #000;
  font-size: 20rpx;
  font-weight: bold;
  padding: 4rpx 12rpx;
  border-radius: 8rpx;
  z-index: 5;
}

.purple-glow {
  background: radial-gradient(ellipse at 30% 50%, rgba(139, 92, 246, 0.3) 0%, transparent 60%) !important;
}

// 用户状态栏
.user-status-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 30rpx;
  margin: 20rpx;
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(10rpx);
  border-radius: 16rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.1);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.avatar {
  width: 60rpx;
  height: 60rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea, #764ba2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32rpx;
}

.username {
  color: #fff;
  font-size: 28rpx;
}

.vip-badge {
  background: linear-gradient(135deg, #FFD700, #FFA500);
  color: #000;
  font-size: 22rpx;
  font-weight: bold;
  padding: 8rpx 20rpx;
  border-radius: 20rpx;
}

.quota-info {
  flex: 1;
  margin: 0 20rpx;
}

.quota-text {
  color: rgba(255, 255, 255, 0.6);
  font-size: 22rpx;
}

.quota-bar {
  height: 8rpx;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 4rpx;
  margin-top: 8rpx;
  overflow: hidden;
}

.quota-fill {
  height: 100%;
  background: linear-gradient(90deg, #FF6B00, #FFA500);
  border-radius: 4rpx;
  transition: width 0.3s ease;
}

// 登录栏
.login-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 30rpx;
  padding: 20rpx 30rpx;
  margin: 20rpx;
}

.vip-btn {
  background: linear-gradient(135deg, #FFD700, #FFA500);
  color: #000;
  font-weight: bold;
  border-radius: 30rpx;
  padding: 12rpx 40rpx;
  font-size: 26rpx;
  border: none;
  line-height: 1.5;
}

.login-link {
  color: rgba(255, 255, 255, 0.6);
  font-size: 26rpx;
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

// VIP 锁
.tool-item.vip-locked {
  position: relative;

  &.locked {
    opacity: 0.7;

    .tool-icon {
      filter: grayscale(50%);
    }
  }
}

.tool-lock {
  position: absolute;
  top: 8rpx;
  right: 8rpx;
  font-size: 20rpx;
  background: rgba(255, 215, 0, 0.2);
  padding: 4rpx 8rpx;
  border-radius: 8rpx;
}

// 最近使用
.recent-section {
  padding: 32px 20px;
  position: relative;
  z-index: 1;
}

.storage-hint {
  text-align: center;
  margin-top: 24rpx;
  padding: 16rpx;

  text {
    font-size: 22rpx;
    color: rgba(255, 255, 255, 0.3);
  }
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
