<template>
  <view class="page">
    <!-- Logo -->
    <view class="logo-section">
      <text class="logo-icon">🥗</text>
      <text class="app-name">NutriAI</text>
      <text class="app-slogan">智能营养助手</text>
    </view>

    <!-- Login Form -->
    <view class="form-section">
      <view class="input-group">
        <text class="input-label">用户名</text>
        <input
          v-model="form.username"
          class="input"
          placeholder="请输入用户名"
          :disabled="loginLoading"
        />
      </view>

      <view class="input-group">
        <text class="input-label">密码</text>
        <input
          v-model="form.password"
          class="input"
          type="password"
          placeholder="请输入密码"
          :disabled="loginLoading"
        />
      </view>

      <!-- Captcha -->
      <view class="captcha-row">
        <view class="input-group captcha-input-wrap">
          <text class="input-label">验证码</text>
          <input
            v-model="form.captchaCode"
            class="input"
            placeholder="请输入验证码"
            :disabled="loginLoading"
          />
        </view>
        <view class="captcha-img-wrap" @tap="loadCaptcha">
          <image
            v-if="captchaImage"
            class="captcha-img"
            :src="captchaImage"
            mode="aspectFit"
          />
          <text v-else class="captcha-placeholder">点击获取</text>
        </view>
      </view>

      <!-- Login Button -->
      <button
        class="btn-primary"
        :loading="loginLoading"
        :disabled="loginLoading"
        @tap="handleLogin"
      >
        登录
      </button>

      <!-- Links -->
      <view class="links">
        <text class="link" @tap="goTo('/pages/auth/forgot-password')">忘记密码?</text>
        <text class="link" @tap="goTo('/pages/auth/register')">注册账号</text>
      </view>

      <!-- Legal -->
      <view class="legal-links">
        <text>登录即表示同意</text>
        <text class="legal-link" @tap="goTo('/pages/legal/index?type=terms')">《用户协议》</text>
        <text>和</text>
        <text class="legal-link" @tap="goTo('/pages/legal/index?type=privacy')">《隐私政策》</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { authApi } from '@/services/api'

const userStore = useUserStore()

const form = reactive({
  username: '',
  password: '',
  captchaCode: ''
})

const captchaImage = ref('')
const captchaKey = ref('')
const loginLoading = ref(false)

onLoad(() => {
  loadCaptcha()
})

async function loadCaptcha() {
  try {
    const res = await authApi.getCaptcha() as any
    if (res.code === 200 && res.data) {
      captchaKey.value = res.data.captchaKey
      captchaImage.value = res.data.captchaImage
    }
  } catch {
    // silently fail
  }
}

async function handleLogin() {
  if (!form.username.trim()) {
    return uni.showToast({ title: '请输入用户名', icon: 'none' })
  }
  if (!form.password) {
    return uni.showToast({ title: '请输入密码', icon: 'none' })
  }
  if (!form.captchaCode.trim()) {
    return uni.showToast({ title: '请输入验证码', icon: 'none' })
  }

  loginLoading.value = true
  try {
    const res = await userStore.login(
      form.username.trim(),
      form.password,
      captchaKey.value,
      form.captchaCode.trim()
    )
    if (res.code === 200) {
      uni.showToast({ title: '登录成功', icon: 'success' })
      setTimeout(() => {
        uni.switchTab({ url: '/pages/index/index' })
      }, 800)
    } else {
      uni.showToast({ title: res.message || '登录失败', icon: 'none' })
      loadCaptcha()
    }
  } catch {
    uni.showToast({ title: '网络错误', icon: 'none' })
    loadCaptcha()
  } finally {
    loginLoading.value = false
  }
}

function goTo(url: string) {
  uni.navigateTo({ url })
}
</script>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  background: #fff;
  padding: 0 48rpx;
}

.logo-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 100rpx 0 60rpx;
}

.logo-icon {
  font-size: 100rpx;
  margin-bottom: 16rpx;
}

.app-name {
  font-size: 48rpx;
  font-weight: 700;
  color: #07c160;
  letter-spacing: 4rpx;
}

.app-slogan {
  font-size: 26rpx;
  color: #999;
  margin-top: 12rpx;
}

.form-section {
  padding-top: 20rpx;
}

.input-group {
  margin-bottom: 28rpx;
}

.input-label {
  display: block;
  font-size: 26rpx;
  color: #666;
  margin-bottom: 12rpx;
  font-weight: 500;
}

.input {
  width: 100%;
  height: 88rpx;
  background: #f7f7f7;
  border-radius: 16rpx;
  padding: 0 28rpx;
  font-size: 28rpx;
  color: #333;
  box-sizing: border-box;
  border: 2rpx solid transparent;

  &:focus {
    border-color: #07c160;
    background: #fff;
  }
}

.captcha-row {
  display: flex;
  align-items: flex-end;
  gap: 20rpx;
  margin-bottom: 40rpx;
}

.captcha-input-wrap {
  flex: 1;
  margin-bottom: 0;
}

.captcha-img-wrap {
  width: 220rpx;
  height: 88rpx;
  border-radius: 16rpx;
  overflow: hidden;
  background: #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.captcha-img {
  width: 100%;
  height: 100%;
}

.captcha-placeholder {
  font-size: 24rpx;
  color: #999;
}

.btn-primary {
  width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  background: #07c160;
  color: #fff;
  font-size: 32rpx;
  font-weight: 600;
  border-radius: 16rpx;
  border: none;
  margin-bottom: 24rpx;

  &[disabled] {
    opacity: 0.6;
  }
}

.links {
  display: flex;
  justify-content: space-between;
  margin-top: 36rpx;
  padding: 0 8rpx;
}

.link {
  font-size: 26rpx;
  color: #07c160;
}

.legal-links {
  text-align: center;
  margin-top: 40rpx;
  font-size: 22rpx;
  color: #999;
}
.legal-link {
  color: #07c160;
}
</style>
