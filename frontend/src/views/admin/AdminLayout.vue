<template>
  <div class="admin-layout">
    <el-container>
      <!-- 侧边栏 -->
      <el-aside width="200px" class="admin-aside">
        <div class="logo">
          <h2>管理后台</h2>
        </div>
        <el-menu :default-active="activeMenu" class="admin-menu" @select="handleMenuSelect">
          <el-menu-item index="/admin/dashboard">
            <el-icon><DataAnalysis /></el-icon>
            <span>数据看板</span>
          </el-menu-item>
          <el-menu-item index="/admin/users">
            <el-icon><User /></el-icon>
            <span>用户管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/nutritionists">
            <el-icon><UserFilled /></el-icon>
            <span>营养师管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/ai-logs">
            <el-icon><ChatDotRound /></el-icon>
            <span>AI日志</span>
          </el-menu-item>
          <el-menu-item index="/admin/feedbacks">
            <el-icon><ChatLineSquare /></el-icon>
            <span>反馈管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/announcements">
            <el-icon><Bell /></el-icon>
            <span>公告管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/community">
            <el-icon><ChatDotRound /></el-icon>
            <span>社区管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/products">
            <el-icon><ShoppingCart /></el-icon>
            <span>产品管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/app-versions">
            <el-icon><Download /></el-icon>
            <span>APP版本</span>
          </el-menu-item>
          <el-menu-item index="/admin/config">
            <el-icon><Setting /></el-icon>
            <span>系统配置</span>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <!-- 主内容区 -->
      <el-container>
        <!-- 顶部栏 -->
        <el-header class="admin-header">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }"> 首页 </el-breadcrumb-item>
            <el-breadcrumb-item>{{ breadcrumbTitle }}</el-breadcrumb-item>
          </el-breadcrumb>

          <div class="header-right">
            <AlertNotification />
            <span class="username">{{ userInfo.username }}</span>
            <el-button type="primary" link @click="handleLogout">
              <el-icon><SwitchButton /></el-icon>
              退出登录
            </el-button>
          </div>
        </el-header>

        <!-- 内容区 -->
        <el-main class="admin-main">
          <router-view />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  DataAnalysis,
  User,
  ChatDotRound,
  Setting,
  Bell,
  SwitchButton,
  ChatLineSquare,
  UserFilled,
  ShoppingCart,
  Download
} from '@element-plus/icons-vue'
import AlertNotification from '@/components/admin/AlertNotification.vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const userInfo = computed(() => ({
  username: authStore.user?.username || authStore.userName || 'Admin'
}))

const activeMenu = computed(() => route.path)

const breadcrumbTitle = computed(() => {
  const titles = {
    '/admin/dashboard': '数据看板',
    '/admin/users': '用户管理',
    '/admin/ai-logs': 'AI日志',
    '/admin/config': '系统配置',
    '/admin/announcements': '公告管理',
    '/admin/feedbacks': '反馈管理',
    '/admin/nutritionists': '营养师管理',
    '/admin/products': '产品管理',
    '/admin/community': '社区管理',
    '/admin/app-versions': 'APP版本管理'
  }
  return titles[route.path] || '管理后台'
})

const handleMenuSelect = index => {
  router.push(index)
}

const handleLogout = async () => {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '退出确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
      center: true,
      customClass: 'custom-message-box',
      showClose: true,
      closeOnClickModal: false
    })

    await authStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  } catch {
    // 用户取消
  }
}

onMounted(() => {
  // 由路由守卫保证已登录，这里仅做二次确认
  if (!authStore.token) {
    ElMessage.error('请先登录')
    router.push('/login')
  }
})
</script>

<style scoped>
.admin-layout {
  height: 100vh;
  overflow: hidden;
}

.el-container {
  height: 100%;
}

.admin-aside {
  background: #001529;
  color: #fff;
}

.logo {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #002140;
}

.logo h2 {
  color: #fff;
  margin: 0;
  font-size: 20px;
}

.admin-menu {
  border: none;
  background: #001529;
}

.admin-menu :deep(.el-menu-item) {
  color: rgba(255, 255, 255, 0.65);
}

.admin-menu :deep(.el-menu-item:hover) {
  color: #fff;
  background: rgba(255, 255, 255, 0.1);
}

.admin-menu :deep(.el-menu-item.is-active) {
  color: #fff;
  background: #1890ff;
}

.admin-header {
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.username {
  color: #666;
  font-size: 14px;
}

.admin-main {
  background: #f0f2f5;
  padding: 24px;
  overflow-y: auto;
}
</style>
