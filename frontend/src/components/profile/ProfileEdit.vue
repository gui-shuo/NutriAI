<template>
  <div class="profile-edit">
    <div class="edit-header">
      <h2 class="title">编辑资料</h2>
    </div>

    <el-form ref="formRef" :model="formData" :rules="rules" label-width="100px" class="edit-form">
      <!-- 头像上传 -->
      <el-form-item label="头像">
        <AvatarUpload v-model="formData.avatar" @uploaded="handleAvatarUploaded" />
      </el-form-item>

      <!-- 昵称 -->
      <el-form-item label="昵称" prop="nickname">
        <el-input
          v-model="formData.nickname"
          placeholder="请输入昵称"
          maxlength="20"
          show-word-limit
          clearable
        />
      </el-form-item>

      <!-- 邮箱 -->
      <el-form-item label="邮箱" prop="email">
        <el-input v-model="formData.email" placeholder="请输入邮箱" clearable>
          <template #prefix>
            <el-icon><Message /></el-icon>
          </template>
        </el-input>
      </el-form-item>

      <!-- 手机号（独立修改，需短信验证） -->
      <el-form-item label="手机号">
        <div class="phone-display">
          <span class="current-phone">{{ maskedPhone || '未绑定' }}</span>
          <el-button type="primary" link @click="showPhoneDialog = true">
            {{ currentPhone ? '更换手机号' : '绑定手机号' }}
          </el-button>
        </div>
      </el-form-item>

      <!-- 操作按钮 -->
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="handleSave">
          <el-icon v-if="!loading">
            <Select />
          </el-icon>
          保存修改
        </el-button>
        <el-button @click="handleCancel">
          <el-icon><Close /></el-icon>
          取消
        </el-button>
      </el-form-item>
    </el-form>

    <!-- 修改手机号对话框 -->
    <el-dialog v-model="showPhoneDialog" title="修改手机号" width="460px" destroy-on-close>
      <el-form
        ref="phoneFormRef"
        :model="phoneForm"
        :rules="phoneRules"
        label-width="100px"
      >
        <el-form-item label="新手机号" prop="newPhone">
          <el-input
            v-model="phoneForm.newPhone"
            placeholder="请输入新手机号"
            maxlength="11"
            clearable
          >
            <template #prefix>
              <el-icon><Phone /></el-icon>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item label="验证码" prop="smsCode">
          <div class="sms-input">
            <el-input
              v-model="phoneForm.smsCode"
              placeholder="请输入验证码"
              maxlength="6"
              clearable
            />
            <el-button
              type="primary"
              :disabled="smsCooldown > 0 || !phoneForm.newPhone"
              :loading="smsSending"
              @click="handleSendSms"
            >
              {{ smsCooldown > 0 ? `${smsCooldown}s后重试` : '发送验证码' }}
            </el-button>
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showPhoneDialog = false">取消</el-button>
        <el-button type="primary" :loading="phoneLoading" @click="handleChangePhone">
          确认修改
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { Message, Phone, Select, Close } from '@element-plus/icons-vue'
import AvatarUpload from './AvatarUpload.vue'
import api from '@/services/api'
import message from '@/utils/message'
import { useAuthStore } from '@/stores/auth'

const emit = defineEmits(['saved', 'cancel'])

const authStore = useAuthStore()
const formRef = ref()
const phoneFormRef = ref()
const loading = ref(false)

// 资料编辑表单
const formData = reactive({
  avatar: '',
  nickname: '',
  email: ''
})

// 当前手机号
const currentPhone = ref('')
const maskedPhone = computed(() => {
  const p = currentPhone.value
  if (!p || p.length < 7) return p || ''
  return p.substring(0, 3) + '****' + p.substring(7)
})

// 手机号修改相关
const showPhoneDialog = ref(false)
const phoneLoading = ref(false)
const smsSending = ref(false)
const smsCooldown = ref(0)
let cooldownTimer = null
const phoneForm = reactive({
  newPhone: '',
  smsCode: '',
  smsKey: ''
})

const rules = {
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { min: 2, max: 20, message: '昵称长度在 2 到 20 个字符', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ]
}

const phoneRules = {
  newPhone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  smsCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码为6位数字', trigger: 'blur' }
  ]
}

// 获取用户资料
const fetchProfile = async () => {
  try {
    const response = await api.get('/user/profile')
    if (response.data.code === 200) {
      const data = response.data.data
      formData.avatar = data.avatar || ''
      formData.nickname = data.nickname || ''
      formData.email = data.email || ''
      currentPhone.value = data.phone || ''
    }
  } catch (error) {
    message.error('获取用户资料失败')
  }
}

// 保存资料（不含手机号）
const handleSave = async () => {
  if (!formRef.value) return

  try {
    const valid = await formRef.value.validate()
    if (!valid) return

    loading.value = true

    const response = await api.put('/user/profile', {
      avatar: formData.avatar,
      nickname: formData.nickname,
      email: formData.email
    })

    if (response.data.code === 200) {
      message.success('资料修改成功')
      authStore.setUser({
        ...authStore.user,
        ...response.data.data
      })
      emit('saved')
    } else {
      message.error(response.data.message || '保存失败')
    }
  } catch (error) {
    const errorMessage = error.response?.data?.message || '资料修改失败，请稍后重试'
    message.error(errorMessage)
  } finally {
    loading.value = false
  }
}

// 发送短信验证码
const handleSendSms = async () => {
  // 先验证手机号
  try {
    await phoneFormRef.value?.validateField('newPhone')
  } catch {
    return
  }

  smsSending.value = true
  try {
    const response = await api.post('/user/sms/send', {
      phone: phoneForm.newPhone
    })

    if (response.data.code === 200) {
      phoneForm.smsKey = response.data.data.smsKey
      message.success('验证码已发送')
      startCooldown()
    } else {
      message.error(response.data.message || '发送失败')
    }
  } catch (error) {
    message.error(error.response?.data?.message || '验证码发送失败')
  } finally {
    smsSending.value = false
  }
}

// 倒计时
const startCooldown = () => {
  smsCooldown.value = 60
  cooldownTimer = setInterval(() => {
    smsCooldown.value--
    if (smsCooldown.value <= 0) {
      clearInterval(cooldownTimer)
      cooldownTimer = null
    }
  }, 1000)
}

// 修改手机号
const handleChangePhone = async () => {
  if (!phoneFormRef.value) return

  try {
    const valid = await phoneFormRef.value.validate()
    if (!valid) return

    if (!phoneForm.smsKey) {
      message.warning('请先获取验证码')
      return
    }

    phoneLoading.value = true

    const response = await api.put('/user/phone', {
      newPhone: phoneForm.newPhone,
      smsCode: phoneForm.smsCode,
      smsKey: phoneForm.smsKey
    })

    if (response.data.code === 200) {
      message.success('手机号修改成功')
      currentPhone.value = phoneForm.newPhone
      authStore.updateUserInfo({ phone: phoneForm.newPhone })
      showPhoneDialog.value = false
      // 重置表单
      phoneForm.newPhone = ''
      phoneForm.smsCode = ''
      phoneForm.smsKey = ''
    } else {
      message.error(response.data.message || '修改失败')
    }
  } catch (error) {
    message.error(error.response?.data?.message || '手机号修改失败')
  } finally {
    phoneLoading.value = false
  }
}

// 取消编辑
const handleCancel = () => {
  emit('cancel')
}

// 头像上传成功后立即更新store
const handleAvatarUploaded = avatarUrl => {
  authStore.setUser({
    ...authStore.user,
    avatar: avatarUrl
  })
}

onMounted(() => {
  fetchProfile()
})

onUnmounted(() => {
  if (cooldownTimer) {
    clearInterval(cooldownTimer)
  }
})
</script>

<style scoped lang="scss">
.profile-edit {
  .edit-header {
    margin-bottom: 32px;
    padding-bottom: 20px;
    border-bottom: 2px solid #f0f0f0;

    .title {
      font-size: 24px;
      font-weight: 600;
      color: #333;
      margin: 0;
    }
  }

  .edit-form {
    max-width: 600px;

    :deep(.el-form-item) {
      margin-bottom: 24px;
    }

    :deep(.el-input) {
      .el-input__inner {
        height: 44px;
      }
    }

    :deep(.el-button) {
      height: 44px;
      padding: 0 32px;
      font-size: 15px;

      .el-icon {
        margin-right: 6px;
      }
    }
  }

  .phone-display {
    display: flex;
    align-items: center;
    gap: 16px;

    .current-phone {
      font-size: 15px;
      color: #333;
      font-weight: 500;
    }
  }

  .sms-input {
    display: flex;
    gap: 12px;
    width: 100%;

    .el-input {
      flex: 1;
    }

    .el-button {
      height: 40px;
      padding: 0 16px;
      white-space: nowrap;
    }
  }
}
</style>
