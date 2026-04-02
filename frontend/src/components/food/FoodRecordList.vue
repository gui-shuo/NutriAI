<template>
  <div class="food-record-list">
    <el-skeleton :loading="loading" animated :rows="5">
      <div v-if="records.length > 0" class="record-grid">
        <el-card
          v-for="record in records"
          :key="record.id"
          class="record-card"
          shadow="hover"
          @click="$emit('view', record)"
        >
          <!-- 卡片顶部：照片区 -->
          <div class="card-photo">
            <el-image v-if="record.photoUrl" :src="record.photoUrl" fit="cover" class="photo-img">
              <template #error>
                <div class="photo-fallback"><el-icon :size="28"><Picture /></el-icon></div>
              </template>
            </el-image>
            <div v-else class="photo-fallback"><el-icon :size="28"><Food /></el-icon></div>
            <el-tag class="meal-badge" :type="getMealTypeTagType(record.mealType)" size="small" effect="dark">
              {{ record.mealTypeName }}
            </el-tag>
          </div>

          <!-- 卡片内容 -->
          <div class="card-body">
            <h3 class="card-food-name">{{ record.foodName }}</h3>

            <div class="card-stats">
              <span class="stat-item stat-cal">🔥 {{ record.calories || 0 }} kcal</span>
              <span class="stat-item">🥩 {{ record.protein || 0 }}g</span>
            </div>

            <div class="card-meta">
              <span class="meta-time">
                <el-icon><Timer /></el-icon>
                {{ formatTime(record.recordTime) }}
              </span>
              <el-button
                type="danger"
                size="small"
                text
                @click.stop="$emit('delete', record)"
              >
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>

            <div v-if="record.notes" class="card-notes">{{ record.notes }}</div>
          </div>
        </el-card>
      </div>

      <el-empty v-else description="暂无饮食记录" />
    </el-skeleton>
  </div>
</template>

<script setup>
import { Picture, Food, Timer, Delete } from '@element-plus/icons-vue'

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
  .record-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    gap: 16px;
  }

  .record-card {
    border-radius: 12px;
    overflow: hidden;
    cursor: pointer;
    transition: all 0.3s;

    &:hover {
      transform: translateY(-4px);
      box-shadow: 0 8px 24px rgba(102, 126, 234, 0.18);
    }

    :deep(.el-card__body) {
      padding: 0;
    }
  }

  .card-photo {
    position: relative;
    width: 100%;
    height: 140px;
    background: #f5f7fa;

    .photo-img {
      width: 100%;
      height: 100%;
    }

    .photo-fallback {
      width: 100%;
      height: 100%;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #c0c4cc;
      background: linear-gradient(135deg, #f5f7fa 0%, #e8ecf1 100%);
    }

    .meal-badge {
      position: absolute;
      top: 8px;
      right: 8px;
    }
  }

  .card-body {
    padding: 14px 16px;
  }

  .card-food-name {
    margin: 0 0 8px 0;
    font-size: 16px;
    font-weight: 600;
    color: #303133;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .card-stats {
    display: flex;
    gap: 12px;
    margin-bottom: 10px;
  }

  .stat-item {
    font-size: 13px;
    color: #606266;
    padding: 3px 8px;
    background: #f5f7fa;
    border-radius: 4px;
  }

  .stat-cal {
    font-weight: 600;
    color: #e6a23c;
    background: #fdf6ec;
  }

  .card-meta {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .meta-time {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 12px;
    color: #909399;
  }

  .card-notes {
    margin-top: 8px;
    padding: 8px 10px;
    background: #f5f7fa;
    border-radius: 6px;
    font-size: 12px;
    color: #606266;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

@media (max-width: 768px) {
  .record-grid {
    grid-template-columns: 1fr !important;
  }
}
</style>
