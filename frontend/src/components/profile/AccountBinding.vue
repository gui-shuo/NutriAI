<template>
  <div class="account-binding">
    <h2 class="section-title">🔗 账号绑定</h2>
    <p class="section-desc">
      绑定第三方账号后，可以使用微信或QQ直接登录。三种登录方式（账号密码、微信、QQ）绑定后指向同一个用户。
    </p>

    <div class="bind-list" v-loading="loading">
      <!-- 邮箱账号 -->
      <div class="bind-item">
        <div class="bind-left">
          <div class="bind-icon email-icon">📧</div>
          <div class="bind-info">
            <div class="bind-name">邮箱账号</div>
            <div class="bind-status bound">
              {{ userEmail || '未设置邮箱' }}
            </div>
          </div>
        </div>
        <el-tag type="success" v-if="userEmail">已绑定</el-tag>
        <el-tag type="info" v-else>未绑定</el-tag>
      </div>

      <!-- 微信 -->
      <div class="bind-item">
        <div class="bind-left">
          <div class="bind-icon wechat-icon">
            <svg viewBox="0 0 24 24" width="24" height="24" fill="#07c160">
              <path d="M8.691 2.188C3.891 2.188 0 5.476 0 9.53c0 2.212 1.17 4.203 3.002 5.55a.59.59 0 0 1 .213.665l-.39 1.48c-.019.07-.048.141-.048.213 0 .163.13.295.29.295a.326.326 0 0 0 .167-.054l1.903-1.114a.864.864 0 0 1 .717-.098 10.16 10.16 0 0 0 2.837.403c.276 0 .543-.027.811-.05-.857-2.578.157-4.972 1.932-6.446 1.703-1.415 3.882-1.98 5.853-1.838-.576-3.583-4.196-6.348-8.596-6.348zM5.785 5.991c.642 0 1.162.529 1.162 1.18a1.17 1.17 0 0 1-1.162 1.178A1.17 1.17 0 0 1 4.623 7.17c0-.651.52-1.18 1.162-1.18zm5.813 0c.642 0 1.162.529 1.162 1.18a1.17 1.17 0 0 1-1.162 1.178 1.17 1.17 0 0 1-1.162-1.178c0-.651.52-1.18 1.162-1.18z"/>
            </svg>
          </div>
          <div class="bind-info">
            <div class="bind-name">微信</div>
            <div :class="['bind-status', bindInfo.wechatBound ? 'bound' : 'unbound']">
              {{ bindInfo.wechatBound ? '已绑定' : '未绑定' }}
            </div>
          </div>
        </div>
        <el-button v-if="!bindInfo.wechatBound" type="success" size="small" @click="handleBind('wechat')">
          绑定
        </el-button>
        <el-popconfirm v-else title="解绑后将无法使用微信登录此账号，确定解绑？" @confirm="handleUnbind('wechat')">
          <template #reference>
            <el-button type="danger" size="small" plain>解绑</el-button>
          </template>
        </el-popconfirm>
      </div>

      <!-- QQ -->
      <div class="bind-item">
        <div class="bind-left">
          <div class="bind-icon qq-icon">
            <svg viewBox="0 0 24 24" width="24" height="24" fill="#12b7f5">
              <path d="M21.395 15.035a39.548 39.548 0 0 0-1.42-2.256c.009-.021.016-.042.025-.063.511-1.205.778-2.503.778-3.774C20.778 4.014 16.764 0 11.836 0S2.893 4.014 2.893 8.942c0 1.271.267 2.569.778 3.774.009.021.016.042.025.063a39.56 39.56 0 0 0-1.42 2.256c-1.025 1.74-1.117 2.673-.897 2.995.226.33 1.213.212 2.705-.527a11.49 11.49 0 0 0 .396 1.006c.455.987 1.016 1.736 1.6 2.233.052.044.096.082.149.124-.074.052-.138.107-.179.161-.535.695-.329 1.17.084 1.578.435.431 1.386.766 2.455.93 1.044.162 1.873.067 2.705-.1.832-.169 1.392-.373 1.905-.563.512.19 1.073.394 1.905.563.832.167 1.661.262 2.705.1 1.069-.164 2.02-.499 2.455-.93.413-.408.619-.883.084-1.578-.041-.054-.105-.109-.179-.161.053-.042.097-.08.149-.124.584-.497 1.145-1.246 1.6-2.233a11.51 11.51 0 0 0 .396-1.006c1.492.739 2.479.857 2.705.527.22-.322.128-1.255-.897-2.995z"/>
            </svg>
          </div>
          <div class="bind-info">
            <div class="bind-name">QQ</div>
            <div :class="['bind-status', bindInfo.qqBound ? 'bound' : 'unbound']">
              {{ bindInfo.qqBound ? '已绑定' : '未绑定' }}
            </div>
          </div>
        </div>
        <el-button v-if="!bindInfo.qqBound" type="primary" size="small" @click="handleBind('qq')">
          绑定
        </el-button>
        <el-popconfirm v-else title="解绑后将无法使用QQ登录此账号，确定解绑？" @confirm="handleUnbind('qq')">
          <template #reference>
            <el-button type="danger" size="small" plain>解绑</el-button>
          </template>
        </el-popconfirm>
      </div>
    </div>

    <div class="bind-tips">
      <el-alert type="info" :closable="false" show-icon>
        <template #title>
          <div>
            <p>• 绑定后三种登录方式指向同一个账号，数据互通</p>
            <p>• 如果该第三方账号已注册过独立账号，需先注销该账号再绑定</p>
            <p>• 至少需要保留一种登录方式，不能全部解绑</p>
          </div>
        </template>
      </el-alert>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { socialAuthApi } from '@/services/socialAuth'
import { ElMessage } from 'element-plus'

const authStore = useAuthStore()
const loading = ref(false)

const userEmail = computed(() => authStore.user?.email)

const bindInfo = reactive({
  wechatBound: false,
  qqBound: false
})

const loadBindInfo = async () => {
  loading.value = true
  try {
    const res = await socialAuthApi.getBindInfo()
    if (res.data?.code === 200 && res.data.data) {
      bindInfo.wechatBound = res.data.data.wechatBound
      bindInfo.qqBound = res.data.data.qqBound
    }
  } catch (e) {
    console.error('Failed to load bind info', e)
  } finally {
    loading.value = false
  }
}

const handleBind = async (provider) => {
  try {
    let response
    if (provider === 'wechat') {
      response = await socialAuthApi.getWechatAuthUrl('bind')
    } else {
      response = await socialAuthApi.getQqAuthUrl('bind')
    }
    if (response?.data?.code === 200 && response.data.data) {
      window.location.href = response.data.data
    } else {
      ElMessage.warning(response?.data?.message || '该功能暂未开通，请联系管理员配置')
    }
  } catch (e) {
    ElMessage.warning(e.response?.data?.message || '该功能暂未开通')
  }
}

const handleUnbind = async (provider) => {
  try {
    if (provider === 'wechat') {
      await socialAuthApi.unbindWechat()
    } else {
      await socialAuthApi.unbindQq()
    }
    ElMessage.success('解绑成功')
    loadBindInfo()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '解绑失败')
  }
}

onMounted(loadBindInfo)
</script>

<style scoped lang="scss">
.account-binding {
  max-width: 600px;
}

.section-title {
  font-size: 22px;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 8px;
}

.section-desc {
  color: #64748b;
  font-size: 14px;
  margin: 0 0 24px;
  line-height: 1.6;
}

.bind-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 24px;
}

.bind-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  background: #f8fafc;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  transition: all 0.2s;

  &:hover {
    border-color: #667eea;
    box-shadow: 0 2px 8px rgba(102, 126, 234, 0.1);
  }
}

.bind-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.bind-icon {
  width: 42px;
  height: 42px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}

.email-icon { background: #fef3c7; }
.wechat-icon { background: #d1fae5; }
.qq-icon { background: #dbeafe; }

.bind-name {
  font-weight: 600;
  font-size: 15px;
  color: #1e293b;
}

.bind-status {
  font-size: 13px;
  margin-top: 2px;
  
  &.bound { color: #22c55e; }
  &.unbound { color: #94a3b8; }
}

.bind-tips {
  p {
    margin: 4px 0;
    font-size: 13px;
    line-height: 1.6;
  }
}
</style>
