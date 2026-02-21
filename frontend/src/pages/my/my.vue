<template>
  <view class="my-page">
    <!-- 动态声波背景 -->
    <view class="wave-bg">
      <view class="wave wave-1"></view>
      <view class="wave wave-2"></view>
      <view class="noise-overlay"></view>
    </view>

    <!-- 顶部用户信息 -->
    <view class="header">
      <!-- 装饰性声波 -->
      <view class="header-waves">
        <view v-for="i in 6" :key="i" class="h-wave" :style="{ '--i': i }"></view>
      </view>

      <!-- 用户信息 -->
      <view class="user-info" @click="showProfileModal = true">
        <view class="avatar-wrap">
          <view class="avatar">
            <text class="avatar-icon">{{ userInfo.nickname ? userInfo.nickname.charAt(0) : '?' }}</text>
          </view>
          <view class="avatar-ring"></view>
        </view>
        <view class="user-detail">
          <text class="nickname">{{ userInfo.nickname || '点击设置昵称' }}</text>
          <view class="user-id-wrap">
            <text class="user-id">ID: {{ userId.slice(0, 8) }}...</text>
          </view>
        </view>
        <view class="edit-btn">
          <text>✏️</text>
        </view>
      </view>
    </view>

    <!-- 操作栏 -->
    <view class="action-bar" v-if="list.length > 0">
      <view class="action-left">
        <view v-if="!editMode" class="action-btn" @click="enterEditMode">
          <text>编辑</text>
        </view>
        <view v-else class="action-btns">
          <view class="action-btn cancel" @click="cancelEdit">
            <text>取消</text>
          </view>
          <view class="action-btn delete" @click="handleBatchDelete">
            <text>删除 ({{ selectedCount }})</text>
          </view>
        </view>
      </view>
      <text class="list-count">共 {{ list.length }} 条</text>
    </view>

    <!-- Tab 切换 -->
    <view class="tabs">
      <view
        class="tab-item"
        :class="{ active: activeTab === 'generate' }"
        @click="switchTab('generate')"
      >
        <text>我的生成</text>
        <view class="tab-indicator" v-if="activeTab === 'generate'"></view>
      </view>
      <view
        class="tab-item"
        :class="{ active: activeTab === 'collect' }"
        @click="switchTab('collect')"
      >
        <text>我的收藏</text>
        <view class="tab-indicator" v-if="activeTab === 'collect'"></view>
      </view>
    </view>

    <!-- 列表内容 -->
    <scroll-view
      class="list-content"
      scroll-y
      refresher-enabled
      :refresher-triggered="refreshing"
      @refresherrefresh="onRefresh"
    >
      <!-- 加载中 -->
      <view v-if="loading" class="loading-state">
        <view class="loading-spinner"></view>
        <text>加载中...</text>
      </view>

      <!-- 空状态 -->
      <view v-else-if="list.length === 0" class="empty-state">
        <view class="empty-illustration">
          <view class="empty-circle">
            <text class="empty-icon">{{ activeTab === 'generate' ? '📝' : '❤️' }}</text>
          </view>
        </view>
        <text class="empty-text">{{ activeTab === 'generate' ? '暂无生成记录' : '暂无收藏' }}</text>
        <text class="empty-hint">{{ activeTab === 'generate' ? '去首页生成音频吧' : '在播放页面点击收藏' }}</text>
      </view>

      <!-- 列表 -->
      <view v-else class="audio-list">
        <view
          v-for="(item, index) in list"
          :key="item.id"
          class="audio-item"
          :class="{ selected: selectedIds.includes(item.id) }"
          :style="{ '--delay': index * 0.05 + 's' }"
          @click="handleItemClick(item)"
        >
          <!-- 编辑模式选择框 -->
          <view v-if="editMode" class="checkbox" @click.stop="toggleSelect(item.id)">
            <view class="checkbox-inner" :class="{ checked: selectedIds.includes(item.id) }">
              <text v-if="selectedIds.includes(item.id)">✓</text>
            </view>
          </view>

          <!-- 内容 -->
          <view class="item-content" @click.stop="playAudio(item)">
            <view class="item-header">
              <text class="item-title">{{ item.title || '未命名' }}</text>
              <text class="item-time">{{ formatTime(item.createTime) }}</text>
            </view>
            <text class="item-text">{{ (item.rawText || '').substring(0, 40) }}{{ (item.rawText || '').length > 40 ? '...' : '' }}</text>
            <view class="item-meta">
              <text class="meta-tag">{{ formatVoice(item.voiceName) }}</text>
              <text class="meta-duration">{{ formatDuration(item.duration) }}</text>
            </view>
          </view>

          <!-- 操作按钮 -->
          <view v-if="!editMode" class="item-actions">
            <view class="action-icon" @click.stop="handleRename(item)">
              <text>✏️</text>
            </view>
            <view class="action-icon delete" @click.stop="handleDelete(item)">
              <text>🗑️</text>
            </view>
          </view>
        </view>
      </view>
    </scroll-view>

    <!-- 昵称设置弹窗 -->
    <view v-if="showProfileModal" class="modal-mask" @click="showProfileModal = false">
      <view class="modal-content" @click.stop>
        <view class="modal-header">
          <text class="modal-title">设置昵称</text>
          <view class="modal-close" @click="showProfileModal = false">
            <text>✕</text>
          </view>
        </view>
        <view class="modal-body">
          <input
            v-model="tempNickname"
            class="nickname-input"
            placeholder="请输入昵称"
            maxlength="20"
          />
        </view>
        <view class="modal-footer">
          <button class="modal-btn cancel" @click="showProfileModal = false">取消</button>
          <button class="modal-btn confirm" @click="saveNickname">保存</button>
        </view>
      </view>
    </view>

    <!-- 重命名弹窗 -->
    <view v-if="showRenameModal" class="modal-mask" @click="showRenameModal = false">
      <view class="modal-content" @click.stop>
        <view class="modal-header">
          <text class="modal-title">重命名</text>
          <view class="modal-close" @click="showRenameModal = false">
            <text>✕</text>
          </view>
        </view>
        <view class="modal-body">
          <input
            v-model="tempName"
            class="nickname-input"
            placeholder="请输入新名称"
            maxlength="50"
          />
        </view>
        <view class="modal-footer">
          <button class="modal-btn cancel" @click="showRenameModal = false">取消</button>
          <button class="modal-btn confirm" @click="confirmRename">保存</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { getMyList, getCollectList, deleteAudio, deleteAudioBatch, renameAudio, deleteCollect, getUserProfile, updateUserProfile, getUserKey } from '@/api'

const userId = ref('')
const activeTab = ref('generate')
const list = ref([])
const loading = ref(false)
const refreshing = ref(false)
const editMode = ref(false)
const selectedIds = ref([])
const showProfileModal = ref(false)
const showRenameModal = ref(false)
const tempNickname = ref('')
const tempName = ref('')
const currentEditItem = ref(null)

const userInfo = ref({
  nickname: '',
  avatar: ''
})

const selectedCount = computed(() => selectedIds.value.length)

onMounted(() => {
  userId.value = getUserKey()
  loadUserProfile()
  loadList()
})

function switchTab(tab) {
  activeTab.value = tab
  selectedIds.value = []
  editMode.value = false
  loadList()
}

async function loadUserProfile() {
  try {
    const res = await getUserProfile()
    if (res.data) {
      userInfo.value = {
        nickname: res.data.nickname || '',
        avatar: res.data.avatar || ''
      }
    }
  } catch (e) {
    console.error('获取用户信息失败:', e)
  }
}

async function loadList() {
  loading.value = true
  try {
    let res
    if (activeTab.value === 'generate') {
      res = await getMyList()
    } else {
      res = await getCollectList()
    }
    list.value = res.data || []
  } catch (e) {
    console.error('加载列表失败:', e)
  } finally {
    loading.value = false
  }
}

async function onRefresh() {
  refreshing.value = true
  await loadList()
  refreshing.value = false
}

// 编辑模式
function enterEditMode() {
  editMode.value = true
}

function cancelEdit() {
  editMode.value = false
  selectedIds.value = []
}

function toggleSelect(id) {
  const index = selectedIds.value.indexOf(id)
  if (index === -1) {
    selectedIds.value.push(id)
  } else {
    selectedIds.value.splice(index, 1)
  }
}

function handleItemClick(item) {
  if (editMode.value) {
    toggleSelect(item.id)
  } else {
    playAudio(item)
  }
}

// 删除
function handleDelete(item) {
  uni.showModal({
    title: '确认删除',
    content: '确定要删除这个音频吗？',
    success: async (res) => {
      if (res.confirm) {
        try {
          if (activeTab.value === 'collect') {
            await deleteCollect(item.id)
          } else {
            await deleteAudio(item.id)
          }
          uni.showToast({ title: '删除成功', icon: 'success' })
          loadList()
        } catch (e) {
          uni.showToast({ title: '删除失败', icon: 'none' })
        }
      }
    }
  })
}

async function handleBatchDelete() {
  if (selectedIds.value.length === 0) {
    uni.showToast({ title: '请选择要删除的内容', icon: 'none' })
    return
  }

  // 根据当前Tab决定删除类型
  const isCollectTab = activeTab.value === 'collect'

  uni.showModal({
    title: '批量删除',
    content: `确定要删除选中的 ${selectedIds.value.length} 条${isCollectTab ? '收藏' : '记录'}吗？`,
    success: async (res) => {
      if (res.confirm) {
        try {
          if (isCollectTab) {
            // 收藏tab调用deleteCollect逐个删除
            for (const id of selectedIds.value) {
              await deleteCollect(id)
            }
          } else {
            // 生成记录tab调用deleteAudioBatch
            await deleteAudioBatch(selectedIds.value)
          }
          uni.showToast({ title: '删除成功', icon: 'success' })
          editMode.value = false
          selectedIds.value = []
          loadList()
        } catch (e) {
          uni.showToast({ title: '删除失败', icon: 'none' })
        }
      }
    }
  })
}

// 重命名
function handleRename(item) {
  currentEditItem.value = item
  tempName.value = item.title || ''
  showRenameModal.value = true
}

async function confirmRename() {
  if (!tempName.value.trim()) {
    uni.showToast({ title: '名称不能为空', icon: 'none' })
    return
  }

  try {
    await renameAudio(currentEditItem.value.id, tempName.value.trim())
    uni.showToast({ title: '重命名成功', icon: 'success' })
    showRenameModal.value = false
    loadList()
  } catch (e) {
    uni.showToast({ title: '重命名失败', icon: 'none' })
  }
}

// 保存昵称
async function saveNickname() {
  if (!tempNickname.value.trim()) {
    uni.showToast({ title: '昵称不能为空', icon: 'none' })
    return
  }

  try {
    await updateUserProfile(tempNickname.value.trim(), '')
    userInfo.value.nickname = tempNickname.value.trim()
    uni.showToast({ title: '保存成功', icon: 'success' })
    showProfileModal.value = false
    tempNickname.value = ''
  } catch (e) {
    uni.showToast({ title: '保存失败', icon: 'none' })
  }
}

function playAudio(item) {
  uni.navigateTo({
    url: `/pages/play/play?id=${item.id}&url=${encodeURIComponent(item.r2Url)}&text=${encodeURIComponent(item.rawText)}`
  })
}

function formatTime(timeStr) {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  const month = date.getMonth() + 1
  const day = date.getDate()
  return `${month}月${day}日`
}

function formatDuration(seconds) {
  if (!seconds) return '0:00'
  const min = Math.floor(seconds / 60)
  const sec = seconds % 60
  return `${min}:${sec.toString().padStart(2, '0')}`
}

function formatVoice(voiceName) {
  if (!voiceName) return ''
  const map = {
    'BV001_streaming': '女声',
    'BV002_streaming': '男声',
    'BV700_streaming': '灿灿',
    'BV102_streaming': '青年',
    'BV113_streaming': '少女',
    'BV033_streaming': '小哥',
    'BV034_streaming': '姐姐'
  }
  return map[voiceName] || voiceName
}
</script>

<style lang="scss" scoped>
.my-page {
  min-height: 100vh;
  min-height: 100dvh;
  background: #0a0a0f;
  position: relative;
  overflow: hidden;
  display: flex;
  flex-direction: column;
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

// 头部
.header {
  position: relative;
  padding: calc(50px + env(safe-area-inset-top, 0px)) 20px 30px;
  background: linear-gradient(180deg, rgba(30, 30, 50, 0.8) 0%, rgba(10, 10, 15, 0.9) 100%);
  overflow: hidden;
  flex-shrink: 0;
}

.header-waves {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 80px;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  gap: 3px;
  opacity: 0.1;
}

.h-wave {
  width: 2px;
  height: 15px;
  background: linear-gradient(180deg, #f59e0b, transparent);
  border-radius: 1px;
  animation: h-wave-float 2.5s ease-in-out infinite;
  animation-delay: calc(var(--i) * 0.12s);

  &:nth-child(odd) {
    animation-duration: 2.8s;
  }
}

@keyframes h-wave-float {
  0%, 100% {
    transform: scaleY(0.5);
  }
  50% {
    transform: scaleY(1);
  }
}

.user-info {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
}

.avatar-wrap {
  position: relative;
  margin-right: 16px;
}

.avatar {
  width: 64px;
  height: 64px;
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 20px rgba(245, 158, 11, 0.3);
}

.avatar-icon {
  font-size: 26px;
  color: #fff;
  font-weight: 600;
}

.avatar-ring {
  position: absolute;
  top: -4px;
  left: -4px;
  right: -4px;
  bottom: -4px;
  border: 2px solid rgba(245, 158, 11, 0.3);
  border-radius: 50%;
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

.user-detail {
  flex: 1;
}

.nickname {
  display: block;
  font-size: 20px;
  font-weight: 600;
  color: #fff;
  margin-bottom: 6px;
}

.user-id-wrap {
  display: inline-flex;
  background: rgba(255, 255, 255, 0.05);
  padding: 4px 10px;
  border-radius: 12px;
}

.user-id {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.4);
}

.edit-btn {
  width: 40px;
  height: 40px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
}

// 操作栏
.action-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: rgba(20, 20, 35, 0.8);
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  flex-shrink: 0;
}

.action-left {
  display: flex;
  gap: 12px;
}

.action-btns {
  display: flex;
  gap: 10px;
}

.action-btn {
  padding: 8px 18px;
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
  border-radius: 20px;
  font-size: 13px;
  color: #fff;
  font-weight: 500;

  &.cancel {
    background: rgba(255, 255, 255, 0.1);
    color: rgba(255, 255, 255, 0.7);
  }
  &.delete {
    background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  }
}

.list-count {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.4);
}

// Tab切换
.tabs {
  display: flex;
  background: rgba(20, 20, 35, 0.8);
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  padding-bottom: env(safe-area-inset-bottom, 0px);
  flex-shrink: 0;
}

.tab-item {
  flex: 1;
  padding: 18px;
  text-align: center;
  font-size: 15px;
  color: rgba(255, 255, 255, 0.5);
  position: relative;
  transition: color 0.3s ease;

  &.active {
    color: #f59e0b;
    font-weight: 600;
  }
}

.tab-indicator {
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 40px;
  height: 3px;
  background: linear-gradient(90deg, #f59e0b, transparent);
  border-radius: 2px;
}

// 列表内容
.list-content {
  flex: 1;
  height: auto;
  min-height: 0;
  padding: 16px;
  padding-bottom: calc(16px + env(safe-area-inset-bottom, 0px));
  position: relative;
  z-index: 1;
  box-sizing: border-box;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
  color: rgba(255, 255, 255, 0.4);
}

.loading-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid rgba(245, 158, 11, 0.2);
  border-top-color: #f59e0b;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 12px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 80px 0;
}

.empty-illustration {
  margin-bottom: 24px;
}

.empty-circle {
  width: 100px;
  height: 100px;
  background: rgba(30, 30, 50, 0.6);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px dashed rgba(255, 255, 255, 0.1);
}

.empty-icon {
  font-size: 40px;
  opacity: 0.5;
}

.empty-text {
  font-size: 17px;
  color: rgba(255, 255, 255, 0.7);
  margin-bottom: 10px;
}

.empty-hint {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.4);
}

// 音频列表
.audio-list {
  background: rgba(30, 30, 50, 0.4);
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.audio-item {
  display: flex;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.03);
  transition: all 0.3s ease;
  animation: item-slide-in 0.4s ease-out backwards;
  animation-delay: var(--delay, 0s);

  &:active {
    background: rgba(255, 255, 255, 0.03);
  }

  &:last-child {
    border-bottom: none;
  }

  &.selected {
    background: rgba(245, 158, 11, 0.1);
  }
}

@keyframes item-slide-in {
  from {
    opacity: 0;
    transform: translateX(-20px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

.checkbox {
  margin-right: 12px;
}

.checkbox-inner {
  width: 22px;
  height: 22px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: transparent;
  transition: all 0.2s ease;

  &.checked {
    background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
    border-color: transparent;
    color: #fff;
  }
}

.item-content {
  flex: 1;
}

.item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.item-title {
  font-size: 15px;
  font-weight: 600;
  color: #fff;
}

.item-time {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.4);
}

.item-text {
  display: block;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.5);
  line-height: 1.5;
  margin-bottom: 10px;
}

.item-meta {
  display: flex;
  gap: 10px;
}

.meta-tag {
  font-size: 11px;
  color: #f59e0b;
  background: rgba(245, 158, 11, 0.15);
  padding: 4px 10px;
  border-radius: 12px;
}

.meta-duration {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.4);
  padding-top: 4px;
}

.item-actions {
  display: flex;
  gap: 6px;
}

.action-icon {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  transition: all 0.2s ease;

  &:active {
    background: rgba(255, 255, 255, 0.1);
  }

  &.delete:active {
    background: rgba(239, 68, 68, 0.2);
  }
}

// Modal
.modal-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  backdrop-filter: blur(4px);
}

.modal-content {
  width: 85%;
  background: #1a1a2e;
  border-radius: 20px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.modal-title {
  font-size: 17px;
  font-weight: 600;
  color: #fff;
}

.modal-close {
  width: 28px;
  height: 28px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.5);
}

.modal-body {
  padding: 24px;
}

.nickname-input {
  width: 100%;
  height: 48px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  padding: 0 16px;
  font-size: 15px;
  color: #fff;

  &::placeholder {
    color: rgba(255, 255, 255, 0.3);
  }
}

.modal-footer {
  display: flex;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
}

.modal-btn {
  flex: 1;
  height: 50px;
  line-height: 50px;
  text-align: center;
  font-size: 15px;
  border: none;
  background: none;

  &.cancel {
    color: rgba(255, 255, 255, 0.6);
    border-right: 1px solid rgba(255, 255, 255, 0.05);
  }

  &.confirm {
    color: #f59e0b;
    font-weight: 600;
  }
}
</style>
