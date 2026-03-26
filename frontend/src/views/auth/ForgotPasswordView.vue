<template>
  <div class="forgot-container">
    <div class="forgot-card">
      <!-- Logo和标题 -->
      <div class="forgot-header">
        <h1 class="logo">🥗 NutriAI</h1>
        <p class="subtitle">重置您的密码</p>
      </div>

      <!-- 步骤指示器 -->
      <el-steps :active="currentStep" align-center class="step-indicator">
        <el-step title="验证邮箱" />
        <el-step title="重置密码" />
        <el-step title="完成" />
      </el-steps>

      <!-- 步骤1：发送验证码 -->
      <el-form
        v-if="currentStep === 0"
        ref="emailFormRef"
        :model="emailForm"
        :rules="emailRules"
        class="forgot-form"
        @submit.prevent="handleSendCode"
      >
        <p class="form-tip">请输入您注册时使用的邮箱地址，我们将发送验证码到该邮箱。</p>

        <el-form-item prop="email">
          <el-input
            v-model="emailForm.email"
            placeholder="请输入邮箱地址"
            size="large"
            prefix-icon="Message"
            clearable
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="sendLoading"
            :disabled="countdown > 0"
            class="submit-button"
            @click="handleSendCode"
          >
            {{ countdown > 0 ? `重新发送 (${countdown}s)` : sendLoading ? '发送中...' : '发送验证码' }}
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 步骤2：验证并重置密码 -->
      <el-form
        v-if="currentStep === 1"
        ref="resetFormRef"
        :model="resetForm"
        :rules="resetRules"
        class="forgot-form"
        label-position="top"
        @submit.prevent="handleResetPassword"
      >
        <p class="form-tip">
          验证码已发送至 <strong>{{ maskedEmail }}</strong>，请查收。
        </p>

        <el-form-item label="验证码" prop="code">
          <div class="code-wrapper">
            <el-input
              v-model="resetForm.code"
              placeholder="请输入6位验证码"
              size="large"
              maxlength="6"
              class="code-input"
            />
            <el-button
              :disabled="countdown > 0"
              :loading="sendLoading"
              @click="handleResendCode"
            >
              {{ countdown > 0 ? `${countdown}s` : '重新发送' }}
            </el-button>
          </div>
        </el-form-item>

        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="resetForm.newPassword"
            type="password"
            placeholder="6-20个字符，含大小写字母和数字"
            size="large"
            prefix-icon="Lock"
            show-password
          />
          <PasswordStrength :password="resetForm.newPassword" />
        </el-form-item>

        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input
            v-model="resetForm.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            size="large"
            prefix-icon="Lock"
            show-password
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="resetLoading"
            class="submit-button"
            @click="handleResetPassword"
          >
            {{ resetLoading ? '重置中...' : '重置密码' }}
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 步骤3：完成 -->
      <div v-if="currentStep === 2" class="success-section">
        <el-result icon="success" title="密码重置成功" sub-title="请使用新密码登录您的账号">
          <template #extra>
            <el-button type="primary" size="large" @click="goLogin"> 去登录 </el-button>
          </template>
        </el-result>
      </div>

      <!-- 返回登录 -->
      <div v-if="currentStep < 2" class="back-link">
        <router-link to="/login" class="link"> ← 返回登录 </router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import PasswordStrength from '@/components/PasswordStrength.vue'
import api from '@/services/api'
import message from '@/utils/message'

const router = useRouter()

// 步骤控制
const currentStep = ref(0)

// 表单引用
const emailFormRef = ref()
const resetFormRef = ref()

// 加载状态
const sendLoading = ref(false)
const resetLoading = ref(false)

// 倒计时
const countdown = ref(0)
let countdownTimer = null

// 表单数据
const emailForm = reactive({
  email: ''
})

const resetForm = reactive({
  code: '',
  newPassword: '',
  confirmPassword: ''
})

// 邮箱脱敏显示
const maskedEmail = computed(() => {
  const email = emailForm.email
  if (!email) return ''
  const [local, domain] = email.split('@')
  if (!domain) return email
  const masked = local.length > 2
    ? local[0] + '***' + local[local.length - 1]
    : local[0] + '***'
  return masked + '@' + domain
})

// 验证规则
const emailRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ]
}

const validateConfirmPassword = (rule, value, callback) => {
  if (!value) {
    return callback(new Error('请再次输入新密码'))
  }
  if (value !== resetForm.newPassword) {
    return callback(new Error('两次输入的密码不一致'))
  }
  callback()
}

const resetRules = {
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码长度为6位', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为6-20个字符', trigger: 'blur' },
    {
      pattern: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)[a-zA-Z\d@$!%*?&]+$/,
      message: '密码必须包含大小写字母和数字',
      trigger: 'blur'
    }
  ],
  confirmPassword: [{ validator: validateConfirmPassword, trigger: 'blur' }]
}

// 启动倒计时
const startCountdown = (seconds = 60) => {
  countdown.value = seconds
  if (countdownTimer) clearInterval(countdownTimer)
  countdownTimer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      clearInterval(countdownTimer)
      countdownTimer = null
    }
  }, 1000)
}

// 发送验证码
const handleSendCode = async () => {
  if (!emailFormRef.value) return

  const valid = await emailFormRef.value.validate().catch(() => false)
  if (!valid) return

  sendLoading.value = true
  try {
    const response = await api.post('/auth/forgot-password', {
      email: emailForm.email
    })

    if (response.data.code === 200) {
      message.success('验证码已发送到您的邮箱')
      startCountdown()
      currentStep.value = 1
    } else {
      message.error(response.data.message || '发送失败')
    }
  } catch (error) {
    const errorData = error.response?.data
    if (errorData?.code === 40010) {
      message.error('该邮箱未注册，请检查后重试')
    } else if (errorData?.code === 429) {
      message.warning(errorData.message || '发送过于频繁，请稍后重试')
    } else if (errorData?.code === 40012) {
      message.error('邮件发送失败，请检查邮箱地址或稍后重试')
    } else {
      message.error(errorData?.message || '网络错误，请稍后重试')
    }
  } finally {
    sendLoading.value = false
  }
}

// 重新发送验证码
const handleResendCode = async () => {
  sendLoading.value = true
  try {
    const response = await api.post('/auth/forgot-password', {
      email: emailForm.email
    })

    if (response.data.code === 200) {
      message.success('验证码已重新发送')
      startCountdown()
    } else {
      message.error(response.data.message || '发送失败')
    }
  } catch (error) {
    const errorData = error.response?.data
    if (errorData?.code === 429) {
      message.warning(errorData.message || '发送过于频繁，请稍后重试')
    } else {
      message.error(errorData?.message || '发送失败')
    }
  } finally {
    sendLoading.value = false
  }
}

// 重置密码
const handleResetPassword = async () => {
  if (!resetFormRef.value) return

  const valid = await resetFormRef.value.validate().catch(() => false)
  if (!valid) return

  resetLoading.value = true
  try {
    const response = await api.post('/auth/reset-password', {
      email: emailForm.email,
      code: resetForm.code,
      newPassword: resetForm.newPassword,
      confirmPassword: resetForm.confirmPassword
    })

    if (response.data.code === 200) {
      message.success('密码重置成功！')
      currentStep.value = 2
    } else {
      message.error(response.data.message || '重置失败')
    }
  } catch (error) {
    const errorData = error.response?.data
    if (errorData?.code === 40013) {
      message.error('验证码错误或已过期，请重新获取')
    } else {
      message.error(errorData?.message || '重置失败，请稍后重试')
    }
  } finally {
    resetLoading.value = false
  }
}

// 跳转登录
const goLogin = () => {
  router.push('/login')
}

// 清理定时器
onUnmounted(() => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
  }
})
</script>

<style scoped lang="scss">
.forgot-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.forgot-card {
  width: 100%;
  max-width: 480px;
  background: white;
  border-radius: 16px;
  padding: 40px 32px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.forgot-header {
  text-align: center;
  margin-bottom: 24px;

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

.step-indicator {
  margin-bottom: 32px;
}

.forgot-form {
  .form-tip {
    color: #6b7280;
    font-size: 14px;
    margin: 0 0 20px 0;
    line-height: 1.6;
  }

  .code-wrapper {
    display: flex;
    gap: 12px;

    .code-input {
      flex: 1;
    }
  }

  .submit-button {
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
}

.success-section {
  padding: 20px 0;
}

.back-link {
  text-align: center;
  margin-top: 20px;

  .link {
    color: #667eea;
    text-decoration: none;
    font-size: 14px;

    &:hover {
      text-decoration: underline;
    }
  }
}

// 深色模式适配
.dark {
  .forgot-card {
    background: #1f2937;
  }

  .forgot-header .subtitle {
    color: #9ca3af;
  }

  .forgot-form .form-tip {
    color: #9ca3af;
  }

  .back-link .link {
    color: #818cf8;
  }
}
</style>
