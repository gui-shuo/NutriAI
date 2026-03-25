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
            <el-link type="primary" :underline="false"> 忘记密码？ </el-link>
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

      <!-- 快速登录提示 -->
      <div class="quick-login-tip">
        <el-divider>或使用测试账号登录</el-divider>
        <p class="tip-text">
          管理员：admin / admin123<br />
          测试用户：testuser / Test123456
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Loading } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
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

// 处理登录
const handleLogin = async () => {
  if (!loginFormRef.value) return

  try {
    // 表单验证
    const valid = await loginFormRef.value.validate().catch(() => false)
    if (!valid) {
      ElMessage.warning('请完善登录信息')
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
      } else if (errorData.code === 40005) {
        message.error('验证码错误或已过期')
        if (showCaptcha.value) {
          refreshCaptcha()
        }
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

    // 失败3次后显示验证码
    if (loginFailCount.value >= 3 && !showCaptcha.value) {
      showCaptcha.value = true
      await getCaptcha()
      message.warning('连续登录失败，请输入验证码')
    }

    // 刷新验证码
    if (showCaptcha.value) {
      refreshCaptcha()
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
</style>
