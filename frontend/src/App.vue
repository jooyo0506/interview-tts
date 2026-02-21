<template>
  <view class="app-container">
    <!-- uni-app 使用页面路由，不需要 router-view -->
  </view>
</template>

<script>
import { getUserInfo } from '@/api/auth'

export default {
  globalData: {
    token: '',
    userInfo: null
  },
  onLaunch: function() {
    console.log('App Launch')
    // 初始化全局数据
    this.globalData.token = uni.getStorageSync('token') || ''
    this.globalData.userInfo = uni.getStorageSync('userInfo') || null

    // 定期刷新用户信息
    this.refreshUserInfo()
  },
  onShow: function() {
    console.log('App Show')
    // 每次打开应用刷新用户信息
    this.refreshUserInfo()
  },
  onHide: function() {
    console.log('App Hide')
  },
  methods: {
    async refreshUserInfo() {
      const token = uni.getStorageSync('token')
      if (!token) return

      try {
        const res = await getUserInfo()
        if (res.data) {
          this.globalData.userInfo = res.data
          uni.setStorageSync('userInfo', res.data)
        }
      } catch (e) {
        console.error('刷新用户信息失败', e)
      }
    },
    setToken(token) {
      this.globalData.token = token
      uni.setStorageSync('token', token)
    },
    setUserInfo(userInfo) {
      this.globalData.userInfo = userInfo
      uni.setStorageSync('userInfo', userInfo)
    },
    logout() {
      this.globalData.token = ''
      this.globalData.userInfo = null
      uni.removeStorageSync('token')
      uni.removeStorageSync('userInfo')
    }
  }
}
</script>

<style lang="scss">
page {
  background-color: #0a0a0f;
}

.app-container {
  min-height: 100vh;
  min-height: 100dvh;
}
</style>
