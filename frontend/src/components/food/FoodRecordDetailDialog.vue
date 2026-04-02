<template>
  <el-dialog v-model="visible" title="饮食记录详情" width="600px" @close="handleClose">
    <div v-if="record" class="record-detail">
      <!-- 食物照片 -->
      <div v-if="record.photoUrl" class="detail-photo">
        <el-image :src="record.photoUrl" fit="cover" class="photo-image">
          <template #error>
            <div class="image-slot">
              <el-icon><Picture /></el-icon>
            </div>
          </template>
        </el-image>
      </div>

      <!-- 基本信息 -->
      <div class="detail-section">
        <h3 class="section-title">基本信息</h3>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="食物名称">
            {{ record.foodName }}
          </el-descriptions-item>
          <el-descriptions-item label="餐次">
            <el-tag :type="getMealTypeTagType(record.mealType)">
              {{ record.mealTypeName }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="记录时间" :span="2">
            {{ formatDateTime(record.recordTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="份量"> {{ record.portion || '-' }} 克 </el-descriptions-item>
          <el-descriptions-item label="卡路里">
            {{ record.calories || '-' }} 千卡
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <!-- 营养成分 -->
      <div class="detail-section">
        <h3 class="section-title">营养成分</h3>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="蛋白质">
            {{ record.protein || '-' }} 克
          </el-descriptions-item>
          <el-descriptions-item label="碳水化合物">
            {{ record.carbohydrates || '-' }} 克
          </el-descriptions-item>
          <el-descriptions-item label="脂肪"> {{ record.fat || '-' }} 克 </el-descriptions-item>
          <el-descriptions-item label="膳食纤维">
            {{ record.fiber || '-' }} 克
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <!-- 备注 -->
      <div v-if="record.notes" class="detail-section">
        <h3 class="section-title">备注</h3>
        <p class="notes-content">
          {{ record.notes }}
        </p>
      </div>
    </div>

    <template #footer>
      <el-button @click="handleClose"> 关闭 </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import { Picture } from '@element-plus/icons-vue'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  record: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['update:modelValue'])

const visible = ref(false)

watch(
  () => props.modelValue,
  val => {
    visible.value = val
  }
)

watch(visible, val => {
  emit('update:modelValue', val)
})

// 获取餐次类型标签类型
const getMealTypeTagType = mealType => {
  const types = {
    BREAKFAST: 'warning',
    LUNCH: 'success',
    DINNER: 'primary',
    SNACK: ''
  }
  return types[mealType] || ''
}

// 格式化日期时间
const formatDateTime = timeStr => {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const handleClose = () => {
  visible.value = false
}
</script>

<style scoped lang="scss">
.record-detail {
  .detail-photo {
    width: 100%;
    height: 300px;
    border-radius: 255px 15px 225px 15px / 15px 225px 15px 255px;
    border: 2px solid #2d2d2d;
    overflow: hidden;
    margin-bottom: 24px;

    .photo-image {
      width: 100%;
      height: 100%;
    }

    .image-slot {
      width: 100%;
      height: 100%;
      display: flex;
      align-items: center;
      justify-content: center;
      background: #fdfbf7;
      color: #e5e0d8;

      .el-icon {
        font-size: 48px;
      }
    }
  }

  .detail-section {
    margin-bottom: 24px;

    .section-title {
      font-size: 16px;
      font-weight: 600;
      color: #2d2d2d;
      font-family: 'Kalam', 'ZCOOL KuaiLe', cursive;
      margin: 0 0 12px 0;
    }

    .notes-content {
      padding: 12px;
      background: #fdfbf7;
      border: 1px dashed #2d2d2d;
      border-radius: 15px 225px 15px 255px / 255px 15px 225px 15px;
      color: #2d2d2d;
      line-height: 1.6;
      margin: 0;
    }
  }
}
</style>
