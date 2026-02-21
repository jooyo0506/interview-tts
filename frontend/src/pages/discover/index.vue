<template>
  <view class="discover-page">
    <!-- 头部 -->
    <view class="header">
      <text class="title">发现</text>
      <text class="subtitle">热门AI播客作品</text>
    </view>

    <!-- 标签切换 -->
    <view class="tabs">
      <view
        v-for="tab in tabs"
        :key="tab"
        class="tab"
        :class="{ active: currentTab === tab }"
        @click="currentTab = tab"
      >
        {{ tab }}
      </view>
    </view>

    <!-- 作品列表 -->
    <view class="works-list">
      <view
        v-for="item in filteredList"
        :key="item.id"
        class="work-card"
        @click="playAudio(item)"
      >
        <view class="work-cover">
          <text class="cover-icon">🎙️</text>
        </view>
        <view class="work-info">
          <text class="work-title">{{ item.title }}</text>
          <text class="work-author">{{ item.author }}</text>
          <view class="work-stats">
            <text class="stat">▶ {{ item.playCount }}</text>
            <text class="stat">❤️ {{ item.likeCount }}</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 空状态 -->
    <view class="empty-state" v-if="filteredList.length === 0">
      <text class="empty-icon">📭</text>
      <text class="empty-text">暂无作品</text>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'

const tabs = ref(['推荐', '最新', '热门'])
const currentTab = ref('推荐')

// 模拟数据
const worksList = ref([
  { id: 1, title: '特斯拉财报深度解读', author: 'AI财经', category: '推荐', playCount: 1234, likeCount: 56 },
  { id: 2, title: '今日科技要闻', author: 'AI科技', category: '最新', playCount: 892, likeCount: 34 },
  { id: 3, title: '一周热点回顾', author: 'AI新闻', category: '热门', playCount: 2567, likeCount: 128 },
  { id: 4, title: 'AI行业趋势分析', author: 'AI观察', category: '推荐', playCount: 567, likeCount: 23 },
  { id: 5, title: '新产品发布解读', author: 'AI评测', category: '最新', playCount: 445, likeCount: 18 },
  { id: 6, title: '月度经济展望', author: 'AI财经', category: '热门', playCount: 1890, likeCount: 89 },
])

const filteredList = computed(() => {
  if (currentTab.value === '推荐') {
    return worksList.value.filter(item => item.category === '推荐')
  }
  if (currentTab.value === '最新') {
    return worksList.value.filter(item => item.category === '最新')
  }
  if (currentTab.value === '热门') {
    return worksList.value.filter(item => item.category === '热门')
  }
  return worksList.value
})

function playAudio(item) {
  uni.showToast({
    title: '即将播放: ' + item.title,
    icon: 'none'
  })
}
</script>

<style lang="scss" scoped>
.discover-page {
  min-height: 100vh;
  min-height: 100dvh;
  background: #0a0a0f;
  padding: 30rpx;
  padding-bottom: calc(120rpx + env(safe-area-inset-bottom, 0px));
}

.header {
  padding: 40rpx 0;
  text-align: center;

  .title {
    font-size: 48rpx;
    font-weight: bold;
    background: linear-gradient(135deg, #FF6B00, #FFD700);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    display: block;
  }

  .subtitle {
    font-size: 26rpx;
    color: rgba(255, 255, 255, 0.5);
    margin-top: 10rpx;
    display: block;
  }
}

.tabs {
  display: flex;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 16rpx;
  padding: 8rpx;
  margin-bottom: 30rpx;
}

.tab {
  flex: 1;
  text-align: center;
  padding: 16rpx;
  font-size: 28rpx;
  color: rgba(255, 255, 255, 0.5);
  border-radius: 12rpx;
  transition: all 0.2s;

  &.active {
    background: linear-gradient(135deg, #FF6B00, #FFD700);
    color: #000;
    font-weight: bold;
  }
}

.works-list {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20rpx;
}

.work-card {
  background: rgba(255, 255, 255, 0.03);
  border-radius: 16rpx;
  overflow: hidden;
  border: 1rpx solid rgba(255, 255, 255, 0.05);
  transition: all 0.2s;

  &:active {
    transform: scale(0.98);
  }
}

.work-cover {
  height: 180rpx;
  background: linear-gradient(135deg, #1a1a2e, #16213e);
  display: flex;
  align-items: center;
  justify-content: center;

  .cover-icon {
    font-size: 60rpx;
  }
}

.work-info {
  padding: 20rpx;
}

.work-title {
  font-size: 28rpx;
  color: #fff;
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.work-author {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.5);
  display: block;
  margin-top: 8rpx;
}

.work-stats {
  display: flex;
  gap: 20rpx;
  margin-top: 12rpx;

  .stat {
    font-size: 22rpx;
    color: rgba(255, 255, 255, 0.4);
  }
}

.empty-state {
  text-align: center;
  padding: 100rpx 0;

  .empty-icon {
    font-size: 80rpx;
    display: block;
  }

  .empty-text {
    font-size: 28rpx;
    color: rgba(255, 255, 255, 0.4);
    margin-top: 20rpx;
    display: block;
  }
}
</style>
