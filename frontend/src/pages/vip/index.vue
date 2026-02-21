<template>
  <view class="vip-page">
    <!-- 头部 -->
    <view class="header">
      <text class="vip-icon">👑</text>
      <text class="title">VIP 会员</text>
      <text class="status">{{ isVip ? 'VIP会员' : '普通用户' }}</text>
      <text class="expire" v-if="isVip && userInfo?.vipExpireDate">
        有效期至：{{ formatDate(userInfo.vipExpireDate) }}
      </text>
    </view>

    <!-- 特权 -->
    <view class="section">
      <text class="section-title">会员特权</text>
      <view class="privileges">
        <view class="privilege-item">
          <text class="check">✓</text>
          <text>无限字数合成</text>
        </view>
        <view class="privilege-item">
          <text class="check">✓</text>
          <text>v2.0 导演模式</text>
        </view>
        <view class="privilege-item">
          <text class="check">✓</text>
          <text>WebSocket 实时流式</text>
        </view>
        <view class="privilege-item">
          <text class="check">✓</text>
          <text>AI 播客完整功能</text>
        </view>
        <view class="privilege-item">
          <text class="check">✓</text>
          <text>边听边问无限次</text>
        </view>
      </view>
    </view>

    <!-- 套餐选择 -->
    <view class="section">
      <text class="section-title">套餐选择</text>
      <view class="plans">
        <view
          v-for="plan in plans"
          :key="plan.type"
          class="plan-item"
          :class="{
            active: selectedPlan === plan.type,
            recommended: plan.type === 'YEARLY'
          }"
          @click="selectedPlan = plan.type"
        >
          <text class="recommend-tag" v-if="plan.type === 'YEARLY'">特惠</text>
          <text class="plan-name">{{ plan.name }}</text>
          <text class="plan-price">¥{{ plan.price / 100 }}</text>
          <text class="plan-days">{{ plan.days }}天</text>
        </view>
      </view>
    </view>

    <!-- 开通按钮 -->
    <view class="action">
      <button
        class="buy-btn"
        :loading="loading"
        @click="handleBuy"
      >
        {{ isVip ? '续费会员' : '立即开通' }}
      </button>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getVipPlans, createVipOrder, mockPay, getUserInfo } from '@/api/auth'

const plans = ref([])
const selectedPlan = ref('MONTHLY')
const loading = ref(false)
const userInfo = ref(null)

const isVip = computed(() => {
  return userInfo.value?.userType === 'VIP'
})

onMounted(async () => {
  try {
    const [plansRes, userRes] = await Promise.all([
      getVipPlans(),
      getUserInfo()
    ])
    plans.value = plansRes.data || []
    userInfo.value = userRes.data
  } catch (e) {
    console.error(e)
  }
})

function formatDate(dateStr) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

async function handleBuy() {
  if (!userInfo.value) {
    uni.navigateTo({ url: '/pages/auth/login' })
    return
  }

  loading.value = true
  try {
    // 创建订单
    const orderRes = await createVipOrder(selectedPlan.value)
    const orderNo = orderRes.data.orderNo

    // Mock 支付
    await mockPay(orderNo)

    // 刷新用户信息
    const userRes = await getUserInfo()
    userInfo.value = userRes.data

    uni.showToast({ title: '开通成功', icon: 'success' })
  } catch (e) {
    uni.showToast({ title: e.message || '开通失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.vip-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #0a0a0f 0%, #1a1a2e 100%);
  padding: 60px 24px 40px;
}

.header {
  text-align: center;
  padding: 60px 40px;
  background: rgba(255, 255, 255, 0.03);
  backdrop-filter: blur(20rpx);
  border-radius: 24rpx;
  border: 1rpx solid rgba(255, 215, 0, 0.2);
  margin-bottom: 40rpx;
}

.vip-icon {
  font-size: 60px;
  display: block;
  margin-bottom: 16px;
}

.title {
  display: block;
  font-size: 28px;
  font-weight: 700;
  background: linear-gradient(135deg, #FFD700, #FFA500);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  margin-bottom: 8px;
}

.status {
  display: inline-block;
  padding: 6px 20px;
  background: linear-gradient(135deg, #FFD700, #FFA500);
  border-radius: 20px;
  font-size: 14px;
  font-weight: 600;
  color: #1E1B4B;
}

.expire {
  display: block;
  margin-top: 12px;
  font-size: 12px;
  color: rgba(220, 218, 255, 0.6);
}

.section {
  background: rgba(255, 255, 255, 0.03);
  backdrop-filter: blur(10rpx);
  border-radius: 20rpx;
  padding: 30rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.05);
  margin-bottom: 16px;
}

.section-title {
  display: block;
  font-size: 16px;
  font-weight: 600;
  color: #F1F0FF;
  margin-bottom: 16px;
}

.privileges {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.privilege-item {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  color: rgba(220, 218, 255, 0.9);

  .check {
    color: #34D399;
    font-weight: 700;
  }
}

.plans {
  display: flex;
  gap: 12px;
}

.plan-item {
  flex: 1;
  background: rgba(255, 255, 255, 0.05);
  border: 2px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  padding: 16px 8px;
  text-align: center;
  transition: all 0.2s;
  position: relative;

  &.active {
    border-color: #FFD700;
    background: rgba(255, 215, 0, 0.1);
  }

  &.recommended {
    transform: scale(1.05);
    border: 2rpx solid;
    border-image: linear-gradient(135deg, #FFD700, #FF6B00) 1;
    animation: glow 2s ease-in-out infinite;
  }
}

@keyframes glow {
  0%, 100% {
    box-shadow: 0 0 20rpx rgba(255, 215, 0, 0.3);
  }
  50% {
    box-shadow: 0 0 40rpx rgba(255, 215, 0, 0.6);
  }
}

.plan-name {
  display: block;
  font-size: 14px;
  color: #F1F0FF;
  margin-bottom: 8px;
}

.plan-price {
  display: block;
  font-size: 20px;
  font-weight: 700;
  color: #FFD700;
  margin-bottom: 4px;
}

.plan-days {
  display: block;
  font-size: 12px;
  color: rgba(220, 218, 255, 0.6);
}

.recommend-tag {
  position: absolute;
  top: -16rpx;
  right: 16rpx;
  background: linear-gradient(135deg, #FFD700, #FF6B00);
  color: #000;
  font-size: 20rpx;
  padding: 4rpx 16rpx;
  border-radius: 20rpx;
  font-weight: bold;
}

.action {
  margin-top: 24px;
}

.buy-btn {
  width: 100%;
  height: 50px;
  background: linear-gradient(135deg, #FFD700, #FFA500);
  border-radius: 12px;
  font-size: 16px;
  font-weight: 700;
  color: #1E1B4B;
  border: none;
}
</style>
