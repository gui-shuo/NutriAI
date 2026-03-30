<template>
  <div class="callback-container">
    <div class="callback-card">
      <div v-if="loading" class="loading-state">
        <el-icon class="is-loading" :size="48"><Loading /></el-icon>
        <p>{{ statusMsg }}</p>
      </div>
      <div v-else-if="error" class="error-state">
        <el-icon :size="48" color="#f56c6c"><CircleCloseFilled /></el-icon>
        <p>{{ statusMsg }}</p>
        <el-button type="primary" @click="goLogin">返回登录</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Loading, CircleCloseFilled } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { socialAuthApi } from '@/services/socialAuth'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const loading = ref(true)
const error = ref(false)
const statusMsg = ref('正在处理登录...')

onMounted(async () => {
  const { code, state } = route.query
  const provider = route.params.provider

  if (!code) {
    error.value = true
    statusMsg.value = '授权失败：未获取到授权码'
    loading.value = false
    return
  }

  try {
    let response
    if (provider === 'wechat') {
      statusMsg.value = '正在通过微信登录...'
      response = await socialAuthApi.wechatLogin(code)
    } else if (provider === 'qq') {
      statusMsg.value = '正在通过QQ登录...'
      response = await socialAuthApi.qqLogin(code)
    } else {
      throw new Error('未知的登录方式')
    }

    if (response.data.code === 200) {
      const loginData = response.data.data
      authStore.setToken(loginData.accessToken)
      authStore.setRefreshToken(loginData.refreshToken)
      authStore.setUser(loginData.userInfo)

      ElMessage.success('登录成功！')

      if (state === 'bind') {
        router.push('/profile')
      } else {
        router.push('/')
      }
    } else {
      throw new Error(response.data.message || '登录失败')
    }
  } catch (e) {
    error.value = true
    statusMsg.value = e.response?.data?.message || e.message || '登录失败'
  } finally {
    loading.value = false
  }
})

const goLogin = () => router.push('/login')
</script>

<style scoped>
.callback-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.callback-card {
  background: white;
  border-radius: 16px;
  padding: 60px 40px;
  text-align: center;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  min-width: 360px;
}

.loading-state, .error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.loading-state p, .error-state p {
  color: #666;
  font-size: 16px;
}
</style>
