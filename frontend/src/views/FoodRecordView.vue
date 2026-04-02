<template>
  <div class="food-record-container">
    <div class="food-record-layout">
      <!-- 顶部导航栏 -->
      <div class="page-header">
        <button class="back-btn" @click="goToHome">
          <el-icon><ArrowLeft /></el-icon>
          <span>返回首页</span>
        </button>
        <div class="page-title">
          <span class="title-icon">🍽️</span>
          <h1>饮食记录</h1>
        </div>
      </div>

      <!-- 头部统计卡片 -->
      <div class="stats-section">
        <NutritionStats :key="statsKey" :date="selectedDate" @date-change="handleDateChange" />
      </div>

      <!-- 主内容区 -->
      <div class="content-section">
        <el-card class="content-card">
          <template #header>
            <div class="card-header">
              <div class="header-left">
                <span class="header-icon">📋</span>
                <span class="title">饮食记录</span>
              </div>
              <el-button class="add-btn" type="primary" @click="showAddDialog = true">
                <el-icon><Plus /></el-icon>
                添加记录
              </el-button>
            </div>
            <!-- 筛选栏 -->
            <div class="filter-bar">
              <div class="filter-item">
                <el-date-picker
                  v-model="dateRange"
                  type="daterange"
                  range-separator="至"
                  start-placeholder="开始日期"
                  end-placeholder="结束日期"
                  format="YYYY-MM-DD"
                  value-format="YYYY-MM-DD"
                  @change="handleFilterChange"
                />
              </div>
              <div class="filter-item">
                <el-select
                  v-model="filterMealType"
                  placeholder="餐次类型"
                  clearable
                  @change="handleFilterChange"
                >
                  <el-option
                    v-for="type in mealTypeList"
                    :key="type.value"
                    :label="type.label"
                    :value="type.value"
                  />
                </el-select>
              </div>
            </div>
          </template>

          <!-- 饮食记录列表 -->
          <FoodRecordList
            :records="records"
            :loading="loading"
            @delete="handleDelete"
            @view="handleView"
          />

          <!-- 分页 -->
          <div v-if="total > 0" class="pagination-wrap">
            <el-pagination
              v-model:current-page="currentPage"
              v-model:page-size="pageSize"
              :total="total"
              :page-sizes="[10, 20, 50, 100]"
              layout="total, sizes, prev, pager, next, jumper"
              @current-change="fetchRecords"
              @size-change="fetchRecords"
            />
          </div>
        </el-card>
      </div>
    </div>

    <!-- 添加记录对话框 -->
    <AddFoodRecordDialog v-model="showAddDialog" @success="handleAddSuccess" />

    <!-- 记录详情对话框 -->
    <FoodRecordDetailDialog v-model="showDetailDialog" :record="selectedRecord" />
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, ArrowLeft } from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus'
import { getFoodRecords, deleteFoodRecord, getMealTypeList } from '@/services/foodRecord'

// 路由
const router = useRouter()
import message from '@/utils/message'
import NutritionStats from '@/components/food/NutritionStats.vue'
import FoodRecordList from '@/components/food/FoodRecordList.vue'
import AddFoodRecordDialog from '@/components/food/AddFoodRecordDialog.vue'
import FoodRecordDetailDialog from '@/components/food/FoodRecordDetailDialog.vue'

// 数据
const loading = ref(false)
const records = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const statsKey = ref(0)

// 格式化日期
const formatDate = date => {
  if (!date) return ''
  const d = new Date(date)
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

// 筛选 — 默认过滤为今天（与统计组件保持同步）
const today = formatDate(new Date())
const selectedDate = ref(new Date())
const dateRange = ref([today, today])
const filterMealType = ref('')
const mealTypeList = getMealTypeList()

// 对话框
const showAddDialog = ref(false)
const showDetailDialog = ref(false)
const selectedRecord = ref(null)

// 获取饮食记录列表
const fetchRecords = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value - 1,
      size: pageSize.value
    }

    // 添加日期范围筛选
    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }

    // 添加餐次类型筛选
    if (filterMealType.value) {
      params.mealType = filterMealType.value
    }

    const response = await getFoodRecords(params)
    if (response.data.code === 200) {
      const data = response.data.data
      if (data && data.content) {
        records.value = data.content
        total.value = data.totalElements || 0
      } else {
        records.value = []
        total.value = 0
      }
      console.log('获取到的记录数:', records.value.length)
    } else {
      message.error(response.data.message || '获取饮食记录失败')
    }
  } catch (error) {
    console.error('获取饮食记录失败:', error)
    message.error('获取饮食记录失败：' + (error.message || '网络错误'))
  } finally {
    loading.value = false
  }
}

// 返回首页
const goToHome = () => {
  router.push('/')
}

// 日期变化（来自NutritionStats组件）
const handleDateChange = date => {
  console.log('日期变化:', date, typeof date)
  // 确保date是Date对象
  if (typeof date === 'string') {
    selectedDate.value = new Date(date)
  } else {
    selectedDate.value = date
  }
  // Sync records list filter with the selected stats date
  const dateStr = typeof date === 'string' ? date : formatDate(date)
  dateRange.value = [dateStr, dateStr]
  currentPage.value = 1
  fetchRecords()
}

// 筛选变化
const handleFilterChange = () => {
  currentPage.value = 1
  fetchRecords()
}

// 添加成功
const handleAddSuccess = () => {
  console.log('添加成功，刷新数据')
  fetchRecords()
  // 通过递增key强制NutritionStats组件重新挂载并刷新数据
  statsKey.value++
}

// 查看详情
const handleView = record => {
  selectedRecord.value = record
  showDetailDialog.value = true
}

// 删除记录
const handleDelete = async record => {
  try {
    await ElMessageBox.confirm(
      `确定要删除"${record.foodName}"这条记录吗？删除后将无法恢复。`,
      '确认删除',
      {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'warning',
        customClass: 'food-delete-confirm',
        dangerouslyUseHTMLString: false,
        showClose: true,
        closeOnClickModal: false,
        closeOnPressEscape: true,
        buttonSize: 'default'
      }
    )

    const response = await deleteFoodRecord(record.id)
    if (response.data.code === 200) {
      message.success('删除成功')
      fetchRecords()
      // 通过递增key强制NutritionStats组件重新挂载并刷新数据
      statsKey.value++
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      message.error('删除失败')
    }
  }
}

onMounted(() => {
  fetchRecords()
})

// 组件卸载前清理
onBeforeUnmount(() => {
  // 清理资源
  records.value = []
})
</script>

<style scoped lang="scss">
.food-record-container {
  min-height: 100vh;
  background: linear-gradient(160deg, #0f172a 0%, #1e293b 50%, #334155 100%);
  padding: 32px 20px 60px;
  position: relative;

  &::before {
    content: '';
    position: absolute;
    top: -120px;
    right: -80px;
    width: 400px;
    height: 400px;
    background: radial-gradient(circle, rgba(99, 102, 241, 0.15) 0%, transparent 70%);
    border-radius: 50%;
    pointer-events: none;
  }

  &::after {
    content: '';
    position: absolute;
    bottom: -60px;
    left: -100px;
    width: 350px;
    height: 350px;
    background: radial-gradient(circle, rgba(168, 85, 247, 0.1) 0%, transparent 70%);
    border-radius: 50%;
    pointer-events: none;
  }
}

.food-record-layout {
  max-width: 1400px;
  margin: 0 auto;
  position: relative;
  z-index: 1;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 28px;

  .back-btn {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 10px 20px;
    border-radius: 50px;
    border: 1px solid rgba(255, 255, 255, 0.12);
    background: rgba(255, 255, 255, 0.06);
    backdrop-filter: blur(12px);
    -webkit-backdrop-filter: blur(12px);
    color: rgba(255, 255, 255, 0.85);
    font-size: 14px;
    cursor: pointer;
    transition: all 0.3s ease;

    &:hover {
      background: rgba(255, 255, 255, 0.12);
      border-color: rgba(255, 255, 255, 0.2);
      color: #fff;
      transform: translateX(-2px);
    }

    .el-icon {
      font-size: 16px;
    }
  }

  .page-title {
    display: flex;
    align-items: center;
    gap: 12px;

    .title-icon {
      font-size: 28px;
    }

    h1 {
      margin: 0;
      font-size: 26px;
      font-weight: 700;
      background: linear-gradient(135deg, #e0e7ff, #c4b5fd);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
      letter-spacing: 0.5px;
    }
  }
}

.stats-section {
  margin-bottom: 28px;
}

.content-section {
  .content-card {
    border-radius: 20px;
    border: 1px solid rgba(255, 255, 255, 0.08);
    background: rgba(30, 41, 59, 0.7);
    backdrop-filter: blur(20px);
    -webkit-backdrop-filter: blur(20px);
    box-shadow:
      0 8px 32px rgba(0, 0, 0, 0.3),
      inset 0 1px 0 rgba(255, 255, 255, 0.05);
    overflow: hidden;

    :deep(.el-card__header) {
      padding: 24px 28px 20px;
      border-bottom: 1px solid rgba(255, 255, 255, 0.06);
      background: rgba(255, 255, 255, 0.02);
    }

    :deep(.el-card__body) {
      padding: 24px 28px;
      background: transparent;
    }
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18px;

  .header-left {
    display: flex;
    align-items: center;
    gap: 10px;

    .header-icon {
      font-size: 22px;
    }
  }

  .title {
    font-size: 20px;
    font-weight: 700;
    color: rgba(255, 255, 255, 0.95);
    letter-spacing: 0.3px;
  }

  .add-btn {
    border-radius: 50px;
    padding: 10px 24px;
    font-weight: 600;
    background: linear-gradient(135deg, #6366f1, #8b5cf6);
    border: none;
    box-shadow: 0 4px 15px rgba(99, 102, 241, 0.4);
    transition: all 0.3s ease;

    &:hover {
      transform: translateY(-1px);
      box-shadow: 0 6px 20px rgba(99, 102, 241, 0.5);
      background: linear-gradient(135deg, #818cf8, #a78bfa);
    }
  }
}

.filter-bar {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;

  .filter-item {
    :deep(.el-date-editor),
    :deep(.el-select) {
      width: 260px;
    }

    :deep(.el-input__wrapper) {
      border-radius: 12px;
      background: rgba(255, 255, 255, 0.06);
      border: 1px solid rgba(255, 255, 255, 0.1);
      box-shadow: none;
      transition: all 0.3s ease;

      &:hover,
      &.is-focus {
        border-color: rgba(99, 102, 241, 0.5);
        background: rgba(255, 255, 255, 0.08);
      }

      .el-input__inner {
        color: rgba(255, 255, 255, 0.85);

        &::placeholder {
          color: rgba(255, 255, 255, 0.35);
        }
      }

      .el-input__icon,
      .el-range-separator,
      .el-range-input {
        color: rgba(255, 255, 255, 0.5);
      }

      .el-range-input {
        background: transparent;
        color: rgba(255, 255, 255, 0.85);

        &::placeholder {
          color: rgba(255, 255, 255, 0.35);
        }
      }
    }

    :deep(.el-select) {
      .el-select__suffix .el-icon {
        color: rgba(255, 255, 255, 0.5);
      }
    }
  }
}

.pagination-wrap {
  margin-top: 28px;
  display: flex;
  justify-content: center;

  :deep(.el-pagination) {
    --el-pagination-bg-color: rgba(255, 255, 255, 0.06);
    --el-pagination-text-color: rgba(255, 255, 255, 0.7);
    --el-pagination-button-color: rgba(255, 255, 255, 0.7);
    --el-pagination-hover-color: #818cf8;
    --el-pagination-button-bg-color: rgba(255, 255, 255, 0.06);
    --el-pagination-button-disabled-color: rgba(255, 255, 255, 0.25);
    --el-pagination-button-disabled-bg-color: rgba(255, 255, 255, 0.03);

    .el-pager li {
      border-radius: 8px;
      margin: 0 3px;
      background: rgba(255, 255, 255, 0.06);
      color: rgba(255, 255, 255, 0.7);

      &.is-active {
        background: linear-gradient(135deg, #6366f1, #8b5cf6);
        color: #fff;
        font-weight: 600;
      }
    }

    .btn-prev,
    .btn-next {
      border-radius: 8px;
      background: rgba(255, 255, 255, 0.06);
    }

    .el-pagination__total,
    .el-pagination__jump {
      color: rgba(255, 255, 255, 0.6);
    }

    .el-input__wrapper {
      border-radius: 8px;
      background: rgba(255, 255, 255, 0.06);
      box-shadow: none;
      border: 1px solid rgba(255, 255, 255, 0.1);

      .el-input__inner {
        color: rgba(255, 255, 255, 0.85);
      }
    }

    .el-select {
      .el-input__wrapper {
        border-radius: 8px;
        background: rgba(255, 255, 255, 0.06);
        box-shadow: none;
        border: 1px solid rgba(255, 255, 255, 0.1);
      }
    }
  }
}

@media (max-width: 768px) {
  .food-record-container {
    padding: 20px 12px 40px;
  }

  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .filter-bar {
    flex-direction: column;

    .filter-item {
      :deep(.el-date-editor),
      :deep(.el-select) {
        width: 100%;
      }
    }
  }
}
</style>

<style lang="scss">
// 删除确认框样式（全局 - 简洁风格）
.el-message-box.food-delete-confirm {
  width: 440px !important;
  max-width: 90vw !important;
  border-radius: 8px !important;
  padding: 0 !important;
  background: #ffffff !important;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12) !important;
  overflow: hidden !important;

  .el-message-box__header {
    position: relative !important;
    padding: 20px 50px 16px 20px !important;
    background: #ffffff !important;
    border-bottom: none !important;
    display: flex !important;
    align-items: center !important;
  }

  .el-message-box__title {
    font-size: 18px !important;
    font-weight: 600 !important;
    color: #1f2937 !important;
    flex: 1 !important;
    line-height: 1.4 !important;
  }

  .el-message-box__headerbtn {
    position: absolute !important;
    top: 20px !important;
    right: 20px !important;
    width: 20px !important;
    height: 20px !important;
    padding: 0 !important;
    margin: 0 !important;

    .el-message-box__close {
      color: rgba(0, 0, 0, 0.45) !important;
      font-size: 16px !important;
      width: 20px !important;
      height: 20px !important;
      line-height: 20px !important;

      &:hover {
        color: rgba(0, 0, 0, 0.75) !important;
      }
    }
  }

  .el-message-box__content {
    padding: 8px 20px 20px !important;
    background: #ffffff !important;
  }

  .el-message-box__container {
    display: flex !important;
    align-items: flex-start !important;

    .el-message-box__status {
      font-size: 20px !important;
      margin-top: 0 !important;
      flex-shrink: 0 !important;

      &.el-message-box-icon--warning {
        color: #f59e0b !important;
      }
    }
  }

  .el-message-box__message {
    color: #4b5563 !important;
    font-size: 14px !important;
    line-height: 1.6 !important;
    padding-left: 4px !important;

    p {
      margin: 0 !important;
      line-height: 1.6 !important;
    }
  }

  .el-message-box__btns {
    padding: 0 20px 20px !important;
    background: #ffffff !important;
    display: flex !important;
    justify-content: flex-end !important;
    gap: 12px !important;
    border-top: none !important;

    .el-button {
      margin: 0 !important;
      padding: 10px 24px !important;
      border-radius: 6px !important;
      font-size: 14px !important;
      font-weight: 500 !important;
      transition: all 0.2s ease !important;
      min-width: 90px !important;

      &.el-button--primary {
        background: #ef4444 !important;
        border-color: #ef4444 !important;
        color: #ffffff !important;
        box-shadow: 0 2px 4px rgba(239, 68, 68, 0.2) !important;

        &:hover {
          background: #dc2626 !important;
          border-color: #dc2626 !important;
          box-shadow: 0 4px 8px rgba(239, 68, 68, 0.3) !important;
        }

        &:active {
          background: #b91c1c !important;
          border-color: #b91c1c !important;
        }
      }

      &.el-button--default {
        background: #ffffff !important;
        border-color: #d1d5db !important;
        color: #6b7280 !important;

        &:hover {
          color: #374151 !important;
          border-color: #9ca3af !important;
          background: #f9fafb !important;
        }
      }
    }
  }
}

// 确保MessageBox居中显示
.el-overlay .el-message-box {
  position: fixed !important;
  top: 50% !important;
  left: 50% !important;
  transform: translate(-50%, -50%) !important;
}
</style>
