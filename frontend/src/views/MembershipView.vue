<template>
  <div class="membership-container">
    <el-button
      :icon="ArrowLeft"
      text
      style="margin-bottom: 16px"
      @click="goToHome"
    >
      返回首页
    </el-button>
    <h1>会员中心</h1>
    <p>会员功能开发中...</p>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'

// 路由
const router = useRouter()

// 返回首页
const goToHome = () => {
  router.push('/')
}
</script>

<style scoped>
.membership-container {
  padding: 20px;
}
</style>
