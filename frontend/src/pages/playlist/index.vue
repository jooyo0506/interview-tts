<template>
  <view class="playlist-page">
    <!-- 顶部导航 -->
    <view class="nav-bar">
      <view class="back-btn" @click="goBack">
        <text class="back-icon">←</text>
      </view>
      <text class="nav-title">播放列表</text>
      <view class="add-btn" @click="showCreateModal = true">
        <text>+</text>
      </view>
    </view>

    <!-- 内容区 -->
    <scroll-view class="content" scroll-y>
      <!-- 空状态 -->
      <view v-if="playlists.length === 0" class="empty-state">
        <text class="empty-icon">📋</text>
        <text class="empty-text">暂无播放列表</text>
        <text class="empty-hint">点击右上角+创建播放列表</text>
      </view>

      <!-- 播放列表 -->
      <view v-else class="playlist-list">
        <view
          v-for="item in playlists"
          :key="item.id"
          class="playlist-item"
          @click="openPlaylist(item)"
        >
          <view class="playlist-cover">
            <text class="cover-icon">🎵</text>
          </view>
          <view class="playlist-info">
            <text class="playlist-name">{{ item.name }}</text>
            <text class="playlist-meta">{{ item.audioCount || 0 }} 首 · {{ formatDuration(item.totalDuration || 0) }}</text>
          </view>
          <view class="playlist-actions">
            <view class="action-icon" @click.stop="openRenameModal(item)">
              <text>✏️</text>
            </view>
            <view class="action-icon delete" @click.stop="handleDelete(item)">
              <text>🗑️</text>
            </view>
          </view>
        </view>
      </view>
    </scroll-view>

    <!-- 创建弹窗 -->
    <view v-if="showCreateModal" class="modal-mask" @click="showCreateModal = false">
      <view class="modal-content" @click.stop>
        <view class="modal-header">
          <text class="modal-title">创建播放列表</text>
          <text class="modal-close" @click="showCreateModal = false">✕</text>
        </view>
        <view class="modal-body">
          <input
            v-model="newPlaylistName"
            class="name-input"
            placeholder="请输入播放列表名称"
            maxlength="20"
          />
          <input
            v-model="newPlaylistDesc"
            class="name-input"
            placeholder="描述（可选）"
            maxlength="100"
          />
        </view>
        <view class="modal-footer">
          <button class="modal-btn cancel" @click="showCreateModal = false">取消</button>
          <button class="modal-btn confirm" @click="handleCreate">创建</button>
        </view>
      </view>
    </view>

    <!-- 重命名弹窗 -->
    <view v-if="isRenameModalVisible" class="modal-mask" @click="isRenameModalVisible = false">
      <view class="modal-content" @click.stop>
        <view class="modal-header">
          <text class="modal-title">重命名</text>
          <text class="modal-close" @click="isRenameModalVisible = false">✕</text>
        </view>
        <view class="modal-body">
          <input
            v-model="tempName"
            class="name-input"
            placeholder="请输入新名称"
            maxlength="20"
          />
        </view>
        <view class="modal-footer">
          <button class="modal-btn cancel" @click="isRenameModalVisible = false">取消</button>
          <button class="modal-btn confirm" @click="confirmRename">保存</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getPlaylistList, createPlaylist, deletePlaylist, renamePlaylist } from '@/api/playlist'

const playlists = ref([])
const showCreateModal = ref(false)
const isRenameModalVisible = ref(false)
const newPlaylistName = ref('')
const newPlaylistDesc = ref('')
const tempName = ref('')
const currentEditItem = ref(null)

onMounted(() => {
  loadPlaylists()
})

async function loadPlaylists() {
  try {
    const res = await getPlaylistList()
    playlists.value = res.data || []
  } catch (e) {
    console.error('加载播放列表失败:', e)
  }
}

function goBack() {
  uni.navigateBack()
}

async function handleCreate() {
  if (!newPlaylistName.value.trim()) {
    uni.showToast({ title: '请输入名称', icon: 'none' })
    return
  }

  try {
    await createPlaylist(newPlaylistName.value.trim(), newPlaylistDesc.value.trim())
    uni.showToast({ title: '创建成功', icon: 'success' })
    showCreateModal.value = false
    newPlaylistName.value = ''
    newPlaylistDesc.value = ''
    loadPlaylists()
  } catch (e) {
    uni.showToast({ title: '创建失败', icon: 'none' })
  }
}

function openRenameModal(item) {
  currentEditItem.value = item
  tempName.value = item.name
  isRenameModalVisible.value = true
}

async function confirmRename() {
  if (!tempName.value.trim()) {
    uni.showToast({ title: '请输入名称', icon: 'none' })
    return
  }

  try {
    await renamePlaylist(currentEditItem.value.id, tempName.value.trim())
    uni.showToast({ title: '重命名成功', icon: 'success' })
    isRenameModalVisible.value = false
    loadPlaylists()
  } catch (e) {
    uni.showToast({ title: '重命名失败', icon: 'none' })
  }
}

function handleDelete(item) {
  uni.showModal({
    title: '确认删除',
    content: `确定要删除"${item.name}"吗？`,
    success: async (res) => {
      if (res.confirm) {
        try {
          await deletePlaylist(item.id)
          uni.showToast({ title: '删除成功', icon: 'success' })
          loadPlaylists()
        } catch (e) {
          uni.showToast({ title: '删除失败', icon: 'none' })
        }
      }
    }
  })
}

function openPlaylist(item) {
  uni.navigateTo({
    url: `/pages/playlist/detail?id=${item.id}&name=${encodeURIComponent(item.name)}`
  })
}

function formatDuration(seconds) {
  if (!seconds) return '0分钟'
  const min = Math.floor(seconds / 60)
  return `${min}分钟`
}
</script>

<style lang="scss" scoped>
.playlist-page {
  min-height: 100vh;
  background: #f5f5f5;
}

.nav-bar {
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

.nav-title {
  font-size: 18px;
  font-weight: 600;
  color: #fff;
}

.add-btn {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: #fff;
}

.content {
  height: calc(100vh - 90px);
  padding: 20px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 100px 0;
}

.empty-icon {
  font-size: 60px;
  margin-bottom: 16px;
}

.empty-text {
  font-size: 16px;
  color: #333;
  margin-bottom: 8px;
}

.empty-hint {
  font-size: 13px;
  color: #999;
}

.playlist-list {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
}

.playlist-item {
  display: flex;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #f5f5f5;
  &:last-child {
    border-bottom: none;
  }
}

.playlist-cover {
  width: 50px;
  height: 50px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
}

.cover-icon {
  font-size: 24px;
}

.playlist-info {
  flex: 1;
}

.playlist-name {
  display: block;
  font-size: 15px;
  font-weight: 600;
  color: #333;
  margin-bottom: 4px;
}

.playlist-meta {
  font-size: 12px;
  color: #999;
}

.playlist-actions {
  display: flex;
  gap: 8px;
}

.action-icon {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
}

// Modal
.modal-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  width: 80%;
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #eee;
}

.modal-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.modal-close {
  font-size: 18px;
  color: #999;
}

.modal-body {
  padding: 20px;
}

.name-input {
  width: 100%;
  height: 44px;
  border: 1px solid #ddd;
  border-radius: 8px;
  padding: 0 12px;
  font-size: 14px;
  margin-bottom: 12px;
}

.modal-footer {
  display: flex;
  border-top: 1px solid #eee;
}

.modal-btn {
  flex: 1;
  height: 44px;
  line-height: 44px;
  text-align: center;
  font-size: 14px;
  border: none;
  background: none;
  &.cancel {
    color: #666;
    border-right: 1px solid #eee;
  }
  &.confirm {
    color: #667eea;
    font-weight: 600;
  }
}
</style>
