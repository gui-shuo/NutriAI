<template>
  <div class="register-container">
    <div class="register-card">
      <!-- Logo和标题 -->
      <div class="register-header">
        <h1 class="logo font-display gradient-text">🥗 NutriAI</h1>
        <p class="subtitle font-sans">创建您的账号，开启智能饮食之旅</p>
      </div>

      <!-- 注册表单 -->
      <el-form
        ref="registerFormRef"
        :model="registerForm"
        :rules="registerRules"
        class="register-form"
        label-position="top"
      >
        <!-- 邮箱 -->
        <el-form-item label="邮箱" prop="email">
          <el-input
            v-model="registerForm.email"
            placeholder="请输入邮箱地址"
            size="large"
            prefix-icon="Message"
            clearable
            @blur="checkEmail"
          />
          <span v-if="emailAvailable === true" class="validation-tip success"> ✓ 邮箱可用 </span>
          <span v-else-if="emailAvailable === false" class="validation-tip error">
            ✗ 邮箱已被注册
          </span>
        </el-form-item>

        <!-- 邮箱验证码 -->
        <el-form-item label="邮箱验证码" prop="emailCode">
          <div class="email-code-wrapper">
            <el-input
              v-model="registerForm.emailCode"
              placeholder="请输入6位验证码"
              size="large"
              maxlength="6"
              class="email-code-input"
            />
            <el-button
              type="primary"
              size="large"
              :disabled="emailCodeCountdown > 0 || !registerForm.email || emailAvailable === false"
              :loading="emailCodeSending"
              class="send-code-btn"
              @click="sendEmailCode"
            >
              {{ emailCodeCountdown > 0 ? `${emailCodeCountdown}s` : '发送验证码' }}
            </el-button>
          </div>
        </el-form-item>

        <!-- 密码 -->
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="registerForm.password"
            type="password"
            placeholder="6-20个字符，含大小写字母和数字"
            size="large"
            prefix-icon="Lock"
            show-password
          />
          <!-- 密码强度指示器 -->
          <PasswordStrength :password="registerForm.password" />
        </el-form-item>

        <!-- 确认密码 -->
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="registerForm.confirmPassword"
            type="password"
            placeholder="请再次输入密码"
            size="large"
            prefix-icon="Lock"
            show-password
          />
        </el-form-item>

        <!-- 用户名（选填） -->
        <el-form-item label="用户名（选填）" prop="username">
          <el-input
            v-model="registerForm.username"
            placeholder="不填则自动生成，3-20个字符"
            size="large"
            prefix-icon="User"
            clearable
            @blur="checkUsername"
          />
          <span v-if="registerForm.username && usernameAvailable === true" class="validation-tip success">
            ✓ 用户名可用
          </span>
          <span v-else-if="registerForm.username && usernameAvailable === false" class="validation-tip error">
            ✗ 用户名已被占用
          </span>
        </el-form-item>

        <!-- 手机号（可选） -->
        <el-form-item label="手机号（可选）" prop="phone">
          <el-input
            v-model="registerForm.phone"
            placeholder="请输入手机号"
            size="large"
            prefix-icon="Phone"
            clearable
            maxlength="11"
          />
        </el-form-item>

        <!-- 昵称（可选） -->
        <el-form-item label="昵称（可选）" prop="nickname">
          <el-input
            v-model="registerForm.nickname"
            placeholder="请输入昵称"
            size="large"
            prefix-icon="Avatar"
            clearable
          />
        </el-form-item>

        <!-- 验证码 -->
        <el-form-item label="验证码" prop="captcha">
          <div class="captcha-wrapper">
            <el-input
              v-model="registerForm.captcha"
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

        <!-- 用户协议 -->
        <el-form-item prop="agree">
          <el-checkbox v-model="registerForm.agree">
            我已阅读并同意
            <router-link to="/legal/terms" target="_blank" style="color:#10B981;text-decoration:none">《用户协议》</router-link>
            和
            <router-link to="/legal/privacy" target="_blank" style="color:#10B981;text-decoration:none">《隐私政策》</router-link>
          </el-checkbox>
        </el-form-item>

        <!-- 注册按钮 -->
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            class="register-button"
            @click="handleRegister"
          >
            {{ loading ? '注册中...' : '立即注册' }}
          </el-button>
        </el-form-item>

        <!-- 登录链接 -->
        <div class="login-link font-sans">
          已有账号？
          <router-link to="/login" class="link"> 立即登录 </router-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Loading } from '@element-plus/icons-vue'
import PasswordStrength from '@/components/PasswordStrength.vue'
import api from '@/services/api'
import message from '@/utils/message'

const router = useRouter()
const route = useRoute()

// 表单引用
const registerFormRef = ref()

// 表单数据
const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  email: '',
  emailCode: '',
  phone: '',
  nickname: '',
  captcha: '',
  captchaKey: '',
  agree: false,
  invitationCode: route.query.code || ''
})

// 自定义验证规则
const validateUsername = (rule, value, callback) => {
  if (!value || value.trim() === '') {
    return callback() // 选填，不填则通过
  }
  if (value.length < 3 || value.length > 20) {
    return callback(new Error('用户名长度为3-20个字符'))
  }
  if (!/^[a-zA-Z0-9_]+$/.test(value)) {
    return callback(new Error('用户名只能包含字母、数字和下划线'))
  }
  callback()
}

const validatePassword = (rule, value, callback) => {
  if (!value) {
    return callback(new Error('请输入密码'))
  }
  if (value.length < 6 || value.length > 20) {
    return callback(new Error('密码长度为6-20个字符'))
  }
  if (!/(?=.*[a-z])(?=.*[A-Z])(?=.*\d)/.test(value)) {
    return callback(new Error('密码必须包含大小写字母和数字'))
  }
  callback()
}

const validateConfirmPassword = (rule, value, callback) => {
  if (!value) {
    return callback(new Error('请再次输入密码'))
  }
  if (value !== registerForm.password) {
    return callback(new Error('两次输入的密码不一致'))
  }
  callback()
}

const validatePhone = (rule, value, callback) => {
  if (value && !/^1[3-9]\d{9}$/.test(value)) {
    return callback(new Error('请输入正确的手机号'))
  }
  callback()
}

const validateAgree = (rule, value, callback) => {
  if (!value) {
    return callback(new Error('请阅读并同意用户协议和隐私政策'))
  }
  callback()
}

// 验证规则
const registerRules = {
  username: [{ validator: validateUsername, trigger: 'blur' }],
  password: [{ validator: validatePassword, trigger: 'blur' }],
  confirmPassword: [{ validator: validateConfirmPassword, trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  emailCode: [
    { required: true, message: '请输入邮箱验证码', trigger: 'blur' },
    { len: 6, message: '验证码为6位数字', trigger: 'blur' }
  ],
  phone: [{ validator: validatePhone, trigger: 'blur' }],
  captcha: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 4, message: '验证码长度为4位', trigger: 'blur' }
  ],
  agree: [{ validator: validateAgree, trigger: 'change' }]
}

// 状态
const loading = ref(false)
const captchaImage = ref('')
const usernameAvailable = ref(null)
const emailAvailable = ref(null)
const emailCodeSending = ref(false)
const emailCodeCountdown = ref(0)
let countdownTimer = null

// 获取验证码
const getCaptcha = async () => {
  try {
    const response = await api.get('/auth/captcha')
    if (response.data.code === 200) {
      const data = response.data.data
      captchaImage.value = data.captchaImage
      registerForm.captchaKey = data.captchaKey
    }
  } catch (error) {
    console.error('获取验证码失败:', error)
  }
}

// 刷新验证码
const refreshCaptcha = () => {
  getCaptcha()
}

// 检查用户名是否可用
const checkUsername = async () => {
  if (!registerForm.username || registerForm.username.length < 3) {
    usernameAvailable.value = null
    return
  }

  try {
    const response = await api.get('/auth/check-username', {
      params: { username: registerForm.username }
    })
    // 检查返回的data值，true表示可用
    if (response.data.code === 200) {
      usernameAvailable.value = response.data.data === true
    } else {
      usernameAvailable.value = false
    }
  } catch (error) {
    // 如果是40001错误，说明用户名已被占用
    usernameAvailable.value = false
  }
}

// 检查邮箱是否可用
const checkEmail = async () => {
  if (!registerForm.email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(registerForm.email)) {
    emailAvailable.value = null
    return
  }

  try {
    const response = await api.get('/auth/check-email', {
      params: { email: registerForm.email }
    })
    // 检查返回的data值，true表示可用
    if (response.data.code === 200) {
      emailAvailable.value = response.data.data === true
    } else {
      emailAvailable.value = false
    }
  } catch (error) {
    // 如果是40002错误，说明邮箱已被注册
    emailAvailable.value = false
  }
}

// 发送邮箱验证码
const sendEmailCode = async () => {
  if (!registerForm.email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(registerForm.email)) {
    message.warning('请先输入正确的邮箱地址')
    return
  }
  emailCodeSending.value = true
  try {
    const response = await api.post('/auth/send-email-code', { email: registerForm.email })
    if (response.data.code === 200) {
      message.success('验证码已发送到您的邮箱')
      emailCodeCountdown.value = 60
      countdownTimer = setInterval(() => {
        emailCodeCountdown.value--
        if (emailCodeCountdown.value <= 0) {
          clearInterval(countdownTimer)
          countdownTimer = null
        }
      }, 1000)
    } else {
      message.error(response.data.message || '发送失败')
    }
  } catch (error) {
    const msg = error.response?.data?.message || '发送失败，请稍后重试'
    message.error(msg)
  } finally {
    emailCodeSending.value = false
  }
}

// 处理注册
const handleRegister = async () => {
  if (!registerFormRef.value) return

  try {
    // 表单验证
    const valid = await registerFormRef.value.validate().catch(() => false)
    if (!valid) {
      message.warning('请完善表单信息')
      return
    }

    loading.value = true

    // 调用注册接口
    const response = await api.post('/auth/register', {
      username: registerForm.username || undefined,
      password: registerForm.password,
      confirmPassword: registerForm.confirmPassword,
      email: registerForm.email,
      emailCode: registerForm.emailCode,
      phone: registerForm.phone || undefined,
      nickname: registerForm.nickname || undefined,
      captcha: registerForm.captcha,
      captchaKey: registerForm.captchaKey,
      invitationCode: registerForm.invitationCode || undefined
    })

    if (response.data.code === 200) {
      message.success('注册成功！请登录')
      // 跳转到登录页面
      setTimeout(() => {
        router.push('/login')
      }, 1500)
    } else {
      message.error(response.data.message || '注册失败')
      refreshCaptcha()
    }
  } catch (error) {
    console.error('注册失败:', error)

    // 处理错误
    if (error.response?.data) {
      const errorData = error.response.data

      // 显示具体错误信息
      if (errorData.code === 40001) {
        message.error('用户名已存在，请更换')
      } else if (errorData.code === 40002) {
        message.error('邮箱已被注册，请更换')
      } else if (errorData.code === 40003) {
        message.error('手机号已被注册，请更换')
      } else if (errorData.code === 40005) {
        message.error('验证码错误或已过期，请重新输入')
      } else {
        message.error(errorData.message || '注册失败')
      }
    } else {
      message.error('网络错误，请稍后重试')
    }

    // 刷新验证码
    refreshCaptcha()
  } finally {
    loading.value = false
  }
}

// 组件挂载时获取验证码
onMounted(() => {
  getCaptcha()
})
</script>

<style scoped lang="scss">
.register-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: #FAFAFA;
  padding: 40px 20px;
}

.register-card {
  width: 100%;
  max-width: 500px;
  background: #FFFFFF;
  border-radius: 16px;
  padding: 48px 32px 40px;
  border: 1px solid #E2E8F0;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.07);
  position: relative;
}

.register-header {
  text-align: center;
  margin-bottom: 32px;

  .logo {
    font-size: 32px;
    font-weight: bold;
    margin: 0 0 8px 0;
  }

  .subtitle {
    color: #64748B;
    font-size: 14px;
    margin: 0;
  }
}

.register-form {
  :deep(.el-form-item__label) {
    font-weight: 500;
    color: #0F172A;
  }

  .validation-tip {
    display: block;
    margin-top: 4px;
    font-size: 12px;

    &.success {
      color: #10B981;
    }

    &.error {
      color: #EF4444;
    }
  }

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
      border-radius: 8px;
      border: 1px solid #E2E8F0;
      transition: all 0.3s;

      &:hover {
        opacity: 0.8;
        border-color: #10B981;
      }
    }

    .captcha-loading {
      width: 100%;
      height: 100%;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      background: #F1F5F9;
      border: 1px solid #E2E8F0;
      border-radius: 8px;
      font-size: 12px;
      color: #64748B;
      gap: 4px;
    }
  }

  .email-code-wrapper {
    display: flex;
    gap: 12px;
    width: 100%;

    .email-code-input {
      flex: 1;
    }

    .send-code-btn {
      width: 120px;
      flex-shrink: 0;
      border-radius: 10px;
    }
  }

  .register-button {
    width: 100%;
    height: 44px;
    font-size: 16px;
    font-weight: 600;
    background: linear-gradient(135deg, #10B981, #34D399);
    border: none;
    border-radius: 12px;
    box-shadow: 0 4px 14px rgba(16, 185, 129, 0.25);
    color: #fff;
    transition: all 0.3s ease;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 8px 24px rgba(16, 185, 129, 0.35);
    }
  }

  .login-link {
    text-align: center;
    color: #64748B;
    font-size: 14px;

    .link {
      color: #10B981;
      text-decoration: none;
      font-weight: 500;
      transition: color 0.2s;

      &:hover {
        color: #34D399;
        text-decoration: underline;
      }
    }
  }
}

:deep(.el-input__wrapper) {
  border-radius: 10px;
  box-shadow: 0 0 0 1px #E2E8F0 inset;
  transition: box-shadow 0.2s;
}

:deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #10B981 inset;
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #10B981 inset, 0 0 0 3px rgba(16, 185, 129, 0.1);
}
</style>
