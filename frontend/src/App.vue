<template>
  <view class="app-container">
    <view class="content">
      <router-view></router-view>
    </view>
  </view>
</template>

<script>
export default {
  onLaunch: function() {
    console.log('App Launch')
  },
  onShow: function() {
    console.log('App Show')
  },
  onHide: function() {
    console.log('App Hide')
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
