<template>
  <view class="clone-page">
    <!-- 顶部导航 -->
    <view class="nav-bar">
      <view class="back-btn" @click="goBack">
        <text class="back-icon">←</text>
      </view>
      <view class="nav-center">
        <text class="nav-title">声音复刻</text>
        <text class="nav-subtitle">克隆您的声音，用于语音合成</text>
      </view>
      <view class="placeholder"></view>
    </view>

    <!-- 主要内容 -->
    <view class="main-content">
      <!-- 创建新克隆 -->
      <view class="create-section">
        <view class="section-title">创建新声音</view>

        <view class="form-item">
          <input
            v-model="voiceName"
            class="name-input"
            placeholder="请输入声音名称..."
          />
        </view>

        <view class="form-item">
          <view class="upload-tip">请上传3-5个语音样本（每个10-30秒）</view>
          <view class="upload-btns">
            <button class="upload-btn" @click="chooseAudio">
              {{ uploading ? '上传中...' : '选择音频文件' }}
            </button>
            <button v-if="sampleUrls.length > 0" class="clear-btn" @click="clearSamples">
              清空
            </button>
          </view>
        </view>

        <!-- 已上传样本 -->
        <view v-if="sampleUrls.length > 0" class="samples-section">
          <view class="samples-title">已上传样本 ({{ sampleUrls.length }})</view>
          <view v-for="(url, index) in sampleUrls" :key="index" class="sample-item">
            <text class="sample-name">样本 {{ index + 1 }}</text>
            <text class="sample-status">已上传</text>
          </view>
        </view>

        <button
          class="create-btn"
          :disabled="!canCreate || creating"
          @click="handleCreate"
        >
          {{ creating ? '创建中...' : '开始复刻' }}
        </button>
      </view>

      <!-- 已有克隆声音 -->
      <view class="list-section">
        <view class="section-title">我的克隆声音</view>

        <view v-if="clonedVoices.length === 0" class="empty-tip">
          暂无克隆声音
        </view>

        <view v-else class="voice-list">
          <view
            v-for="voice in clonedVoices"
            :key="voice.id"
            class="voice-item"
            @click="useVoice(voice)"
          >
            <view class="voice-info">
              <text class="voice-name">{{ voice.name }}</text>
              <text class="voice-status" :class="voice.status">
                {{ getStatusText(voice.status) }}
              </text>
            </view>
            <view class="voice-time">{{ voice.createTime }}</view>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { getUserKey, initUser, uploadAudio } from '@/api'
import { getVoiceCloneList, createVoiceClone, getVoiceCloneStatus } from '@/api/voice'

const voiceName = ref('')
const sampleUrls = ref([])
const uploading = ref(false)
const creating = ref(false)
const clonedVoices = ref([])
const pollingIntervals = {}

const canCreate = computed(() => {
  return voiceName.value.trim().length > 0 && sampleUrls.value.length >= 3
})

onMounted(async () => {
  const userKey = getUserKey()
  await initUser(userKey)
  await loadClonedVoices()
})

// 页面卸载时清理所有轮询，防止内存泄漏
onUnmounted(() => {
  for (const voiceId in pollingIntervals) {
    if (pollingIntervals[voiceId]) {
      clearInterval(pollingIntervals[voiceId])
    }
  }
})

async function loadClonedVoices() {
  try {
    const res = await getVoiceCloneList()
    clonedVoices.value = res.data || []
  } catch (e) {
    console.error('获取克隆声音失败:', e)
  }
}

async function chooseAudio() {
  if (sampleUrls.value.length >= 5) {
    uni.showToast({
      title: '最多上传5个样本',
      icon: 'none'
    })
    return
  }

  // 真实文件选择
  uni.chooseFile({
    count: 5 - sampleUrls.value.length,
    type: 'audio',
    success: async (res) => {
      uploading.value = true
      let uploadedCount = 0

      try {
        for (const file of res.tempFiles) {
          if (sampleUrls.value.length >= 5) break

          // 检查文件大小 (最大30MB)
          if (file.size > 30 * 1024 * 1024) {
            uni.showToast({
              title: `${file.name} 超过30MB`,
              icon: 'none'
            })
            continue
          }

          // 上传到服务器
          try {
            const uploadRes = await uploadAudio(file.path)
            if (uploadRes.data && uploadRes.data.url) {
              sampleUrls.value.push(uploadRes.data.url)
              uploadedCount++
            }
          } catch (e) {
            console.error(`上传 ${file.name} 失败:`, e)
            uni.showToast({
              title: `${file.name} 上传失败`,
              icon: 'none'
            })
          }
        }

        if (uploadedCount > 0) {
          uni.showToast({
            title: `已上传 ${uploadedCount} 个样本`,
            icon: 'success'
          })
        }
      } catch (e) {
        console.error('上传失败:', e)
        uni.showToast({
          title: '上传失败',
          icon: 'none'
        })
      } finally {
        uploading.value = false
      }
    },
    fail: () => {
      // 用户取消选择
    }
  })
}

function clearSamples() {
  uni.showModal({
    title: '确认清空',
    content: '确定要清空所有已选样本吗？',
    success: (res) => {
      if (res.confirm) {
        sampleUrls.value = []
        uni.showToast({
          title: '已清空',
          icon: 'none'
        })
      }
    }
  })
}

async function handleCreate() {
  if (!canCreate.value || creating.value) return

  creating.value = true
  try {
    const res = await createVoiceClone(voiceName.value, sampleUrls.value)
    const newVoice = res.data
    clonedVoices.value.unshift(newVoice)

    // 开始轮询状态
    pollStatus(newVoice.id)

    uni.showToast({
      title: '创建成功',
      icon: 'success'
    })

    // 重置表单
    voiceName.value = ''
    sampleUrls.value = []
  } catch (e) {
    console.error('创建失败:', e)
    uni.showToast({
      title: '创建失败',
      icon: 'none'
    })
  } finally {
    creating.value = false
  }
}

function pollStatus(voiceId) {
  if (pollingIntervals[voiceId]) {
    clearInterval(pollingIntervals[voiceId])
  }

  pollingIntervals[voiceId] = setInterval(async () => {
    try {
      const res = await getVoiceCloneStatus(voiceId)
      const voice = res.data

      // 更新列表中的状态
      const index = clonedVoices.value.findIndex(v => v.id === voiceId)
      if (index !== -1) {
        clonedVoices.value[index] = voice
      }

      // 如果完成或失败，停止轮询
      if (voice.status === 'COMPLETED' || voice.status === 'FAILED') {
        clearInterval(pollingIntervals[voiceId])
        delete pollingIntervals[voiceId]
      }
    } catch (e) {
      console.error('轮询状态失败:', e)
    }
  }, 3000)
}

function getStatusText(status) {
  const statusMap = {
    'PENDING': '等待中',
    'PROCESSING': '处理中',
    'COMPLETED': '已完成',
    'FAILED': '失败'
  }
  return statusMap[status] || status
}

function useVoice(voice) {
  if (voice.status !== 'COMPLETED') {
    uni.showToast({
      title: '该声音尚未完成复刻',
      icon: 'none'
    })
    return
  }

  // 使用克隆的声音
  uni.showToast({
    title: '已选择该声音',
    icon: 'success'
  })

  // 返回上一页并传递选中的声音
  const pages = getCurrentPages()
  const prevPage = pages[pages.length - 2]
  if (prevPage) {
    prevPage.$vm.selectedVoice = voice.voiceId
  }
  uni.navigateBack()
}

function goBack() {
  uni.navigateBack()
}
</script>

<style lang="scss" scoped>
.clone-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #4facfe 0%, #00f2fe 100%);
  padding-bottom: 50px;
}

.nav-bar {
  display: flex;
  align-items: center;
  padding: 60px 20px 30px;
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
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

.create-section, .list-section {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 20px;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin-bottom: 16px;
}

.form-item {
  margin-bottom: 16px;
}

.name-input {
  width: 100%;
  height: 44px;
  font-size: 14px;
  border: 1px solid #eee;
  border-radius: 8px;
  padding: 0 12px;
  box-sizing: border-box;
}

.upload-tip {
  font-size: 13px;
  color: #666;
  margin-bottom: 10px;
}

.upload-btn {
  flex: 1;
  height: 40px;
  line-height: 40px;
  background: #4facfe;
  color: #fff;
  border-radius: 20px;
  font-size: 14px;
  border: none;
}

.upload-btns {
  display: flex;
  gap: 10px;
}

.clear-btn {
  width: 80px;
  height: 40px;
  line-height: 40px;
  background: #f5f5f5;
  color: #666;
  border-radius: 20px;
  font-size: 14px;
  border: none;
}

.samples-section {
  margin: 16px 0;
}

.samples-title {
  font-size: 14px;
  color: #666;
  margin-bottom: 10px;
}

.sample-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px;
  background: #f9f9f9;
  border-radius: 8px;
  margin-bottom: 8px;
}

.sample-name {
  font-size: 14px;
  color: #333;
}

.sample-status {
  font-size: 12px;
  color: #4facfe;
}

.create-btn {
  width: 100%;
  height: 45px;
  line-height: 45px;
  background: #00f2fe;
  color: #007aff;
  border-radius: 22px;
  font-size: 16px;
  font-weight: 600;
  border: none;
  margin-top: 16px;
  &:disabled {
    background: #ccc;
    color: #fff;
  }
}

.empty-tip {
  text-align: center;
  color: #999;
  padding: 40px 0;
}

.voice-list {
  max-height: 300px;
  overflow-y: auto;
}

.voice-item {
  padding: 16px;
  background: #f9f9f9;
  border-radius: 10px;
  margin-bottom: 12px;
}

.voice-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.voice-name {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.voice-status {
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 12px;
  &.PENDING, &.PROCESSING {
    background: #fff3e0;
    color: #ff9800;
  }
  &.COMPLETED {
    background: #e8f5e9;
    color: #4caf50;
  }
  &.FAILED {
    background: #ffebee;
    color: #f44336;
  }
}

.voice-time {
  font-size: 12px;
  color: #999;
}
</style>
