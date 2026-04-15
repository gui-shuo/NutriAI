<template>
  <view class="register-page">
    <view class="header-section">
      <text class="page-title">创建账号</text>
      <text class="page-desc">注册后即可享受营养健康服务</text>
    </view>

    <view class="form-section">
      <view class="input-group">
        <text class="input-icon">👤</text>
        <input v-model="form.username" placeholder="用户名（3-20个字符）" />
      </view>

      <view class="input-group mt-md">
        <text class="input-icon">📧</text>
        <input v-model="form.email" placeholder="邮箱地址" type="text" />
      </view>

      <view class="email-code-row mt-md">
        <view class="input-group" style="flex:1">
          <input v-model="form.verificationCode" placeholder="邮箱验证码" type="number" />
        </view>
        <button
          class="send-code-btn"
          :disabled="codeCooldown > 0"
          @tap="sendCode"
        >
          {{ codeCooldown > 0 ? `${codeCooldown}s` : '发送验证码' }}
        </button>
      </view>

      <view class="input-group mt-md">
        <text class="input-icon">🔒</text>
        <input v-model="form.password" placeholder="密码（至少6位）" :password="true" />
      </view>

      <view class="input-group mt-md">
        <text class="input-icon">🔒</text>
        <input v-model="form.confirmPassword" placeholder="确认密码" :password="true" />
      </view>

      <!-- Password strength -->
      <view v-if="form.password" class="pwd-strength mt-sm">
        <view class="strength-bar">
          <view class="strength-fill" :style="{ width: pwdStrength.width, background: pwdStrength.color }"></view>
        </view>
        <text class="text-xs" :style="{ color: pwdStrength.color }">{{ pwdStrength.label }}</text>
      </view>

      <button class="btn-primary btn-block btn-lg mt-xl" :disabled="loading" @tap="handleRegister">
        {{ loading ? '注册中...' : '注 册' }}
      </button>

      <view class="bottom-links mt-lg">
        <text class="link-text" @tap="goLogin">已有账号？立即登录</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useUserStore } from '../../stores/user'
import { authApi } from '../../services/api'

const userStore = useUserStore()
const loading = ref(false)
const codeCooldown = ref(0)
let cooldownTimer: ReturnType<typeof setInterval>

const form = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
  verificationCode: ''
})

const pwdStrength = computed(() => {
  const pwd = form.password
  if (pwd.length < 6) return { width: '20%', color: '#EF4444', label: '太短' }
  let score = 0
  if (pwd.length >= 8) score++
  if (/[A-Z]/.test(pwd)) score++
  if (/[0-9]/.test(pwd)) score++
  if (/[^A-Za-z0-9]/.test(pwd)) score++
  if (score <= 1) return { width: '40%', color: '#F59E0B', label: '较弱' }
  if (score <= 2) return { width: '60%', color: '#3B82F6', label: '一般' }
  if (score <= 3) return { width: '80%', color: '#10B981', label: '较强' }
  return { width: '100%', color: '#059669', label: '很强' }
})

async function sendCode() {
  if (!form.email) {
    return uni.showToast({ title: '请输入邮箱地址', icon: 'none' })
  }
  try {
    await authApi.sendEmailCode(form.email)
    uni.showToast({ title: '验证码已发送', icon: 'success' })
    codeCooldown.value = 60
    cooldownTimer = setInterval(() => {
      codeCooldown.value--
      if (codeCooldown.value <= 0) clearInterval(cooldownTimer)
    }, 1000)
  } catch (e: any) {
    uni.showToast({ title: e?.message || '发送失败', icon: 'none' })
  }
}

async function handleRegister() {
  if (!form.username.trim() || form.username.length < 3) {
    return uni.showToast({ title: '用户名至少3个字符', icon: 'none' })
  }
  if (!form.email) {
    return uni.showToast({ title: '请输入邮箱', icon: 'none' })
  }
  if (!form.verificationCode) {
    return uni.showToast({ title: '请输入验证码', icon: 'none' })
  }
  if (form.password.length < 6) {
    return uni.showToast({ title: '密码至少6位', icon: 'none' })
  }
  if (form.password !== form.confirmPassword) {
    return uni.showToast({ title: '两次密码不一致', icon: 'none' })
  }

  loading.value = true
  const result = await userStore.register({
    username: form.username.trim(),
    email: form.email.trim(),
    password: form.password,
    verificationCode: form.verificationCode
  })
  loading.value = false

  if (result.success) {
    uni.showToast({ title: '注册成功', icon: 'success' })
    setTimeout(() => {
      uni.navigateBack()
    }, 1000)
  } else {
    uni.showToast({ title: result.message || '注册失败', icon: 'none' })
  }
}

function goLogin() {
  uni.navigateBack()
}
</script>

<style lang="scss" scoped>
.register-page {
  min-height: 100vh;
  padding: 0 60rpx;
  background: $bg-page;
}

.header-section {
  padding-top: 80rpx;
  padding-bottom: 60rpx;
}

.page-title {
  display: block;
  font-size: $font-3xl;
  font-weight: 700;
  color: $text-primary;
}

.page-desc {
  display: block;
  font-size: $font-base;
  color: $text-secondary;
  margin-top: $spacing-sm;
}

.email-code-row {
  display: flex;
  gap: $spacing-sm;
  align-items: center;
}

.send-code-btn {
  flex-shrink: 0;
  height: 88rpx;
  padding: 0 28rpx;
  font-size: $font-sm;
  color: $primary;
  background: $primary-50;
  border: none;
  border-radius: $radius-lg;
  line-height: 88rpx;

  &::after { border: none; }
  &[disabled] { opacity: 0.5; }
}

.pwd-strength {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
}

.strength-bar {
  flex: 1;
  height: 6rpx;
  background: $bg-muted;
  border-radius: 3rpx;
  overflow: hidden;
}

.strength-fill {
  height: 100%;
  border-radius: 3rpx;
  transition: all 0.3s;
}

.bottom-links {
  text-align: center;
}

.link-text {
  font-size: $font-sm;
  color: $primary;
}
</style>
