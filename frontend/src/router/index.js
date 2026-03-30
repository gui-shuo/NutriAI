import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/HomeView.vue'),
    meta: { title: '首页' }
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/LoginView.vue'),
    meta: { title: '登录', hideForAuth: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/auth/RegisterView.vue'),
    meta: { title: '注册', hideForAuth: true }
  },
  {
    path: '/forgot-password',
    name: 'ForgotPassword',
    component: () => import('@/views/auth/ForgotPasswordView.vue'),
    meta: { title: '忘记密码', hideForAuth: true }
  },
  {
    path: '/auth/callback/:provider',
    name: 'SocialCallback',
    component: () => import('@/views/auth/SocialCallbackView.vue'),
    meta: { title: '社交登录' }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/ProfileView.vue'),
    meta: { requiresAuth: true, title: '个人中心' }
  },
  {
    path: '/membership',
    name: 'Membership',
    component: () => import('@/views/MemberView.vue'),
    meta: { requiresAuth: true, title: '会员中心' }
  },
  {
    path: '/ai-chat',
    name: 'AIChat',
    component: () => import('@/views/AIChatView.vue'),
    meta: { requiresAuth: true, title: 'AI营养师' }
  },
  {
    path: '/food-records',
    name: 'FoodRecords',
    component: () => import('@/views/FoodRecordView.vue'),
    meta: { requiresAuth: true, title: '饮食记录' }
  },
  {
    path: '/diet-plan',
    name: 'DietPlan',
    component: () => import('@/views/DietPlanView.vue'),
    meta: { requiresAuth: true, requiresVip: true, title: 'AI饮食计划' }
  },
  {
    path: '/food-recognition',
    name: 'FoodRecognition',
    component: () => import('@/views/FoodRecognitionView.vue'),
    meta: { requiresAuth: true, title: 'AI食物识别' }
  },
  {
    path: '/consultation',
    name: 'Consultation',
    component: () => import('@/views/ConsultationView.vue'),
    meta: { requiresAuth: true, title: '营养师咨询' }
  },
  {
    path: '/product-shop',
    name: 'ProductShop',
    component: () => import('@/views/ProductShopView.vue'),
    meta: { requiresAuth: true, title: '营养产品商城' }
  },
  {
    path: '/community',
    name: 'Community',
    component: () => import('@/views/CommunityView.vue'),
    meta: { requiresAuth: true, title: '营养圈' }
  },
  {
    path: '/feedback',
    name: 'Feedback',
    component: () => import('@/views/FeedbackView.vue'),
    meta: { requiresAuth: true, title: '意见反馈' }
  },
  {
    path: '/announcements',
    name: 'Announcements',
    component: () => import('@/views/user/Announcements.vue'),
    meta: { title: '系统公告' }
  },
  {
    path: '/legal/terms',
    name: 'Terms',
    component: () => import('@/views/legal/TermsView.vue'),
    meta: { title: '用户协议' }
  },
  {
    path: '/legal/privacy',
    name: 'Privacy',
    component: () => import('@/views/legal/PrivacyView.vue'),
    meta: { title: '隐私政策' }
  },
  {
    path: '/legal/disclaimer',
    name: 'Disclaimer',
    component: () => import('@/views/legal/DisclaimerView.vue'),
    meta: { title: '免责声明' }
  },
  {
    path: '/download',
    name: 'Download',
    component: () => import('@/views/DownloadView.vue'),
    meta: { title: 'APP下载' }
  },
  {
    path: '/admin',
    component: () => import('@/views/admin/AdminLayout.vue'),
    meta: { requiresAuth: true, requiresAdmin: true },
    children: [
      {
        path: '',
        redirect: '/admin/dashboard'
      },
      {
        path: 'dashboard',
        name: 'AdminDashboard',
        component: () => import('@/views/admin/Dashboard.vue'),
        meta: { title: '数据看板' }
      },
      {
        path: 'users',
        name: 'AdminUsers',
        component: () => import('@/views/admin/UserManagement.vue'),
        meta: { title: '用户管理' }
      },
      {
        path: 'ai-logs',
        name: 'AdminAILogs',
        component: () => import('@/views/admin/AILogs.vue'),
        meta: { title: 'AI日志' }
      },
      {
        path: 'config',
        name: 'AdminConfig',
        component: () => import('@/views/admin/SystemConfig.vue'),
        meta: { title: '系统配置' }
      },
      {
        path: 'announcements',
        name: 'AdminAnnouncements',
        component: () => import('@/views/admin/AnnouncementManagement.vue'),
        meta: { title: '公告管理' }
      },
      {
        path: 'feedbacks',
        name: 'AdminFeedbacks',
        component: () => import('@/views/admin/FeedbackManagement.vue'),
        meta: { title: '反馈管理' }
      },
      {
        path: 'nutritionists',
        name: 'AdminNutritionists',
        component: () => import('@/views/admin/NutritionistManagement.vue'),
        meta: { title: '营养师管理' }
      },
      {
        path: 'products',
        name: 'AdminProducts',
        component: () => import('@/views/admin/ProductManagement.vue'),
        meta: { title: '产品管理' }
      },
      {
        path: 'community',
        name: 'AdminCommunity',
        component: () => import('@/views/admin/CommunityManagement.vue'),
        meta: { title: '社区管理' }
      },
      {
        path: 'app-versions',
        name: 'AdminAppVersions',
        component: () => import('@/views/admin/AppVersionManagement.vue'),
        meta: { title: 'APP版本管理' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/HomeView.vue'),
    meta: { title: '页面未找到' }
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    } else {
      return { top: 0 }
    }
  }
})

// 全局前置守卫
router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore()

  // 设置页面标题
  document.title = to.meta.title ? `${to.meta.title} - AI健康饮食规划助手` : 'AI健康饮食规划助手'

  // 如果已登录，不允许访问登录/注册页面
  if (to.meta.hideForAuth && authStore.isLoggedIn) {
    ElMessage.info('您已登录')
    next({ name: 'Home' })
    return
  }

  // 检查是否需要登录
  if (to.meta.requiresAuth) {
    // 有token但已过期，尝试静默刷新
    if (authStore.token && authStore.isTokenExpired && authStore.refreshToken) {
      try {
        const refreshed = await authStore.refreshAccessToken()
        if (refreshed) {
          // 刷新成功，获取最新权限
          await authStore.fetchPermissions()
          // 继续导航
          if (to.meta.requiresAdmin && !authStore.isAdmin) {
            ElMessage.error('您没有访问权限')
            next({ name: 'Home' })
            return
          }
          next()
          return
        }
      } catch (e) {
        // 刷新失败，走下面的未登录逻辑
      }
    }

    if (!authStore.isLoggedIn) {
      ElMessage.warning('请先登录')
      next({
        name: 'Login',
        query: { redirect: to.fullPath }
      })
      return
    }

    // 懒加载权限信息
    if (!authStore.permissions) {
      try {
        await authStore.fetchPermissions()
      } catch (e) {
        // 忽略，使用默认权限
      }
    }

    // 检查是否需要管理员权限
    if (to.meta.requiresAdmin && !authStore.isAdmin) {
      ElMessage.error('您没有访问权限')
      next({ name: 'Home' })
      return
    }

    // 检查是否需要VIP会员
    if (to.meta.requiresVip) {
      if (!authStore.isVip) {
        ElMessage.warning('该功能需要开通 NutriAI 营养卡')
        next({ name: 'Membership' })
        return
      }
    }
  }

  next()
})

// 全局后置钩子
router.afterEach((to, from) => {
  console.log(`路由跳转: ${from.path} -> ${to.path}`)
})

// 捕获并忽略导航错误
const originalPush = router.push
const originalReplace = router.replace

router.push = function push(location) {
  return originalPush.call(this, location).catch(err => {
    // 忽略导航相关的错误
    if (
      err.name !== 'NavigationDuplicated' &&
      !err.message.includes('Avoided redundant navigation')
    ) {
      console.error('Navigation error:', err)
      throw err
    }
    // 静默忽略重复导航
    return Promise.resolve(false)
  })
}

router.replace = function replace(location) {
  return originalReplace.call(this, location).catch(err => {
    // 忽略导航相关的错误
    if (
      err.name !== 'NavigationDuplicated' &&
      !err.message.includes('Avoided redundant navigation')
    ) {
      console.error('Navigation error:', err)
      throw err
    }
    // 静默忽略重复导航
    return Promise.resolve(false)
  })
}

// 全局错误处理
router.onError(error => {
  console.error('Router error:', error)
})

export default router
