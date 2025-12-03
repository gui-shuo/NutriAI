<template>
  <el-card class="benefits-list">
    <template #header>
      <div class="card-header">
        <span class="title">{{ levelName }} 专属权益</span>
        <el-tag type="primary">{{ benefitItems.length }}项权益</el-tag>
      </div>
    </template>

    <div class="benefits-content">
      <!-- 使用Element Plus的虚拟化列表 -->
      <el-scrollbar height="400px">
        <div class="benefit-items">
          <div
            v-for="(benefit, index) in benefitItems"
            :key="index"
            class="benefit-item"
          >
            <div class="benefit-icon" :style="{ background: benefit.color }">
              <el-icon :size="20">
                <component :is="benefit.icon" />
              </el-icon>
            </div>
            <div class="benefit-content">
              <div class="benefit-title">{{ benefit.title }}</div>
              <div class="benefit-desc">{{ benefit.description }}</div>
            </div>
            <div class="benefit-badge">
              <el-icon color="#67c23a"><CircleCheck /></el-icon>
            </div>
          </div>
        </div>
      </el-scrollbar>

      <!-- 数值权益 -->
      <div class="benefit-stats">
        <div class="stat-item">
          <span class="stat-label">AI咨询次数</span>
          <span class="stat-value">{{ aiQueriesLimit }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">饮食记录</span>
          <span class="stat-value">{{ foodRecordsLimit }}</span>
        </div>
      </div>
    </div>
  </el-card>
</template>

<script setup>
import { computed } from 'vue'
import {
  CircleCheck,
  ChatDotRound,
  DataAnalysis,
  Document,
  Star,
  Trophy,
  TrendCharts,
  Service,
  Medal,
  Present
} from '@element-plus/icons-vue'

const props = defineProps({
  benefits: {
    type: Object,
    default: () => ({})
  },
  levelName: {
    type: String,
    default: '新手会员'
  }
})

// 解析权益列表
const benefitItems = computed(() => {
  const features = props.benefits.features || []
  const icons = [ChatDotRound, DataAnalysis, Document, Star, Trophy, TrendCharts, Service, Medal, Present]
  const colors = [
    'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
    'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
    'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
    'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)',
    'linear-gradient(135deg, #fa709a 0%, #fee140 100%)',
    'linear-gradient(135deg, #30cfd0 0%, #330867 100%)',
    'linear-gradient(135deg, #a8edea 0%, #fed6e3 100%)',
    'linear-gradient(135deg, #ff9a9e 0%, #fecfef 100%)',
    'linear-gradient(135deg, #ffecd2 0%, #fcb69f 100%)'
  ]

  return features.map((feature, index) => ({
    title: feature,
    description: getBenefitDescription(feature),
    icon: icons[index % icons.length],
    color: colors[index % colors.length]
  }))
})

// AI咨询次数限制
const aiQueriesLimit = computed(() => {
  const limit = props.benefits.maxAiQueries
  return limit === -1 ? '无限制' : `${limit}次/天`
})

// 饮食记录限制
const foodRecordsLimit = computed(() => {
  const limit = props.benefits.maxFoodRecords
  return limit === -1 ? '无限制' : `${limit}条/天`
})

// 获取权益描述
const getBenefitDescription = (feature) => {
  const descriptions = {
    '基础营养记录': '记录每日饮食，追踪营养摄入',
    '营养记录无限': '不限次数记录饮食，全面追踪',
    'AI咨询(3次/天)': '每天3次智能营养咨询',
    'AI咨询(10次/天)': '每天10次智能营养咨询',
    'AI咨询(30次/天)': '每天30次智能营养咨询',
    'AI咨询无限': '无限次数AI智能咨询服务',
    '基础数据分析': '查看基础营养数据统计',
    '高级数据分析': '深度数据分析和趋势预测',
    '专属营养报告': '定期生成专业营养报告',
    '自定义目标': '设置个性化健康目标',
    '优先客服': '享受优先客服支持',
    '个性化食谱': 'AI生成个性化饮食方案',
    '健康顾问': '专属健康顾问一对一服务',
    '数据导出': '导出所有健康数据',
    '专属营养师': '真人营养师在线指导',
    '线下活动': '参与线下健康活动',
    '合作商家折扣': '享受合作商家优惠',
    '终身成长值加成': '成长值获取额外加成'
  }
  return descriptions[feature] || '专属会员权益'
}
</script>

<style scoped lang="scss">
.benefits-list {
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .title {
      font-size: 18px;
      font-weight: 600;
      color: #1f2937;
    }
  }

  .benefits-content {
    .benefit-items {
      .benefit-item {
        display: flex;
        align-items: center;
        gap: 16px;
        padding: 16px;
        border-radius: 8px;
        transition: all 0.3s;

        &:hover {
          background: #f9fafb;
          transform: translateX(4px);
        }

        &:not(:last-child) {
          border-bottom: 1px solid #f3f4f6;
        }

        .benefit-icon {
          width: 48px;
          height: 48px;
          border-radius: 12px;
          display: flex;
          align-items: center;
          justify-content: center;
          color: white;
          flex-shrink: 0;
        }

        .benefit-content {
          flex: 1;

          .benefit-title {
            font-size: 15px;
            font-weight: 600;
            color: #1f2937;
            margin-bottom: 4px;
          }

          .benefit-desc {
            font-size: 13px;
            color: #6b7280;
          }
        }

        .benefit-badge {
          flex-shrink: 0;
          font-size: 20px;
        }
      }
    }

    .benefit-stats {
      margin-top: 16px;
      padding: 16px;
      background: linear-gradient(135deg, #667eea20 0%, #764ba220 100%);
      border-radius: 8px;
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 16px;

      .stat-item {
        display: flex;
        flex-direction: column;
        gap: 8px;

        .stat-label {
          font-size: 13px;
          color: #6b7280;
        }

        .stat-value {
          font-size: 20px;
          font-weight: 600;
          color: #667eea;
        }
      }
    }
  }
}
</style>
