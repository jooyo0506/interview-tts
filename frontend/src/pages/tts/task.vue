<template>
  <view class="task-page">
    <!-- 顶部导航 -->
    <view class="nav-bar">
      <view class="back-btn" @click="goBack">
        <text class="back-icon">←</text>
      </view>
      <text class="nav-title">处理中</text>
      <view class="placeholder"></view>
    </view>

    <!-- 主要内容 -->
    <view class="content">
      <!-- 动画区域 -->
      <view class="animation-section">
        <!-- 外圈旋转 -->
        <view class="outer-ring">
          <view class="ring-dot" v-for="i in 12" :key="i" :style="{ '--i': i }"></view>
        </view>

        <!-- 内圈动画 -->
        <view class="inner-circle" :class="{ spinning: status === 'processing' }">
          <text class="status-icon">{{ statusIcon }}</text>
        </view>

        <!-- 脉冲效果 -->
        <view class="pulse-ring" :class="{ active: status === 'processing' }"></view>
      </view>

      <!-- 状态文字 -->
      <view class="status-section">
        <text class="status-title">{{ statusTitle }}</text>
        <text class="status-desc">{{ statusDesc }}</text>
      </view>

      <!-- 进度阶段 -->
      <view class="progress-steps">
        <view class="step" :class="{ active: stepIndex >= 0, current: stepIndex === 0 }">
          <view class="step-icon">
            <text v-if="stepIndex > 0">✓</text>
            <text v-else>1</text>
          </view>
          <text class="step-text">提交任务</text>
        </view>

        <view class="step-line" :class="{ active: stepIndex >= 1 }"></view>

        <view class="step" :class="{ active: stepIndex >= 1, current: stepIndex === 1 }">
          <view class="step-icon">
            <text v-if="stepIndex > 1">✓</text>
            <text v-else>2</text>
          </view>
          <text class="step-text">AI处理中</text>
        </view>

        <view class="step-line" :class="{ active: stepIndex >= 2 }"></view>

        <view class="step" :class="{ active: stepIndex >= 2, current: stepIndex === 2 }">
          <view class="step-icon">
            <text v-if="stepIndex > 2">✓</text>
            <text v-else>3</text>
          </view>
          <text class="step-text">合成完成</text>
        </view>
      </view>

      <!-- 估算时间 -->
      <view class="time-estimate" v-if="status === 'processing'">
        <text class="time-label">预计等待</text>
        <text class="time-value">{{ estimatedTime }}</text>
      </view>

      <!-- 操作按钮 -->
      <view class="actions">
        <button class="btn-secondary" @click="goToHistory">
          <text>查看我的记录</text>
        </button>
        <button class="btn-primary" @click="cancelTask">
          <text>取消</text>
        </button>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { queryTaskStatus } from '@/api'

const audioFileId = ref('')
const taskId = ref('')
const rawText = ref('')
const useEmotion = ref(false)
const status = ref('processing') // processing, success, error
const stepIndex = ref(1)
const pollCount = ref(0)
const estimatedTime = ref('10-30秒')
const pollingInterval = ref(null)
const minPollCount = ref(3) // 最少轮询次数，根据文本字数动态调整
const hasNavigated = ref(false) // 防止多次跳转

const statusIcon = computed(() => {
  switch (status.value) {
    case 'processing': return '🎵'
    case 'success': return '✅'
    case 'error': return '❌'
    default: return '⏳'
  }
})

const statusTitle = computed(() => {
  switch (status.value) {
    case 'processing': return 'AI正在合成语音'
    case 'success': return '合成完成！'
    case 'error': return '合成失败'
    default: return '等待中'
  }
})

const statusDesc = computed(() => {
  switch (status.value) {
    case 'processing': return '长文本需要较长时间处理，请耐心等待...'
    case 'success': return '正在跳转播放页面...'
    case 'error': return '抱歉，语音合成遇到问题'
    default: return ''
  }
})

onMounted(() => {
  const pages = getCurrentPages()
  const page = pages[pages.length - 1]
  const options = page.options || {}

  audioFileId.value = options.id || ''
  taskId.value = options.taskId || ''
  rawText.value = decodeURIComponent(options.text || '')
  useEmotion.value = options.emotion === 'true'

  if (taskId.value) {
    startPolling()
  } else {
    status.value = 'error'
  }
})

onUnmounted(() => {
  stopPolling()
})

// 根据文本字数计算最少轮询次数
function calculateMinPollCount(textLength) {
  if (!textLength || textLength < 100) return 2      // 100字以下：2次
  if (textLength < 500) return 3                     // 100-500字：3次
  if (textLength < 1000) return 4                    // 500-1000字：4次
  if (textLength < 3000) return 5                    // 1000-3000字：5次
  if (textLength < 5000) return 6                    // 3000-5000字：6次
  return 8                                           // 5000字以上：8次
}

function startPolling() {
  if (pollingInterval.value) return

  // 重置跳转标志，允许新跳转
  hasNavigated.value = false
  stepIndex.value = 1

  // 根据文本字数计算最少轮询次数
  minPollCount.value = calculateMinPollCount(rawText.value.length)
  console.log(`文本字数: ${rawText.value.length}, 最少轮询次数: ${minPollCount.value}`)

  updateEstimatedTime()

  pollingInterval.value = setInterval(async () => {
    pollCount.value++

    // 更新估算时间
    if (pollCount.value % 5 === 0) {
      updateEstimatedTime()
    }

    try {
      const res = await queryTaskStatus(audioFileId.value, taskId.value, useEmotion.value)
      console.log('任务状态:', res.data)

      if (res.data && (res.data.r2Url || res.data.audioUrl)) {
        // 必须满足最少轮询次数才跳转，确保音频已准备好
        if (pollCount.value < minPollCount.value) {
          console.log(`轮询次数 ${pollCount.value} < 最少 ${minPollCount.value}，继续等待...`)
          return // 继续轮询
        }

        // 防止多次跳转
        if (hasNavigated.value) {
          console.log('已经跳转过，跳过')
          return
        }
        hasNavigated.value = true

        // 满足最少轮询次数，可以跳转
        stopPolling()  // 确保先停止轮询
        stepIndex.value = 2
        status.value = 'success'

        const audioUrlFinal = res.data.r2Url || res.data.audioUrl

        // 延迟跳转，确保轮询已停止
        setTimeout(() => {
          const title = rawText.value.substring(0, 20) + (rawText.value.length > 20 ? '...' : '')
          uni.navigateTo({
            url: `/pages/play/play?id=${audioFileId.value}&url=${encodeURIComponent(audioUrlFinal)}&text=${encodeURIComponent(rawText.value.substring(0, 1000))}&title=${encodeURIComponent(title)}`
          })
        }, 500)
      } else if (pollCount.value >= 180) {
        // 超时 3分钟
        stopPolling()
        status.value = 'error'
        uni.showToast({
          title: '处理超时，请稍后查看',
          icon: 'none'
        })
      }
    } catch (e) {
      console.error('查询任务失败:', e)
      // 网络错误等也要停止轮询，避免一直失败
      uni.showToast({
        title: '网络异常，请检查网络',
        icon: 'none'
      })
    }
  }, 1000)
}

function stopPolling() {
  if (pollingInterval.value) {
    clearInterval(pollingInterval.value)
    pollingInterval.value = null
  }
}

function updateEstimatedTime() {
  const seconds = pollCount.value
  if (seconds < 10) {
    estimatedTime.value = '10-30秒'
  } else if (seconds < 30) {
    estimatedTime.value = '20-40秒'
  } else if (seconds < 60) {
    estimatedTime.value = '约1分钟'
  } else {
    estimatedTime.value = '1-2分钟'
  }
}

function goBack() {
  // 如果任务正在处理中，提示用户
  if (status.value === 'processing') {
    uni.showModal({
      title: '提示',
      content: '任务正在处理中，离开后将稍后在"我的记录"中查看',
      success: (res) => {
        if (res.confirm) {
          stopPolling()
          uni.switchTab({
            url: '/pages/index/index'
          })
        }
      }
    })
  } else {
    uni.switchTab({
      url: '/pages/index/index'
    })
  }
}

function goToHistory() {
  uni.switchTab({
    url: '/pages/my/my'
  })
}

function cancelTask() {
  stopPolling()
  uni.showModal({
    title: '确认取消',
    content: '确定要取消当前任务吗？',
    success: (res) => {
      if (res.confirm) {
        uni.switchTab({
          url: '/pages/index/index'
        })
      } else {
        startPolling()
      }
    }
  })
}
</script>

<style lang="scss" scoped>
.task-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
  display: flex;
  flex-direction: column;
}

.nav-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 50px 20px 16px;
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

.nav-title {
  font-size: 18px;
  font-weight: 600;
  color: #fff;
}

.placeholder {
  width: 40px;
}

.content {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 24px;
}

// 动画区域
.animation-section {
  position: relative;
  width: 180px;
  height: 180px;
  margin-bottom: 40px;
}

// 外圈
.outer-ring {
  position: absolute;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  border: 2px solid rgba(102, 126, 234, 0.2);
}

.ring-dot {
  position: absolute;
  width: 8px;
  height: 8px;
  background: #667eea;
  border-radius: 50%;
  top: -4px;
  left: 50%;
  transform: translateX(-50%);
  opacity: 0;
  animation: orbit 3s linear infinite;
  animation-delay: calc(var(--i) * 0.1s);
}

@keyframes orbit {
  0% {
    transform: translateX(-50%) rotate(0deg);
    opacity: 0;
  }
  20% {
    opacity: 1;
  }
  80% {
    opacity: 1;
  }
  100% {
    transform: translateX(-50%) rotate(360deg);
    opacity: 0;
  }
}

// 内圈
.inner-circle {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 100px;
  height: 100px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 10px 40px rgba(102, 126, 234, 0.4);

  &.spinning {
    animation: pulse 2s ease-in-out infinite;
  }
}

@keyframes pulse {
  0%, 100% {
    transform: translate(-50%, -50%) scale(1);
  }
  50% {
    transform: translate(-50%, -50%) scale(1.05);
  }
}

.status-icon {
  font-size: 40px;
}

// 脉冲
.pulse-ring {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 140px;
  height: 140px;
  border-radius: 50%;
  border: 2px solid rgba(102, 126, 234, 0.3);

  &.active {
    animation: ripple 2s ease-out infinite;
  }
}

@keyframes ripple {
  0% {
    transform: translate(-50%, -50%) scale(0.8);
    opacity: 0.8;
  }
  100% {
    transform: translate(-50%, -50%) scale(1.5);
    opacity: 0;
  }
}

// 状态文字
.status-section {
  text-align: center;
  margin-bottom: 40px;
}

.status-title {
  display: block;
  font-size: 22px;
  font-weight: 600;
  color: #fff;
  margin-bottom: 8px;
}

.status-desc {
  display: block;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.6);
}

// 进度阶段
.progress-steps {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 40px;
}

.step {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.step-icon {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.5);
  margin-bottom: 8px;
  transition: all 0.3s ease;

  .step.active & {
    background: #667eea;
    color: #fff;
  }

  .step.current & {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: #fff;
    box-shadow: 0 4px 15px rgba(102, 126, 234, 0.5);
  }
}

.step-text {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);

  .step.active & {
    color: #fff;
  }
}

.step-line {
  width: 40px;
  height: 2px;
  background: rgba(255, 255, 255, 0.1);
  margin: 0 8px;
  margin-bottom: 24px;
  transition: all 0.3s ease;

  &.active {
    background: #667eea;
  }
}

// 估算时间
.time-estimate {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 40px;
}

.time-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
  margin-bottom: 4px;
}

.time-value {
  font-size: 16px;
  color: #667eea;
  font-weight: 500;
}

// 操作按钮
.actions {
  display: flex;
  gap: 16px;
  width: 100%;
}

.btn-secondary, .btn-primary {
  flex: 1;
  height: 48px;
  border-radius: 24px;
  font-size: 15px;
  font-weight: 500;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
}

.btn-secondary {
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.8);
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}
</style>
