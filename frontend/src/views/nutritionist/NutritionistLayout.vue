<template>
  <div class="nutritionist-layout">
    <aside class="sidebar">
      <div class="sidebar-header">
        <el-avatar :size="48" :src="profile?.avatar" class="n-avatar">
          {{ profile?.name?.charAt(0) || 'N' }}
        </el-avatar>
        <div class="profile-info">
          <h3>{{ profile?.name || '营养师' }}</h3>
          <p>{{ profile?.title }}</p>
        </div>
      </div>
      <div class="status-switch">
        <span>在线状态</span>
        <el-switch
          v-model="isOnline"
          active-text="在线"
          inactive-text="离线"
          @change="toggleStatus"
          :loading="statusLoading"
        />
      </div>
      <el-menu
        :default-active="activeMenu"
        router
        class="sidebar-menu"
      >
        <el-menu-item index="/nutritionist/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <span>工作台</span>
        </el-menu-item>
        <el-menu-item index="/nutritionist/consultations">
          <el-icon><ChatDotRound /></el-icon>
          <span>我的咨询</span>
        </el-menu-item>
      </el-menu>
      <div class="sidebar-footer">
        <el-button text @click="$router.push('/')">
          <el-icon><HomeFilled /></el-icon>
          返回首页
        </el-button>
      </div>
    </aside>
    <main class="main-content">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { DataAnalysis, ChatDotRound, HomeFilled } from '@element-plus/icons-vue'
import { getNutritionistProfile, updateNutritionistStatus } from '@/services/consultation'
import message from '@/utils/message'

const route = useRoute()
const profile = ref(null)
const statusLoading = ref(false)

const activeMenu = computed(() => route.path)
const isOnline = computed({
  get: () => profile.value?.status === 'ONLINE',
  set: () => {}
})

onMounted(async () => {
  try {
    const res = await getNutritionistProfile()
    if (res.data.code === 200) {
      profile.value = res.data.data
    }
  } catch (e) {
    console.error('获取营养师信息失败', e)
  }
})

async function toggleStatus(val) {
  statusLoading.value = true
  try {
    const status = val ? 'ONLINE' : 'OFFLINE'
    const res = await updateNutritionistStatus(status)
    if (res.data.code === 200) {
      profile.value = res.data.data
      message.success(val ? '已上线' : '已下线')
    }
  } catch (e) {
    message.error('状态更新失败')
  } finally {
    statusLoading.value = false
  }
}
</script>

<style scoped lang="scss">
.nutritionist-layout {
  display: flex;
  min-height: 100vh;
  background: #f5f7fa;
}

.sidebar {
  width: 240px;
  background: #fff;
  border-right: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;

  .sidebar-header {
    padding: 24px 20px;
    display: flex;
    align-items: center;
    gap: 12px;
    border-bottom: 1px solid #f3f4f6;

    .n-avatar {
      background: linear-gradient(135deg, #667eea, #764ba2);
      color: #fff;
      font-weight: 600;
    }

    .profile-info {
      h3 { margin: 0; font-size: 16px; font-weight: 600; }
      p { margin: 2px 0 0; font-size: 12px; color: #9ca3af; }
    }
  }

  .status-switch {
    padding: 16px 20px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    border-bottom: 1px solid #f3f4f6;
    font-size: 14px;
    color: #6b7280;
  }

  .sidebar-menu {
    border-right: none;
    flex: 1;
  }

  .sidebar-footer {
    padding: 16px 20px;
    border-top: 1px solid #f3f4f6;
  }
}

.main-content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}
</style>
