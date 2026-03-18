<template>
  <div
    id="app"
    :class="{ dark: isDark }"
  >
    <router-view v-slot="{ Component }">
      <transition 
        name="fade" 
        mode="out-in" 
        @before-enter="onBeforeEnter"
        @after-leave="onTransitionEnd"
      >
        <component
          :is="Component"
          :key="$route.fullPath"
        />
      </transition>
    </router-view>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useThemeStore } from '@/stores/theme'

const router = useRouter()
const themeStore = useThemeStore()
const isDark = computed(() => themeStore.isDark)

// 过渡开始前清理
const onBeforeEnter = () => {
  // 立即清理遮罩层
  removeInvalidOverlays()
}

// 过渡结束回调，确保动画完成
const onTransitionEnd = () => {
  // 清理可能残留的遮罩层
  cleanupOverlays()
}

// 清理遮罩层的函数
const cleanupOverlays = () => {
  // 立即清理一次
  removeInvalidOverlays()
  
  // 延迟再清理一次，确保彻底
  setTimeout(() => {
    removeInvalidOverlays()
  }, 100)
  
  // 再延迟清理一次（针对慢速动画）
  setTimeout(() => {
    removeInvalidOverlays()
  }, 500)
}

const removeInvalidOverlays = () => {
  // 检查是否有活动的弹窗组件（包括 el-dialog）
  const hasMessageBox = document.querySelector('.el-message-box__wrapper')
  const hasDialog = document.querySelector('.el-dialog')
  const hasDrawer = document.querySelector('.el-drawer')
  
  // 如果有任何活动的弹窗，不要清理遮罩层
  if (hasMessageBox || hasDialog || hasDrawer) {
    console.log('🔒 检测到活动弹窗，跳过遮罩层清理')
    return
  }
  
  // 只清理真正孤立的遮罩层（没有内容的）
  const bodyOverlays = document.querySelectorAll('body > .el-overlay')
  
  bodyOverlays.forEach(overlay => {
    // 检查遮罩层内是否有内容
    const hasContent = overlay.querySelector('.el-dialog, .el-message-box, .el-drawer')
    if (!hasContent && overlay.children.length === 0) {
      console.log('🧹 清理空的遮罩层')
      overlay.remove()
    }
  })
}

// 路由切换后清理遮罩层（更保守的策略）
onMounted(() => {
  // 只在路由切换完成后清理真正孤立的遮罩层
  // 不再使用激进的点击和鼠标移动监听
})
</script>

<style>
#app {
  min-height: 100vh;
  background-color: var(--el-bg-color);
  transition: background-color 0.3s ease;
}

/* 页面切换动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 防止过渡动画卡住导致的遮罩层问题 */
.fade-leave-active {
  position: absolute;
  width: 100%;
  z-index: -1 !important;
  pointer-events: none !important;
}

.fade-enter-active {
  z-index: 1 !important;
}

/* 遮罩层样式 - 不再激进隐藏，让 Element Plus 正常管理 */
/* 只隐藏真正孤立的遮罩层（没有对话框内容的） */
body > .el-overlay:empty,
body > .el-loading-mask.is-orphan {
  display: none !important;
  pointer-events: none !important;
}

/* 确保所有交互元素可以点击 */
button, a, input, textarea, select, 
.el-button, .el-input, .el-select,
.el-card, .el-form, .el-table {
  pointer-events: auto !important;
  position: relative !important;
  z-index: 1 !important;
}
</style>
