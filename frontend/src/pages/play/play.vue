<template>
  <view class="play-page">
    <!-- 顶部音频可视化区域 - 随声音流动 -->
    <view class="visualizer-section" :class="{ playing: isPlaying }">
      <!-- 动态波形 - 根据播放进度变化 -->
      <view class="wave-container">
        <view
          v-for="i in 20"
          :key="i"
          class="wave-bar"
          :class="'bar-' + i"
          :style="getWaveStyle(i)"
        ></view>
      </view>
      <!-- 浮动粒子效果 -->
      <view class="particles">
        <view v-for="i in 8" :key="i" class="particle" :style="getParticleStyle(i)"></view>
      </view>
      <view class="visualizer-glow"></view>

      <!-- 当前播放的字幕 - 跟随声音流动 -->
      <view class="current-subtitle-display" :class="{ active: isPlaying }">
        <text class="current-subtitle-text">{{ currentSubtitleText || '等待播放...' }}</text>
      </view>

      <view class="header">
        <view class="back-btn" @click="goBack">
          <text class="back-icon">←</text>
        </view>
        <text class="header-title">{{ pageTitle }}</text>
        <view class="header-actions">
          <view class="collect-btn" @click="toggleCollect">
            <text class="collect-icon">{{ isCollected ? '★' : '☆' }}</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 主要内容区域 -->
    <view class="main-content">
      <!-- 播放控制区 -->
      <view class="player-card">
        <!-- 进度条 -->
        <view class="progress-wrapper">
          <text class="time-current">{{ formatTime(currentTime) }}</text>
          <slider
            class="progress-slider"
            :value="currentTime"
            :max="duration"
            activeColor="#667eea"
            backgroundColor="#e8e8e8"
            block-color="#667eea"
            block-size="14"
            @change="onSeek"
          />
          <text class="time-total">{{ formatTime(duration) }}</text>
        </view>

        <!-- 控制按钮 -->
        <view class="controls-row">
          <view class="speed-control" @click="changeSpeed">
            <text class="speed-value">{{ playbackRate }}x</text>
          </view>

          <view class="play-controls">
            <view class="control-btn prev" @click="prev">
              <text class="control-icon">⏮</text>
            </view>
            <view class="control-btn play-main" @click="togglePlay">
              <text class="play-icon">{{ isPlaying ? '⏸' : '▶' }}</text>
            </view>
            <view class="control-btn next" @click="next">
              <text class="control-icon">⏭</text>
            </view>
          </view>

          <view class="action-btns">
            <view class="action-btn" :class="{ active: showSubtitle }" @click="showSubtitle = !showSubtitle">
              <text class="action-text">词</text>
            </view>
            <view class="action-btn" :class="{ active: showChatPanel }" @click="toggleChatPanel">
              <text class="action-text">问</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 文本/字幕区域 -->
      <view v-if="showSubtitle && rawText" class="text-section">
        <view class="section-header">
          <text class="section-title">文本内容</text>
        </view>
        <scroll-view
          ref="textScrollViewRef"
          class="text-scroll"
          scroll-y
          :scroll-into-view="scrollToSubtitleId"
          scroll-with-animation
          @scrolltoupper="onScrollUpper"
          @scrolltolower="onScrollLower"
        >
          <view
            v-for="(sentence, index) in subtitles"
            :key="index"
            :id="'sentence-' + index"
            class="sentence-item"
            :class="{ active: currentSubtitleIndex === index }"
            @click="seekToSubtitle(index, sentence.startTime)"
          >
            <text class="sentence-text">{{ sentence.text }}</text>
          </view>
        </scroll-view>
      </view>

      <!-- 聊天面板 -->
      <view v-if="showChatPanel" class="chat-drawer">
        <view class="chat-header">
          <text class="chat-title">边听边问</text>
          <view class="chat-close" @click="showChatPanel = false">
            <text>✕</text>
          </view>
        </view>
        <scroll-view class="chat-messages" scroll-y>
          <view v-if="chatMessages.length === 0" class="chat-empty">
            <text>有问题随时问我</text>
          </view>
          <view
            v-for="(msg, index) in chatMessages"
            :key="index"
            class="chat-message"
            :class="msg.role"
          >
            <view class="message-content">{{ msg.content }}</view>
          </view>
        </scroll-view>
        <view class="chat-input-area">
          <input
            v-model="chatInput"
            class="chat-input"
            placeholder="输入问题..."
            @confirm="sendChatMessage"
          />
          <view class="send-btn" @click="sendChatMessage">
            <text>发送</text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { collectAudio, getAudioDetail } from '@/api'
import { getChatSessions, createChatSession, getChatMessages, sendChatMessage as sendMessage } from '@/api/chat'

const audioId = ref('')
const audioUrl = ref('')
const rawText = ref('')
const audioTitle = ref('')
const isCollected = ref(false)
const pageType = ref('normal') // normal, podcast
const podcastId = ref('')

// 播放状态
const isPlaying = ref(false)
const currentTime = ref(0)
const duration = ref(0)
const playbackRate = ref(1)
const audioContext = ref(null)
const chatAudioContext = ref(null) // 聊天音频上下文
const isPageInitialized = ref(false) // 防止页面重复初始化

// UI状态
const showSubtitle = ref(true)
const showChatPanel = ref(false)

// 字幕
const subtitles = ref([])
const currentSubtitleIndex = ref(0)
const textScrollViewRef = ref(null)
const scrollToSubtitleId = ref('') // 用于控制scroll-view滚动到指定位置

// 当前播放的字幕文本（用于顶部显示）
const currentSubtitleText = computed(() => {
  if (subtitles.value.length > 0 && currentSubtitleIndex.value >= 0) {
    return subtitles.value[currentSubtitleIndex.value]?.text || ''
  }
  return ''
})

// 从文本生成字幕 - 使用字符数比例计算时间，实现更精确的音文同步
function generateSubtitles(text) {
  if (!text) {
    subtitles.value = []
    return
  }

  const lines = []
  // 按句号、问号、感叹号分隔，确保过滤掉空句子
  const sentences = text.split(/[。！？\n]+/).filter(s => s && s.trim())

  if (sentences.length === 0 || sentences.length === 1) {
    // 如果没有分隔符或只有一个句子，按固定长度分割
    const charsPerLine = 50
    const charsCount = text.length
    const estimatedDuration = duration.value || 180
    const timePerChar = estimatedDuration / charsCount

    for (let i = 0; i < text.length; i += charsPerLine) {
      const chunkText = text.substring(i, Math.min(i + charsPerLine, text.length))
      if (chunkText.trim()) {
        const startTime = Math.floor(i * timePerChar)
        const endTime = Math.floor(Math.min(i + charsPerLine, text.length) * timePerChar)
        lines.push({
          startTime,
          endTime,
          text: chunkText.trim()
        })
      }
    }
  } else {
    // 根据字符数比例计算时间 - 更精确的同步
    const totalChars = text.length
    const estimatedDuration = duration.value || 180 // 默认3分钟
    let currentTime = 0

    sentences.forEach((sentence) => {
      const trimmedSentence = sentence.trim()
      if (trimmedSentence) {
        const sentenceLength = trimmedSentence.length
        // 按字符数比例计算该句子的时长
        const sentenceDuration = (sentenceLength / totalChars) * estimatedDuration
        const startTime = Math.floor(currentTime)
        const endTime = Math.floor(currentTime + sentenceDuration)
        currentTime += sentenceDuration

        lines.push({
          startTime,
          endTime,
          text: trimmedSentence
        })
      }
    })

    // 修正最后一个句子的结束时间
    if (lines.length > 0) {
      lines[lines.length - 1].endTime = estimatedDuration
    }
  }

  // 过滤掉空句子，确保索引正确
  subtitles.value = lines.filter(l => l.text && l.text.length > 0)

  console.log(`字幕生成完成: ${subtitles.value.length} 句, 时长=${duration.value}秒`)
  // 打印前几句字幕用于调试
  subtitles.value.slice(0, 3).forEach((sub, i) => {
    console.log(`  字幕${i}: startTime=${sub.startTime}s, text=${sub.text.substring(0, 15)}...`)
  })

  // 重置当前字幕索引
  currentSubtitleIndex.value = 0
}

// 监听duration变化，重新生成更精确的字幕时间轴
watch(duration, (newDuration) => {
  if (newDuration > 0 && rawText.value) {
    generateSubtitles(rawText.value)
  }
})

// 聊天
const chatMessages = ref([])
const chatInput = ref('')
const recording = ref(false)
const chatSessionId = ref('')

const pageTitle = computed(() => {
  if (pageType.value === 'podcast') return '播客播放'
  return audioTitle.value || '音频播放'
})

onMounted(async () => {
  // 防止重复初始化 - 先清理旧音频
  if (isPageInitialized.value) {
    // 如果已经初始化过，先停止并销毁旧音频
    if (audioContext.value) {
      try {
        audioContext.value.stop()
        audioContext.value.destroy()
      } catch (e) {
        console.warn('清理旧音频失败:', e)
      }
      audioContext.value = null
    }
  }
  isPageInitialized.value = true

  const pages = getCurrentPages()
  const page = pages[pages.length - 1]
  const options = page.options || {}

  audioId.value = options.id || ''
  audioUrl.value = decodeURIComponent(options.url || '')
  audioTitle.value = decodeURIComponent(options.title || '音频播放')
  pageType.value = options.type || 'normal'
  podcastId.value = options.podcastId || ''

  // 优先使用URL参数中的文本，同时异步获取完整文本
  const textFromUrl = decodeURIComponent(options.text || '')
  rawText.value = textFromUrl

  // 异步获取完整文本（优先使用）- 添加错误处理，避免阻塞音频播放
  if (audioId.value) {
    getAudioDetail(audioId.value).then(res => {
      if (res.data && res.data.rawText) {
        rawText.value = res.data.rawText
      }
    }).catch(e => {
      console.warn('获取音频详情失败，使用URL参数文本:', e.message)
      // 使用URL参数中的文本作为后备，不阻塞音频播放
    })
  }

  // 检查音频URL是否有效
  if (!audioUrl.value) {
    uni.showToast({ title: '音频地址无效', icon: 'none' })
    return
  }

  // 先停止并销毁任何已存在的音频上下文，防止重复播放
  if (audioContext.value) {
    try {
      audioContext.value.stop()
      audioContext.value.destroy()
    } catch (e) {
      console.warn('销毁旧音频上下文失败:', e)
    }
  }

  // 创建新的音频上下文
  audioContext.value = uni.createInnerAudioContext()
  audioContext.value.src = audioUrl.value
  audioContext.value.autoplay = true
  audioContext.value.playbackRate = playbackRate.value // 修复：初始化时设置倍速
  isPlaying.value = true

  // 音频事件监听 - 添加安全检查
  if (audioContext.value.onTimeUpdate) {
    audioContext.value.onTimeUpdate(() => {
      if (!audioContext.value) return
      currentTime.value = audioContext.value.currentTime
      // 确保有有效的时长才更新
      const audioDuration = audioContext.value.duration
      if (audioDuration && audioDuration > 0 && duration.value !== audioDuration) {
        duration.value = audioDuration
        // 重新生成字幕以获得更精确的时间
        if (rawText.value && subtitles.value.length === 0) {
          generateSubtitles(rawText.value)
        }
      }
      updateCurrentSubtitle()
    })
  }

  // H5 兼容：使用 onCanPlay 替代 onLoadedMetadata
  if (audioContext.value.onCanPlay) {
    audioContext.value.onCanPlay(() => {
      if (!audioContext.value) return
      const audioDuration = audioContext.value.duration || 0
      if (audioDuration > 0) {
        duration.value = audioDuration
        // 生成字幕
        if (rawText.value) {
          generateSubtitles(rawText.value)
        }
      }
    })
  }

  // 等待音频可以播放时再生成一次字幕，确保时长正确
  if (audioContext.value.onCanPlayThrough) {
    audioContext.value.onCanPlayThrough(() => {
      if (!audioContext.value) return
      const audioDuration = audioContext.value.duration || 0
      if (audioDuration > 0 && rawText.value) {
        duration.value = audioDuration
        generateSubtitles(rawText.value)
      }
    })
  }

  if (audioContext.value.onEnded) {
    audioContext.value.onEnded(() => {
      isPlaying.value = false
      currentTime.value = 0
    })
  }

  if (audioContext.value.onError) {
    audioContext.value.onError((e) => {
      console.error('播放错误:', e)
    })
  }

  // 如果是播客，加载字幕
  if (pageType.value === 'podcast') {
    loadPodcastData()
  }

  // 如果显示聊天面板，加载聊天数据
  if (showChatPanel.value) {
    loadChatData()
  }
})

onUnmounted(() => {
  // 页面卸载时彻底清理所有音频上下文，防止内存泄漏和重复播放
  // 清理主音频
  if (audioContext.value) {
    try {
      audioContext.value.stop()
      audioContext.value.destroy()
    } catch (e) {
      console.error('清理主音频上下文失败:', e)
    }
    audioContext.value = null
  }
  // 清理聊天音频
  if (chatAudioContext.value) {
    try {
      chatAudioContext.value.stop()
      chatAudioContext.value.destroy()
    } catch (e) {
      console.error('清理聊天音频上下文失败:', e)
    }
    chatAudioContext.value = null
  }
})

function loadPodcastData() {
  // 模拟加载播客字幕数据
  subtitles.value = [
    { startTime: 0, text: '主播A：大家好，欢迎收听今天的节目！' },
    { startTime: 5, text: '主播B：今天我们要讨论的话题是...' },
    { startTime: 10, text: '主播A：首先，让我们来了解一下背景...' },
    { startTime: 15, text: '主播B：没错，这个话题非常重要...' }
  ]
}

async function loadChatData() {
  try {
    const res = await getChatSessions()
    if (res.data && res.data.length > 0) {
      chatSessionId.value = res.data[0].id
      const msgRes = await getChatMessages(chatSessionId.value)
      chatMessages.value = msgRes.data || []
    } else {
      // 创建新会话
      const newSession = await createChatSession('新对话', rawText.value || '')
      if (newSession.data?.id) {
        chatSessionId.value = newSession.data.id
      }
    }
  } catch (e) {
    console.error('加载聊天数据失败:', e)
  }
}

// 记录上一次滚动到的字幕索引，避免重复滚动
let lastScrollIndex = -1

function updateCurrentSubtitle() {
  const time = currentTime.value
  for (let i = 0; i < subtitles.value.length; i++) {
    const sub = subtitles.value[i]
    // 使用开始时间和结束时间范围来判断当前字幕
    if (time >= sub.startTime && time < sub.endTime) {
      if (currentSubtitleIndex.value !== i) {
        console.log(`字幕切换: index=${i}, time=${time.toFixed(1)}s, startTime=${sub.startTime}s, text=${sub.text.substring(0, 10)}...`)
        currentSubtitleIndex.value = i
        // 自动滚动到当前字幕（仅当变化时）
        scrollToCurrentSubtitle()
      }
      break
    }
  }
}

// 滚动到当前播放的字幕位置
function scrollToCurrentSubtitle() {
  // 避免重复滚动
  if (lastScrollIndex === currentSubtitleIndex.value) {
    return
  }
  lastScrollIndex = currentSubtitleIndex.value

  // 使用 nextTick 确保 DOM 更新后再滚动
  nextTick(() => {
    scrollToSubtitleId.value = 'sentence-' + currentSubtitleIndex.value
    // 滚动后重置，以便下次可以再次触发
    setTimeout(() => {
      scrollToSubtitleId.value = ''
    }, 500)
  })
}

// 滚动到顶部
function onScrollUpper() {
  // 可以在这里处理滚动到顶的事件
}

// 滚动到底部
function onScrollLower() {
  // 可以在这里处理滚动到底的事件
}

function seekToSubtitle(index, startTime) {
  // 获取点击的句子信息
  const clickedSentence = subtitles.value[index]
  if (!clickedSentence) return

  // 更新当前字幕索引为点击的句子
  currentSubtitleIndex.value = index
  lastScrollIndex = index
  scrollToSubtitleId.value = 'sentence-' + index
  setTimeout(() => {
    scrollToSubtitleId.value = ''
  }, 100)

  // 跳转到对应时间 - 使用字幕的实际 startTime
  if (audioContext.value) {
    const seekTime = clickedSentence.startTime
    console.log(`点击跳转: 句子${index}, 时间=${seekTime}, 文本=${clickedSentence.text.substring(0, 10)}...`)
    audioContext.value.seek(seekTime)
  }
}

function goBack() {
  // 离开页面时停止音频
  if (audioContext.value) {
    audioContext.value.stop()
    audioContext.value.destroy()
    audioContext.value = null
  }
  uni.navigateBack()
}

// 防抖标志，防止快速点击
let isOperating = false

function togglePlay() {
  // 防止快速点击
  if (isOperating || !audioContext.value) return
  isOperating = true

  try {
    if (isPlaying.value) {
      audioContext.value.pause()
    } else {
      audioContext.value.play()
    }
    isPlaying.value = !isPlaying.value
  } catch (e) {
    console.error('播放控制失败:', e)
  } finally {
    setTimeout(() => {
      isOperating = false
    }, 200)
  }
}

function onSeek(e) {
  // 防止快速点击
  if (isOperating || !audioContext.value) return
  isOperating = true

  try {
    audioContext.value.seek(e.detail.value)
  } catch (e) {
    console.error('跳转失败:', e)
  } finally {
    setTimeout(() => {
      isOperating = false
    }, 200)
  }
}

function changeSpeed() {
  // 防止快速点击
  if (isOperating) return
  isOperating = true

  try {
    const speeds = [0.5, 0.75, 1.0, 1.25, 1.5, 1.75, 2.0]
    let index = speeds.indexOf(playbackRate.value)
    index = (index + 1) % speeds.length
    playbackRate.value = speeds[index]

    if (audioContext.value) {
      audioContext.value.playbackRate = playbackRate.value
    }

    uni.showToast({
      title: `已切换至 ${playbackRate.value}x`,
      icon: 'none'
    })
  } catch (e) {
    console.error('切换倍速失败:', e)
  } finally {
    setTimeout(() => {
      isOperating = false
    }, 300)
  }
}

function prev() {
  // 防止快速点击
  if (isOperating || !audioContext.value) return
  isOperating = true

  try {
    audioContext.value.seek(0)
    currentTime.value = 0
  } catch (e) {
    console.error('快退失败:', e)
  } finally {
    setTimeout(() => {
      isOperating = false
    }, 200)
  }
}

function next() {
  // 暂时没有下一首
}

function toggleChatPanel() {
  showChatPanel.value = !showChatPanel.value
  if (showChatPanel.value) {
    loadChatData()
  }
}

async function toggleCollect() {
  if (!audioId.value) return
  try {
    const res = await collectAudio(audioId.value)
    isCollected.value = res.data.isCollected
    uni.showToast({
      title: isCollected.value ? '已收藏' : '已取消收藏',
      icon: 'none'
    })
  } catch (e) {
    console.error('收藏失败:', e)
  }
}

async function sendChatMessage() {
  if (!chatInput.value.trim()) return

  const content = chatInput.value
  chatInput.value = ''

  // 添加用户消息
  chatMessages.value.push({ role: 'user', content })

  try {
    // 如果没有sessionId，先创建会话
    if (!chatSessionId.value) {
      const sessionRes = await getChatSessions()
      if (sessionRes.data && sessionRes.data.length > 0) {
        chatSessionId.value = sessionRes.data[0].id
      } else {
        // 创建新会话
        const newSession = await createChatSession('新对话', rawText.value || '')
        chatSessionId.value = newSession.data?.id || Date.now().toString()
      }
    }

    // 发送消息
    const res = await sendMessage(chatSessionId.value, content)
    if (res.data) {
      chatMessages.value.push({
        role: 'assistant',
        content: res.data.answer,
        audioUrl: res.data.audioUrl
      })
    }
  } catch (e) {
    console.error('发送消息失败:', e)
    chatMessages.value.push({
      role: 'assistant',
      content: '抱歉，我暂时无法回答您的问题。'
    })
  }
}

function playMessageAudio(url) {
  // 先停止之前的聊天音频
  if (chatAudioContext.value) {
    chatAudioContext.value.stop()
    chatAudioContext.value.destroy()
  }
  // 创建新的音频上下文
  chatAudioContext.value = uni.createInnerAudioContext()
  chatAudioContext.value.src = url
  chatAudioContext.value.play()
}

function startVoiceRecord() {
  recording.value = true
  uni.showToast({ title: '开始录音', icon: 'none' })
}

function endVoiceRecord() {
  recording.value = false
  // 模拟识别结果
  const mockText = '这是模拟的语音识别结果'
  chatInput.value = mockText
  uni.showToast({ title: '识别完成', icon: 'none' })
}

// 动态波形样式 - 根据音频时间变化
function getWaveStyle(index) {
  if (!isPlaying.value) {
    return { height: '20px', opacity: 0.3 }
  }
  // 使用正弦函数创建更自然的波动
  const time = currentTime.value
  const baseHeight = 25
  const variance = 60
  // 每个波形条有不同的相位偏移
  const phase = (index * 0.5) + (time * 2)
  const height = baseHeight + Math.abs(Math.sin(phase)) * variance
  const opacity = 0.4 + Math.abs(Math.sin(phase * 0.7)) * 0.6

  return {
    height: `${height}px`,
    opacity: opacity
  }
}

// 动态粒子样式
function getParticleStyle(index) {
  if (!isPlaying.value) {
    return { opacity: 0 }
  }
  const time = currentTime.value
  // 粒子浮动动画
  const baseTop = 20 + (index * 8) % 60
  const offset = Math.sin(time + index * 0.8) * 15

  return {
    top: `${baseTop + offset}%`,
    opacity: 0.2 + Math.abs(Math.sin(time * 0.5 + index)) * 0.3,
    animationDelay: `${index * 0.3}s`
  }
}

function formatTime(seconds) {
  if (!seconds || isNaN(seconds)) return '00:00'
  const min = Math.floor(seconds / 60)
  const sec = Math.floor(seconds % 60)
  return `${min.toString().padStart(2, '0')}:${sec.toString().padStart(2, '0')}`
}
</script>

<style lang="scss" scoped>
.play-page {
  min-height: 100vh;
  background: #0a0a0f;
  display: flex;
  flex-direction: column;
}

// 顶部可视化区域 - 随声音流动
.visualizer-section {
  position: relative;
  background: linear-gradient(180deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
  padding: 50px 20px 60px;
  overflow: hidden;

  &.playing {
    .visualizer-glow {
      animation: glow 3s ease-in-out infinite alternate;
    }
    .particle {
      animation: float 4s ease-in-out infinite;
    }
  }
}

// 动态波形容器
.wave-container {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 140px;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  padding: 0 10px;
  pointer-events: none;
}

// 动态波形条 - 高度由JS控制
.wave-bar {
  width: 4px;
  background: linear-gradient(180deg, rgba(102, 126, 234, 0.9) 0%, rgba(118, 75, 162, 0.6) 40%, transparent 100%);
  border-radius: 2px;
  transition: height 0.15s ease-out, opacity 0.15s ease-out;
  margin: 0 3px;
}

// 浮动粒子
.particles {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
}

.particle {
  position: absolute;
  width: 6px;
  height: 6px;
  background: rgba(102, 126, 234, 0.4);
  border-radius: 50%;
  left: 10%;
  transition: all 0.3s ease;
}

@keyframes float {
  0%, 100% {
    transform: translateY(0) scale(1);
  }
  50% {
    transform: translateY(-20px) scale(1.2);
  }
}

.visualizer-glow {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, rgba(102, 126, 234, 0.3) 0%, transparent 70%);
  pointer-events: none;
}

// 当前播放的字幕显示 - 跟随声音流动
.current-subtitle-display {
  position: absolute;
  bottom: 180px;
  left: 20px;
  right: 20px;
  padding: 20px 24px;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  border: 1px solid rgba(102, 126, 234, 0.3);
  min-height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 20;
  transition: all 0.3s ease;
}

.current-subtitle-display.active {
  background: rgba(102, 126, 234, 0.3);
  border-color: rgba(102, 126, 234, 0.6);
}

.current-subtitle-text {
  font-size: 18px;
  line-height: 1.6;
  color: #fff;
  text-align: center;
  font-weight: 500;
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.5);
  transition: opacity 0.2s ease;
}

@keyframes glow {
  0% {
    transform: translate(-50%, -50%) scale(0.6) rotate(0deg);
    opacity: 0.3;
  }
  50% {
    transform: translate(-50%, -50%) scale(1) rotate(180deg);
    opacity: 0.5;
  }
  100% {
    transform: translate(-50%, -50%) scale(0.6) rotate(360deg);
    opacity: 0.3;
  }
}

.header {
  position: relative;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: space-between;
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
  color: rgba(255, 255, 255, 0.9);
}

.header-title {
  font-size: 18px;
  font-weight: 600;
  color: #fff;
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.3);
}

.header-actions {
  display: flex;
  align-items: center;
}

.collect-btn {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.collect-icon {
  font-size: 24px;
  color: #ffd700;
  text-shadow: 0 2px 10px rgba(255, 215, 0, 0.5);
}

// 主要内容区
.main-content {
  flex: 1;
  padding: 20px;
  background: #0a0a0f;
}

// 播放控制卡片
.player-card {
  background: linear-gradient(145deg, #1e1e32 0%, #16162a 100%);
  border-radius: 24px;
  padding: 24px;
  margin-bottom: 20px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.4);
  border: 1px solid rgba(102, 126, 234, 0.1);
}

.progress-wrapper {
  display: flex;
  align-items: center;
  margin-bottom: 24px;
}

.time-current, .time-total {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  font-family: 'Courier New', monospace;
  width: 45px;
}

.progress-slider {
  flex: 1;
  margin: 0 8px;
}

.controls-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.speed-control {
  width: 50px;
  height: 36px;
  background: rgba(102, 126, 234, 0.15);
  border-radius: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.speed-value {
  font-size: 13px;
  color: #667eea;
  font-weight: 600;
}

.play-controls {
  display: flex;
  align-items: center;
  gap: 20px;
}

.control-btn {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.05);

  &.play-main {
    width: 64px;
    height: 64px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    box-shadow: 0 4px 20px rgba(102, 126, 234, 0.4);
  }

  &.prev, &.next {
    width: 40px;
    height: 40px;
  }
}

.control-icon {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.8);
}

.play-icon {
  font-size: 24px;
  color: #fff;
}

.action-btns {
  display: flex;
  gap: 8px;
}

.action-btn {
  width: 36px;
  height: 36px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.05);
  display: flex;
  align-items: center;
  justify-content: center;

  &.active {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  }
}

.action-text {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.8);
}

// 文本/字幕区域
.text-section {
  background: linear-gradient(145deg, #1e1e32 0%, #16162a 100%);
  border-radius: 24px;
  padding: 20px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.4);
  border: 1px solid rgba(102, 126, 234, 0.1);
  max-height: 45vh;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
}

.text-scroll {
  max-height: 35vh;
}

.sentence-item {
  padding: 12px 16px;
  border-radius: 12px;
  margin-bottom: 8px;
  transition: all 0.3s ease;
  background: rgba(255, 255, 255, 0.02);
}

.sentence-item.active {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.3) 0%, rgba(118, 75, 162, 0.3) 100%);
  border: 1px solid rgba(102, 126, 234, 0.3);
}

.sentence-item.active .sentence-text {
  color: #fff;
  font-weight: 500;
}

.sentence-text {
  font-size: 15px;
  line-height: 1.8;
  color: rgba(255, 255, 255, 0.7);
}

// 聊天抽屉
.chat-drawer {
  background: linear-gradient(145deg, #1e1e32 0%, #16162a 100%);
  border-radius: 24px 24px 0 0;
  box-shadow: 0 -10px 40px rgba(0, 0, 0, 0.4);
  border: 1px solid rgba(102, 126, 234, 0.1);
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  height: 60vh;
  display: flex;
  flex-direction: column;
  z-index: 100;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.chat-title {
  font-size: 16px;
  font-weight: 600;
  color: #fff;
}

.chat-close {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 50%;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.6);
}

.chat-messages {
  flex: 1;
  padding: 20px;
}

.chat-empty {
  text-align: center;
  color: rgba(255, 255, 255, 0.4);
  padding: 40px 0;
}

.chat-message {
  margin-bottom: 16px;

  &.user {
    text-align: right;

    .message-content {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: #fff;
      display: inline-block;
      padding: 12px 18px;
      border-radius: 18px 18px 4px 18px;
    }
  }

  &.assistant {
    text-align: left;

    .message-content {
      background: rgba(255, 255, 255, 0.08);
      color: rgba(255, 255, 255, 0.9);
      display: inline-block;
      padding: 12px 18px;
      border-radius: 18px 18px 18px 4px;
    }
  }
}

.message-content {
  font-size: 14px;
  line-height: 1.6;
  max-width: 80%;
}

.chat-input-area {
  display: flex;
  gap: 12px;
  padding: 16px 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.chat-input {
  flex: 1;
  height: 44px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 22px;
  padding: 0 18px;
  font-size: 14px;
  color: #fff;

  &::placeholder {
    color: rgba(255, 255, 255, 0.4);
  }
}

.send-btn {
  padding: 0 20px;
  height: 44px;
  line-height: 44px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border-radius: 22px;
  font-size: 14px;
  font-weight: 500;
}
</style>
