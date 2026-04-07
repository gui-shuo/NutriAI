<template>
  <div class="callback-container">
    <div class="callback-card">
      <div v-if="loading" class="loading-state">
        <el-icon class="is-loading" :size="48"><Loading /></el-icon>
        <p class="font-sans">{{ statusMsg }}</p>
      </div>
      <div v-else-if="error" class="error-state">
        <el-icon :size="48" color="#EF4444"><CircleCloseFilled /></el-icon>
        <p class="font-sans">{{ statusMsg }}</p>
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
    const stateStr = String(state || '')
    let response

    // H5 绑定模式：将code传回H5由H5端完成绑定（H5有自己的auth token）
    if (stateStr.startsWith('h5_bind')) {
      const bindProvider = stateStr.replace('h5_bind_', '') || provider
      statusMsg.value = '正在绑定账号...'
      const h5Base = window.location.origin + '/h5/'
      const params = new URLSearchParams({
        action: 'bind',
        code: String(code),
        provider: bindProvider
      })
      window.location.href = `${h5Base}#/pages/auth/social-callback?${params.toString()}`
      return
    }

    // APP 绑定模式：将code传回APP，由APP端完成绑定（APP有auth token）
    if (stateStr.startsWith('app_bind')) {
      const bindProvider = stateStr.replace('app_bind_', '') || provider
      statusMsg.value = '正在返回APP...'
      const params = new URLSearchParams({
        action: 'bind',
        code: String(code),
        provider: bindProvider
      })
      window.location.href = `nutriai://callback?${params.toString()}`
      return
    }

    // Web 绑定模式：用户已登录，将QQ绑定到当前账号
    if (stateStr === 'bind') {
      if (provider === 'qq') {
        statusMsg.value = '正在绑定QQ账号...'
        response = await socialAuthApi.bindQq(code)
      } else if (provider === 'wechat') {
        statusMsg.value = '正在绑定微信账号...'
        response = await socialAuthApi.bindWechat(code)
      }
      if (response.data.code === 200) {
        ElMessage.success('绑定成功！')
        if (window.opener) {
          window.opener.postMessage({ type: 'qq-bind-success' }, window.location.origin)
          window.close()
          return
        }
        router.push('/profile')
      } else {
        throw new Error(response.data.message || '绑定失败')
      }
      return
    }

    // 登录模式
    if (provider === 'wechat') {
      statusMsg.value = '正在通过微信登录...'
      response = await socialAuthApi.wechatLogin(code)
    } else if (provider === 'qq') {
      statusMsg.value = '正在通过QQ登录...'
      response = await socialAuthApi.qqLogin(code, stateStr)
    } else {
      throw new Error('未知的登录方式')
    }

    if (response.data.code === 200) {
      const loginData = response.data.data

      // APP redirect: pass token back via URL scheme
      if (stateStr.startsWith('app_')) {
        const params = new URLSearchParams({
          token: loginData.accessToken,
          refreshToken: loginData.refreshToken || ''
        })
        statusMsg.value = '登录成功，正在返回APP...'
        window.location.href = `nutriai://callback?${params.toString()}`
        return
      }

      // H5 redirect: pass token back to H5 app
      if (stateStr.startsWith('h5_')) {
        const h5Base = window.location.origin + '/h5/'
        const params = new URLSearchParams({
          token: loginData.accessToken,
          refreshToken: loginData.refreshToken || ''
        })
        window.location.href = `${h5Base}#/pages/auth/social-callback?${params.toString()}`
        return
      }

      // Web frontend: normal flow
      authStore.setToken(loginData.accessToken)
      authStore.setRefreshToken(loginData.refreshToken)
      authStore.setUser(loginData.userInfo)

      // 弹窗模式：通知父窗口并关闭
      if (window.opener) {
        window.opener.postMessage({ type: 'qq-login-success' }, window.location.origin)
        window.close()
        return
      }

      ElMessage.success('登录成功！')
      router.push('/')
    } else {
      throw new Error(response.data.message || '登录失败')
    }
  } catch (e) {
    error.value = true
    statusMsg.value = e.response?.data?.message || e.message || '登录失败'

    // 弹窗模式下绑定成功也需通知父窗口
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
  background: #FAFAFA;
}

.callback-card {
  background: #FFFFFF;
  border-radius: 16px;
  padding: 60px 40px;
  text-align: center;
  border: 1px solid #E2E8F0;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.07);
  min-width: 360px;
}

.loading-state, .error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.loading-state p, .error-state p {
  color: #0F172A;
  font-size: 16px;
}

.loading-state :deep(.el-icon) {
  color: #10B981;
}

.error-state :deep(.el-button--primary) {
  background: linear-gradient(135deg, #10B981, #34D399);
  border: none;
  border-radius: 12px;
  box-shadow: 0 4px 14px rgba(16, 185, 129, 0.25);
  font-weight: 600;
  transition: all 0.3s ease;
}

.error-state :deep(.el-button--primary:hover) {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(16, 185, 129, 0.35);
}
</style>
