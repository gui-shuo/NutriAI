<template>
  <div class="food-recognition-view">
    <div class="recognition-header">
      <el-button :icon="ArrowLeft" @click="goToHome" text style="margin-bottom: 16px">
        返回首页
      </el-button>
      <h2>🍎 AI食物识别</h2>
      <p>输入食物名称，AI智能识别营养成分</p>
    </div>

    <div class="recognition-body">
      <!-- 左侧：输入区域 -->
      <el-card class="input-card">
        <template #header>
          <h3>食物识别</h3>
        </template>

        <!-- 文本输入方式 -->
        <div class="input-section">
          <h4>📝 文本输入</h4>
          <el-input
            v-model="foodName"
            placeholder="请输入食物名称，如：苹果、鸡胸肉、燕麦"
            size="large"
            clearable
            @keyup.enter="recognizeByName"
          >
            <template #append>
              <el-button 
                type="primary" 
                @click="recognizeByName"
                :loading="isRecognizing"
              >
                识别
              </el-button>
            </template>
          </el-input>
          <div class="input-tip">
            💡 提示：输入常见食物名称，AI会自动识别营养成分
          </div>
        </div>

        <!-- 图片上传方式 -->
        <div class="upload-section">
          <h4>📷 图片识别</h4>
          <el-upload
            ref="uploadRef"
            class="upload-demo"
            drag
            :auto-upload="false"
            :on-change="handleImageChange"
            :show-file-list="false"
            accept="image/*"
          >
            <el-icon class="el-icon--upload"><upload-filled /></el-icon>
            <div class="el-upload__text">
              拖拽图片到此处或 <em>点击上传</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">
                支持 jpg/png 格式，大小不超过 5MB
              </div>
            </template>
          </el-upload>
          
          <!-- 预览图片 -->
          <div v-if="previewUrl" class="preview-section">
            <div class="preview-image-wrapper">
              <img :src="previewUrl" alt="预览" class="preview-image" />
              <el-button 
                class="delete-image-btn"
                type="danger" 
                :icon="Delete"
                circle
                size="small"
                @click="clearImage"
                title="删除图片"
              />
            </div>
            <el-button 
              type="primary" 
              @click="recognizeByImage"
              :loading="isRecognizing"
              style="width: 100%; margin-top: 10px;"
            >
              开始识别
            </el-button>
          </div>
        </div>

        <!-- 快捷输入 -->
        <div class="quick-input">
          <h4>⚡ 快捷输入</h4>
          <el-space wrap>
            <el-tag 
              v-for="food in quickFoods" 
              :key="food"
              @click="foodName = food; recognizeByName()"
              style="cursor: pointer"
              effect="plain"
            >
              {{ food }}
            </el-tag>
          </el-space>
        </div>
      </el-card>

      <!-- 右侧：识别结果或空状态 -->
      <el-card class="result-card" v-if="recognitionResult">
        <template #header>
          <div class="result-header">
            <h3>识别结果</h3>
            <el-tag type="success">{{ recognitionResult.totalCount }} 项</el-tag>
          </div>
        </template>

        <div class="result-list">
          <div 
            v-for="(food, index) in recognitionResult.foods" 
            :key="index"
            class="food-result-item"
          >
            <div class="food-header">
              <h4>{{ food.name }}</h4>
              <el-tag 
                :type="getConfidenceType(food.confidence)"
                size="small"
              >
                置信度: {{ (food.confidence * 100).toFixed(0) }}%
              </el-tag>
            </div>

            <div class="nutrition-grid">
              <div class="nutrition-item">
                <div class="nutrition-label">热量</div>
                <div class="nutrition-value">
                  {{ food.nutrition.energy.toFixed(1) }}
                  <span class="unit">kcal</span>
                </div>
              </div>
              <div class="nutrition-item">
                <div class="nutrition-label">蛋白质</div>
                <div class="nutrition-value">
                  {{ food.nutrition.protein.toFixed(1) }}
                  <span class="unit">g</span>
                </div>
              </div>
              <div class="nutrition-item">
                <div class="nutrition-label">碳水</div>
                <div class="nutrition-value">
                  {{ food.nutrition.carbohydrate.toFixed(1) }}
                  <span class="unit">g</span>
                </div>
              </div>
              <div class="nutrition-item">
                <div class="nutrition-label">脂肪</div>
                <div class="nutrition-value">
                  {{ food.nutrition.fat.toFixed(1) }}
                  <span class="unit">g</span>
                </div>
              </div>
            </div>

            <div class="data-source">
              <el-icon><InfoFilled /></el-icon>
              数据来源: {{ getSourceText(food.nutrition.source) }}
            </div>
          </div>
        </div>

        <div class="result-footer">
          <div class="recognition-time">
            识别耗时: {{ recognitionResult.recognitionTime }}ms
          </div>
        </div>
      </el-card>

      <!-- 空状态提示 -->
      <el-card class="empty-card" v-if="!recognitionResult && !isRecognizing">
        <div class="empty-content">
          <el-icon :size="80" color="#dcdfe6"><Picture /></el-icon>
          <h3>开始识别食物</h3>
          <p>输入食物名称或上传图片，AI将为您分析营养成分</p>
        </div>
      </el-card>

      <!-- 加载状态 -->
      <el-card class="loading-card" v-if="isRecognizing">
        <div class="loading-content">
          <el-icon class="loading-icon"><Loading /></el-icon>
          <h3>AI正在识别食物...</h3>
          <p>请稍候</p>
        </div>
      </el-card>
    </div>

    <!-- 识别历史 -->
    <el-card class="history-card" v-if="history.length > 0">
      <template #header>
        <div class="history-header">
          <h3>📜 识别历史</h3>
          <el-tag type="info">{{ history.length }} 条记录</el-tag>
        </div>
      </template>
      <el-timeline>
        <el-timeline-item
          v-for="item in history"
          :key="item.id"
          :timestamp="formatTime(item.createdAt)"
        >
          <div class="history-item">
            <div class="history-summary">
              <span class="history-text" @click="toggleHistoryDetail(item.id)">
                {{ getHistoryText(item.recognitionResult) }}
              </span>
              <div class="history-actions">
                <el-icon class="expand-icon" @click="toggleHistoryDetail(item.id)">
                  <ArrowDown v-if="!expandedHistory[item.id]" />
                  <ArrowUp v-else />
                </el-icon>
                <el-button
                  type="danger"
                  :icon="Delete"
                  size="small"
                  circle
                  @click="confirmDeleteHistory(item.id)"
                  title="删除记录"
                />
              </div>
            </div>
            
            <!-- 详细信息 -->
            <el-collapse-transition>
              <div v-show="expandedHistory[item.id]" class="history-detail">
                <div class="detail-info">
                  <div class="detail-row">
                    <span class="detail-label">识别方式:</span>
                    <el-tag size="small" :type="item.recognitionType === 'TEXT' ? 'success' : 'primary'">
                      {{ item.recognitionType === 'TEXT' ? '📝 文本识别' : '📷 图片识别' }}
                    </el-tag>
                  </div>
                  <div class="detail-row">
                    <span class="detail-label">识别内容:</span>
                    <span>{{ item.inputText || '图片' }}</span>
                  </div>
                  <div class="detail-row">
                    <span class="detail-label">识别时间:</span>
                    <span>{{ formatFullTime(item.createdAt) }}</span>
                  </div>
                </div>
                
                <!-- 识别结果详情 -->
                <div v-if="item.recognitionResult" class="result-detail">
                  <h5>识别结果:</h5>
                  <div 
                    v-for="(food, index) in parseRecognitionResult(item.recognitionResult).foods" 
                    :key="index"
                    class="history-food-item"
                  >
                    <div class="food-name-row">
                      <strong>{{ food.name }}</strong>
                      <el-tag size="small" :type="getConfidenceType(food.confidence)">
                        {{ (food.confidence * 100).toFixed(0) }}%
                      </el-tag>
                    </div>
                    <div class="nutrition-mini-grid">
                      <span>热量: {{ food.nutrition.energy.toFixed(1) }}kcal</span>
                      <span>蛋白质: {{ food.nutrition.protein.toFixed(1) }}g</span>
                      <span>碳水: {{ food.nutrition.carbohydrate.toFixed(1) }}g</span>
                      <span>脂肪: {{ food.nutrition.fat.toFixed(1) }}g</span>
                    </div>
                  </div>
                </div>
              </div>
            </el-collapse-transition>
          </div>
        </el-timeline-item>
      </el-timeline>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading, InfoFilled, UploadFilled, Picture, Delete, ArrowDown, ArrowUp, ArrowLeft } from '@element-plus/icons-vue'
import api from '@/services/api'

// 路由
const router = useRouter()

const foodName = ref('')
const isRecognizing = ref(false)
const recognitionResult = ref(null)
const history = ref([])
const previewUrl = ref(null)
const selectedFile = ref(null)
const expandedHistory = reactive({})
const uploadRef = ref(null)

const quickFoods = [
  '苹果', '香蕉', '鸡胸肉', '鸡蛋', '牛奶',
  '燕麦', '西兰花', '三文鱼', '糙米', '红薯'
]

// 处理图片选择
const handleImageChange = (file) => {
  selectedFile.value = file.raw
  previewUrl.value = URL.createObjectURL(file.raw)
}

// 通过图片识别
const recognizeByImage = async () => {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择图片')
    return
  }

  isRecognizing.value = true
  recognitionResult.value = null

  try {
    const formData = new FormData()
    formData.append('image', selectedFile.value)

    const response = await api.post('/food-recognition/recognize-by-image', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })

    const data = response.data

    if (data.code === 200) {
      recognitionResult.value = data.data
      ElMessage.success('识别成功！')
      loadHistory()
    } else if (data.code === 501) {
      ElMessage.warning(data.message)
    } else {
      throw new Error(data.message)
    }
  } catch (error) {
    console.error('识别失败:', error)
    ElMessage.error('识别失败: ' + (error.response?.data?.message || error.message))
  } finally {
    isRecognizing.value = false
  }
}

// 返回首页
const goToHome = () => {
  router.push('/')
}

// 通过名称识别
const recognizeByName = async () => {
  if (!foodName.value.trim()) {
    ElMessage.warning('请输入食物名称')
    return
  }

  isRecognizing.value = true
  recognitionResult.value = null

  try {
    const response = await api.post('/food-recognition/recognize-by-name', null, {
      params: {
        foodName: foodName.value.trim()
      }
    })

    const data = response.data

    if (data.code === 200) {
      recognitionResult.value = data.data
      ElMessage.success('识别成功！')
      loadHistory()
    } else {
      throw new Error(data.message)
    }
  } catch (error) {
    console.error('识别失败:', error)
    ElMessage.error('识别失败: ' + (error.response?.data?.message || error.message))
  } finally {
    isRecognizing.value = false
  }
}

// 加载识别历史
const loadHistory = async () => {
  try {
    const response = await api.get('/food-recognition/history')
    const data = response.data
    if (data.code === 200) {
      history.value = data.data || []
    }
  } catch (error) {
    console.error('加载历史失败:', error)
  }
}

// 获取置信度类型
const getConfidenceType = (confidence) => {
  if (confidence >= 0.9) return 'success'
  if (confidence >= 0.7) return 'warning'
  return 'info'
}

// 获取数据来源文本
const getSourceText = (source) => {
  const sourceMap = {
    'database': '数据库（准确）',
    'estimated': 'AI估算',
    'default': '默认值'
  }
  return sourceMap[source] || source
}

// 格式化时间
const formatTime = (time) => {
  return new Date(time).toLocaleString('zh-CN')
}

// 获取历史文本
const getHistoryText = (resultJson) => {
  try {
    const result = JSON.parse(resultJson)
    const foods = result.foods.map(f => f.name).join('、')
    return `识别了 ${foods}`
  } catch {
    return '识别记录'
  }
}

// 清除图片
const clearImage = () => {
  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value)
  }
  previewUrl.value = null
  selectedFile.value = null
  ElMessage.success('已清除图片')
}

// 切换历史详情展开/收起
const toggleHistoryDetail = (id) => {
  expandedHistory[id] = !expandedHistory[id]
}

// 解析识别结果
const parseRecognitionResult = (resultJson) => {
  try {
    return JSON.parse(resultJson)
  } catch {
    return { foods: [] }
  }
}

// 格式化完整时间
const formatFullTime = (time) => {
  const date = new Date(time)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

// 确认删除历史记录
const confirmDeleteHistory = (id) => {
  ElMessageBox.confirm(
    '确定要删除这条识别记录吗？',
    '删除确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
      center: true,
      customClass: 'custom-message-box'
    }
  ).then(() => {
    deleteHistory(id)
  }).catch(() => {
    // 取消删除
  })
}

// 删除历史记录
const deleteHistory = async (id) => {
  try {
    const response = await api.delete(`/food-recognition/history/${id}`)
    const data = response.data
    
    if (data.code === 200) {
      ElMessage.success('删除成功')
      // 重新加载历史记录
      loadHistory()
    } else {
      throw new Error(data.message)
    }
  } catch (error) {
    console.error('删除失败:', error)
    ElMessage.error('删除失败: ' + (error.response?.data?.message || error.message))
  }
}

// 初始化
onMounted(() => {
  loadHistory()
})

// 组件卸载前清理
onBeforeUnmount(() => {
  console.log('FoodRecognitionView 组件卸载，开始清理...')
  
  // 释放 blob URL，防止内存泄漏
  if (previewUrl.value) {
    console.log('释放预览图片 blob URL')
    URL.revokeObjectURL(previewUrl.value)
    previewUrl.value = null
  }
  
  // 清理 Upload 组件
  if (uploadRef.value) {
    console.log('清理 Upload 组件')
    uploadRef.value.clearFiles?.()
  }
  
  // 清空所有数据
  selectedFile.value = null
  recognitionResult.value = null
  history.value = []
  foodName.value = ''
  isRecognizing.value = false
  
  // 清空展开状态
  Object.keys(expandedHistory).forEach(key => {
    delete expandedHistory[key]
  })
  
  // 强制关闭所有可能打开的 MessageBox
  const messageBoxes = document.querySelectorAll('.el-message-box__wrapper')
  messageBoxes.forEach(box => {
    console.log('强制关闭 MessageBox')
    box.remove()
  })
  
  // 强制清理可能残留的 DOM 元素
  setTimeout(() => {
    const uploadElements = document.querySelectorAll('.el-upload, .el-upload-dragger')
    uploadElements.forEach(el => {
      if (!document.body.contains(el.closest('.food-recognition-view'))) {
        console.log('清理残留的 upload 元素')
        el.remove()
      }
    })
    
    // 清理所有残留的遮罩层
    const overlays = document.querySelectorAll('body > .el-overlay')
    overlays.forEach(overlay => {
      const hasActiveModal = document.querySelector('.el-message-box__wrapper, .el-dialog__wrapper, .el-drawer__wrapper')
      if (!hasActiveModal) {
        console.log('清理残留的遮罩层')
        overlay.remove()
      }
    })
  }, 100)
  
  console.log('FoodRecognitionView 清理完成')
})
</script>

<style scoped>
.food-recognition-view {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 24px;
}

.recognition-header {
  text-align: center;
  margin-bottom: 32px;
}

.recognition-header h2 {
  font-size: 32px;
  margin: 0 0 8px 0;
  color: #303133;
}

.recognition-header p {
  font-size: 16px;
  color: #909399;
  margin: 0;
}

.recognition-body {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  margin-bottom: 24px;
}

@media (max-width: 768px) {
  .recognition-body {
    grid-template-columns: 1fr;
  }
}

.input-card,
.result-card,
.loading-card,
.history-card {
  height: fit-content;
}

.input-section,
.upload-section,
.quick-input {
  margin-bottom: 24px;
}

.input-section h4,
.upload-section h4,
.quick-input h4 {
  font-size: 16px;
  margin: 0 0 12px 0;
  color: #303133;
}

.input-tip {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.result-header h3 {
  margin: 0;
}

.result-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.food-result-item {
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
}

.food-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.food-header h4 {
  margin: 0;
  font-size: 18px;
  color: #303133;
}

.nutrition-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 12px;
}

.nutrition-item {
  text-align: center;
  padding: 12px;
  background: white;
  border-radius: 8px;
}

.nutrition-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}

.nutrition-value {
  font-size: 20px;
  font-weight: bold;
  color: #303133;
}

.nutrition-value .unit {
  font-size: 12px;
  font-weight: normal;
  color: #909399;
  margin-left: 2px;
}

.data-source {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
}

.result-footer {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
}

.recognition-time {
  font-size: 12px;
  color: #909399;
  text-align: right;
}

.loading-card {
  grid-column: 1 / -1;
}

.loading-content {
  text-align: center;
  padding: 48px 24px;
}

.loading-icon {
  font-size: 48px;
  color: #409eff;
  animation: rotate 1s linear infinite;
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.loading-content h3 {
  margin: 16px 0 8px 0;
  color: #303133;
}

.loading-content p {
  margin: 0;
  color: #909399;
}

.preview-section {
  margin-top: 16px;
  text-align: center;
}

.preview-image {
  max-width: 100%;
  max-height: 300px;
  border-radius: 8px;
  margin-bottom: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.upload-demo {
  width: 100%;
}

.empty-card {
  grid-column: 2;
}

.empty-content {
  text-align: center;
  padding: 80px 24px;
}

.empty-content h3 {
  margin: 24px 0 12px 0;
  color: #909399;
  font-size: 20px;
}

.empty-content p {
  margin: 0;
  color: #c0c4cc;
  font-size: 14px;
}

.quick-input .el-tag {
  cursor: pointer;
  transition: all 0.3s;
}

.quick-input .el-tag:hover {
  transform: translateY(-2px);
  box-shadow: 0 2px 8px rgba(34, 197, 94, 0.3);
}

.history-card {
  max-width: 100%;
}

@media (max-width: 768px) {
  .recognition-header h2 {
    font-size: 24px;
  }
  
  .nutrition-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .empty-card {
    grid-column: 1;
  }
}

/* 图片预览删除按钮 */
.preview-image-wrapper {
  position: relative;
  display: inline-block;
  width: 100%;
}

.delete-image-btn {
  position: absolute;
  top: 10px;
  right: 10px;
  z-index: 10;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.delete-image-btn:hover {
  transform: scale(1.1);
}

/* 历史记录样式 */
.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.history-header h3 {
  margin: 0;
}

.history-item {
  width: 100%;
}

.history-summary {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  border-radius: 6px;
  transition: all 0.3s;
}

.history-summary:hover {
  background-color: #f5f7fa;
}

.history-text {
  flex: 1;
  font-size: 14px;
  color: #303133;
  cursor: pointer;
}

.history-text:hover {
  color: #409eff;
}

.history-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.expand-icon {
  cursor: pointer;
  transition: transform 0.3s;
  color: #909399;
  font-size: 16px;
}

.expand-icon:hover {
  color: #409eff;
}

.history-detail {
  margin-top: 12px;
  padding: 16px;
  background-color: #f9fafb;
  border-radius: 8px;
  border-left: 3px solid #409eff;
}

.detail-info {
  margin-bottom: 16px;
}

.detail-row {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
  font-size: 14px;
}

.detail-label {
  font-weight: 600;
  color: #606266;
  margin-right: 12px;
  min-width: 80px;
}

.result-detail h5 {
  margin: 0 0 12px 0;
  font-size: 14px;
  color: #303133;
  font-weight: 600;
}

.history-food-item {
  background: white;
  padding: 12px;
  border-radius: 6px;
  margin-bottom: 8px;
}

.history-food-item:last-child {
  margin-bottom: 0;
}

.food-name-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.food-name-row strong {
  font-size: 15px;
  color: #303133;
}

.nutrition-mini-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
  font-size: 13px;
  color: #606266;
}

.nutrition-mini-grid span {
  padding: 4px 8px;
  background: #f5f7fa;
  border-radius: 4px;
}

@media (max-width: 768px) {
  .nutrition-mini-grid {
    grid-template-columns: 1fr;
  }
}
</style>

