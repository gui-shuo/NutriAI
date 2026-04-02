<template>
  <el-dialog v-model="visible" title="添加饮食记录" width="860px" @close="handleClose">
    <el-form ref="formRef" :model="formData" :rules="rules" label-width="auto" class="record-form">
      <div class="form-grid">
        <!-- 左栏：基本信息 -->
        <div class="form-left">
          <h4 class="form-section-title">📝 基本信息</h4>

          <!-- 餐次类型 -->
          <el-form-item label="餐次类型" prop="mealType">
            <el-radio-group v-model="formData.mealType">
              <el-radio-button v-for="type in mealTypeList" :key="type.value" :value="type.value">
                {{ type.label }}
              </el-radio-button>
            </el-radio-group>
          </el-form-item>

          <!-- 食物名称 -->
          <el-form-item label="食物名称" prop="foodName">
            <div class="food-name-input">
              <el-input
                v-model="formData.foodName"
                placeholder="请输入食物名称"
                maxlength="100"
                show-word-limit
                @keyup.enter="handleRecognizeByName"
              />
              <el-button
                type="primary"
                :loading="recognizing"
                :disabled="!formData.foodName?.trim()"
                @click="handleRecognizeByName"
              >
                <el-icon v-if="!recognizing"><Search /></el-icon>
                识别
              </el-button>
            </div>
          </el-form-item>

          <!-- 记录时间 -->
          <el-form-item label="记录时间" prop="recordTime">
            <el-date-picker
              v-model="formData.recordTime"
              type="datetime"
              placeholder="选择日期时间"
              format="YYYY-MM-DD HH:mm"
              value-format="YYYY-MM-DDTHH:mm:ss"
              style="width: 100%"
            />
          </el-form-item>

          <!-- 食物照片 -->
          <el-form-item label="食物照片">
            <FoodPhotoUpload
              v-model="formData.photoUrl"
              auto-recognize
              @recognized="handlePhotoRecognized"
            />
            <div v-if="recognitionSource" class="recognition-hint">
              <el-tag :type="recognitionSource === 'database' ? 'success' : 'warning'" size="small">
                {{ recognitionSource === 'database' ? '营养数据来自数据库' : 'AI智能估算营养数据' }}
              </el-tag>
            </div>
          </el-form-item>

          <!-- 备注 -->
          <el-form-item label="备注">
            <el-input
              v-model="formData.notes"
              type="textarea"
              :rows="2"
              placeholder="添加备注信息"
              maxlength="500"
              show-word-limit
            />
          </el-form-item>
        </div>

        <!-- 右栏：营养信息 -->
        <div class="form-right">
          <div class="nutrition-panel">
            <div class="panel-header">
              <h4 class="form-section-title">🍎 营养信息</h4>
              <el-tag v-if="nutritionAutoFilled" type="success" size="small" effect="plain">
                已自动填充
              </el-tag>
            </div>

            <el-form-item label="份量(克)" prop="portion">
              <el-input-number
                v-model="formData.portion"
                :min="0"
                :precision="2"
                :step="10"
                style="width: 100%"
              />
            </el-form-item>

            <el-form-item label="卡路里(千卡)" prop="calories">
              <el-input-number
                v-model="formData.calories"
                :min="0"
                :precision="2"
                :step="10"
                style="width: 100%"
              />
            </el-form-item>

            <el-form-item label="蛋白质(克)">
              <el-input-number
                v-model="formData.protein"
                :min="0"
                :precision="2"
                :step="1"
                style="width: 100%"
              />
            </el-form-item>

            <el-form-item label="碳水(克)">
              <el-input-number
                v-model="formData.carbohydrates"
                :min="0"
                :precision="2"
                :step="1"
                style="width: 100%"
              />
            </el-form-item>

            <el-form-item label="脂肪(克)">
              <el-input-number
                v-model="formData.fat"
                :min="0"
                :precision="2"
                :step="1"
                style="width: 100%"
              />
            </el-form-item>

            <el-form-item label="膳食纤维(克)">
              <el-input-number
                v-model="formData.fiber"
                :min="0"
                :precision="2"
                :step="1"
                style="width: 100%"
              />
            </el-form-item>
          </div>
        </div>
      </div>
    </el-form>

    <template #footer>
      <el-button @click="handleClose"> 取消 </el-button>
      <el-button type="primary" :loading="loading" @click="handleSubmit"> 保存 </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, watch } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { getMealTypeList, createFoodRecord, recognizeByName } from '@/services/foodRecord'
import FoodPhotoUpload from './FoodPhotoUpload.vue'
import message from '@/utils/message'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'success'])

const formRef = ref()
const loading = ref(false)
const recognizing = ref(false)
const visible = ref(false)
const mealTypeList = getMealTypeList()
const nutritionAutoFilled = ref(false)
const recognitionSource = ref('')

// 生成本地时区的日期时间字符串（避免 toISOString 返回 UTC 时间）
const localNow = () => {
  const now = new Date()
  return new Date(now.getTime() - now.getTimezoneOffset() * 60000).toISOString().slice(0, 19)
}

const formData = reactive({
  mealType: 'BREAKFAST',
  foodName: '',
  photoUrl: '',
  portion: null,
  calories: null,
  protein: null,
  carbohydrates: null,
  fat: null,
  fiber: null,
  recordTime: localNow(),
  notes: ''
})

const rules = {
  mealType: [{ required: true, message: '请选择餐次类型', trigger: 'change' }],
  foodName: [
    { required: true, message: '请输入食物名称', trigger: 'blur' },
    { max: 100, message: '食物名称最多100个字符', trigger: 'blur' }
  ],
  recordTime: [{ required: true, message: '请选择记录时间', trigger: 'change' }]
}

watch(
  () => props.modelValue,
  val => {
    visible.value = val
  }
)

watch(visible, val => {
  emit('update:modelValue', val)
})

/**
 * 将识别结果自动填充到表单的营养信息字段
 */
const applyRecognitionResult = result => {
  if (!result || !result.foods || result.foods.length === 0) return

  const food = result.foods[0]
  const nutrition = food.nutrition

  // 如果识别结果有食物名称且当前表单没有填写，则自动填写
  if (food.name && !formData.foodName) {
    formData.foodName = food.name
  }

  // 默认100g份量（营养信息基于100g）
  if (!formData.portion) {
    formData.portion = 100
  }

  // 自动填充营养信息
  if (nutrition) {
    formData.calories = nutrition.energy ? Math.round(nutrition.energy * 100) / 100 : null
    formData.protein = nutrition.protein ? Math.round(nutrition.protein * 100) / 100 : null
    formData.carbohydrates = nutrition.carbohydrate
      ? Math.round(nutrition.carbohydrate * 100) / 100
      : null
    formData.fat = nutrition.fat ? Math.round(nutrition.fat * 100) / 100 : null
    formData.fiber = nutrition.fiber ? Math.round(nutrition.fiber * 100) / 100 : null
    recognitionSource.value = nutrition.source || 'estimated'
  }

  nutritionAutoFilled.value = true
}

/**
 * 通过食物名称智能识别营养信息
 */
const handleRecognizeByName = async () => {
  const name = formData.foodName?.trim()
  if (!name) {
    message.warning('请先输入食物名称')
    return
  }

  recognizing.value = true
  try {
    const response = await recognizeByName(name)
    if (response.data.code === 200 && response.data.data) {
      applyRecognitionResult(response.data.data)
      message.success('营养信息识别成功')
    } else {
      message.warning(response.data.message || '未能识别到营养信息')
    }
  } catch (error) {
    console.error('识别失败:', error)
    message.error('营养识别失败，请手动填写')
  } finally {
    recognizing.value = false
  }
}

/**
 * 处理照片上传后的识别结果
 */
const handlePhotoRecognized = result => {
  if (result && result.foods && result.foods.length > 0) {
    applyRecognitionResult(result)
  }
}

const handleSubmit = async () => {
  try {
    await formRef.value.validate()

    loading.value = true
    const response = await createFoodRecord(formData)

    if (response.data.code === 200) {
      message.success('添加成功')
      emit('success')
      handleClose()
    } else {
      message.error(response.data.message || '添加失败')
    }
  } catch (error) {
    if (error !== false) {
      console.error('添加失败:', error)
      message.error('添加失败，请稍后重试')
    }
  } finally {
    loading.value = false
  }
}

const handleClose = () => {
  formRef.value?.resetFields()
  Object.assign(formData, {
    mealType: 'BREAKFAST',
    foodName: '',
    photoUrl: '',
    portion: null,
    calories: null,
    protein: null,
    carbohydrates: null,
    fat: null,
    fiber: null,
    recordTime: localNow(),
    notes: ''
  })
  nutritionAutoFilled.value = false
  recognitionSource.value = ''
  visible.value = false
}
</script>

<style scoped lang="scss">
.record-form {
  .form-grid {
    display: grid;
    grid-template-columns: 1fr 320px;
    gap: 24px;
  }

  .form-section-title {
    margin: 0 0 16px 0;
    font-size: 15px;
    font-weight: 600;
    color: #303133;
  }

  .food-name-input {
    display: flex;
    gap: 8px;
    width: 100%;

    .el-input {
      flex: 1;
    }
  }

  .recognition-hint {
    margin-top: 8px;
  }

  .nutrition-panel {
    background: #f9fafb;
    border: 1px solid #ebeef5;
    border-radius: 10px;
    padding: 20px;
    height: 100%;

    .panel-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;

      .form-section-title { margin: 0; }
    }

    .el-form-item {
      margin-bottom: 14px;
    }
  }
}

@media (max-width: 768px) {
  .form-grid {
    grid-template-columns: 1fr !important;
  }
}
</style>
