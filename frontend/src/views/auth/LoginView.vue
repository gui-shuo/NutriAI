<template>
  <div class="login-container">
    <div class="login-card">
      <!-- Logo和标题 -->
      <div class="login-header">
        <h1 class="logo">🥗 NutriAI</h1>
        <p class="subtitle">AI健康饮食规划助手</p>
      </div>

      <!-- 登录表单 -->
      <el-form
        ref="loginFormRef"
        :model="loginForm"
        :rules="loginRules"
        class="login-form"
        @submit.prevent="handleLogin"
      >
        <!-- 用户名 -->
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="请输入用户名"
            size="large"
            prefix-icon="User"
            clearable
          />
        </el-form-item>

        <!-- 密码 -->
        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            size="large"
            prefix-icon="Lock"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <!-- 验证码（登录失败3次后显示） -->
        <el-form-item v-if="showCaptcha" prop="captcha">
          <div class="captcha-wrapper">
            <el-input
              v-model="loginForm.captcha"
              placeholder="请输入验证码"
              size="large"
              maxlength="4"
              class="captcha-input"
            />
            <div class="captcha-image-wrapper" @click="refreshCaptcha">
              <img
                v-if="captchaImage"
                :src="captchaImage"
                class="captcha-image"
                alt="验证码"
                title="点击刷新验证码"
              />
              <div v-else class="captcha-loading">
                <el-icon class="is-loading">
                  <Loading />
                </el-icon>
                加载中...
              </div>
            </div>
          </div>
        </el-form-item>

        <!-- 记住我 -->
        <el-form-item>
          <div class="form-options">
            <el-checkbox v-model="loginForm.rememberMe"> 记住我（7天免登录） </el-checkbox>
            <el-link type="primary" :underline="false" @click="goForgotPassword">
              忘记密码？
            </el-link>
          </div>
        </el-form-item>

        <!-- 登录按钮 -->
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            class="login-button"
            @click="handleLogin"
          >
            {{ loading ? '登录中...' : '登录' }}
          </el-button>
        </el-form-item>

        <!-- 注册链接 -->
        <div class="register-link">
          还没有账号？
          <router-link to="/register" class="link"> 立即注册 </router-link>
        </div>
      </el-form>

      <!-- 第三方登录 -->
      <div class="social-login-section">
        <el-divider>其他登录方式</el-divider>
        <div class="social-buttons">
          <el-tooltip content="微信登录" placement="top">
            <button class="social-btn wechat-btn" @click="handleSocialLogin('wechat')" :disabled="socialLoading">
              <svg viewBox="0 0 24 24" width="24" height="24" fill="currentColor">
                <path d="M8.691 2.188C3.891 2.188 0 5.476 0 9.53c0 2.212 1.17 4.203 3.002 5.55a.59.59 0 0 1 .213.665l-.39 1.48c-.019.07-.048.141-.048.213 0 .163.13.295.29.295a.326.326 0 0 0 .167-.054l1.903-1.114a.864.864 0 0 1 .717-.098 10.16 10.16 0 0 0 2.837.403c.276 0 .543-.027.811-.05-.857-2.578.157-4.972 1.932-6.446 1.703-1.415 3.882-1.98 5.853-1.838-.576-3.583-4.196-6.348-8.596-6.348zM5.785 5.991c.642 0 1.162.529 1.162 1.18a1.17 1.17 0 0 1-1.162 1.178A1.17 1.17 0 0 1 4.623 7.17c0-.651.52-1.18 1.162-1.18zm5.813 0c.642 0 1.162.529 1.162 1.18a1.17 1.17 0 0 1-1.162 1.178 1.17 1.17 0 0 1-1.162-1.178c0-.651.52-1.18 1.162-1.18zm5.34 2.867c-1.797-.052-3.746.512-5.28 1.786-1.72 1.428-2.687 3.72-1.78 6.22.942 2.453 3.666 4.229 6.884 4.229.826 0 1.622-.12 2.361-.336a.722.722 0 0 1 .598.082l1.584.926a.272.272 0 0 0 .14.047c.134 0 .24-.111.24-.247 0-.06-.023-.12-.038-.177l-.327-1.233a.582.582 0 0 1-.023-.156.49.49 0 0 1 .201-.398C23.024 18.48 24 16.82 24 14.98c0-3.21-2.931-5.837-7.062-6.122zm-2.18 2.769c.535 0 .969.44.969.982a.976.976 0 0 1-.969.983.976.976 0 0 1-.969-.983c0-.542.434-.982.97-.982zm4.553 0c.535 0 .969.44.969.982a.976.976 0 0 1-.969.983.976.976 0 0 1-.969-.983c0-.542.434-.982.97-.982z"/>
              </svg>
            </button>
          </el-tooltip>
          <el-tooltip content="QQ登录" placement="top">
            <button class="social-btn qq-btn" @click="handleSocialLogin('qq')" :disabled="socialLoading">
              <svg viewBox="0 0 24 24" width="24" height="24" fill="currentColor">
                <path d="M21.395 15.035a39.548 39.548 0 0 0-1.42-2.256c.009-.021.016-.042.025-.063.511-1.205.778-2.503.778-3.774C20.778 4.014 16.764 0 11.836 0S2.893 4.014 2.893 8.942c0 1.271.267 2.569.778 3.774.009.021.016.042.025.063a39.56 39.56 0 0 0-1.42 2.256c-1.025 1.74-1.117 2.673-.897 2.995.226.33 1.213.212 2.705-.527a11.49 11.49 0 0 0 .396 1.006c.455.987 1.016 1.736 1.6 2.233.052.044.096.082.149.124-.074.052-.138.107-.179.161-.535.695-.329 1.17.084 1.578.435.431 1.386.766 2.455.93 1.044.162 1.873.067 2.705-.1.832-.169 1.392-.373 1.905-.563.512.19 1.073.394 1.905.563.832.167 1.661.262 2.705.1 1.069-.164 2.02-.499 2.455-.93.413-.408.619-.883.084-1.578-.041-.054-.105-.109-.179-.161.053-.042.097-.08.149-.124.584-.497 1.145-1.246 1.6-2.233a11.51 11.51 0 0 0 .396-1.006c1.492.739 2.479.857 2.705.527.22-.322.128-1.255-.897-2.995z"/>
              </svg>
            </button>
          </el-tooltip>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Loading } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { socialAuthApi } from '@/services/socialAuth'
import api from '@/services/api'
import message from '@/utils/message'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

// 表单引用
const loginFormRef = ref()

// 表单数据
const loginForm = reactive({
  username: '',
  password: '',
  captcha: '',
  captchaKey: '',
  rememberMe: false
})

// 验证规则
const loginRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度为3-20个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为6-20个字符', trigger: 'blur' }
  ],
  captcha: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 4, message: '验证码长度为4位', trigger: 'blur' }
  ]
}

// 状态
const loading = ref(false)
const showCaptcha = ref(false)
const captchaImage = ref('')
const loginFailCount = ref(0)

// 获取验证码
const getCaptcha = async () => {
  try {
    const response = await api.get('/auth/captcha')
    if (response.data.code === 200) {
      const data = response.data.data
      captchaImage.value = data.captchaImage
      loginForm.captchaKey = data.captchaKey
    }
  } catch (error) {
    console.error('获取验证码失败:', error)
  }
}

// 刷新验证码
const refreshCaptcha = () => {
  getCaptcha()
}

// 跳转到忘记密码页面
const goForgotPassword = () => {
  router.push('/forgot-password')
}

// 处理登录
const handleLogin = async () => {
  if (!loginFormRef.value) return

  try {
    // 表单验证
    const valid = await loginFormRef.value.validate().catch(() => false)
    if (!valid) {
      message.warning('请完善登录信息')
      return
    }

    loading.value = true

    // 调用登录接口
    const response = await api.post('/auth/login', {
      username: loginForm.username,
      password: loginForm.password,
      captcha: loginForm.captcha || undefined,
      captchaKey: loginForm.captchaKey || undefined,
      rememberMe: loginForm.rememberMe
    })

    if (response.data.code === 200) {
      // 登录成功
      const loginData = response.data.data

      // 保存到store
      authStore.setToken(loginData.accessToken)
      authStore.setRefreshToken(loginData.refreshToken)
      authStore.setUser(loginData.userInfo)

      message.success('登录成功！')

      // 重置失败计数
      loginFailCount.value = 0
      showCaptcha.value = false

      // 跳转到目标页面或首页
      const redirect = route.query.redirect || '/'
      router.push(redirect)
    } else {
      message.error(response.data.message || '登录失败')
      if (showCaptcha.value) {
        refreshCaptcha()
      }
    }
  } catch (error) {
    console.error('登录失败:', error)

    // 增加失败计数
    loginFailCount.value++

    // 标记是否已经刷新了验证码（避免重复刷新）
    let captchaRefreshed = false

    // 处理错误
    if (error.response?.data) {
      const errorData = error.response.data

      // 显示具体错误信息
      if (errorData.code === 40006) {
        message.error('用户名或密码错误')
      } else if (errorData.code === 40011) {
        message.error('登录失败次数过多，请输入验证码')
        showCaptcha.value = true
        await getCaptcha()
        captchaRefreshed = true
      } else if (errorData.code === 40005) {
        message.error('验证码错误或已过期')
      } else if (errorData.code === 40007) {
        message.error('账号已被禁用，请联系管理员')
      } else if (errorData.code === 40008) {
        message.error('账号已被封禁，请联系管理员')
      } else {
        message.error(errorData.message || '登录失败')
      }
    } else {
      message.error('网络错误，请稍后重试')
    }

    // 失败3次后显示验证码（前端补充保护，与后端同步）
    if (!captchaRefreshed) {
      if (loginFailCount.value >= 3 && !showCaptcha.value) {
        showCaptcha.value = true
        await getCaptcha()
      } else if (showCaptcha.value) {
        refreshCaptcha()
      }
    }
  } finally {
    loading.value = false
  }
}

// 组件挂载时检查是否已登录
onMounted(() => {
  if (authStore.isLoggedIn) {
    router.push('/')
  }
})

// 第三方社交登录
const socialLoading = ref(false)

const handleSocialLogin = async (provider) => {
  socialLoading.value = true
  try {
    let response
    if (provider === 'wechat') {
      response = await socialAuthApi.getWechatAuthUrl('login')
    } else if (provider === 'qq') {
      response = await socialAuthApi.getQqAuthUrl('login')
    }
    if (response?.data?.code === 200 && response.data.data) {
      window.location.href = response.data.data
    } else {
      message.warning(response?.data?.message || '该登录方式暂未开通，请使用账号密码登录')
    }
  } catch (error) {
    const errMsg = error.response?.data?.message || '该登录方式暂未开通'
    message.warning(errMsg)
  } finally {
    socialLoading.value = false
  }
}
</script>

<style scoped lang="scss">
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.login-card {
  width: 100%;
  max-width: 420px;
  background: white;
  border-radius: 16px;
  padding: 40px 32px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.login-header {
  text-align: center;
  margin-bottom: 32px;

  .logo {
    font-size: 32px;
    font-weight: bold;
    color: #667eea;
    margin: 0 0 8px 0;
  }

  .subtitle {
    color: #6b7280;
    font-size: 14px;
    margin: 0;
  }
}

.login-form {
  .captcha-wrapper {
    display: flex;
    gap: 12px;

    .captcha-input {
      flex: 1;
    }

    .captcha-image-wrapper {
      width: 120px;
      height: 40px;
      cursor: pointer;
    }

    .captcha-image {
      width: 100%;
      height: 100%;
      border-radius: 4px;
      border: 1px solid #dcdfe6;
      transition: all 0.3s;

      &:hover {
        opacity: 0.8;
        border-color: #667eea;
      }
    }

    .captcha-loading {
      width: 100%;
      height: 100%;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      background: #f5f5f5;
      border: 1px solid #dcdfe6;
      border-radius: 4px;
      font-size: 12px;
      color: #909399;
      gap: 4px;
    }
  }

  .form-options {
    display: flex;
    justify-content: space-between;
    align-items: center;
    width: 100%;
  }

  .login-button {
    width: 100%;
    height: 44px;
    font-size: 16px;
    font-weight: 500;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border: none;

    &:hover {
      opacity: 0.9;
    }
  }

  .register-link {
    text-align: center;
    color: #6b7280;
    font-size: 14px;

    .link {
      color: #667eea;
      text-decoration: none;
      font-weight: 500;

      &:hover {
        text-decoration: underline;
      }
    }
  }
}

.quick-login-tip {
  margin-top: 24px;

  .tip-text {
    text-align: center;
    font-size: 12px;
    color: #9ca3af;
    line-height: 1.6;
    margin: 8px 0 0 0;
  }
}

// 深色模式适配
.dark {
  .login-card {
    background: #1f2937;
  }

  .login-header {
    .subtitle {
      color: #9ca3af;
    }
  }

  .register-link {
    color: #9ca3af;
  }
}

.social-login-section {
  margin-top: 8px;

  :deep(.el-divider__text) {
    font-size: 13px;
    color: #9ca3af;
  }
}

.social-buttons {
  display: flex;
  justify-content: center;
  gap: 24px;
  margin-top: 8px;
}

.social-btn {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  border: 1px solid #e5e7eb;
  background: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
    transform: none;
  }
}

.wechat-btn {
  color: #07c160;
  &:hover { border-color: #07c160; background: #f0fdf4; }
}

.qq-btn {
  color: #12b7f5;
  &:hover { border-color: #12b7f5; background: #eff6ff; }
}
</style>
