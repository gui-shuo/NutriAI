<template>
  <div class="health-record">
    <div class="record-header">
      <h2 class="title">健康档案</h2>
      <el-button type="primary" :loading="saving" @click="handleSave">
        <el-icon v-if="!saving"><Select /></el-icon>
        保存档案
      </el-button>
    </div>

    <el-skeleton :loading="loading" animated :rows="8">
      <el-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        label-width="100px"
        class="health-form"
      >
        <!-- 基础信息 -->
        <h3 class="section-title">基础信息</h3>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="性别" prop="gender">
              <el-radio-group v-model="formData.gender">
                <el-radio value="MALE">男</el-radio>
                <el-radio value="FEMALE">女</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="出生日期" prop="birthDate">
              <el-date-picker
                v-model="formData.birthDate"
                type="date"
                placeholder="选择日期"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                :disabled-date="d => d > new Date()"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 身体指标 -->
        <h3 class="section-title">身体指标</h3>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="身高(cm)" prop="height">
              <el-input-number
                v-model="formData.height"
                :min="50"
                :max="300"
                :step="0.1"
                :precision="1"
                style="width: 100%"
                @change="calculateBMI"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="体重(kg)" prop="weight">
              <el-input-number
                v-model="formData.weight"
                :min="20"
                :max="500"
                :step="0.1"
                :precision="1"
                style="width: 100%"
                @change="calculateBMI"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="目标体重">
              <el-input-number
                v-model="formData.targetWeight"
                :min="20"
                :max="500"
                :step="0.1"
                :precision="1"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="腰围(cm)">
              <el-input-number
                v-model="formData.waistCircumference"
                :min="30"
                :max="300"
                :step="0.1"
                :precision="1"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="体脂率(%)">
              <el-input-number
                v-model="formData.bodyFatPercentage"
                :min="1"
                :max="80"
                :step="0.1"
                :precision="1"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="活动量">
              <el-select v-model="formData.activityLevel" placeholder="请选择" style="width: 100%">
                <el-option label="久坐不动" value="SEDENTARY" />
                <el-option label="轻度活动" value="LIGHT" />
                <el-option label="中度活动" value="MODERATE" />
                <el-option label="重度活动" value="ACTIVE" />
                <el-option label="专业运动" value="VERY_ACTIVE" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 健康信息 -->
        <h3 class="section-title">健康信息</h3>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="健康目标">
              <el-select
                v-model="formData.healthGoals"
                multiple
                placeholder="请选择"
                style="width: 100%"
              >
                <el-option label="减重" value="减重" />
                <el-option label="增肌" value="增肌" />
                <el-option label="保持体重" value="保持体重" />
                <el-option label="改善饮食" value="改善饮食" />
                <el-option label="控制血糖" value="控制血糖" />
                <el-option label="降低血压" value="降低血压" />
                <el-option label="改善睡眠" value="改善睡眠" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="饮食限制">
              <el-select
                v-model="formData.dietaryRestrictions"
                multiple
                placeholder="请选择"
                style="width: 100%"
              >
                <el-option label="素食" value="素食" />
                <el-option label="纯素" value="纯素" />
                <el-option label="无麸质" value="无麸质" />
                <el-option label="低碳水" value="低碳水" />
                <el-option label="低脂" value="低脂" />
                <el-option label="高蛋白" value="高蛋白" />
                <el-option label="清真" value="清真" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="过敏源">
              <el-select
                v-model="formData.allergies"
                multiple
                filterable
                allow-create
                placeholder="选择或输入"
                style="width: 100%"
              >
                <el-option label="花生" value="花生" />
                <el-option label="牛奶" value="牛奶" />
                <el-option label="鸡蛋" value="鸡蛋" />
                <el-option label="海鲜" value="海鲜" />
                <el-option label="大豆" value="大豆" />
                <el-option label="坚果" value="坚果" />
                <el-option label="小麦" value="小麦" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="健康状况">
              <el-select
                v-model="formData.medicalConditions"
                multiple
                filterable
                allow-create
                placeholder="选择或输入"
                style="width: 100%"
              >
                <el-option label="糖尿病" value="糖尿病" />
                <el-option label="高血压" value="高血压" />
                <el-option label="高血脂" value="高血脂" />
                <el-option label="痛风" value="痛风" />
                <el-option label="胃炎" value="胃炎" />
                <el-option label="贫血" value="贫血" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="卡路里目标">
              <el-input-number
                v-model="formData.dailyCalorieTarget"
                :min="500"
                :max="10000"
                :step="50"
                style="width: 100%"
              />
              <div class="form-hint" v-if="!formData.dailyCalorieTarget">
                留空则系统根据身体指标自动计算
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="备注">
              <el-input
                v-model="formData.notes"
                type="textarea"
                :rows="2"
                maxlength="1000"
                show-word-limit
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </el-skeleton>

    <!-- BMI结果显示 -->
    <div v-if="bmiResult" class="bmi-calculator">
      <h3 class="section-title">BMI 分析结果</h3>
      <div class="bmi-result">
        <div class="result-card" :class="bmiResult.level">
          <div class="result-value">
            <span class="value">{{ bmiResult.value }}</span>
            <span class="unit">BMI</span>
          </div>
          <div class="result-status">
            <el-tag :type="bmiResult.tagType" size="large" effect="dark">
              {{ bmiResult.status }}
            </el-tag>
          </div>
        </div>

        <div class="bmi-info">
          <h4>BMI 指数说明</h4>
          <div class="info-grid">
            <div class="info-item" :class="{ active: bmiResult.level === 'underweight' }">
              <div class="info-label">偏瘦</div>
              <div class="info-range">&lt; 18.5</div>
            </div>
            <div class="info-item" :class="{ active: bmiResult.level === 'normal' }">
              <div class="info-label">正常</div>
              <div class="info-range">18.5 - 23.9</div>
            </div>
            <div class="info-item" :class="{ active: bmiResult.level === 'overweight' }">
              <div class="info-label">超重</div>
              <div class="info-range">24.0 - 27.9</div>
            </div>
            <div class="info-item" :class="{ active: bmiResult.level === 'obese' }">
              <div class="info-label">肥胖</div>
              <div class="info-range">≥ 28.0</div>
            </div>
          </div>

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

      <!-- 理想体重范围 -->
      <div class="ideal-weight">
        <h4>理想体重范围</h4>
        <div class="weight-range">
          <div class="range-item">
            <div class="range-label">最小值</div>
            <div class="range-value">{{ idealWeight.min }} kg</div>
          </div>
          <div class="range-divider">~</div>
          <div class="range-item">
            <div class="range-label">最大值</div>
            <div class="range-value">{{ idealWeight.max }} kg</div>
          </div>
        </div>
        <div class="weight-tip">
          <el-icon><InfoFilled /></el-icon>
          根据您的身高 {{ formData.height }} cm，理想体重范围为 {{ idealWeight.min }} -
          {{ idealWeight.max }} kg
        </div>
      </div>

      <!-- 每日推荐卡路里 -->
      <div v-if="savedProfile?.dailyCalorieTarget" class="calorie-card">
        <h4>每日推荐摄入</h4>
        <div class="calorie-value">{{ savedProfile.dailyCalorieTarget }} <span>kcal</span></div>
      </div>
    </div>

    <!-- 空状态 -->
    <el-empty
      v-if="!bmiResult && !loading"
      description="请填写身高和体重后保存，系统将自动计算 BMI"
      :image-size="120"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { InfoFilled, Select } from '@element-plus/icons-vue'
import api from '@/services/api'
import message from '@/utils/message'

const formRef = ref()
const loading = ref(false)
const saving = ref(false)
const savedProfile = ref(null)

const formData = reactive({
  gender: null,
  birthDate: null,
  height: null,
  weight: null,
  targetWeight: null,
  activityLevel: null,
  healthGoals: [],
  dietaryRestrictions: [],
  allergies: [],
  medicalConditions: [],
  dailyCalorieTarget: null,
  waistCircumference: null,
  bodyFatPercentage: null,
  notes: ''
})

const rules = {
  height: [{ type: 'number', min: 50, max: 300, message: '身高范围 50-300 cm', trigger: 'change' }],
  weight: [{ type: 'number', min: 20, max: 500, message: '体重范围 20-500 kg', trigger: 'change' }]
}

const bmiResult = ref(null)

const idealWeight = computed(() => {
  if (!formData.height) return { min: 0, max: 0 }
  const h = formData.height / 100
  return {
    min: (18.5 * h * h).toFixed(1),
    max: (23.9 * h * h).toFixed(1)
  }
})

const calculateBMI = () => {
  if (!formData.height || !formData.weight) {
    bmiResult.value = null
    return
  }
  const h = formData.height / 100
  const bmi = (formData.weight / (h * h)).toFixed(1)
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
  bmiResult.value = { value: bmi, status, level, tagType, advice }
}

// 从后端加载健康档案
const fetchHealthProfile = async () => {
  loading.value = true
  try {
    const res = await api.get('/user/health-profile')
    if (res.data.code === 200 && res.data.data) {
      const d = res.data.data
      savedProfile.value = d
      formData.gender = d.gender || null
      formData.birthDate = d.birthDate || null
      formData.height = d.height ? Number(d.height) : null
      formData.weight = d.weight ? Number(d.weight) : null
      formData.targetWeight = d.targetWeight ? Number(d.targetWeight) : null
      formData.activityLevel = d.activityLevel || null
      formData.healthGoals = d.healthGoals || []
      formData.dietaryRestrictions = d.dietaryRestrictions || []
      formData.allergies = d.allergies || []
      formData.medicalConditions = d.medicalConditions || []
      formData.dailyCalorieTarget = d.dailyCalorieTarget || null
      formData.waistCircumference = d.waistCircumference ? Number(d.waistCircumference) : null
      formData.bodyFatPercentage = d.bodyFatPercentage ? Number(d.bodyFatPercentage) : null
      formData.notes = d.notes || ''
      calculateBMI()
    } else {
      // 用户尚未创建健康档案，显示空表单
      savedProfile.value = null
    }
  } catch (error) {
    console.error('获取健康档案失败:', error)
    message.error('获取健康档案失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 保存健康档案到后端
const handleSave = async () => {
  if (formRef.value) {
    try {
      const valid = await formRef.value.validate()
      if (!valid) return
    } catch {
      return
    }
  }

  saving.value = true
  try {
    const res = await api.put('/user/health-profile', {
      gender: formData.gender,
      birthDate: formData.birthDate,
      height: formData.height,
      weight: formData.weight,
      targetWeight: formData.targetWeight,
      activityLevel: formData.activityLevel,
      healthGoals: formData.healthGoals,
      dietaryRestrictions: formData.dietaryRestrictions,
      allergies: formData.allergies,
      medicalConditions: formData.medicalConditions,
      dailyCalorieTarget: formData.dailyCalorieTarget,
      waistCircumference: formData.waistCircumference,
      bodyFatPercentage: formData.bodyFatPercentage,
      notes: formData.notes
    })

    if (res.data.code === 200) {
      savedProfile.value = res.data.data
      message.success('健康档案保存成功')
      calculateBMI()
    } else {
      message.error(res.data.message || '保存失败')
    }
  } catch (error) {
    message.error(error.response?.data?.message || '保存失败，请稍后重试')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  fetchHealthProfile()
})
</script>

<style scoped lang="scss">
.health-record {
  .record-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
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
    margin: 24px 0 20px 0;
    padding-bottom: 12px;
    border-bottom: 1px solid #f0f0f0;
  }

  .health-form {
    :deep(.el-input-number) {
      width: 100%;
    }
  }

  .form-hint {
    font-size: 12px;
    color: #909399;
    margin-top: 4px;
  }

  .bmi-calculator {
    margin-top: 40px;

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
    margin-top: 24px;

    h4 {
      font-size: 16px;
      font-weight: 600;
      color: #333;
      margin: 0 0 16px 0;
    }

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

  .calorie-card {
    margin-top: 24px;
    text-align: center;
    background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
    border-radius: 16px;
    padding: 24px;
    color: white;

    h4 {
      font-size: 16px;
      margin: 0 0 12px;
      opacity: 0.9;
    }

    .calorie-value {
      font-size: 48px;
      font-weight: 700;
      span {
        font-size: 18px;
        opacity: 0.8;
      }
    }
  }
}
</style>
