<template>
  <el-dialog
    v-model="visible"
    title="添加饮食记录"
    width="700px"
    class="add-record-dialog"
    @close="handleClose"
  >
    <template #header>
      <div class="dialog-header">
        <span class="dialog-icon">🍴</span>
        <span class="dialog-title">添加饮食记录</span>
      </div>
    </template>

    <el-form ref="formRef" :model="formData" :rules="rules" label-width="110px" class="record-form">
      <!-- 餐次类型 -->
      <el-form-item label="餐次类型" prop="mealType">
        <el-radio-group v-model="formData.mealType" class="meal-type-group">
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
            class="recognize-btn"
            :loading="recognizing"
            :disabled="!formData.foodName?.trim()"
            @click="handleRecognizeByName"
          >
            <el-icon v-if="!recognizing"><Search /></el-icon>
            智能识别
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

      <!-- 营养信息 -->
      <div class="nutrition-section">
        <div class="section-header">
          <div class="section-title-wrap">
            <span class="section-icon">⚡</span>
            <h3 class="section-title">营养信息</h3>
          </div>
          <el-tag v-if="nutritionAutoFilled" type="success" size="small" effect="plain">
            已自动填充
          </el-tag>
        </div>

        <div class="nutrition-grid">
          <div class="nut-card nut-portion">
            <div class="nut-card-label">份量 (克)</div>
            <el-form-item prop="portion">
              <el-input-number
                v-model="formData.portion"
                :min="0"
                :precision="2"
                :step="10"
                style="width: 100%"
              />
            </el-form-item>
          </div>
          <div class="nut-card nut-calorie">
            <div class="nut-card-label">🔥 卡路里 (千卡)</div>
            <el-form-item prop="calories">
              <el-input-number
                v-model="formData.calories"
                :min="0"
                :precision="2"
                :step="10"
                style="width: 100%"
              />
            </el-form-item>
          </div>
          <div class="nut-card nut-protein">
            <div class="nut-card-label">💪 蛋白质 (克)</div>
            <el-form-item>
              <el-input-number
                v-model="formData.protein"
                :min="0"
                :precision="2"
                :step="1"
                style="width: 100%"
              />
            </el-form-item>
          </div>
          <div class="nut-card nut-carbs">
            <div class="nut-card-label">🌾 碳水 (克)</div>
            <el-form-item>
              <el-input-number
                v-model="formData.carbohydrates"
                :min="0"
                :precision="2"
                :step="1"
                style="width: 100%"
              />
            </el-form-item>
          </div>
          <div class="nut-card nut-fat">
            <div class="nut-card-label">🧈 脂肪 (克)</div>
            <el-form-item>
              <el-input-number
                v-model="formData.fat"
                :min="0"
                :precision="2"
                :step="1"
                style="width: 100%"
              />
            </el-form-item>
          </div>
          <div class="nut-card nut-fiber">
            <div class="nut-card-label">🥦 膳食纤维 (克)</div>
            <el-form-item>
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

      <!-- 备注 -->
      <el-form-item label="备注">
        <el-input
          v-model="formData.notes"
          type="textarea"
          :rows="3"
          placeholder="添加备注信息"
          maxlength="500"
          show-word-limit
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button class="cancel-btn" @click="handleClose"> 取消 </el-button>
        <el-button class="submit-btn" type="primary" :loading="loading" @click="handleSubmit"> 保存记录 </el-button>
      </div>
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
  .food-name-input {
    display: flex;
    gap: 10px;
    width: 100%;

    .el-input {
      flex: 1;
    }

    .recognize-btn {
      border-radius: 10px;
      font-weight: 600;
      background: linear-gradient(135deg, #6366f1, #8b5cf6);
      border: none;
      box-shadow: 0 2px 8px rgba(99, 102, 241, 0.3);
      transition: all 0.3s ease;

      &:hover:not(:disabled) {
        transform: translateY(-1px);
        box-shadow: 0 4px 12px rgba(99, 102, 241, 0.45);
      }
    }
  }

  .recognition-hint {
    margin-top: 10px;
  }

  .meal-type-group {
    :deep(.el-radio-button__inner) {
      border-radius: 10px !important;
      border: 1px solid #e2e8f0 !important;
      margin: 0 4px;
      padding: 8px 20px;
      font-weight: 500;
      transition: all 0.25s ease;
      box-shadow: none !important;
    }

    :deep(.el-radio-button:first-child .el-radio-button__inner) {
      border-left: 1px solid #e2e8f0 !important;
      border-radius: 10px !important;
    }

    :deep(.el-radio-button:last-child .el-radio-button__inner) {
      border-radius: 10px !important;
    }

    :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
      background: linear-gradient(135deg, #6366f1, #8b5cf6) !important;
      border-color: transparent !important;
      color: #fff !important;
      box-shadow: 0 2px 10px rgba(99, 102, 241, 0.35) !important;
    }
  }

  .nutrition-section {
    background: linear-gradient(135deg, #f8fafc, #f1f5f9);
    border: 1px solid #e2e8f0;
    border-radius: 16px;
    padding: 24px;
    margin-bottom: 24px;

    .section-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 20px;

      .section-title-wrap {
        display: flex;
        align-items: center;
        gap: 8px;

        .section-icon {
          font-size: 18px;
        }
      }
    }

    .section-title {
      font-size: 16px;
      font-weight: 700;
      color: #1e293b;
      margin: 0;
    }

    .nutrition-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 14px;
    }

    .nut-card {
      background: #fff;
      border-radius: 12px;
      padding: 14px 16px 8px;
      border: 1px solid #e2e8f0;
      transition: border-color 0.2s ease, box-shadow 0.2s ease;

      &:hover {
        box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
      }

      .nut-card-label {
        font-size: 13px;
        font-weight: 600;
        color: #64748b;
        margin-bottom: 8px;
      }

      :deep(.el-form-item) {
        margin-bottom: 4px;
      }

      :deep(.el-form-item__label) {
        display: none;
      }

      &.nut-calorie {
        border-left: 3px solid #f97316;
        .nut-card-label { color: #ea580c; }
      }

      &.nut-protein {
        border-left: 3px solid #3b82f6;
        .nut-card-label { color: #2563eb; }
      }

      &.nut-carbs {
        border-left: 3px solid #22c55e;
        .nut-card-label { color: #16a34a; }
      }

      &.nut-fat {
        border-left: 3px solid #eab308;
        .nut-card-label { color: #ca8a04; }
      }

      &.nut-fiber {
        border-left: 3px solid #a855f7;
        .nut-card-label { color: #9333ea; }
      }

      &.nut-portion {
        border-left: 3px solid #64748b;
        .nut-card-label { color: #475569; }
      }
    }
  }

  :deep(.el-form-item__label) {
    font-weight: 600;
    color: #374151;
  }

  :deep(.el-input__wrapper),
  :deep(.el-textarea__inner) {
    border-radius: 10px;
    transition: all 0.25s ease;

    &:focus-within,
    &.is-focus {
      box-shadow: 0 0 0 2px rgba(99, 102, 241, 0.2);
    }
  }
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;

  .cancel-btn {
    border-radius: 10px;
    padding: 10px 24px;
    font-weight: 500;
    border-color: #e2e8f0;
    color: #64748b;

    &:hover {
      border-color: #cbd5e1;
      color: #475569;
      background: #f8fafc;
    }
  }

  .submit-btn {
    border-radius: 10px;
    padding: 10px 32px;
    font-weight: 700;
    background: linear-gradient(135deg, #6366f1, #8b5cf6);
    border: none;
    box-shadow: 0 4px 14px rgba(99, 102, 241, 0.35);
    transition: all 0.3s ease;

    &:hover:not(:disabled) {
      transform: translateY(-1px);
      box-shadow: 0 6px 20px rgba(99, 102, 241, 0.45);
      background: linear-gradient(135deg, #818cf8, #a78bfa);
    }
  }
}
</style>

<style lang="scss">
.add-record-dialog {
  .el-dialog {
    border-radius: 20px;
    overflow: hidden;
    box-shadow: 0 25px 60px rgba(0, 0, 0, 0.15);
  }

  .el-dialog__header {
    padding: 24px 28px 18px;
    margin-right: 0;
    border-bottom: 1px solid #f1f5f9;
    background: linear-gradient(135deg, #fafbff, #f5f3ff);
  }

  .el-dialog__headerbtn {
    top: 24px;
    right: 24px;
  }

  .dialog-header {
    display: flex;
    align-items: center;
    gap: 10px;

    .dialog-icon {
      font-size: 22px;
    }

    .dialog-title {
      font-size: 19px;
      font-weight: 700;
      color: #1e293b;
    }
  }

  .el-dialog__body {
    padding: 28px;
    max-height: 65vh;
    overflow-y: auto;
  }

  .el-dialog__footer {
    padding: 18px 28px;
    border-top: 1px solid #f1f5f9;
    background: #fafbfc;
  }
}
</style>
