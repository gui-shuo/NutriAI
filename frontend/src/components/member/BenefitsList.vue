<template>
  <el-card class="benefits-list">
    <template #header>
      <div class="card-header">
        <span class="title">{{ levelName }} 专属权益</span>
        <el-tag type="primary"> {{ benefitItems.length }}项权益 </el-tag>
      </div>
    </template>

    <div class="benefits-content">
      <!-- 使用Element Plus的虚拟化列表 -->
      <el-scrollbar height="400px">
        <div class="benefit-items">
          <div v-for="(benefit, index) in benefitItems" :key="index" class="benefit-item">
            <div class="benefit-icon" :style="{ background: benefit.color }">
              <el-icon :size="20">
                <component :is="benefit.icon" />
              </el-icon>
            </div>
            <div class="benefit-content">
              <div class="benefit-title">
                {{ benefit.title }}
              </div>
              <div class="benefit-desc">
                {{ benefit.description }}
              </div>
            </div>
            <div class="benefit-badge">
              <el-icon color="#67c23a">
                <CircleCheck />
              </el-icon>
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
  const icons = [
    ChatDotRound,
    DataAnalysis,
    Document,
    Star,
    Trophy,
    TrendCharts,
    Service,
    Medal,
    Present
  ]
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
const getBenefitDescription = feature => {
  const descriptions = {
    基础营养记录: '记录每日饮食，追踪营养摄入',
    营养记录无限: '不限次数记录饮食，全面追踪',
    'AI咨询(3次/天)': '每天3次智能营养咨询',
    'AI咨询(10次/天)': '每天10次智能营养咨询',
    'AI咨询(30次/天)': '每天30次智能营养咨询',
    AI咨询无限: '无限次数AI智能咨询服务',
    基础数据分析: '查看基础营养数据统计',
    高级数据分析: '深度数据分析和趋势预测',
    专属营养报告: '定期生成专业营养报告',
    自定义目标: '设置个性化饮食目标',
    优先客服: '享受优先客服支持',
    个性化食谱: 'AI生成个性化饮食方案',
    营养顾问: '专属营养顾问一对一服务',
    数据导出: '导出所有个人数据',
    专属营养师: '真人营养师在线指导',
    线下活动: '参与线下营养活动',
    合作商家折扣: '享受合作商家优惠',
    终身成长值加成: '成长值获取额外加成'
  }
  return descriptions[feature] || '专属会员权益'
}
</script>

<style scoped lang="scss">
.benefits-list {
  border-radius: 15px 225px 15px 255px / 255px 15px 225px 15px;
  border: 2px solid #2d2d2d;
  box-shadow: 4px 4px 0px 0px #2d2d2d;

  :deep(.el-card__header) {
    padding: 12px 16px;
  }

  :deep(.el-card__body) {
    padding: 14px 16px;
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .title {
      font-size: 15px;
      font-weight: 600;
      color: #2d2d2d;
      font-family: 'Kalam', 'ZCOOL KuaiLe', cursive;
    }
  }

  .benefits-content {
    .benefit-items {
      .benefit-item {
        display: flex;
        align-items: center;
        gap: 10px;
        padding: 10px;
        border-radius: 6px;
        transition: all 0.3s;

        &:hover {
          background: #fff9c4;
          transform: translateX(4px);
        }

        &:not(:last-child) {
          border-bottom: 1px dashed #2d2d2d;
        }

        .benefit-icon {
          width: 36px;
          height: 36px;
          border-radius: 10px;
          display: flex;
          align-items: center;
          justify-content: center;
          color: white;
          flex-shrink: 0;
        }

        &:nth-child(3n+1) .benefit-icon {
          background: #2d5da1;
        }
        &:nth-child(3n+2) .benefit-icon {
          background: #ff4d4d;
        }
        &:nth-child(3n+3) .benefit-icon {
          background: #2d2d2d;
        }

        .benefit-content {
          flex: 1;

          .benefit-title {
            font-size: 13px;
            font-weight: 600;
            color: #2d2d2d;
            margin-bottom: 2px;
          }

          .benefit-desc {
            font-size: 12px;
            color: #e5e0d8;
          }
        }

        .benefit-badge {
          flex-shrink: 0;
          font-size: 16px;
        }
      }
    }

    .benefit-stats {
      margin-top: 10px;
      padding: 10px;
      background: #fdfbf7;
      border: 1px dashed #2d2d2d;
      border-radius: 255px 15px 225px 15px / 15px 225px 15px 255px;
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 10px;

      .stat-item {
        display: flex;
        flex-direction: column;
        gap: 4px;

        .stat-label {
          font-size: 12px;
          color: #e5e0d8;
        }

        .stat-value {
          font-size: 16px;
          font-weight: 600;
          color: #2d5da1;
        }
      }
    }
  }
}
</style>
