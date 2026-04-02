<template>
  <el-card class="member-info-card" :body-style="{ padding: '0' }">
    <el-skeleton :loading="loading" animated :rows="3">
      <div v-if="memberInfo" class="card-content">
        <!-- 顶部背景 -->
        <div class="card-header" :style="{ background: levelGradient }">
          <div class="level-badge">
            <div class="badge-icon" :style="{ background: levelColor }">
              <el-icon :size="24">
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
            :stroke-width="8"
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
  border-radius: 255px 15px 225px 15px / 15px 225px 15px 255px;
  overflow: hidden;
  border: 2px solid #2d2d2d;
  box-shadow: 4px 4px 0px 0px #2d2d2d;
  background: #fdfbf7;

  .card-content {
    .card-header {
      padding: 20px 24px;
      position: relative;
      overflow: hidden;
      display: flex;
      align-items: center;
      gap: 20px;
      background: #e5e0d8;

      .level-badge {
        display: flex;
        align-items: center;
        gap: 12px;
        flex-shrink: 0;

        .badge-icon {
          width: 48px;
          height: 48px;
          border-radius: 50%;
          display: flex;
          align-items: center;
          justify-content: center;
          color: white;
          background: #2d5da1;
          box-shadow: 4px 4px 0px 0px #2d2d2d;
        }

        .badge-text {
          display: flex;
          flex-direction: column;
          gap: 2px;

          .level-name {
            font-size: 18px;
            font-weight: 600;
            color: #2d2d2d;
            font-family: 'Kalam', 'ZCOOL KuaiLe', cursive;
          }

          .member-days {
            font-size: 12px;
            color: #e5e0d8;
          }
        }
      }

      .user-info {
        margin-left: auto;
        text-align: right;

        h2 {
          font-size: 18px;
          font-weight: 600;
          color: #2d2d2d;
          margin: 0 0 2px 0;
          font-family: 'Kalam', 'ZCOOL KuaiLe', cursive;
        }

        p {
          font-size: 12px;
          color: #e5e0d8;
          margin: 0;
        }
      }
    }

    .growth-section {
      padding: 14px 24px;
      background: #fdfbf7;
      border-top: 2px dashed #2d2d2d;
      border-bottom: 2px dashed #2d2d2d;

      .growth-header {
        display: flex;
        justify-content: space-between;
        margin-bottom: 8px;

        .growth-title {
          font-size: 13px;
          font-weight: 600;
          color: #2d2d2d;
        }

        .growth-value {
          font-size: 12px;
          font-weight: 500;
          color: #e5e0d8;
        }
      }

      .growth-footer {
        display: flex;
        justify-content: space-between;
        margin-top: 8px;
        font-size: 12px;
        color: #e5e0d8;
      }
    }

    .stats-section {
      padding: 14px 24px;
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 16px;

      .stat-item {
        display: flex;
        align-items: center;
        gap: 10px;

        .stat-icon {
          width: 36px;
          height: 36px;
          border-radius: 8px;
          display: flex;
          align-items: center;
          justify-content: center;
          color: white;
          font-size: 18px;
        }

        &:nth-child(1) .stat-icon {
          background: #2d5da1;
        }
        &:nth-child(2) .stat-icon {
          background: #ff4d4d;
        }
        &:nth-child(3) .stat-icon {
          background: #2d5da1;
        }

        .stat-content {
          display: flex;
          flex-direction: column;
          gap: 1px;

          .stat-value {
            font-size: 18px;
            font-weight: 600;
            color: #2d2d2d;
            font-family: 'Kalam', 'ZCOOL KuaiLe', cursive;
          }

          .stat-label {
            font-size: 11px;
            color: #e5e0d8;
          }
        }
      }
    }
  }
}
</style>
