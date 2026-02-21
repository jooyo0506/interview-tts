<template>
  <view class="register-page">
    <view class="header">
      <text class="title">注册</text>
      <text class="subtitle">创建您的账号</text>
    </view>

    <view class="form">
      <!-- 手机号 -->
      <view class="input-group">
        <text class="label">手机号</text>
        <input
          class="input"
          v-model="form.phone"
          type="number"
          placeholder="请输入手机号"
          maxlength="11"
        />
      </view>

      <!-- 数学验证码 -->
      <view class="input-group">
        <text class="label">图形验证码</text>
        <view class="captcha-wrap">
          <view class="captcha-question">
            <text>{{ captchaQuestion }}</text>
          </view>
          <input
            class="input captcha-input"
            v-model="form.captcha"
            type="number"
            placeholder="请输入答案"
          />
        </view>
      </view>

      <!-- 短信验证码 -->
      <view class="input-group">
        <text class="label">短信验证码</text>
        <view class="code-group">
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
      </view>

      <!-- 密码 -->
      <view class="input-group">
        <text class="label">设置密码</text>
        <input
          class="input"
          v-model="form.password"
          type="password"
          placeholder="请设置密码（至少6位）"
        />
      </view>

      <!-- 提交按钮 -->
      <button
        class="submit-btn"
        :loading="loading"
        @click="handleRegister"
      >
        注册
      </button>

      <!-- 登录链接 -->
      <view class="footer">
        <text class="footer-text">已有账号？</text>
        <text class="link" @click="goLogin">立即登录</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { sendCode as apiSendCode, register } from '@/api/auth'

const loading = ref(false)
const countdown = ref(0)
const captchaQuestion = ref('')
const captchaAnswer = ref(0)

let countdownTimer = null

const form = ref({
  phone: '',
  code: '',
  password: '',
  captcha: '',
  captchaId: ''
})

onMounted(() => {
  generateCaptcha()
})

function generateCaptcha() {
  const a = Math.floor(Math.random() * 10) + 1
  const b = Math.floor(Math.random() * 10) + 1
  const isPlus = Math.random() > 0.5
  captchaAnswer.value = isPlus ? a + b : a - b
  captchaQuestion.value = `${a} ${isPlus ? '+' : '-'} ${b} = ?`
}

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
  if (String(form.value.captcha) !== String(captchaAnswer.value)) {
    uni.showToast({ title: '图形验证码错误', icon: 'none' })
    generateCaptcha()
    return
  }

  try {
    await apiSendCode(form.value.phone, 'REGISTER')
    uni.showToast({ title: '验证码已发送', icon: 'success' })
    startCountdown()
  } catch (e) {
    uni.showToast({ title: e.message || '发送失败', icon: 'none' })
  }
}

async function handleRegister() {
  if (!form.value.phone || form.value.phone.length !== 11) {
    uni.showToast({ title: '请输入正确的手机号', icon: 'none' })
    return
  }
  if (String(form.value.captcha) !== String(captchaAnswer.value)) {
    uni.showToast({ title: '图形验证码错误', icon: 'none' })
    return
  }
  if (!form.value.code || form.value.code.length !== 6) {
    uni.showToast({ title: '请输入6位验证码', icon: 'none' })
    return
  }
  if (!form.value.password || form.value.password.length < 6) {
    uni.showToast({ title: '密码至少6位', icon: 'none' })
    return
  }

  loading.value = true
  try {
    const res = await register(form.value)
    uni.setStorageSync('token', res.data.token)
    uni.setStorageSync('userInfo', res.data.user)
    uni.showToast({ title: '注册成功', icon: 'success' })
    setTimeout(() => {
      uni.navigateBack()
    }, 1000)
  } catch (e) {
    uni.showToast({ title: e.message || '注册失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

function goLogin() {
  uni.navigateBack()
}
</script>

<style lang="scss" scoped>
.register-page {
  min-height: 100vh;
  background: linear-gradient(160deg, #1E1B4B 0%, #312E81 100%);
  padding: 60px 24px 40px;
}

.header {
  text-align: center;
  margin-bottom: 32px;
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

.form {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 16px;
  padding: 24px;
}

.input-group {
  margin-bottom: 20px;
}

.label {
  display: block;
  font-size: 14px;
  color: rgba(220, 218, 255, 0.8);
  margin-bottom: 8px;
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

.captcha-wrap {
  display: flex;
  gap: 12px;
}

.captcha-question {
  width: 100px;
  height: 50px;
  background: rgba(91, 80, 240, 0.4);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  color: #fff;
  font-weight: 600;
}

.captcha-input {
  flex: 1;
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
