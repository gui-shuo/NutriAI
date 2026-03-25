<template>
  <div class="food-record-list">
    <el-skeleton
      :loading="loading"
      animated
      :rows="5"
    >
      <div
        v-if="records.length > 0"
        class="record-items"
      >
        <div
          v-for="record in records"
          :key="record.id"
          class="record-item"
          @click="$emit('view', record)"
        >
          <!-- 左侧照片 -->
          <div class="record-photo">
            <el-image
              v-if="record.photoUrl"
              :src="record.photoUrl"
              fit="cover"
              class="photo-image"
            >
              <template #error>
                <div class="image-slot">
                  <el-icon><Picture /></el-icon>
                </div>
              </template>
            </el-image>
            <div
              v-else
              class="photo-placeholder"
            >
              <el-icon><Food /></el-icon>
            </div>
          </div>

          <!-- 中间信息 -->
          <div class="record-info">
            <div class="info-header">
              <h3 class="food-name">
                {{ record.foodName }}
              </h3>
              <el-tag
                :type="getMealTypeTagType(record.mealType)"
                size="small"
              >
                {{ record.mealTypeName }}
              </el-tag>
            </div>

            <div class="info-details">
              <span class="detail-item">
                <el-icon><Timer /></el-icon>
                {{ formatTime(record.recordTime) }}
              </span>
              <span class="detail-item">
                <el-icon><Odometer /></el-icon>
                {{ record.calories || 0 }} 千卡
              </span>
              <span class="detail-item">
                蛋白质 {{ record.protein || 0 }}g
              </span>
            </div>

            <div
              v-if="record.notes"
              class="info-notes"
            >
              <el-icon><Document /></el-icon>
              {{ record.notes }}
            </div>
          </div>

          <!-- 右侧操作 -->
          <div
            class="record-actions"
            @click.stop
          >
            <el-button
              type="danger"
              size="small"
              text
              @click="$emit('delete', record)"
            >
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </div>
        </div>
      </div>

      <el-empty
        v-else
        description="暂无饮食记录"
      />
    </el-skeleton>
  </div>
</template>

<script setup>
import { Picture, Food, Timer, Odometer, Document, Delete } from '@element-plus/icons-vue'

defineProps({
  records: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  }
})

defineEmits(['view', 'delete'])

// 获取餐次类型标签类型
const getMealTypeTagType = (mealType) => {
  const types = {
    BREAKFAST: 'warning',
    LUNCH: 'success',
    DINNER: 'primary',
    SNACK: ''
  }
  return types[mealType] || ''
}

// 格式化时间
const formatTime = (timeStr) => {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  return `${month}-${day} ${hour}:${minute}`
}
</script>

<style scoped lang="scss">
.food-record-list {
  .record-items {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  .record-item {
    display: flex;
    gap: 16px;
    padding: 20px;
    background: white;
    border-radius: 12px;
    border: 2px solid #f0f0f0;
    cursor: pointer;
    transition: all 0.3s;

    &:hover {
      border-color: #667eea;
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(102, 126, 234, 0.15);
    }

    .record-photo {
      flex-shrink: 0;
      width: 100px;
      height: 100px;
      border-radius: 8px;
      overflow: hidden;

      .photo-image {
        width: 100%;
        height: 100%;
      }

      .photo-placeholder,
      .image-slot {
        width: 100%;
        height: 100%;
        display: flex;
        align-items: center;
        justify-content: center;
        background: #f5f7fa;
        color: #909399;

        .el-icon {
          font-size: 32px;
        }
      }
    }

    .record-info {
      flex: 1;
      display: flex;
      flex-direction: column;
      gap: 8px;

      .info-header {
        display: flex;
        justify-content: space-between;
        align-items: center;

        .food-name {
          font-size: 18px;
          font-weight: 600;
          color: #303133;
          margin: 0;
        }
      }

      .info-details {
        display: flex;
        gap: 16px;
        color: #909399;
        font-size: 14px;

        .detail-item {
          display: flex;
          align-items: center;
          gap: 4px;
        }
      }

      .info-notes {
        display: flex;
        align-items: center;
        gap: 4px;
        color: #606266;
        font-size: 14px;
        padding: 8px 12px;
        background: #f5f7fa;
        border-radius: 6px;
      }
    }

    .record-actions {
      display: flex;
      flex-direction: column;
      justify-content: center;
    }
  }
}

@media (max-width: 768px) {
  .record-item {
    flex-direction: column !important;

    .record-photo {
      width: 100% !important;
      height: 200px !important;
    }
  }
}
</style>
