<template>
  <view class="login-page">
    <!-- Logo -->
    <view class="logo-section">
      <view class="logo-circle">
        <text class="logo-emoji">🥗</text>
      </view>
      <text class="app-name">NutriAI 营养助手</text>
      <text class="app-desc">科学营养 · 健康生活</text>
    </view>

    <!-- Login Form -->
    <view class="form-section">
      <view class="input-group">
        <text class="input-icon">📧</text>
        <input
          v-model="form.username"
          placeholder="请输入邮箱/用户名"
          placeholder-class="input-placeholder"
          type="text"
        />
      </view>

      <view class="input-group mt-md">
        <text class="input-icon">🔒</text>
        <input
          v-model="form.password"
          placeholder="请输入密码"
          placeholder-class="input-placeholder"
          :password="!showPassword"
          type="text"
        />
        <text class="toggle-pwd" @tap="showPassword = !showPassword">
          {{ showPassword ? '🙈' : '👁️' }}
        </text>
      </view>

      <!-- Captcha (shown after failed attempts) -->
      <view v-if="showCaptcha" class="captcha-row mt-md">
        <view class="input-group" style="flex:1">
          <input v-model="form.captchaCode" placeholder="验证码" />
        </view>
        <image
          v-if="captchaImg"
          :src="captchaImg"
          class="captcha-img"
          mode="aspectFit"
          @tap="loadCaptcha"
        />
      </view>

      <button class="btn-primary btn-block btn-lg mt-lg" :disabled="loading" @tap="handleLogin">
        {{ loading ? '登录中...' : '登 录' }}
      </button>

      <!-- WeChat Login -->
      <!-- #ifdef MP-WEIXIN -->
      <view class="divider-text mt-lg">
        <view class="divider-line"></view>
        <text class="divider-label">快捷登录</text>
        <view class="divider-line"></view>
      </view>

      <button class="wx-login-btn mt-md" @tap="handleWxLogin" :disabled="wxLoading">
        <text class="wx-icon">💬</text>
        <text>{{ wxLoading ? '登录中...' : '微信一键登录' }}</text>
      </button>
      <!-- #endif -->

      <view class="bottom-links mt-lg">
        <text class="link-text" @tap="goRegister">还没有账号？立即注册</text>
      </view>
    </view>

    <!-- Agreement -->
    <view class="agreement safe-bottom">
      <text class="text-muted text-xs">登录即表示同意</text>
      <text class="text-primary text-xs" @tap="goLegal('terms')">《用户协议》</text>
      <text class="text-muted text-xs">和</text>
      <text class="text-primary text-xs" @tap="goLegal('privacy')">《隐私政策》</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useUserStore } from '../../stores/user'
import { authApi } from '../../services/api'

const userStore = useUserStore()
const loading = ref(false)
const wxLoading = ref(false)
const showPassword = ref(false)
const showCaptcha = ref(false)
const captchaImg = ref('')
const failCount = ref(0)

const form = reactive({
  username: '',
  password: '',
  captchaKey: '',
  captchaCode: ''
})

async function loadCaptcha() {
  try {
    const res: any = await authApi.getCaptcha()
    captchaImg.value = res.image || res.captchaImage
    form.captchaKey = res.key || res.captchaKey
  } catch {}
}

async function handleLogin() {
  if (!form.username.trim()) {
    return uni.showToast({ title: '请输入邮箱/用户名', icon: 'none' })
  }
  if (!form.password) {
    return uni.showToast({ title: '请输入密码', icon: 'none' })
  }
  if (showCaptcha.value && !form.captchaCode) {
    return uni.showToast({ title: '请输入验证码', icon: 'none' })
  }

  loading.value = true
  const result = await userStore.login(
    form.username.trim(),
    form.password,
    showCaptcha.value ? form.captchaKey : undefined,
    showCaptcha.value ? form.captchaCode : undefined
  )
  loading.value = false

  if (result.success) {
    uni.showToast({ title: '登录成功', icon: 'success' })
    setTimeout(() => {
      uni.switchTab({ url: '/pages/index/index' })
    }, 500)
  } else {
    failCount.value++
    if (failCount.value >= 3) {
      showCaptcha.value = true
      loadCaptcha()
    }
    uni.showToast({ title: result.message || '登录失败', icon: 'none' })
  }
}

async function handleWxLogin() {
  wxLoading.value = true
  const result = await userStore.wxLogin()
  wxLoading.value = false

  if (result.success) {
    uni.showToast({ title: '登录成功', icon: 'success' })
    setTimeout(() => {
      uni.switchTab({ url: '/pages/index/index' })
    }, 500)
  } else {
    uni.showToast({ title: result.message || '微信登录失败', icon: 'none' })
  }
}

function goRegister() {
  uni.navigateTo({ url: '/pages/auth/register' })
}

function goLegal(type: string) {
  // Navigate to legal pages if available
}
</script>

<style lang="scss" scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  padding: 0 60rpx;
  background: $bg-page;
}

.logo-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 160rpx;
  padding-bottom: 80rpx;
}

.logo-circle {
  width: 140rpx;
  height: 140rpx;
  border-radius: 50%;
  background: $gradient-primary;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: $shadow-primary;
}

.logo-emoji {
  font-size: 64rpx;
}

.app-name {
  font-size: $font-2xl;
  font-weight: 700;
  color: $text-primary;
  margin-top: $spacing-lg;
  letter-spacing: 2rpx;
}

.app-desc {
  font-size: $font-sm;
  color: $text-muted;
  margin-top: $spacing-xs;
}

.form-section {
  flex: 1;
}

.input-placeholder {
  color: $text-muted;
  font-size: $font-base;
}

.toggle-pwd {
  font-size: 36rpx;
  padding: 0 8rpx;
}

.captcha-row {
  display: flex;
  align-items: center;
  gap: $spacing-md;
}

.captcha-img {
  width: 200rpx;
  height: 88rpx;
  border-radius: $radius-md;
}

.divider-text {
  display: flex;
  align-items: center;
  gap: $spacing-md;
}

.divider-line {
  flex: 1;
  height: 1rpx;
  background: $border-color;
}

.divider-label {
  font-size: $font-sm;
  color: $text-muted;
}

.wx-login-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: $spacing-sm;
  height: 88rpx;
  border-radius: $radius-xl;
  background: #07C160;
  color: #fff;
  font-size: $font-base;
  font-weight: 500;
  border: none;

  &::after { border: none; }
}

.wx-icon {
  font-size: 36rpx;
}

.bottom-links {
  text-align: center;
}

.link-text {
  font-size: $font-sm;
  color: $primary;
}

.agreement {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  padding: $spacing-lg 0;
  gap: 4rpx;
}
</style>
