<template>
  <div class="health-record">
    <div class="record-header">
      <h2 class="title">
        健康档案
      </h2>
    </div>

    <!-- BMI计算器 -->
    <div class="bmi-calculator">
      <h3 class="section-title">
        BMI 计算器
      </h3>
      
      <el-form
        :model="bmiData"
        label-width="80px"
        class="bmi-form"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="身高">
              <el-input-number
                v-model="bmiData.height"
                :min="100"
                :max="250"
                :step="0.1"
                :precision="1"
                placeholder="厘米"
                style="width: 100%"
                @change="calculateBMI"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="体重">
              <el-input-number
                v-model="bmiData.weight"
                :min="30"
                :max="200"
                :step="0.1"
                :precision="1"
                placeholder="千克"
                style="width: 100%"
                @change="calculateBMI"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <!-- BMI结果显示 -->
      <div
        v-if="bmiResult"
        class="bmi-result"
      >
        <div
          class="result-card"
          :class="bmiResult.level"
        >
          <div class="result-value">
            <span class="value">{{ bmiResult.value }}</span>
            <span class="unit">BMI</span>
          </div>
          <div class="result-status">
            <el-tag
              :type="bmiResult.tagType"
              size="large"
              effect="dark"
            >
              {{ bmiResult.status }}
            </el-tag>
          </div>
        </div>

        <!-- BMI详细说明 -->
        <div class="bmi-info">
          <h4>BMI 指数说明</h4>
          <div class="info-grid">
            <div
              class="info-item"
              :class="{ active: bmiResult.level === 'underweight' }"
            >
              <div class="info-label">
                偏瘦
              </div>
              <div class="info-range">
                &lt; 18.5
              </div>
            </div>
            <div
              class="info-item"
              :class="{ active: bmiResult.level === 'normal' }"
            >
              <div class="info-label">
                正常
              </div>
              <div class="info-range">
                18.5 - 23.9
              </div>
            </div>
            <div
              class="info-item"
              :class="{ active: bmiResult.level === 'overweight' }"
            >
              <div class="info-label">
                超重
              </div>
              <div class="info-range">
                24.0 - 27.9
              </div>
            </div>
            <div
              class="info-item"
              :class="{ active: bmiResult.level === 'obese' }"
            >
              <div class="info-label">
                肥胖
              </div>
              <div class="info-range">
                ≥ 28.0
              </div>
            </div>
          </div>

          <!-- 健康建议 -->
          <div class="health-advice">
            <el-alert
              :title="bmiResult.advice.title"
              :type="bmiResult.tagType"
              :description="bmiResult.advice.description"
              show-icon
              :closable="false"
            />
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <el-empty
        v-else
        description="请输入身高和体重计算 BMI"
        :image-size="120"
      />
    </div>

    <!-- 理想体重计算 -->
    <div
      v-if="bmiResult"
      class="ideal-weight"
    >
      <h3 class="section-title">
        理想体重范围
      </h3>
      <div class="weight-range">
        <div class="range-item">
          <div class="range-label">
            最小值
          </div>
          <div class="range-value">
            {{ idealWeight.min }} kg
          </div>
        </div>
        <div class="range-divider">
          ~
        </div>
        <div class="range-item">
          <div class="range-label">
            最大值
          </div>
          <div class="range-value">
            {{ idealWeight.max }} kg
          </div>
        </div>
      </div>
      <div class="weight-tip">
        <el-icon><InfoFilled /></el-icon>
        根据您的身高 {{ bmiData.height }} cm，理想体重范围为 {{ idealWeight.min }} - {{ idealWeight.max }} kg
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { InfoFilled } from '@element-plus/icons-vue'

const bmiData = reactive({
  height: null,
  weight: null
})

const bmiResult = ref(null)

// 计算理想体重范围
const idealWeight = computed(() => {
  if (!bmiData.height) return { min: 0, max: 0 }
  
  const heightInMeters = bmiData.height / 100
  const min = (18.5 * heightInMeters * heightInMeters).toFixed(1)
  const max = (23.9 * heightInMeters * heightInMeters).toFixed(1)
  
  return { min, max }
})

// 计算BMI
const calculateBMI = () => {
  if (!bmiData.height || !bmiData.weight) {
    bmiResult.value = null
    return
  }

  const heightInMeters = bmiData.height / 100
  const bmi = (bmiData.weight / (heightInMeters * heightInMeters)).toFixed(1)

  let status, level, tagType, advice

  if (bmi < 18.5) {
    status = '偏瘦'
    level = 'underweight'
    tagType = 'info'
    advice = {
      title: '体重偏低，需要增加营养',
      description: '建议增加优质蛋白质和健康脂肪的摄入，配合适当的力量训练，逐步增加体重。'
    }
  } else if (bmi < 24) {
    status = '正常'
    level = 'normal'
    tagType = 'success'
    advice = {
      title: '体重正常，请继续保持',
      description: '继续保持均衡饮食和适量运动的良好习惯，定期监测体重变化。'
    }
  } else if (bmi < 28) {
    status = '超重'
    level = 'overweight'
    tagType = 'warning'
    advice = {
      title: '体重超重，建议减重',
      description: '建议控制饮食摄入，增加有氧运动，每周至少进行150分钟中等强度运动。'
    }
  } else {
    status = '肥胖'
    level = 'obese'
    tagType = 'danger'
    advice = {
      title: '体重肥胖，需要减重',
      description: '建议咨询专业营养师制定减重计划，结合饮食控制和规律运动，必要时就医检查。'
    }
  }

  bmiResult.value = {
    value: bmi,
    status,
    level,
    tagType,
    advice
  }
}
</script>

<style scoped lang="scss">
.health-record {
  .record-header {
    margin-bottom: 32px;
    padding-bottom: 20px;
    border-bottom: 2px solid #f0f0f0;

    .title {
      font-size: 24px;
      font-weight: 600;
      color: #333;
      margin: 0;
    }
  }

  .section-title {
    font-size: 18px;
    font-weight: 600;
    color: #333;
    margin: 0 0 20px 0;
    padding-bottom: 12px;
    border-bottom: 1px solid #f0f0f0;
  }

  .bmi-calculator {
    margin-bottom: 40px;

    .bmi-form {
      margin-bottom: 24px;

      :deep(.el-input-number) {
        width: 100%;
      }
    }

    .bmi-result {
      .result-card {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        border-radius: 16px;
        padding: 32px;
        color: white;
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 24px;

        &.underweight {
          background: linear-gradient(135deg, #3b82f6 0%, #8b5cf6 100%);
        }

        &.normal {
          background: linear-gradient(135deg, #10b981 0%, #059669 100%);
        }

        &.overweight {
          background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
        }

        &.obese {
          background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
        }

        .result-value {
          .value {
            font-size: 56px;
            font-weight: 700;
            line-height: 1;
          }

          .unit {
            font-size: 18px;
            margin-left: 8px;
            opacity: 0.9;
          }
        }

        .result-status {
          :deep(.el-tag) {
            font-size: 18px;
            padding: 12px 24px;
            border: none;
          }
        }
      }

      .bmi-info {
        h4 {
          font-size: 16px;
          font-weight: 600;
          color: #333;
          margin: 0 0 16px 0;
        }

        .info-grid {
          display: grid;
          grid-template-columns: repeat(4, 1fr);
          gap: 12px;
          margin-bottom: 24px;

          @media (max-width: 768px) {
            grid-template-columns: repeat(2, 1fr);
          }

          .info-item {
            background: #f5f7fa;
            border-radius: 8px;
            padding: 16px;
            text-align: center;
            transition: all 0.3s;

            &.active {
              background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
              color: white;
              transform: translateY(-4px);
              box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
            }

            .info-label {
              font-size: 14px;
              font-weight: 600;
              margin-bottom: 8px;
            }

            .info-range {
              font-size: 13px;
              opacity: 0.8;
            }
          }
        }

        .health-advice {
          :deep(.el-alert) {
            border-radius: 12px;
          }
        }
      }
    }
  }

  .ideal-weight {
    .weight-range {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 24px;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      border-radius: 16px;
      padding: 32px;
      color: white;
      margin-bottom: 16px;

      .range-item {
        text-align: center;

        .range-label {
          font-size: 14px;
          opacity: 0.9;
          margin-bottom: 8px;
        }

        .range-value {
          font-size: 32px;
          font-weight: 700;
        }
      }

      .range-divider {
        font-size: 32px;
        font-weight: 300;
        opacity: 0.7;
      }
    }

    .weight-tip {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 14px;
      color: #909399;
      padding: 12px 16px;
      background: #f5f7fa;
      border-radius: 8px;

      .el-icon {
        font-size: 16px;
      }
    }
  }
}
</style>
