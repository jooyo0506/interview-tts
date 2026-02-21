<template>
  <view class="detail-page">
    <!-- 顶部 -->
    <view class="header">
      <view class="back-btn" @click="goBack">
        <text class="back-icon">←</text>
      </view>
      <text class="header-title">{{ playlistName }}</text>
      <view class="placeholder"></view>
    </view>

    <!-- 播放控制 -->
    <view class="play-bar" v-if="currentAudio">
      <view class="play-bar-info">
        <text class="play-bar-title">{{ currentAudio.title }}</text>
      </view>
      <view class="play-bar-actions">
        <view class="play-btn" @click="togglePlay">
          <text>{{ isPlaying ? '⏸' : '▶' }}</text>
        </view>
      </view>
    </view>

    <!-- 列表 -->
    <scroll-view class="content" scroll-y>
      <view v-if="audios.length === 0" class="empty-state">
        <text class="empty-icon">🎵</text>
        <text class="empty-text">播放列表为空</text>
      </view>

      <view v-else class="audio-list">
        <view
          v-for="(item, index) in audios"
          :key="item.id"
          class="audio-item"
          :class="{ playing: currentIndex === index }"
          @click="playAudio(item, index)"
        >
          <view class="item-index">
            <text v-if="currentIndex !== index">{{ index + 1 }}</text>
            <text v-else>{{ isPlaying ? '🔊' : '⏸' }}</text>
          </view>
          <view class="item-content">
            <text class="item-title">{{ item.title }}</text>
            <text class="item-meta">{{ formatDuration(item.duration) }}</text>
          </view>
          <view class="item-action" @click.stop="removeAudio(item)">
            <text>✕</text>
          </view>
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { getPlaylistAudios, removeAudioFromPlaylist } from '@/api/playlist'

const playlistId = ref('')
const playlistName = ref('')
const audios = ref([])
const currentIndex = ref(-1)
const currentAudio = ref(null)
const isPlaying = ref(false)
let audioContext = null

onMounted(() => {
  const pages = getCurrentPages()
  const page = pages[pages.length - 1]
  const options = page.options || {}

  playlistId.value = options.id || ''
  playlistName.value = decodeURIComponent(options.name || '播放列表')

  loadAudios()
})

async function loadAudios() {
  try {
    const res = await getPlaylistAudios(playlistId.value)
    audios.value = res.data || []
  } catch (e) {
    console.error('加载失败:', e)
  }
}

function goBack() {
  uni.navigateBack()
}

function playAudio(item, index) {
  currentIndex.value = index
  currentAudio.value = item

  if (audioContext) {
    audioContext.stop()
  }

  audioContext = uni.createInnerAudioContext()
  audioContext.src = item.r2Url
  audioContext.play()
  isPlaying.value = true

  audioContext.onEnded(() => {
    isPlaying.value = false
    // 播放下一首
    if (index < audios.value.length - 1) {
      playAudio(audios.value[index + 1], index + 1)
    }
  })
}

function togglePlay() {
  if (!audioContext) return

  if (isPlaying.value) {
    audioContext.pause()
    isPlaying.value = false
  } else {
    audioContext.play()
    isPlaying.value = true
  }
}

async function removeAudio(item) {
  uni.showModal({
    title: '确认移除',
    content: '确定要从播放列表中移除这首音频吗？',
    success: async (res) => {
      if (res.confirm) {
        try {
          await removeAudioFromPlaylist(playlistId.value, item.id)
          uni.showToast({ title: '已移除', icon: 'success' })
          loadAudios()
        } catch (e) {
          uni.showToast({ title: '移除失败', icon: 'none' })
        }
      }
    }
  })
}

function formatDuration(seconds) {
  if (!seconds) return '0:00'
  const min = Math.floor(seconds / 60)
  const sec = seconds % 60
  return `${min}:${sec.toString().padStart(2, '0')}`
}

onUnmounted(() => {
  if (audioContext) {
    audioContext.stop()
    audioContext.destroy()
  }
})
</script>

<style lang="scss" scoped>
.detail-page {
  min-height: 100vh;
  background: #f5f5f5;
  display: flex;
  flex-direction: column;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 50px 20px 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
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

.header-title {
  font-size: 18px;
  font-weight: 600;
  color: #fff;
}

.placeholder {
  width: 40px;
}

.play-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  background: #fff;
  border-bottom: 1px solid #eee;
}

.play-bar-title {
  font-size: 14px;
  color: #333;
}

.play-bar-actions {
  display: flex;
  gap: 12px;
}

.play-btn {
  width: 40px;
  height: 40px;
  background: #667eea;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.content {
  flex: 1;
  padding: 15px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 80px 0;
}

.empty-icon {
  font-size: 50px;
  margin-bottom: 16px;
}

.empty-text {
  font-size: 14px;
  color: #999;
}

.audio-list {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
}

.audio-item {
  display: flex;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #f5f5f5;
  &.playing {
    background: #f0f7ff;
    .item-title {
      color: #667eea;
    }
  }
  &:last-child {
    border-bottom: none;
  }
}

.item-index {
  width: 30px;
  font-size: 14px;
  color: #999;
  text-align: center;
}

.item-content {
  flex: 1;
}

.item-title {
  display: block;
  font-size: 14px;
  color: #333;
  margin-bottom: 4px;
}

.item-meta {
  font-size: 12px;
  color: #999;
}

.item-action {
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
}
</style>
