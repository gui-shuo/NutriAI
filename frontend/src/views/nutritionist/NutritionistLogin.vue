<template>
  <div class="nut-login-container">
    <div class="nut-login-card">
      <div class="nut-login-header">
        <div class="logo-icon">🩺</div>
        <h1>营养师登录</h1>
        <p class="subtitle">NutriAI 专业营养师服务平台</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="nut-form" @submit.prevent="handleLogin">
        <el-form-item label="邮箱" prop="email">
          <el-input
            v-model="form.email"
            placeholder="请输入营养师账号邮箱"
            size="large"
            prefix-icon="Message"
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            size="large"
            prefix-icon="Lock"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <el-form-item v-if="showCaptcha" label="验证码" prop="captcha">
          <div class="captcha-row">
            <el-input v-model="form.captcha" placeholder="请输入验证码" size="large" maxlength="4" @keyup.enter="handleLogin" />
            <img v-if="captchaImage" :src="captchaImage" class="captcha-img" @click="refreshCaptcha" title="点击刷新" />
          </div>
        </el-form-item>

        <el-checkbox v-model="form.rememberMe" class="remember-me">记住登录状态</el-checkbox>

        <el-button type="primary" size="large" class="login-btn" :loading="loading" @click="handleLogin">
          登 录
        </el-button>
      </el-form>

      <div class="login-footer">
        <router-link to="/nutritionist/register" class="register-link">申请成为营养师</router-link>
        <router-link to="/login" class="user-link">普通用户登录 →</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import api from '@/services/api'
import message from '@/utils/message'

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref()
const loading = ref(false)
const showCaptcha = ref(false)
const captchaImage = ref('')

const form = reactive({
  email: '',
  password: '',
  captcha: '',
  captchaKey: '',
  rememberMe: false
})

const rules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ],
  captcha: [
    { required: true, message: '请输入验证码', trigger: 'blur' }
  ]
}

const refreshCaptcha = async () => {
  try {
    const res = await api.get('/auth/captcha')
    if (res.data.code === 200) {
      captchaImage.value = res.data.data.captchaImage
      form.captchaKey = res.data.data.captchaKey
    }
  } catch { /* ignore */ }
}

onMounted(() => {
  if (authStore.isLoggedIn && authStore.userRole === 'NUTRITIONIST') {
    router.replace('/nutritionist/dashboard')
  }
})

const handleLogin = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res = await api.post('/auth/login', {
      username: form.email,
      password: form.password,
      captcha: form.captcha || undefined,
      captchaKey: form.captchaKey || undefined,
      rememberMe: form.rememberMe
    })

    if (res.data.code === 200) {
      const data = res.data.data
      if (data.userInfo?.role !== 'NUTRITIONIST' && data.userInfo?.role !== 'ADMIN' && data.userInfo?.role !== 'SUPER_ADMIN') {
        message.error('该账号不是营养师账号，请使用普通登录')
        return
      }
      authStore.setToken(data.accessToken)
      authStore.setRefreshToken(data.refreshToken)
      authStore.setUser(data.userInfo)
      message.success('登录成功')
      router.push('/nutritionist/dashboard')
    } else {
      message.error(res.data.message || '登录失败')
      if (res.data.data?.requireCaptcha || res.data.message?.includes('验证码')) {
        showCaptcha.value = true
        refreshCaptcha()
      }
    }
  } catch (e) {
    const msg = e.response?.data?.message || '登录失败'
    message.error(msg)
    if (msg.includes('验证码') || e.response?.data?.data?.requireCaptcha) {
      showCaptcha.value = true
      refreshCaptcha()
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.nut-login-container {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: #E2E8F0;
}

.nut-login-card {
  width: 100%;
  max-width: 420px;
  background: #FAFAFA;
  border-radius: 16px;
  padding: 40px 36px;
  box-shadow: 0 4px 14px rgba(16, 185, 129, 0.25);
  border: 1px solid #E2E8F0;
}

.nut-login-header {
  text-align: center;
  margin-bottom: 32px;
  .logo-icon { font-size: 48px; }
  h1 { font-size: 24px; color: #0F172A; margin: 8px 0 4px; font-weight: 700; font-family: 'Calistoga', serif; }
  .subtitle { color: #0F172A; font-size: 14px; margin: 0; }
}

.captcha-row {
  display: flex;
  gap: 12px;
  .el-input { flex: 1; }
}

.captcha-img {
  height: 40px;
  border-radius: 12px;
  border: 1px solid #E2E8F0;
  cursor: pointer;
  &:hover { opacity: 0.8; }
}

.remember-me { margin-bottom: 20px; }

.login-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
  background: #10B981;
  border: 1px solid #E2E8F0;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  transition: all 0.15s ease;
  &:hover {
    background: #10B981;
    box-shadow: 0 4px 14px rgba(16, 185, 129, 0.25);
    transform: translateY(-1px);
  }
}

.login-footer {
  display: flex;
  justify-content: space-between;
  margin-top: 20px;
  font-size: 14px;

  a {
    color: #10B981;
    text-decoration: none;
    font-weight: 500;
    &:hover { text-decoration: underline; }
  }

  .user-link { color: #0F172A; font-weight: 400; }
}

:deep(.el-form-item__label) { font-weight: 500; font-family: 'Calistoga', serif; color: #0F172A; }
:deep(.el-input__wrapper) {
  border-radius: 12px;
  border: 1px solid #E2E8F0 !important;
  box-shadow: none !important;
  background: #FAFAFA;
}
</style>
