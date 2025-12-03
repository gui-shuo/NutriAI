<template>
  <div class="member-container">
    <div class="member-layout">
      <!-- 会员信息卡片 -->
      <div class="member-header">
        <MemberInfoCard :member-info="memberInfo" :loading="loading" />
      </div>

      <!-- 主要内容区 -->
      <div class="member-content">
        <!-- 左侧：成长值图表和邀请面板 -->
        <div class="left-section">
          <!-- 成长值折线图 -->
          <GrowthChart :user-id="userId" />
          
          <!-- 邀请面板 -->
          <InvitationPanel :member-info="memberInfo" />
        </div>

        <!-- 右侧：权益列表和等级对比 -->
        <div class="right-section">
          <!-- 权益列表 -->
          <BenefitsList :benefits="currentBenefits" :level-name="memberInfo?.currentLevel?.levelName" />
          
          <!-- 等级对比表 -->
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
import { ref, onMounted, computed } from 'vue'
import { getMemberInfo } from '@/services/member'
import MemberInfoCard from '@/components/member/MemberInfoCard.vue'
import GrowthChart from '@/components/member/GrowthChart.vue'
import InvitationPanel from '@/components/member/InvitationPanel.vue'
import BenefitsList from '@/components/member/BenefitsList.vue'
import LevelComparisonTable from '@/components/member/LevelComparisonTable.vue'
import message from '@/utils/message'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const userId = computed(() => authStore.user?.id)

const loading = ref(false)
const memberInfo = ref(null)

// 当前等级权益
const currentBenefits = computed(() => {
  return memberInfo.value?.currentLevel?.benefits || {}
})

// 获取会员信息
const fetchMemberInfo = async () => {
  loading.value = true
  try {
    const response = await getMemberInfo()
    if (response.data.code === 200) {
      memberInfo.value = response.data.data
      console.log('会员信息:', memberInfo.value)
    } else {
      message.error(response.data.message || '获取会员信息失败')
    }
  } catch (error) {
    console.error('获取会员信息失败:', error)
    message.error('获取会员信息失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchMemberInfo()
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
  margin-bottom: 24px;
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
