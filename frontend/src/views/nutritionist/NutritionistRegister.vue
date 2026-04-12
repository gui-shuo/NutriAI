<template>
  <div class="nut-register-container">
    <div class="nut-register-card">
      <div class="nut-register-header">
        <div class="logo-icon">🩺</div>
        <h1>营养师入驻申请</h1>
        <p class="subtitle">加入NutriAI专业营养师团队，为用户提供专业饮食规划服务</p>
      </div>

      <el-steps :active="currentStep" finish-status="success" align-center class="register-steps">
        <el-step title="账号信息" />
        <el-step title="专业资质" />
        <el-step title="提交审核" />
      </el-steps>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="nut-form">
        <!-- Step 1: 账号信息 -->
        <div v-show="currentStep === 0">
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="form.email" placeholder="请输入邮箱" size="large" prefix-icon="Message" @blur="checkEmail" />
            <span v-if="emailAvailable === true" class="tip success">✓ 邮箱可用</span>
            <span v-else-if="emailAvailable === false" class="tip error">✗ 邮箱已被注册</span>
          </el-form-item>

          <el-form-item label="邮箱验证码" prop="emailCode">
            <div class="code-row">
              <el-input v-model="form.emailCode" placeholder="6位数字验证码" size="large" maxlength="6" />
              <el-button size="large" :disabled="emailCooldown > 0" @click="sendEmailCode">
                {{ emailCooldown > 0 ? `${emailCooldown}s` : '发送验证码' }}
              </el-button>
            </div>
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input v-model="form.password" type="password" placeholder="6-20个字符，含大小写字母和数字" size="large" prefix-icon="Lock" show-password />
          </el-form-item>

          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input v-model="form.confirmPassword" type="password" placeholder="请再次输入密码" size="large" prefix-icon="Lock" show-password />
          </el-form-item>

          <el-form-item label="用户名（选填）" prop="username">
            <el-input v-model="form.username" placeholder="不填则自动生成" size="large" prefix-icon="User" />
          </el-form-item>

          <el-form-item label="验证码" prop="captcha">
            <div class="code-row">
              <el-input v-model="form.captcha" placeholder="请输入验证码" size="large" maxlength="4" />
              <img v-if="captchaImage" :src="captchaImage" class="captcha-img" @click="refreshCaptcha" title="点击刷新" />
            </div>
          </el-form-item>

          <el-button type="primary" size="large" class="full-btn" @click="nextStep">下一步</el-button>
        </div>

        <!-- Step 2: 专业信息 -->
        <div v-show="currentStep === 1">
          <el-form-item label="姓名" prop="name">
            <el-input v-model="form.name" placeholder="请输入真实姓名" size="large" />
          </el-form-item>

          <el-form-item label="职称" prop="title">
            <el-select v-model="form.title" placeholder="请选择职称" size="large" style="width: 100%">
              <el-option label="初级营养师" value="初级营养师" />
              <el-option label="中级营养师" value="中级营养师" />
              <el-option label="高级营养师" value="高级营养师" />
              <el-option label="注册营养师" value="注册营养师" />
            </el-select>
          </el-form-item>

          <el-form-item label="专业领域">
            <el-select v-model="form.specialties" multiple placeholder="请选择专业领域" size="large" style="width: 100%">
              <el-option label="体重管理" value="体重管理" />
              <el-option label="糖尿病饮食" value="糖尿病饮食" />
              <el-option label="心血管饮食" value="心血管饮食" />
              <el-option label="运动营养" value="运动营养" />
              <el-option label="孕期营养" value="孕期营养" />
              <el-option label="儿童营养" value="儿童营养" />
              <el-option label="老年营养" value="老年营养" />
              <el-option label="肠胃调理" value="肠胃调理" />
            </el-select>
          </el-form-item>

          <el-form-item label="从业年限">
            <el-input-number v-model="form.experienceYears" :min="0" :max="50" size="large" style="width: 100%" />
          </el-form-item>

          <el-form-item label="咨询费（元/次）" prop="consultationFee">
            <el-input-number v-model="form.consultationFee" :min="0" :precision="2" size="large" style="width: 100%" />
          </el-form-item>

          <el-form-item label="个人简介">
            <el-input v-model="form.introduction" type="textarea" :rows="3" placeholder="请简要介绍您的专业背景和服务特色" maxlength="500" show-word-limit />
          </el-form-item>

          <el-form-item label="工作时间">
            <el-input v-model="form.workingHours" placeholder="如：周一至周五 9:00-18:00" size="large" />
          </el-form-item>

          <el-form-item label="资质证书">
            <el-upload
              action="#"
              :auto-upload="false"
              :on-change="handleCertUpload"
              :on-remove="handleCertRemove"
              :file-list="certFiles"
              list-type="picture-card"
              accept="image/*"
              :limit="5"
            >
              <el-icon><Plus /></el-icon>
              <template #tip>
                <div class="upload-tip">支持jpg/png，最多5张，每张不超过5MB</div>
              </template>
            </el-upload>
          </el-form-item>

          <div class="step-btns">
            <el-button size="large" @click="currentStep = 0">上一步</el-button>
            <el-button type="primary" size="large" @click="nextStep">下一步</el-button>
          </div>
        </div>

        <!-- Step 3: 确认提交 -->
        <div v-show="currentStep === 2">
          <div class="confirm-section">
            <h3>📋 申请信息确认</h3>
            <el-descriptions :column="1" border>
              <el-descriptions-item label="邮箱">{{ form.email }}</el-descriptions-item>
              <el-descriptions-item label="姓名">{{ form.name }}</el-descriptions-item>
              <el-descriptions-item label="职称">{{ form.title }}</el-descriptions-item>
              <el-descriptions-item label="专业领域">{{ (form.specialties || []).join('、') || '未填写' }}</el-descriptions-item>
              <el-descriptions-item label="从业年限">{{ form.experienceYears || 0 }}年</el-descriptions-item>
              <el-descriptions-item label="咨询费">¥{{ form.consultationFee || 0 }}/次</el-descriptions-item>
            </el-descriptions>

            <div class="notice-box">
              <el-icon><InfoFilled /></el-icon>
              <p>提交后，管理员将在1-3个工作日内完成审核。审核通过后，您将以营养师身份登录系统，开始接受用户咨询。</p>
            </div>
          </div>

          <div class="step-btns">
            <el-button size="large" @click="currentStep = 1">上一步</el-button>
            <el-button type="primary" size="large" :loading="loading" @click="handleSubmit">提交申请</el-button>
          </div>
        </div>
      </el-form>

      <div class="login-link">
        已有营养师账号？<router-link to="/nutritionist/login">立即登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, InfoFilled } from '@element-plus/icons-vue'
import api from '@/services/api'
import message from '@/utils/message'

const router = useRouter()
const formRef = ref()
const currentStep = ref(0)
const loading = ref(false)
const emailAvailable = ref(null)
const emailCooldown = ref(0)
const captchaImage = ref('')
const certFiles = ref([])

const form = reactive({
  email: '',
  emailCode: '',
  password: '',
  confirmPassword: '',
  username: '',
  captcha: '',
  captchaKey: '',
  name: '',
  title: '',
  specialties: [],
  experienceYears: 0,
  consultationFee: 50,
  introduction: '',
  workingHours: '',
  certificateUrls: []
})

const rules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  emailCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { pattern: /^\d{6}$/, message: '验证码为6位数字', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为6-20个字符', trigger: 'blur' },
    { pattern: /(?=.*[a-z])(?=.*[A-Z])(?=.*\d)/, message: '需包含大小写字母和数字', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: (r, v, cb) => v === form.password ? cb() : cb(new Error('两次密码不一致')), trigger: 'blur' }
  ],
  captcha: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  title: [{ required: true, message: '请选择职称', trigger: 'change' }],
  consultationFee: [{ required: true, message: '请填写咨询费', trigger: 'blur' }]
}

const refreshCaptcha = async () => {
  try {
    const res = await api.get('/auth/captcha')
    if (res.data.code === 200) {
      captchaImage.value = res.data.data.captchaImage
      form.captchaKey = res.data.data.captchaKey
    }
  } catch (e) { /* ignore */ }
}
refreshCaptcha()

const checkEmail = async () => {
  if (!form.email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
    emailAvailable.value = null
    return
  }
  try {
    const res = await api.get('/auth/check-email', { params: { email: form.email } })
    emailAvailable.value = res.data.code === 200 && res.data.data === true
  } catch { emailAvailable.value = false }
}

const sendEmailCode = async () => {
  if (!form.email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
    message.warning('请先输入正确的邮箱')
    return
  }
  try {
    await api.post('/auth/send-email-code', { email: form.email })
    message.success('验证码已发送')
    emailCooldown.value = 60
    const timer = setInterval(() => {
      emailCooldown.value--
      if (emailCooldown.value <= 0) clearInterval(timer)
    }, 1000)
  } catch (e) {
    message.error(e.response?.data?.message || '发送失败')
  }
}

const handleCertUpload = async (uploadFile) => {
  const file = uploadFile.raw
  if (!file) return

  // Validate file size
  if (file.size > 5 * 1024 * 1024) {
    message.error('图片大小不能超过5MB')
    certFiles.value = certFiles.value.filter(f => f.uid !== uploadFile.uid)
    return
  }

  // Validate file type
  if (!['image/jpeg', 'image/png', 'image/jpg', 'image/gif'].includes(file.type)) {
    message.error('仅支持jpg/png/gif格式')
    certFiles.value = certFiles.value.filter(f => f.uid !== uploadFile.uid)
    return
  }

  try {
    const formData = new FormData()
    formData.append('file', file)
    const res = await api.post('/auth/upload-certificate', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 60000
    })
    if (res.data.code === 200 && res.data.data) {
      form.certificateUrls.push(res.data.data)
      message.success('证书图片上传成功')
    } else {
      message.error(res.data.message || '上传失败')
      certFiles.value = certFiles.value.filter(f => f.uid !== uploadFile.uid)
    }
  } catch (e) {
    message.error('上传失败，请重试')
    certFiles.value = certFiles.value.filter(f => f.uid !== uploadFile.uid)
  }
}

const handleCertRemove = (uploadFile) => {
  const index = certFiles.value.findIndex(f => f.uid === uploadFile.uid)
  if (index !== -1) {
    certFiles.value.splice(index, 1)
    form.certificateUrls.splice(index, 1)
  }
}

const nextStep = async () => {
  if (currentStep.value === 0) {
    // 验证步骤1字段
    try {
      await formRef.value.validateField(['email', 'emailCode', 'password', 'confirmPassword', 'captcha'])
      if (emailAvailable.value === false) {
        message.error('该邮箱已被注册')
        return
      }
      currentStep.value = 1
    } catch { message.warning('请完善账号信息') }
  } else if (currentStep.value === 1) {
    try {
      await formRef.value.validateField(['name', 'title', 'consultationFee'])
      currentStep.value = 2
    } catch { message.warning('请完善专业信息') }
  }
}

const handleSubmit = async () => {
  loading.value = true
  try {
    const res = await api.post('/auth/nutritionist/register', {
      email: form.email,
      emailCode: form.emailCode,
      password: form.password,
      confirmPassword: form.confirmPassword,
      username: form.username || undefined,
      captcha: form.captcha,
      captchaKey: form.captchaKey,
      name: form.name,
      title: form.title,
      specialties: form.specialties,
      experienceYears: form.experienceYears,
      consultationFee: form.consultationFee,
      introduction: form.introduction,
      workingHours: form.workingHours,
      certificateUrls: form.certificateUrls
    })
    if (res.data.code === 200) {
      message.success('申请已提交，请等待管理员审核')
      setTimeout(() => router.push('/login'), 2000)
    } else {
      message.error(res.data.message || '提交失败')
      refreshCaptcha()
    }
  } catch (e) {
    message.error(e.response?.data?.message || '提交失败')
    refreshCaptcha()
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.nut-register-container {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  padding: 40px 20px;
  background: #E2E8F0;
}

.nut-register-card {
  width: 100%;
  max-width: 580px;
  background: #FAFAFA;
  border-radius: 16px;
  padding: 40px 36px;
  box-shadow: 0 4px 14px rgba(16, 185, 129, 0.25);
  border: 1px solid #E2E8F0;
}

.nut-register-header {
  text-align: center;
  margin-bottom: 24px;
  .logo-icon { font-size: 48px; }
  h1 { font-size: 24px; color: #0F172A; margin: 8px 0; font-family: 'Calistoga', serif; }
  .subtitle { color: #0F172A; font-size: 14px; margin: 0; }
}

.register-steps { margin-bottom: 32px; }

.code-row {
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

.full-btn {
  width: 100%; height: 44px; font-size: 16px;
  background: #10B981;
  border: 1px solid #E2E8F0;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  transition: all 0.15s ease;
  &:hover {
    box-shadow: 0 4px 14px rgba(16, 185, 129, 0.25);
    transform: translateY(-1px);
  }
}
.step-btns { display: flex; gap: 12px; margin-top: 8px;
  .el-button {
    flex: 1; height: 44px;
    border: 1px solid #E2E8F0;
    border-radius: 12px;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
    transition: all 0.15s ease;
    &:hover {
      box-shadow: 0 4px 14px rgba(16, 185, 129, 0.25);
      transform: translateY(-1px);
    }
  }
}

.tip {
  font-size: 12px; margin-top: 4px; display: block;
  &.success { color: #10B981; }
  &.error { color: #EF4444; }
}

.confirm-section {
  h3 { color: #0F172A; margin-bottom: 16px; font-family: 'Calistoga', serif; }
  .notice-box {
    display: flex; gap: 8px; align-items: flex-start;
    background: #F1F5F9; border: 1px solid #E2E8F0; border-radius: 12px;
    padding: 12px 16px; margin-top: 16px;
    .el-icon { color: #10B981; font-size: 20px; margin-top: 2px; }
    p { margin: 0; color: #0F172A; font-size: 13px; line-height: 1.6; }
  }
}

.upload-tip { font-size: 12px; color: #0F172A; margin-top: 8px; }

.login-link {
  text-align: center; margin-top: 20px; font-size: 14px; color: #0F172A;
  a { color: #10B981; text-decoration: none; font-weight: 500;
    &:hover { text-decoration: underline; }
  }
}

:deep(.el-form-item__label) { font-weight: 500; font-family: 'Calistoga', serif; color: #0F172A; }
:deep(.el-input__wrapper) {
  border-radius: 12px;
  border: 1px solid #E2E8F0 !important;
  box-shadow: none !important;
  background: #FAFAFA;
}
:deep(.el-textarea__inner) {
  border-radius: 12px;
  border: 1px solid #E2E8F0 !important;
  box-shadow: none !important;
  background: #FAFAFA;
}

:deep(.el-steps) {
  .el-step__head.is-finish { color: #10B981; border-color: #10B981; }
  .el-step__title.is-finish { color: #10B981; }
  .el-step__head.is-process { color: #10B981; border-color: #10B981; }
  .el-step__title.is-process { color: #10B981; }
}

:deep(.el-upload--picture-card) {
  border: 1px solid #E2E8F0;
  border-radius: 12px;
  background: #FAFAFA;
}
</style>
