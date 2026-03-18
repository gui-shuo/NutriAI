<template>
  <el-dialog
    v-model="visible"
    title="添加饮食记录"
    width="700px"
    @close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-width="110px"
      class="record-form"
    >
      <!-- 餐次类型 -->
      <el-form-item label="餐次类型" prop="mealType">
        <el-radio-group v-model="formData.mealType">
          <el-radio-button
            v-for="type in mealTypeList"
            :key="type.value"
            :value="type.value"
          >
            {{ type.label }}
          </el-radio-button>
        </el-radio-group>
      </el-form-item>

      <!-- 食物名称 -->
      <el-form-item label="食物名称" prop="foodName">
        <el-input
          v-model="formData.foodName"
          placeholder="请输入食物名称"
          maxlength="100"
          show-word-limit
        />
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
        <FoodPhotoUpload v-model="formData.photoUrl" />
      </el-form-item>

      <!-- 营养信息 -->
      <div class="nutrition-section">
        <h3 class="section-title">营养信息</h3>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="份量(克)" prop="portion">
              <el-input-number
                v-model="formData.portion"
                :min="0"
                :precision="2"
                :step="10"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="卡路里(千卡)" prop="calories">
              <el-input-number
                v-model="formData.calories"
                :min="0"
                :precision="2"
                :step="10"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="蛋白质(克)">
              <el-input-number
                v-model="formData.protein"
                :min="0"
                :precision="2"
                :step="1"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="碳水(克)">
              <el-input-number
                v-model="formData.carbohydrates"
                :min="0"
                :precision="2"
                :step="1"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="脂肪(克)">
              <el-input-number
                v-model="formData.fat"
                :min="0"
                :precision="2"
                :step="1"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="膳食纤维(克)">
              <el-input-number
                v-model="formData.fiber"
                :min="0"
                :precision="2"
                :step="1"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
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
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" :loading="loading" @click="handleSubmit">
        保存
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, watch } from 'vue'
import { getMealTypeList, createFoodRecord } from '@/services/foodRecord'
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
const visible = ref(false)
const mealTypeList = getMealTypeList()

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
  recordTime: new Date().toISOString().slice(0, 19),
  notes: ''
})

const rules = {
  mealType: [
    { required: true, message: '请选择餐次类型', trigger: 'change' }
  ],
  foodName: [
    { required: true, message: '请输入食物名称', trigger: 'blur' },
    { max: 100, message: '食物名称最多100个字符', trigger: 'blur' }
  ],
  recordTime: [
    { required: true, message: '请选择记录时间', trigger: 'change' }
  ]
}

watch(() => props.modelValue, (val) => {
  visible.value = val
})

watch(visible, (val) => {
  emit('update:modelValue', val)
})

const handleSubmit = async () => {
  try {
    await formRef.value.validate()

    loading.value = true
    const response = await createFoodRecord(formData)

    if (response.data.code === 200) {
      message.success('添加成功')
      emit('success')
      handleClose()
    }
  } catch (error) {
    if (error !== false) {
      console.error('添加失败:', error)
      message.error('添加失败')
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
    recordTime: new Date().toISOString().slice(0, 19),
    notes: ''
  })
  visible.value = false
}
</script>

<style scoped lang="scss">
.record-form {
  .nutrition-section {
    border: 2px solid #f0f0f0;
    border-radius: 8px;
    padding: 20px;
    margin-bottom: 20px;

    .section-title {
      font-size: 16px;
      font-weight: 600;
      color: #303133;
      margin: 0 0 16px 0;
    }

    .el-form-item {
      margin-bottom: 16px;
    }
  }
}
</style>
