<template>
  <div id="app" :class="{ dark: isDark }">
    <router-view v-slot="{ Component }">
      <transition name="fade" mode="out-in">
        <component :is="Component" :key="$route.path" />
      </transition>
    </router-view>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useThemeStore } from '@/stores/theme'

const themeStore = useThemeStore()
const isDark = computed(() => themeStore.isDark)
</script>

<style>
#app {
  min-height: 100vh;
  background-color: #fdfbf7;
  background-image: radial-gradient(#e5e0d8 1px, transparent 1px);
  background-size: 24px 24px;
  transition: background-color 0.3s ease;
  font-family: 'Patrick Hand', 'PingFang SC', 'Microsoft YaHei', cursive;
}

/* 页面切换动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
