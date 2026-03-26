<template>
  <div class="member-container">
    <div class="member-layout">
      <!-- 会员信息卡片 -->
      <div class="member-header">
        <MemberInfoCard :member-info="memberInfo" :loading="loading" />
        <!-- 每日签到按钮 -->
        <el-button
          v-if="!loading && memberInfo"
          type="warning"
          :loading="signInLoading"
          class="sign-in-btn"
          @click="handleSignIn"
        >
          <el-icon><Calendar /></el-icon>
          每日签到 +10成长值
        </el-button>
      </div>

      <!-- 主要内容区 -->
      <div class="member-content">
        <!-- 左侧：成长值图表 / 邀请面板 -->
        <div class="left-section">
          <GrowthChart :user-id="userId" />
          <InvitationPanel :member-info="memberInfo" />
        </div>

        <!-- 右侧：VIP充值 / 权益列表 / 等级对比 -->
        <div class="right-section">
          <VipPurchasePanel @vip-activated="fetchMemberInfo" />
          <BenefitsList
            :benefits="currentBenefits"
            :level-name="memberInfo?.currentLevel?.levelName"
          />
          <LevelComparisonTable
            :current-level="memberInfo?.currentLevel"
            :total-growth="memberInfo?.totalGrowth || 0"
            :next-level="memberInfo?.nextLevel"
            :growth-to-next-level="memberInfo?.growthToNextLevel || 0"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, onBeforeUnmount } from 'vue'
import { getMemberInfo, dailySignIn } from '@/services/member'
import MemberInfoCard from '@/components/member/MemberInfoCard.vue'
import GrowthChart from '@/components/member/GrowthChart.vue'
import InvitationPanel from '@/components/member/InvitationPanel.vue'
import BenefitsList from '@/components/member/BenefitsList.vue'
import LevelComparisonTable from '@/components/member/LevelComparisonTable.vue'
import VipPurchasePanel from '@/components/member/VipPurchasePanel.vue'
import message from '@/utils/message'
import { useAuthStore } from '@/stores/auth'
import { Calendar } from '@element-plus/icons-vue'

const authStore = useAuthStore()
const userId = computed(() => authStore.user?.id)

const loading = ref(false)
const memberInfo = ref(null)
const signInLoading = ref(false)

const currentBenefits = computed(() => memberInfo.value?.currentLevel?.benefits || {})

const fetchMemberInfo = async () => {
  loading.value = true
  try {
    const res = await getMemberInfo()
    if (res.data.code === 200) {
      memberInfo.value = res.data.data
    } else {
      message.error(res.data.message || '获取会员信息失败')
    }
  } catch (err) {
    console.error('获取会员信息失败:', err)
    message.error('获取会员信息失败')
  } finally {
    loading.value = false
  }
}

const handleSignIn = async () => {
  signInLoading.value = true
  try {
    const res = await dailySignIn()
    if (res.data.code === 200) {
      message.success(res.data.message || '签到成功')
      // 刷新成长值
      await fetchMemberInfo()
    } else {
      message.info(res.data.message || '今日已签到')
    }
  } catch (err) {
    console.error('签到失败:', err)
    message.error('签到失败，请稍后重试')
  } finally {
    signInLoading.value = false
  }
}

onMounted(() => {
  fetchMemberInfo()
})

onBeforeUnmount(() => {
  // 清理可能残留的 Element Plus 弹层
  setTimeout(() => {
    document.querySelectorAll('body > .el-overlay').forEach(el => el.remove())
  }, 50)
})
</script>

<style scoped lang="scss">
.member-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 40px 20px;
}

.member-layout {
  max-width: 1400px;
  margin: 0 auto;
}

.member-header {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 24px;

  > :first-child {
    flex: 1;
  }

  .sign-in-btn {
    flex-shrink: 0;
    margin-top: 8px;
    border-radius: 24px;
    padding: 10px 20px;
    font-weight: 600;
  }
}

.member-content {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;

  @media (max-width: 1024px) {
    grid-template-columns: 1fr;
  }
}

.left-section,
.right-section {
  display: flex;
  flex-direction: column;
  gap: 24px;
}
</style>
