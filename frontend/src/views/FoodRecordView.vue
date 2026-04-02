<template>
  <div class="food-record-container">
    <div class="food-record-layout">
      <!-- 顶部导航栏 -->
      <div class="page-topbar">
        <el-button :icon="ArrowLeft" text @click="goToHome">返回首页</el-button>
        <div class="topbar-title">
          <h2>📋 饮食记录</h2>
          <p>记录每日饮食，追踪营养摄入</p>
        </div>
      </div>

      <!-- 两栏布局：左侧统计 + 右侧记录 -->
      <div class="main-grid">
        <!-- 左栏：营养统计 -->
        <aside class="stats-aside">
          <NutritionStats :key="statsKey" :date="selectedDate" @date-change="handleDateChange" />
        </aside>

        <!-- 右栏：筛选 + 列表 -->
        <section class="records-main">
          <!-- 工具栏：筛选 + 添加 -->
          <div class="toolbar">
            <div class="toolbar-filters">
              <el-date-picker
                v-model="dateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                size="default"
                @change="handleFilterChange"
              />
              <el-select
                v-model="filterMealType"
                placeholder="餐次类型"
                clearable
                size="default"
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
            <el-button type="primary" @click="showAddDialog = true">
              <el-icon><Plus /></el-icon>
              添加记录
            </el-button>
          </div>

          <!-- 记录列表 -->
          <FoodRecordList
            :records="records"
            :loading="loading"
            @delete="handleDelete"
            @view="handleView"
          />

          <!-- 分页 -->
          <el-pagination
            v-if="total > 0"
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :total="total"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            class="list-pagination"
            @current-change="fetchRecords"
            @size-change="fetchRecords"
          />
        </section>
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
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 0 24px 40px;
}

.food-record-layout {
  max-width: 1400px;
  margin: 0 auto;
}

/* 顶部导航栏 */
.page-topbar {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.15);
  margin-bottom: 24px;

  :deep(.el-button) {
    color: rgba(255, 255, 255, 0.85);
    &:hover { color: #fff; }
  }
}

.topbar-title h2 {
  font-size: 24px;
  margin: 0;
  color: #fff;
}

.topbar-title p {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
  margin: 2px 0 0 0;
}

/* 两栏主布局 */
.main-grid {
  display: grid;
  grid-template-columns: 380px 1fr;
  gap: 24px;
  align-items: start;
}

.stats-aside {
  position: sticky;
  top: 24px;
}

/* 工具栏 */
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
  padding: 16px 20px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.toolbar-filters {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.list-pagination {
  margin-top: 24px;
  display: flex;
  justify-content: center;
}

@media (max-width: 1024px) {
  .main-grid {
    grid-template-columns: 1fr;
  }

  .stats-aside {
    position: static;
  }
}

@media (max-width: 768px) {
  .food-record-container {
    padding: 0 12px 24px;
  }

  .page-topbar {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .toolbar-filters {
    flex-direction: column;
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
