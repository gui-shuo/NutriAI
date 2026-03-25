<template>
  <el-card class="member-info-card" :body-style="{ padding: '0' }">
    <el-skeleton :loading="loading" animated :rows="3">
      <div v-if="memberInfo" class="card-content">
        <!-- 顶部背景 -->
        <div class="card-header" :style="{ background: levelGradient }">
          <div class="level-badge">
            <div class="badge-icon" :style="{ background: levelColor }">
              <el-icon :size="32">
                <Trophy />
              </el-icon>
            </div>
            <div class="badge-text">
              <span class="level-name">{{ memberInfo.currentLevel?.levelName }}</span>
              <span class="member-days">会员{{ memberInfo.memberDays }}天</span>
            </div>
          </div>
          <div class="user-info">
            <h2>{{ memberInfo.username }}</h2>
            <p>会员ID: {{ memberInfo.memberId }}</p>
          </div>
        </div>

        <!-- 成长值进度 -->
        <div class="growth-section">
          <div class="growth-header">
            <span class="growth-title">成长值进度</span>
            <span class="growth-value">{{ memberInfo.currentGrowth }} / {{ nextLevelGrowth }}</span>
          </div>
          <el-progress
            :percentage="memberInfo.upgradeProgress"
            :stroke-width="12"
            :color="progressColors"
            striped
            striped-flow
          />
          <div class="growth-footer">
            <span>总成长值: {{ memberInfo.totalGrowth }}</span>
            <span v-if="memberInfo.nextLevel"
              >距离{{ memberInfo.nextLevel.levelName }}还需{{
                memberInfo.growthToNextLevel
              }}成长值</span
            >
            <span v-else>已达到最高等级</span>
          </div>
        </div>

        <!-- 统计信息 -->
        <div class="stats-section">
          <div class="stat-item">
            <div
              class="stat-icon"
              style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%)"
            >
              <el-icon><User /></el-icon>
            </div>
            <div class="stat-content">
              <span class="stat-value">{{ memberInfo.invitationCount }}</span>
              <span class="stat-label">邀请好友</span>
            </div>
          </div>
          <div class="stat-item">
            <div
              class="stat-icon"
              style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%)"
            >
              <el-icon><TrendCharts /></el-icon>
            </div>
            <div class="stat-content">
              <span class="stat-value">{{ memberInfo.currentLevel?.levelOrder }}</span>
              <span class="stat-label">当前等级</span>
            </div>
          </div>
          <div class="stat-item">
            <div
              class="stat-icon"
              style="background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)"
            >
              <el-icon><Calendar /></el-icon>
            </div>
            <div class="stat-content">
              <span class="stat-value">{{ memberInfo.memberDays }}</span>
              <span class="stat-label">会员天数</span>
            </div>
          </div>
        </div>
      </div>
    </el-skeleton>
  </el-card>
</template>

<script setup>
import { computed } from 'vue'
import { Trophy, User, TrendCharts, Calendar } from '@element-plus/icons-vue'

const props = defineProps({
  memberInfo: {
    type: Object,
    default: null
  },
  loading: {
    type: Boolean,
    default: false
  }
})

// 等级颜色
const levelColor = computed(() => {
  return props.memberInfo?.currentLevel?.color || '#95a5a6'
})

// 等级渐变背景
const levelGradient = computed(() => {
  const color = levelColor.value
  return `linear-gradient(135deg, ${color}20 0%, ${color}40 100%)`
})

// 下一等级所需成长值
const nextLevelGrowth = computed(() => {
  if (!props.memberInfo?.nextLevel) return 0
  return props.memberInfo.nextLevel.growthRequired - props.memberInfo.currentLevel.growthRequired
})

// 进度条颜色
const progressColors = [
  { color: '#f56c6c', percentage: 20 },
  { color: '#e6a23c', percentage: 40 },
  { color: '#5cb87a', percentage: 60 },
  { color: '#1989fa', percentage: 80 },
  { color: '#6f7ad3', percentage: 100 }
]
</script>

<style scoped lang="scss">
.member-info-card {
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);

  .card-content {
    .card-header {
      padding: 32px;
      position: relative;
      overflow: hidden;

      &::before {
        content: '';
        position: absolute;
        top: -50%;
        right: -20%;
        width: 300px;
        height: 300px;
        border-radius: 50%;
        background: rgba(255, 255, 255, 0.1);
      }

      .level-badge {
        display: flex;
        align-items: center;
        gap: 16px;
        margin-bottom: 24px;

        .badge-icon {
          width: 64px;
          height: 64px;
          border-radius: 50%;
          display: flex;
          align-items: center;
          justify-content: center;
          color: white;
          box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
        }

        .badge-text {
          display: flex;
          flex-direction: column;
          gap: 4px;

          .level-name {
            font-size: 24px;
            font-weight: 600;
            color: #1f2937;
          }

          .member-days {
            font-size: 14px;
            color: #6b7280;
          }
        }
      }

      .user-info {
        h2 {
          font-size: 28px;
          font-weight: 600;
          color: #1f2937;
          margin: 0 0 8px 0;
        }

        p {
          font-size: 14px;
          color: #6b7280;
          margin: 0;
        }
      }
    }

    .growth-section {
      padding: 24px 32px;
      background: #f9fafb;
      border-top: 1px solid #e5e7eb;
      border-bottom: 1px solid #e5e7eb;

      .growth-header {
        display: flex;
        justify-content: space-between;
        margin-bottom: 12px;

        .growth-title {
          font-size: 16px;
          font-weight: 600;
          color: #1f2937;
        }

        .growth-value {
          font-size: 14px;
          font-weight: 500;
          color: #6b7280;
        }
      }

      .growth-footer {
        display: flex;
        justify-content: space-between;
        margin-top: 12px;
        font-size: 13px;
        color: #6b7280;
      }
    }

    .stats-section {
      padding: 24px 32px;
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 24px;

      .stat-item {
        display: flex;
        align-items: center;
        gap: 16px;

        .stat-icon {
          width: 48px;
          height: 48px;
          border-radius: 12px;
          display: flex;
          align-items: center;
          justify-content: center;
          color: white;
          font-size: 24px;
        }

        .stat-content {
          display: flex;
          flex-direction: column;
          gap: 4px;

          .stat-value {
            font-size: 24px;
            font-weight: 600;
            color: #1f2937;
          }

          .stat-label {
            font-size: 13px;
            color: #6b7280;
          }
        }
      }
    }
  }
}
</style>
