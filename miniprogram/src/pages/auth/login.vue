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
        <text class="input-label">邮箱</text>
        <input
          v-model="form.username"
          class="input"
          placeholder="请输入邮箱"
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

      <!-- Social Login Divider -->
      <view class="divider-row">
        <view class="divider-line" />
        <text class="divider-text">其他登录方式</text>
        <view class="divider-line" />
      </view>

      <!-- Social Login Buttons -->
      <view class="social-row">
        <view class="social-btn wechat-btn" @tap="handleSocialLogin('wechat')">
          <text class="social-icon">💬</text>
          <text class="social-label">微信登录</text>
        </view>
        <view class="social-btn qq-btn" @tap="handleSocialLogin('qq')">
          <text class="social-icon">🐧</text>
          <text class="social-label">QQ登录</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { authApi, socialAuthApi } from '@/services/api'

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
    return uni.showToast({ title: '请输入邮箱', icon: 'none' })
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
  } catch (e: any) {
    uni.showToast({ title: e?.message || '登录失败，请重试', icon: 'none' })
    loadCaptcha()
  } finally {
    loginLoading.value = false
  }
}

async function handleSocialLogin(provider: 'wechat' | 'qq') {
  if (provider === 'wechat') {
    uni.showToast({ title: '微信登录需企业身份认证，建议使用邮箱或QQ注册登录', icon: 'none', duration: 3000 })
    return
  }

  // #ifdef APP-PLUS
  // APP端使用原生QQ SDK登录
  try {
    uni.showLoading({ title: '正在登录...', mask: true })
    const providers = await new Promise<any[]>((resolve, reject) => {
      uni.getProvider({
        service: 'oauth',
        success: (res) => resolve(res.provider || []),
        fail: reject
      })
    })
    if (!providers.includes('qq')) {
      uni.hideLoading()
      uni.showToast({ title: '请先安装QQ', icon: 'none' })
      return
    }
    const loginRes = await new Promise<any>((resolve, reject) => {
      uni.login({
        provider: 'qq',
        success: resolve,
        fail: reject
      })
    })
    const accessToken = loginRes.authResult?.access_token
    if (!accessToken) {
      uni.hideLoading()
      uni.showToast({ title: 'QQ授权失败', icon: 'none' })
      return
    }
    const res = await socialAuthApi.qqTokenLogin(accessToken) as any
    uni.hideLoading()
    if (res.code === 200 && res.data) {
      uni.setStorageSync('token', res.data.token)
      uni.setStorageSync('user', JSON.stringify(res.data.user))
      uni.showToast({ title: '登录成功', icon: 'success' })
      setTimeout(() => uni.switchTab({ url: '/pages/index/index' }), 500)
    } else {
      uni.showToast({ title: res.message || 'QQ登录失败', icon: 'none' })
    }
  } catch (e: any) {
    uni.hideLoading()
    const msg = e?.errMsg || e?.message || '登录失败'
    if (msg.includes('cancel')) return
    uni.showToast({ title: 'QQ登录失败，请稍后重试', icon: 'none' })
  }
  return
  // #endif

  // #ifdef H5
  try {
    uni.showLoading({ title: '正在跳转...', mask: true })
    const res = await socialAuthApi.getQqAuthUrl('h5_login') as any
    uni.hideLoading()
    if (res.code === 200 && res.data) {
      window.location.href = res.data
    } else {
      uni.showToast({ title: res.message || '获取授权地址失败', icon: 'none' })
    }
  } catch (e: any) {
    uni.hideLoading()
    uni.showToast({ title: e?.message || '登录失败，请稍后重试', icon: 'none' })
  }
  // #endif
}

function goTo(url: string) {
  uni.navigateTo({ url })
}
</script>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  background: $background;
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
  background: $gradient-accent;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  letter-spacing: 4rpx;
  font-family: 'Calistoga', Georgia, 'PingFang SC', serif;
}

.app-slogan {
  font-size: 26rpx;
  color: $muted-foreground;
  margin-top: 12rpx;
  font-family: 'Inter', 'PingFang SC', 'Microsoft YaHei', sans-serif;
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
  color: $muted-foreground;
  margin-bottom: 12rpx;
  font-weight: 500;
  font-family: 'Inter', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.input {
  width: 100%;
  height: 88rpx;
  background: $card;
  border-radius: $radius-xl;
  padding: 0 28rpx;
  font-size: 28rpx;
  color: $foreground;
  box-sizing: border-box;
  border: 2rpx solid $border;
  transition: border-color 0.2s ease;

  &:focus {
    border-color: $accent;
    box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.1);
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
  border-radius: $radius-xl;
  overflow: hidden;
  background: $muted;
  border: 2rpx solid $border;
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
  color: $muted-foreground;
}

.btn-primary {
  width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  background: $gradient-accent;
  color: #fff;
  font-size: 32rpx;
  font-weight: 600;
  border-radius: $radius-xl;
  border: none;
  box-shadow: $shadow-accent;
  margin-bottom: 24rpx;
  font-family: 'Inter', 'PingFang SC', 'Microsoft YaHei', sans-serif;

  &::after {
    border: none;
  }

  &:active {
    transform: translateY(1px);
    box-shadow: 0 2px 6px rgba(16, 185, 129, 0.15);
    opacity: 0.95;
  }

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
  color: $accent;
  font-family: 'Inter', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.legal-links {
  text-align: center;
  margin-top: 40rpx;
  font-size: 22rpx;
  color: $muted-foreground;
}

.legal-link {
  color: $accent;
}

/* Social Login */
.divider-row {
  display: flex;
  align-items: center;
  margin: 48rpx 0 32rpx;
  gap: 16rpx;
}

.divider-line {
  flex: 1;
  height: 0;
  border-top: 1rpx solid $border;
}

.divider-text {
  font-size: 24rpx;
  color: $muted-foreground;
  white-space: nowrap;
}

.social-row {
  display: flex;
  justify-content: center;
  gap: 60rpx;
}

.social-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12rpx;
  padding: 20rpx 32rpx;
  border-radius: $radius-xl;
  background: $card;
  border: 1rpx solid $border;
  min-width: 160rpx;
  box-shadow: $shadow-sm;
  transition: transform 0.15s;
}

.social-btn:active {
  transform: translateY(1px);
  box-shadow: none;
}

.social-icon {
  font-size: 48rpx;
}

.social-label {
  font-size: 22rpx;
  color: $muted-foreground;
}

.wechat-btn {
  background: #f0faf3;
  border-color: $border;
}

.qq-btn {
  background: #f0f4ff;
  border-color: $border;
}
</style>
