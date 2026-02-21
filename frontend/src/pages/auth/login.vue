<template>
  <view class="login-page">
    <view class="header">
      <text class="title">登录 / 注册</text>
      <text class="subtitle">欢迎使用声读</text>
    </view>

    <!-- Tab 切换 -->
    <view class="tabs">
      <view
        class="tab"
        :class="{ active: tab === 'password' }"
        @click="tab = 'password'"
      >
        密码登录
      </view>
      <view
        class="tab"
        :class="{ active: tab === 'code' }"
        @click="tab = 'code'"
      >
        验证码登录
      </view>
    </view>

    <!-- 表单 -->
    <view class="form">
      <view class="input-group">
        <input
          class="input"
          v-model="form.phone"
          type="number"
          placeholder="请输入手机号"
          maxlength="11"
        />
      </view>

      <!-- 密码登录 -->
      <view class="input-group" v-if="tab === 'password'">
        <input
          class="input"
          v-model="form.password"
          type="password"
          placeholder="请输入密码"
        />
      </view>

      <!-- 数学验证码 -->
      <view class="input-group captcha-group" v-if="tab === 'code'">
        <input
          class="input captcha-input"
          v-model="form.captcha"
          type="number"
          placeholder="请输入计算结果"
          maxlength="3"
        />
        <view class="captcha-question" @click="refreshCaptcha">
          <text>{{ captchaQuestion }}</text>
        </view>
      </view>

      <!-- 验证码登录 -->
      <view class="input-group code-group" v-if="tab === 'code'">
        <input
          class="input"
          v-model="form.code"
          type="number"
          placeholder="请输入验证码"
          maxlength="6"
        />
        <view
          class="code-btn"
          :class="{ disabled: countdown > 0 }"
          @click="sendCode"
        >
          {{ countdown > 0 ? countdown + 's' : '获取验证码' }}
        </view>
      </view>

      <!-- 提交按钮 -->
      <button
        class="submit-btn"
        :loading="loading"
        @click="handleSubmit"
      >
        {{ tab === 'password' ? '登录' : '获取验证码' }}
      </button>

      <!-- 注册链接 -->
      <view class="footer">
        <text class="footer-text">没有账号？</text>
        <text class="link" @click="goRegister">立即注册</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { login, loginByCode, sendCode as apiSendCode } from '@/api/auth'

const tab = ref('password')
const loading = ref(false)
const countdown = ref(0)

const form = ref({
  phone: '',
  password: '',
  code: '',
  captcha: ''
})

const captchaQuestion = ref('')
const captchaAnswer = ref(0)

// 生成数学验证码
function generateCaptcha() {
  const a = Math.floor(Math.random() * 10) + 1
  const b = Math.floor(Math.random() * 10) + 1
  const isPlus = Math.random() > 0.5
  captchaAnswer.value = isPlus ? a + b : a - b
  captchaQuestion.value = `${a} ${isPlus ? '+' : '-'} ${b} = ?`
}

function refreshCaptcha() {
  generateCaptcha()
}

// 初始化验证码
generateCaptcha()

let countdownTimer = null

function startCountdown() {
  countdown.value = 60
  countdownTimer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      clearInterval(countdownTimer)
    }
  }, 1000)
}

async function sendCode() {
  if (countdown.value > 0) return
  if (!form.value.phone || form.value.phone.length !== 11) {
    uni.showToast({ title: '请输入正确的手机号', icon: 'none' })
    return
  }

  // 验证数学验证码
  if (String(form.value.captcha) !== String(captchaAnswer.value)) {
    uni.showToast({ title: '请先完成计算验证', icon: 'none' })
    refreshCaptcha()
    return
  }

  try {
    await apiSendCode(form.value.phone)
    uni.showToast({ title: '验证码已发送', icon: 'success' })
    startCountdown()
  } catch (e) {
    uni.showToast({ title: e.message || '发送失败', icon: 'none' })
  }
}

async function handleSubmit() {
  if (!form.value.phone || form.value.phone.length !== 11) {
    uni.showToast({ title: '请输入正确的手机号', icon: 'none' })
    return
  }

  if (tab.value === 'password') {
    if (!form.value.password) {
      uni.showToast({ title: '请输入密码', icon: 'none' })
      return
    }
    loading.value = true
    try {
      const res = await login({
        phone: form.value.phone,
        password: form.value.password
      })
      handleLoginSuccess(res.data)
    } catch (e) {
      uni.showToast({ title: e.message || '登录失败', icon: 'none' })
    } finally {
      loading.value = false
    }
  } else {
    // 验证码登录 - 先获取验证码
    if (countdown.value === 0) {
      await sendCode()
    }
  }
}

async function handleLoginSuccess(data) {
  uni.setStorageSync('token', data.token)
  uni.setStorageSync('userInfo', data.user)
  uni.showToast({ title: '登录成功', icon: 'success' })
  setTimeout(() => {
    uni.navigateBack()
  }, 1000)
}

function goRegister() {
  uni.navigateTo({ url: '/pages/auth/register' })
}
</script>

<style lang="scss" scoped>
.login-page {
  min-height: 100vh;
  background: linear-gradient(160deg, #1E1B4B 0%, #312E81 100%);
  padding: 60px 24px 40px;
}

.header {
  text-align: center;
  margin-bottom: 40px;
}

.title {
  display: block;
  font-size: 28px;
  font-weight: 700;
  color: #F1F0FF;
  margin-bottom: 8px;
}

.subtitle {
  font-size: 14px;
  color: rgba(220, 218, 255, 0.6);
}

.tabs {
  display: flex;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  padding: 4px;
  margin-bottom: 24px;
}

.tab {
  flex: 1;
  text-align: center;
  padding: 12px;
  font-size: 14px;
  color: rgba(220, 218, 255, 0.6);
  border-radius: 10px;
  transition: all 0.2s;

  &.active {
    background: rgba(91, 80, 240, 0.8);
    color: #fff;
    font-weight: 600;
  }
}

.form {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 16px;
  padding: 24px;
}

.input-group {
  margin-bottom: 16px;
}

// 数学验证码
.captcha-group {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.captcha-input {
  flex: 1;
}

.captcha-question {
  background: linear-gradient(135deg, #667eea, #764ba2);
  padding: 24rpx 30rpx;
  border-radius: 12rpx;
  color: #fff;
  font-weight: bold;
  font-size: 28rpx;
  white-space: nowrap;
}

.input {
  width: 100%;
  height: 50px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 12px;
  padding: 0 16px;
  font-size: 15px;
  color: #fff;
  box-sizing: border-box;

  &::placeholder {
    color: rgba(255, 255, 255, 0.4);
  }
}

.code-group {
  display: flex;
  gap: 12px;

  .input {
    flex: 1;
  }
}

.code-btn {
  width: 110px;
  height: 50px;
  background: rgba(91, 80, 240, 0.8);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  color: #fff;
  flex-shrink: 0;

  &.disabled {
    opacity: 0.6;
  }
}

.submit-btn {
  width: 100%;
  height: 50px;
  background: linear-gradient(135deg, #6D5EF7, #4338CA);
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  color: #fff;
  margin-top: 8px;
  border: none;
}

.footer {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 20px;
}

.footer-text {
  font-size: 14px;
  color: rgba(220, 218, 255, 0.6);
}

.link {
  font-size: 14px;
  color: #A78BFA;
  margin-left: 6px;
  font-weight: 600;
}
</style>
