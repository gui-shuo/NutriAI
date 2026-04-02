<template>
  <div class="food-record-list">
    <el-skeleton :loading="loading" animated :rows="5">
      <div v-if="records.length > 0" class="record-items">
        <div
          v-for="record in records"
          :key="record.id"
          class="record-item"
          @click="$emit('view', record)"
        >
          <!-- 左侧照片 -->
          <div class="record-photo">
            <el-image v-if="record.photoUrl" :src="record.photoUrl" fit="cover" class="photo-image">
              <template #error>
                <div class="image-slot">
                  <el-icon><Picture /></el-icon>
                </div>
              </template>
            </el-image>
            <div v-else class="photo-placeholder">
              <el-icon><Food /></el-icon>
            </div>
            <div class="photo-overlay"></div>
          </div>

          <!-- 中间信息 -->
          <div class="record-info">
            <div class="info-header">
              <h3 class="food-name">
                {{ record.foodName }}
              </h3>
              <el-tag :type="getMealTypeTagType(record.mealType)" size="small" class="meal-badge">
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
              <span class="detail-item"> 蛋白质 {{ record.protein || 0 }}g </span>
            </div>

            <div v-if="record.notes" class="info-notes">
              <el-icon><Document /></el-icon>
              {{ record.notes }}
            </div>
          </div>

          <!-- 右侧卡路里 + 操作 -->
          <div class="record-right">
            <div class="calorie-display">
              <span class="cal-num">{{ record.calories || 0 }}</span>
              <span class="cal-unit">kcal</span>
            </div>
            <div class="record-actions" @click.stop>
              <el-button type="danger" size="small" text @click="$emit('delete', record)">
                <el-icon><Delete /></el-icon>
                删除
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <div v-else class="empty-state">
        <div class="empty-icon">🍽️</div>
        <el-empty description="暂无饮食记录" />
      </div>
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
const getMealTypeTagType = mealType => {
  const types = {
    BREAKFAST: 'warning',
    LUNCH: 'success',
    DINNER: 'primary',
    SNACK: ''
  }
  return types[mealType] || ''
}

// 格式化时间
const formatTime = timeStr => {
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
  :deep(.el-skeleton) {
    --el-skeleton-color: rgba(255, 255, 255, 0.06);
    --el-skeleton-to-color: rgba(255, 255, 255, 0.12);
  }

  .record-items {
    display: flex;
    flex-direction: column;
    gap: 14px;
  }

  .record-item {
    display: flex;
    gap: 18px;
    padding: 18px;
    background: rgba(255, 255, 255, 0.04);
    border-radius: 16px;
    border: 1px solid rgba(255, 255, 255, 0.06);
    cursor: pointer;
    transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);

    &:hover {
      background: rgba(255, 255, 255, 0.07);
      border-color: rgba(99, 102, 241, 0.3);
      transform: translateY(-3px);
      box-shadow:
        0 12px 32px rgba(0, 0, 0, 0.2),
        0 0 0 1px rgba(99, 102, 241, 0.15);
    }

    .record-photo {
      flex-shrink: 0;
      width: 90px;
      height: 90px;
      border-radius: 14px;
      overflow: hidden;
      position: relative;

      .photo-image {
        width: 100%;
        height: 100%;
      }

      .photo-overlay {
        position: absolute;
        bottom: 0;
        left: 0;
        right: 0;
        height: 40%;
        background: linear-gradient(transparent, rgba(0, 0, 0, 0.3));
        pointer-events: none;
        border-radius: 0 0 14px 14px;
      }

      .photo-placeholder,
      .image-slot {
        width: 100%;
        height: 100%;
        display: flex;
        align-items: center;
        justify-content: center;
        background: rgba(99, 102, 241, 0.1);
        color: rgba(165, 180, 252, 0.6);

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
      min-width: 0;

      .info-header {
        display: flex;
        align-items: center;
        gap: 10px;

        .food-name {
          font-size: 17px;
          font-weight: 700;
          color: rgba(255, 255, 255, 0.95);
          margin: 0;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }

        .meal-badge {
          border-radius: 20px;
          font-weight: 600;
          font-size: 11px;
          flex-shrink: 0;
        }
      }

      .info-details {
        display: flex;
        gap: 16px;
        color: rgba(255, 255, 255, 0.5);
        font-size: 13px;
        flex-wrap: wrap;

        .detail-item {
          display: flex;
          align-items: center;
          gap: 4px;

          .el-icon {
            font-size: 14px;
            opacity: 0.7;
          }
        }
      }

      .info-notes {
        display: flex;
        align-items: center;
        gap: 6px;
        color: rgba(255, 255, 255, 0.55);
        font-size: 13px;
        padding: 8px 12px;
        background: rgba(255, 255, 255, 0.04);
        border-radius: 8px;
        border: 1px solid rgba(255, 255, 255, 0.05);

        .el-icon {
          flex-shrink: 0;
          opacity: 0.5;
        }
      }
    }

    .record-right {
      display: flex;
      flex-direction: column;
      align-items: flex-end;
      justify-content: space-between;
      flex-shrink: 0;

      .calorie-display {
        text-align: right;

        .cal-num {
          font-size: 28px;
          font-weight: 800;
          background: linear-gradient(135deg, #818cf8, #c084fc);
          -webkit-background-clip: text;
          -webkit-text-fill-color: transparent;
          background-clip: text;
          line-height: 1;
        }

        .cal-unit {
          display: block;
          font-size: 12px;
          color: rgba(255, 255, 255, 0.4);
          font-weight: 500;
          margin-top: 2px;
        }
      }

      .record-actions {
        :deep(.el-button) {
          color: rgba(248, 113, 113, 0.7);
          font-size: 12px;
          border-radius: 8px;
          padding: 4px 10px;
          transition: all 0.2s ease;

          &:hover {
            color: #f87171;
            background: rgba(248, 113, 113, 0.1);
          }
        }
      }
    }
  }

  .empty-state {
    text-align: center;
    padding: 40px 0;

    .empty-icon {
      font-size: 48px;
      margin-bottom: 8px;
      opacity: 0.5;
    }

    :deep(.el-empty__description p) {
      color: rgba(255, 255, 255, 0.45);
    }

    :deep(.el-empty__image) {
      opacity: 0.3;
    }
  }
}

@media (max-width: 768px) {
  .food-record-list .record-item {
    flex-direction: column;

    .record-photo {
      width: 100%;
      height: 180px;
    }

    .record-right {
      flex-direction: row;
      align-items: center;
    }
  }
}
</style>
