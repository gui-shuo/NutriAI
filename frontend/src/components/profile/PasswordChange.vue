<template>
  <div class="password-change">
    <div class="change-header">
      <h2 class="title">修改密码</h2>
    </div>

    <el-form ref="formRef" :model="formData" :rules="rules" label-width="120px" class="change-form">
      <!-- 原密码 -->
      <el-form-item label="原密码" prop="oldPassword">
        <el-input
          v-model="formData.oldPassword"
          type="password"
          placeholder="请输入原密码"
          show-password
          clearable
        >
          <template #prefix>
            <el-icon><Lock /></el-icon>
          </template>
        </el-input>
      </el-form-item>

      <!-- 新密码 -->
      <el-form-item label="新密码" prop="newPassword">
        <el-input
          v-model="formData.newPassword"
          type="password"
          placeholder="请输入新密码"
          show-password
          clearable
        >
          <template #prefix>
            <el-icon><Lock /></el-icon>
          </template>
        </el-input>
        <PasswordStrength :password="formData.newPassword" />
        <div class="password-hint">密码需包含大小写字母、数字，长度8-20位</div>
      </el-form-item>

      <!-- 确认密码 -->
      <el-form-item label="确认新密码" prop="confirmPassword">
        <el-input
          v-model="formData.confirmPassword"
          type="password"
          placeholder="请再次输入新密码"
          show-password
          clearable
        >
          <template #prefix>
            <el-icon><Lock /></el-icon>
          </template>
        </el-input>
      </el-form-item>

      <!-- 操作按钮 -->
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="handleSubmit">
          <el-icon v-if="!loading">
            <Select />
          </el-icon>
          确认修改
        </el-button>
        <el-button @click="handleReset">
          <el-icon><RefreshLeft /></el-icon>
          重置
        </el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { Lock, Select, RefreshLeft } from '@element-plus/icons-vue'
import PasswordStrength from '@/components/PasswordStrength.vue'
import api from '@/services/api'
import message from '@/utils/message'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref()
const loading = ref(false)

const formData = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 验证新密码
const validateNewPassword = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请输入新密码'))
  } else if (value === formData.oldPassword) {
    callback(new Error('新密码不能与原密码相同'))
  } else if (!/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)[A-Za-z\d@$!%*?&]{8,20}$/.test(value)) {
    callback(new Error('密码需包含大小写字母、数字，长度8-20位'))
  } else {
    callback()
  }
}

// 验证确认密码
const validateConfirmPassword = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请再次输入新密码'))
  } else if (value !== formData.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [{ validator: validateNewPassword, trigger: 'blur' }],
  confirmPassword: [{ validator: validateConfirmPassword, trigger: 'blur' }]
}

// 提交修改
const handleSubmit = async () => {
  if (!formRef.value) return

  try {
    const valid = await formRef.value.validate()
    if (!valid) return

    loading.value = true

    const response = await api.put('/user/password', {
      oldPassword: formData.oldPassword,
      newPassword: formData.newPassword,
      confirmPassword: formData.confirmPassword
    })

    if (response.data.code === 200) {
      message.success('密码修改成功，即将重新登录...')
      handleReset()

      // 立即清除认证状态，延迟跳转让用户看到提示
      authStore.logout().finally(() => {
        setTimeout(() => {
          router.push({ path: '/login', query: { reason: 'password_changed' } })
        }, 1000)
      })
    } else {
      message.error(response.data.message || '修改失败')
    }
  } catch (error) {
    const errorMessage = error.response?.data?.message || '密码修改失败，请稍后重试'
    message.error(errorMessage)
  } finally {
    loading.value = false
  }
}

// 重置表单
const handleReset = () => {
  formRef.value?.resetFields()
}
</script>

<style scoped lang="scss">
.password-change {
  .change-header {
    margin-bottom: 32px;
    padding-bottom: 20px;
    border-bottom: 2px dashed #2d2d2d;

    .title {
      font-size: 24px;
      font-weight: 600;
      color: #2d2d2d;
      margin: 0;
      font-family: 'Kalam', 'ZCOOL KuaiLe', cursive;
    }
  }

  .change-form {
    max-width: 600px;
    border-left: 3px solid #2d5da1;
    padding-left: 20px;

    :deep(.el-form-item) {
      margin-bottom: 24px;
    }

    :deep(.el-input) {
      .el-input__inner {
        height: 44px;
      }
    }

    .password-hint {
      font-size: 12px;
      color: #e5e0d8;
      margin-top: 4px;
      line-height: 1.5;
      font-family: 'Patrick Hand', cursive;
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
}
</style>
