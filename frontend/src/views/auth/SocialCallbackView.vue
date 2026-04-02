<template>
  <div class="callback-container">
    <div class="callback-card">
      <div v-if="loading" class="loading-state">
        <el-icon class="is-loading" :size="48"><Loading /></el-icon>
        <p class="font-hand">{{ statusMsg }}</p>
      </div>
      <div v-else-if="error" class="error-state">
        <el-icon :size="48" color="#ff4d4d"><CircleCloseFilled /></el-icon>
        <p class="font-hand">{{ statusMsg }}</p>
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

    // 绑定模式：用户已登录，将QQ绑定到当前账号
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
      response = await socialAuthApi.qqLogin(code)
    } else {
      throw new Error('未知的登录方式')
    }

    if (response.data.code === 200) {
      const loginData = response.data.data

      // H5/APP redirect: pass token back to H5 app
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

      ElMessage.success('登录成功！')
      router.push('/')
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
  background-color: #fdfbf7;
  background-image: radial-gradient(#e5e0d8 1px, transparent 1px);
  background-size: 24px 24px;
}

.callback-card {
  background: #fff;
  border-radius: 255px 15px 225px 15px / 15px 225px 15px 255px;
  padding: 60px 40px;
  text-align: center;
  border: 3px solid #2d2d2d;
  box-shadow: 8px 8px 0px 0px #2d2d2d;
  min-width: 360px;
}

.loading-state, .error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.loading-state p, .error-state p {
  color: #2d2d2d;
  font-size: 16px;
  font-family: 'Patrick Hand', cursive;
}
</style>
