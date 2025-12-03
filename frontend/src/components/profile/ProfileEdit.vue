<template>
  <div class="profile-edit">
    <div class="edit-header">
      <h2 class="title">编辑资料</h2>
    </div>

    <el-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-width="100px"
      class="edit-form"
    >
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
        <el-input
          v-model="formData.email"
          placeholder="请输入邮箱"
          clearable
        >
          <template #prefix>
            <el-icon><Message /></el-icon>
          </template>
        </el-input>
      </el-form-item>

      <!-- 手机号 -->
      <el-form-item label="手机号" prop="phone">
        <el-input
          v-model="formData.phone"
          placeholder="请输入手机号"
          maxlength="11"
          clearable
        >
          <template #prefix>
            <el-icon><Phone /></el-icon>
          </template>
        </el-input>
      </el-form-item>

      <!-- 操作按钮 -->
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="handleSave">
          <el-icon v-if="!loading"><Select /></el-icon>
          保存修改
        </el-button>
        <el-button @click="handleCancel">
          <el-icon><Close /></el-icon>
          取消
        </el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Message, Phone, Select, Close } from '@element-plus/icons-vue'
import AvatarUpload from './AvatarUpload.vue'
import api from '@/services/api'
import message from '@/utils/message'
import { useAuthStore } from '@/stores/auth'

const emit = defineEmits(['saved', 'cancel'])

const authStore = useAuthStore()
const formRef = ref()
const loading = ref(false)

const formData = reactive({
  avatar: '',
  nickname: '',
  email: '',
  phone: ''
})

const rules = {
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { min: 2, max: 20, message: '昵称长度在 2 到 20 个字符', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
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
      formData.phone = data.phone || ''
    }
  } catch (error) {
    console.error('获取用户资料失败:', error)
    message.error('获取用户资料失败')
  }
}

// 保存修改
const handleSave = async () => {
  if (!formRef.value) return

  try {
    const valid = await formRef.value.validate()
    if (!valid) return

    loading.value = true

    const response = await api.put('/user/profile', {
      avatar: formData.avatar,
      nickname: formData.nickname,
      email: formData.email,
      phone: formData.phone || undefined
    })

    if (response.data.code === 200) {
      message.success('资料修改成功')
      
      // 更新store中的用户信息
      authStore.setUser({
        ...authStore.user,
        ...response.data.data
      })
      
      emit('saved')
    } else {
      message.error(response.data.message || '保存失败')
    }
  } catch (error) {
    console.error('保存失败 - 完整错误对象:', error)
    console.error('错误响应:', error.response)
    console.error('错误请求:', error.request)
    console.error('错误消息:', error.message)
    
    // 提取详细的错误信息
    let errorMessage = '资料修改失败'
    
    if (error.response) {
      // 服务器返回了错误响应
      const { status, data } = error.response
      console.log('HTTP状态码:', status)
      console.log('响应数据:', data)
      
      if (data?.message) {
        // 优先使用后端返回的具体错误信息
        errorMessage = data.message
      } else if (data?.msg) {
        // 有些后端可能使用msg字段
        errorMessage = data.msg
      } else if (status === 400) {
        // 字段验证错误
        if (data?.errors && typeof data.errors === 'object') {
          const fieldErrors = []
          for (const [field, msgs] of Object.entries(data.errors)) {
            const msgArray = Array.isArray(msgs) ? msgs : [msgs]
            if (field === 'email') {
              fieldErrors.push('邮箱格式不正确')
            } else if (field === 'phone') {
              fieldErrors.push('手机号格式不正确（应为11位数字）')
            } else if (field === 'nickname') {
              fieldErrors.push('昵称' + msgArray.join('，'))
            } else {
              fieldErrors.push(msgArray.join('，'))
            }
          }
          errorMessage = fieldErrors.length > 0 ? fieldErrors.join('；') : '输入信息格式不正确'
        } else {
          errorMessage = '输入的信息格式不正确，请检查邮箱和手机号格式'
        }
      } else if (status === 401) {
        errorMessage = '登录已过期，请重新登录'
      } else if (status === 403) {
        errorMessage = '没有权限修改此信息'
      } else if (status === 409) {
        errorMessage = data?.message || '邮箱或手机号已被其他用户使用'
      } else if (status === 500) {
        errorMessage = '服务器错误：' + (data?.message || '请稍后重试')
      } else {
        errorMessage = `请求失败 (错误代码: ${status})`
      }
    } else if (error.request) {
      // 请求已发送但没有收到响应（网络问题）
      console.error('网络请求失败，没有收到响应')
      errorMessage = '网络连接失败，请检查网络后重试'
    } else {
      // 请求配置出错或其他错误
      console.error('请求配置错误或其他异常')
      if (error.message) {
        errorMessage = '操作失败：' + error.message
      } else {
        errorMessage = '操作失败，请重试'
      }
    }
    
    console.log('最终错误提示:', errorMessage)
    message.error(errorMessage)
  } finally {
    loading.value = false
  }
}

// 取消编辑
const handleCancel = () => {
  emit('cancel')
}

// 头像上传成功后立即更新store
const handleAvatarUploaded = (avatarUrl) => {
  // 立即更新store中的用户信息
  authStore.setUser({
    ...authStore.user,
    avatar: avatarUrl
  })
}

onMounted(() => {
  fetchProfile()
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
}
</style>
