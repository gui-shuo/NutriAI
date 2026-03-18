<template>
  <div class="food-record-container">
    <div class="food-record-layout">
      <!-- 返回首页按钮 -->
      <div style="margin-bottom: 16px;">
        <el-button
          :icon="ArrowLeft"
          text
          @click="goToHome"
        >
          返回首页
        </el-button>
      </div>
      
      <!-- 头部统计卡片 -->
      <div class="stats-section">
        <NutritionStats
          :date="selectedDate"
          @date-change="handleDateChange"
        />
      </div>

      <!-- 主内容区 -->
      <div class="content-section">
        <el-card class="content-card">
          <template #header>
            <div class="card-header">
              <span class="title">饮食记录</span>
              <el-button
                type="primary"
                @click="showAddDialog = true"
              >
                <el-icon><Plus /></el-icon>
                添加记录
              </el-button>
            </div>
          </template>

          <!-- 筛选栏 -->
          <div class="filter-bar">
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

          <!-- 饮食记录列表 -->
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
            @current-change="fetchRecords"
            @size-change="fetchRecords"
          />
        </el-card>
      </div>
    </div>

    <!-- 添加记录对话框 -->
    <AddFoodRecordDialog
      v-model="showAddDialog"
      @success="handleAddSuccess"
    />

    <!-- 记录详情对话框 -->
    <FoodRecordDetailDialog
      v-model="showDetailDialog"
      :record="selectedRecord"
    />
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

// 筛选
const selectedDate = ref(new Date())
const dateRange = ref([])
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
      records.value = data.content || []
      total.value = data.totalElements || 0
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
const handleDateChange = (date) => {
  console.log('日期变化:', date, typeof date)
  // 确保date是Date对象
  if (typeof date === 'string') {
    selectedDate.value = new Date(date)
  } else {
    selectedDate.value = date
  }
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
  // 强制刷新统计数据
  const temp = selectedDate.value
  selectedDate.value = new Date('2000-01-01')
  setTimeout(() => {
    selectedDate.value = temp
  }, 10)
}

// 查看详情
const handleView = (record) => {
  selectedRecord.value = record
  showDetailDialog.value = true
}

// 删除记录
const handleDelete = async (record) => {
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
      // 强制刷新统计数据
      const temp = selectedDate.value
      selectedDate.value = new Date('2000-01-01')
      setTimeout(() => {
        selectedDate.value = temp
      }, 10)
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
  console.log('FoodRecordView 组件卸载，开始清理...')
  
  // 清理可能残留的 MessageBox
  setTimeout(() => {
    const messageBoxes = document.querySelectorAll('.el-message-box__wrapper')
    messageBoxes.forEach(box => {
      console.log('FoodRecordView 卸载时清理 MessageBox')
      box.remove()
    })
    
    const overlays = document.querySelectorAll('body > .el-overlay')
    overlays.forEach(overlay => {
      const hasActiveModal = document.querySelector('.el-message-box__wrapper, .el-dialog__wrapper, .el-drawer__wrapper')
      if (!hasActiveModal) {
        console.log('FoodRecordView 卸载时清理遮罩层')
        overlay.remove()
      }
    })
  }, 50)
  
  console.log('FoodRecordView 清理完成')
})
</script>

<style scoped lang="scss">
.food-record-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 40px 20px;
}

.food-record-layout {
  max-width: 1400px;
  margin: 0 auto;
}

.stats-section {
  margin-bottom: 24px;
}

.content-section {
  .content-card {
    border-radius: 16px;
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);

    :deep(.el-card__header) {
      padding: 24px;
      border-bottom: 1px solid #f0f0f0;
    }

    :deep(.el-card__body) {
      padding: 24px;
    }
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;

  .title {
    font-size: 20px;
    font-weight: 600;
    color: #303133;
  }
}

.filter-bar {
  display: flex;
  gap: 16px;
  margin-bottom: 24px;

  .el-date-picker,
  .el-select {
    width: 240px;
  }
}

.el-pagination {
  margin-top: 24px;
  display: flex;
  justify-content: center;
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

// 确保遮罩层正确显示
.el-overlay {
  background-color: rgba(0, 0, 0, 0.5) !important;
}

// 确保MessageBox居中显示
.el-overlay .el-message-box {
  position: fixed !important;
  top: 50% !important;
  left: 50% !important;
  transform: translate(-50%, -50%) !important;
}
</style>
