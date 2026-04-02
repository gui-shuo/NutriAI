<template>
  <div class="food-recognition-view">
    <!-- 顶部导航栏 -->
    <div class="page-topbar">
      <el-button :icon="ArrowLeft" text @click="goToHome">返回首页</el-button>
      <div class="topbar-title">
        <h2>🍎 AI食物识别</h2>
        <p>输入食物名称或上传图片，AI智能识别营养成分</p>
      </div>
    </div>

    <div class="page-content">
      <el-alert type="info" :closable="true" show-icon class="disclaimer-alert">
        <template #title>
          上传食物图片即可自动识别并获取完整营养成分数据，支持各类常见食物。营养数据仅供参考。
          <router-link to="/legal/disclaimer" style="color:#0052FF">详细声明</router-link>
        </template>
      </el-alert>

      <!-- 输入区域：左右双栏 — 左边输入方式，右边快捷+提示 -->
      <div class="input-area">
        <el-card class="input-card">
          <el-tabs v-model="inputMode" class="input-tabs">
            <el-tab-pane label="📝 文本输入" name="text">
              <div class="input-section">
                <el-input
                  v-model="foodName"
                  placeholder="请输入食物名称，如：苹果、鸡胸肉、燕麦"
                  size="large"
                  clearable
                  @keyup.enter="recognizeByName"
                >
                  <template #append>
                    <el-button type="primary" :loading="isRecognizing" @click="recognizeByName">
                      识别
                    </el-button>
                  </template>
                </el-input>
                <div class="input-tip">💡 提示：输入常见食物名称，AI会自动识别营养成分</div>
              </div>
            </el-tab-pane>

            <el-tab-pane label="📷 图片识别" name="image">
              <div class="upload-section">
                <el-upload
                  ref="uploadRef"
                  class="upload-demo"
                  drag
                  :auto-upload="false"
                  :on-change="handleImageChange"
                  :show-file-list="false"
                  :disabled="isRecognizing"
                  :limit="1"
                  accept="image/jpeg,image/png,image/gif,image/webp,image/bmp"
                >
                  <el-icon class="el-icon--upload">
                    <upload-filled />
                  </el-icon>
                  <div class="el-upload__text">拖拽图片到此处或 <em>点击上传</em></div>
                  <template #tip>
                    <div class="el-upload__tip">支持 JPG / PNG / GIF / WebP / BMP，大小不超过 5MB（大图将自动压缩）</div>
                    <div class="el-upload__tip" style="color: #0052FF; margin-top: 4px;">💡 识别结果请以置信度为准，置信度越高结果越可靠</div>
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
                      title="删除图片"
                      @click="clearImage"
                    />
                  </div>
                  <el-button
                    type="primary"
                    :loading="isRecognizing"
                    style="width: 100%; margin-top: 10px"
                    @click="recognizeByImage"
                  >
                    开始识别
                  </el-button>
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </el-card>

        <!-- 快捷输入侧栏 -->
        <el-card class="quick-card">
          <template #header>
            <h4>⚡ 快捷识别</h4>
          </template>
          <div class="quick-tags">
            <el-tag
              v-for="food in quickFoods"
              :key="food"
              :style="{
                cursor: isRecognizing ? 'not-allowed' : 'pointer',
                opacity: isRecognizing ? 0.5 : 1
              }"
              effect="plain"
              @click="quickRecognize(food)"
            >
              {{ food }}
            </el-tag>
          </div>
        </el-card>
      </div>

      <!-- 加载状态 — 全宽 -->
      <el-card v-if="isRecognizing" class="loading-card">
        <div class="loading-content">
          <el-icon class="loading-icon">
            <Loading />
          </el-icon>
          <h3>AI正在识别食物...</h3>
          <p>正在分析食物营养成分，通常需要几秒钟，请耐心等待</p>
        </div>
      </el-card>

      <!-- 空状态提示 — 全宽 -->
      <el-card v-if="!recognitionResult && !isRecognizing" class="empty-card">
        <div class="empty-content">
          <el-icon :size="64" color="#dcdfe6">
            <Picture />
          </el-icon>
          <h3>开始识别食物</h3>
          <p>在上方输入食物名称或上传图片，AI将为您分析完整营养成分</p>
        </div>
      </el-card>

      <!-- 识别结果 — 全宽，每个食物一张卡片 -->
      <div v-if="recognitionResult" class="result-area">
        <div class="result-bar">
          <h3>🔬 识别结果</h3>
          <div class="result-bar-right">
            <el-tag type="success">共 {{ recognitionResult.totalCount }} 项</el-tag>
            <span class="recognition-time">耗时 {{ recognitionResult.recognitionTime }}ms</span>
          </div>
        </div>

        <div class="result-cards">
          <el-card
            v-for="(food, index) in recognitionResult.foods"
            :key="index"
            class="food-result-card"
          >
            <!-- 食物标题行 -->
            <div class="food-header">
              <div class="food-name-wrap">
                <h4>{{ food.name }}</h4>
                <el-tag v-if="food.category" :type="food.category === '果蔬' ? 'success' : 'warning'" size="small" effect="plain">
                  {{ food.category }}
                </el-tag>
              </div>
              <el-tag :type="getConfidenceType(food.confidence)" size="small" effect="dark" round>
                置信度 {{ (food.confidence * 100).toFixed(0) }}%
              </el-tag>
            </div>

            <!-- 核心营养 — 横向大卡片 -->
            <div class="core-nutrition">
              <div class="core-item energy-item">
                <div class="core-label">🔥 热量</div>
                <div class="core-value">{{ safeFixed(food.nutrition.energy) }} <span class="unit">kcal</span></div>
              </div>
              <div class="core-item">
                <div class="core-label">🥩 蛋白质</div>
                <div class="core-value">{{ safeFixed(food.nutrition.protein) }} <span class="unit">g</span></div>
              </div>
              <div class="core-item">
                <div class="core-label">🍚 碳水</div>
                <div class="core-value">{{ safeFixed(food.nutrition.carbohydrate) }} <span class="unit">g</span></div>
              </div>
              <div class="core-item">
                <div class="core-label">🧈 脂肪</div>
                <div class="core-value">{{ safeFixed(food.nutrition.fat) }} <span class="unit">g</span></div>
              </div>
              <div class="core-item">
                <div class="core-label">🌾 纤维</div>
                <div class="core-value">{{ safeFixed(food.nutrition.fiber) }} <span class="unit">g</span></div>
              </div>
              <div class="core-item">
                <div class="core-label">💧 胆固醇</div>
                <div class="core-value">{{ safeFixed(food.nutrition.cholesterol) }} <span class="unit">mg</span></div>
              </div>
            </div>

            <!-- 矿物质 + 维生素 — 折叠面板 -->
            <el-collapse v-if="hasMineral(food.nutrition) || hasVitamin(food.nutrition)" class="detail-collapse">
              <el-collapse-item v-if="hasMineral(food.nutrition)" title="💎 矿物质" name="mineral">
                <div class="nutrition-grid mineral-grid">
                  <div v-if="food.nutrition.calcium != null" class="nutrition-item-sm">
                    <span class="label-sm">钙</span>
                    <span class="value-sm">{{ safeFixed(food.nutrition.calcium) }} <em>mg</em></span>
                  </div>
                  <div v-if="food.nutrition.iron != null" class="nutrition-item-sm">
                    <span class="label-sm">铁</span>
                    <span class="value-sm">{{ safeFixed(food.nutrition.iron) }} <em>mg</em></span>
                  </div>
                  <div v-if="food.nutrition.zinc != null" class="nutrition-item-sm">
                    <span class="label-sm">锌</span>
                    <span class="value-sm">{{ safeFixed(food.nutrition.zinc) }} <em>mg</em></span>
                  </div>
                  <div v-if="food.nutrition.sodium != null" class="nutrition-item-sm">
                    <span class="label-sm">钠</span>
                    <span class="value-sm">{{ safeFixed(food.nutrition.sodium) }} <em>mg</em></span>
                  </div>
                  <div v-if="food.nutrition.potassium != null" class="nutrition-item-sm">
                    <span class="label-sm">钾</span>
                    <span class="value-sm">{{ safeFixed(food.nutrition.potassium) }} <em>mg</em></span>
                  </div>
                  <div v-if="food.nutrition.magnesium != null" class="nutrition-item-sm">
                    <span class="label-sm">镁</span>
                    <span class="value-sm">{{ safeFixed(food.nutrition.magnesium) }} <em>mg</em></span>
                  </div>
                  <div v-if="food.nutrition.phosphorus != null" class="nutrition-item-sm">
                    <span class="label-sm">磷</span>
                    <span class="value-sm">{{ safeFixed(food.nutrition.phosphorus) }} <em>mg</em></span>
                  </div>
                  <div v-if="food.nutrition.selenium != null" class="nutrition-item-sm">
                    <span class="label-sm">硒</span>
                    <span class="value-sm">{{ safeFixed(food.nutrition.selenium) }} <em>μg</em></span>
                  </div>
                  <div v-if="food.nutrition.copper != null" class="nutrition-item-sm">
                    <span class="label-sm">铜</span>
                    <span class="value-sm">{{ safeFixed(food.nutrition.copper, 2) }} <em>mg</em></span>
                  </div>
                  <div v-if="food.nutrition.manganese != null" class="nutrition-item-sm">
                    <span class="label-sm">锰</span>
                    <span class="value-sm">{{ safeFixed(food.nutrition.manganese, 2) }} <em>mg</em></span>
                  </div>
                </div>
              </el-collapse-item>

              <el-collapse-item v-if="hasVitamin(food.nutrition)" title="🧪 维生素" name="vitamin">
                <div class="nutrition-grid mineral-grid">
                  <div v-if="food.nutrition.vitaminA != null" class="nutrition-item-sm">
                    <span class="label-sm">维生素A</span>
                    <span class="value-sm">{{ safeFixed(food.nutrition.vitaminA) }} <em>μg</em></span>
                  </div>
                  <div v-if="food.nutrition.vitaminC != null" class="nutrition-item-sm">
                    <span class="label-sm">维生素C</span>
                    <span class="value-sm">{{ safeFixed(food.nutrition.vitaminC) }} <em>mg</em></span>
                  </div>
                  <div v-if="food.nutrition.vitaminE != null" class="nutrition-item-sm">
                    <span class="label-sm">维生素E</span>
                    <span class="value-sm">{{ safeFixed(food.nutrition.vitaminE, 2) }} <em>mg</em></span>
                  </div>
                  <div v-if="food.nutrition.carotene != null" class="nutrition-item-sm">
                    <span class="label-sm">胡萝卜素</span>
                    <span class="value-sm">{{ safeFixed(food.nutrition.carotene) }} <em>μg</em></span>
                  </div>
                  <div v-if="food.nutrition.thiamine != null" class="nutrition-item-sm">
                    <span class="label-sm">硫胺素(B1)</span>
                    <span class="value-sm">{{ safeFixed(food.nutrition.thiamine, 2) }} <em>mg</em></span>
                  </div>
                  <div v-if="food.nutrition.riboflavin != null" class="nutrition-item-sm">
                    <span class="label-sm">核黄素(B2)</span>
                    <span class="value-sm">{{ safeFixed(food.nutrition.riboflavin, 2) }} <em>mg</em></span>
                  </div>
                  <div v-if="food.nutrition.niacin != null" class="nutrition-item-sm">
                    <span class="label-sm">烟酸(B3)</span>
                    <span class="value-sm">{{ safeFixed(food.nutrition.niacin) }} <em>mg</em></span>
                  </div>
                  <div v-if="food.nutrition.retinolEquivalent != null" class="nutrition-item-sm">
                    <span class="label-sm">视黄醇当量</span>
                    <span class="value-sm">{{ safeFixed(food.nutrition.retinolEquivalent) }} <em>μg</em></span>
                  </div>
                </div>
              </el-collapse-item>
            </el-collapse>

            <div class="data-source">
              <el-icon><InfoFilled /></el-icon>
              数据来源: {{ getSourceText(food.nutrition.source) }}
            </div>
          </el-card>
        </div>
      </div>

      <!-- 识别历史 — 卡片网格 -->
      <div v-if="history.length > 0" class="history-area">
        <div class="history-bar">
          <h3>📜 识别历史</h3>
          <el-tag type="info">{{ history.length }} 条记录</el-tag>
        </div>

        <div class="history-grid">
          <el-card
            v-for="item in history"
            :key="item.id"
            class="history-card-item"
            shadow="hover"
          >
            <div class="history-summary">
              <div class="history-left">
                <el-tag
                  size="small"
                  :type="item.recognitionType === 'TEXT' ? 'success' : 'primary'"
                >
                  {{ item.recognitionType === 'TEXT' ? '📝 文本' : '📷 图片' }}
                </el-tag>
                <span class="history-text" @click="toggleHistoryDetail(item.id)">
                  {{ getHistoryText(item.recognitionResult) }}
                </span>
              </div>
              <div class="history-actions">
                <span class="history-time">{{ formatTime(item.createdAt) }}</span>
                <el-button
                  type="danger"
                  :icon="Delete"
                  size="small"
                  circle
                  title="删除记录"
                  @click="confirmDeleteHistory(item.id)"
                />
                <el-icon class="expand-icon" @click="toggleHistoryDetail(item.id)">
                  <ArrowDown v-if="!expandedHistory[item.id]" />
                  <ArrowUp v-else />
                </el-icon>
              </div>
            </div>

            <!-- 详细信息 -->
            <el-collapse-transition>
              <div v-show="expandedHistory[item.id]" class="history-detail">
                <div class="detail-info">
                  <div class="detail-row">
                    <span class="detail-label">识别内容:</span>
                    <span>{{ item.inputText || '图片' }}</span>
                  </div>
                  <div class="detail-row">
                    <span class="detail-label">识别时间:</span>
                    <span>{{ formatFullTime(item.createdAt) }}</span>
                  </div>
                </div>

                <!-- 图片识别时显示原始图片 -->
                <div v-if="item.recognitionType === 'IMAGE' && item.imageUrl" class="history-image-section">
                  <h5>识别图片:</h5>
                  <div class="history-image-wrapper">
                    <img :src="item.imageUrl" alt="识别图片" class="history-image" />
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
                      <span>热量: {{ safeFixed(food.nutrition?.energy) }}kcal</span>
                      <span>蛋白质: {{ safeFixed(food.nutrition?.protein) }}g</span>
                      <span>碳水: {{ safeFixed(food.nutrition?.carbohydrate) }}g</span>
                      <span>脂肪: {{ safeFixed(food.nutrition?.fat) }}g</span>
                    </div>
                    <div v-if="food.nutrition?.source" class="data-source-mini">
                      <el-icon><InfoFilled /></el-icon>
                      {{ getSourceText(food.nutrition.source) }}
                    </div>
                  </div>
                </div>
              </div>
            </el-collapse-transition>
          </el-card>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Loading,
  InfoFilled,
  UploadFilled,
  Picture,
  Delete,
  ArrowDown,
  ArrowUp,
  ArrowLeft
} from '@element-plus/icons-vue'
import api from '@/services/api'

// ─── 常量 ────────────────────────────────────────────────────────
const ALLOWED_IMAGE_TYPES = new Set([
  'image/jpeg',
  'image/png',
  'image/gif',
  'image/webp',
  'image/bmp'
])
const MAX_IMAGE_SIZE = 5 * 1024 * 1024 // 5MB
const COMPRESS_THRESHOLD = 1 * 1024 * 1024 // 1MB — 超过此大小自动压缩

const router = useRouter()

// ─── 状态 ────────────────────────────────────────────────────────
const inputMode = ref('text')
const foodName = ref('')
const isRecognizing = ref(false)
const recognitionResult = ref(null)
const history = ref([])
const previewUrl = ref(null)
const selectedFile = ref(null)
const expandedHistory = reactive({})
const uploadRef = ref(null)

const quickFoods = [
  '苹果',
  '香蕉',
  '鸡胸肉',
  '鸡蛋',
  '牛奶',
  '燕麦',
  '西兰花',
  '三文鱼',
  '糙米',
  '红薯'
]

// ─── 图片处理 ────────────────────────────────────────────────────

/** canvas压缩图片 — 大图自动缩小以加速识别 */
const compressImage = (file, maxWidth = 1280, quality = 0.7) => {
  return new Promise((resolve) => {
    const img = new Image()
    img.onload = () => {
      let { width, height } = img
      if (width > maxWidth) {
        height = Math.round(height * maxWidth / width)
        width = maxWidth
      }
      const canvas = document.createElement('canvas')
      canvas.width = width
      canvas.height = height
      const ctx = canvas.getContext('2d')
      ctx.drawImage(img, 0, 0, width, height)
      canvas.toBlob(
        blob => resolve(new File([blob], file.name, { type: 'image/jpeg' })),
        'image/jpeg',
        quality
      )
    }
    img.onerror = () => resolve(file)
    img.src = URL.createObjectURL(file)
  })
}

/** el-upload onChange 回调：校验文件类型和大小，大图自动压缩 */
const handleImageChange = async file => {
  const raw = file.raw
  if (!raw) return

  if (!ALLOWED_IMAGE_TYPES.has(raw.type)) {
    ElMessage.error('仅支持 JPG / PNG / GIF / WebP / BMP 格式的图片')
    uploadRef.value?.clearFiles()
    return
  }
  if (raw.size > MAX_IMAGE_SIZE) {
    ElMessage.error('图片大小不能超过 5MB，请压缩后重试')
    uploadRef.value?.clearFiles()
    return
  }

  // 大图自动压缩
  let finalFile = raw
  if (raw.size > COMPRESS_THRESHOLD) {
    ElMessage.info('图片较大，正在自动压缩...')
    finalFile = await compressImage(raw)
    ElMessage.success(`已压缩: ${(raw.size / 1024).toFixed(0)}KB → ${(finalFile.size / 1024).toFixed(0)}KB`)
  }

  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
  selectedFile.value = finalFile
  previewUrl.value = URL.createObjectURL(finalFile)
}

/** 清除已选图片 */
const clearImage = () => {
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
  previewUrl.value = null
  selectedFile.value = null
  uploadRef.value?.clearFiles()
  ElMessage.success('已清除图片')
}

// ─── 识别逻辑 ─────────────────────────────────────────────────────

/** 图片识别 */
const recognizeByImage = async () => {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择图片')
    return
  }
  if (isRecognizing.value) return // 防止重复提交

  isRecognizing.value = true
  recognitionResult.value = null

  try {
    const formData = new FormData()
    formData.append('image', selectedFile.value)

    const response = await api.post('/food-recognition/recognize-by-image', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 120000 // 菜品+果蔬并行识别需要更多时间
    })
    const data = response.data

    if (data.code === 200) {
      recognitionResult.value = data.data
      ElMessage.success(`识别成功，共识别到 ${data.data.totalCount} 种食物`)
      loadHistory()
    } else if (data.code === 4001) {
      // 未识别到食物的业务状态
      ElMessage.warning(data.message || '未能从图片中识别到食物，请换一张更清晰的照片')
    } else if (data.code === 501) {
      ElMessage.info('图片识别功能暂未开放，建议使用文本输入或快捷输入')
    } else {
      ElMessage.error(data.message || '识别失败，请稍后重试')
    }
  } catch (error) {
    console.error('图片识别请求失败:', error)
    const msg = error.response?.data?.message || error.message || '网络错误，请检查网络后重试'
    ElMessage.error('识别失败: ' + msg)
  } finally {
    isRecognizing.value = false
  }
}

/** 文本识别 */
const recognizeByName = async () => {
  const name = foodName.value.trim()
  if (!name) {
    ElMessage.warning('请输入食物名称')
    return
  }
  if (isRecognizing.value) return // 防止重复提交

  isRecognizing.value = true
  recognitionResult.value = null

  try {
    const response = await api.post('/food-recognition/recognize-by-name', null, {
      params: { foodName: name },
      timeout: 30000
    })
    const data = response.data

    if (data.code === 200) {
      recognitionResult.value = data.data
      ElMessage.success('识别成功！')
      loadHistory()
    } else {
      ElMessage.error(data.message || '识别失败，请稍后重试')
    }
  } catch (error) {
    console.error('文本识别请求失败:', error)
    const msg = error.response?.data?.message || error.message || '网络错误，请检查网络后重试'
    ElMessage.error('识别失败: ' + msg)
  } finally {
    isRecognizing.value = false
  }
}

/** 快捷识别（防止正在识别时重复触发） */
const quickRecognize = food => {
  if (isRecognizing.value) return
  foodName.value = food
  recognizeByName()
}

// ─── 历史记录 ─────────────────────────────────────────────────────

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

const confirmDeleteHistory = id => {
  ElMessageBox.confirm('确定要删除这条识别记录吗？', '删除确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
    center: true,
    customClass: 'custom-message-box'
  })
    .then(() => deleteHistory(id))
    .catch(() => {})
}

const deleteHistory = async id => {
  try {
    const response = await api.delete(`/food-recognition/history/${id}`)
    const data = response.data
    if (data.code === 200) {
      ElMessage.success('删除成功')
      loadHistory()
    } else {
      ElMessage.error(data.message || '删除失败')
    }
  } catch (error) {
    console.error('删除失败:', error)
    ElMessage.error('删除失败: ' + (error.response?.data?.message || error.message))
  }
}

const toggleHistoryDetail = id => {
  expandedHistory[id] = !expandedHistory[id]
}

// ─── 工具函数 ─────────────────────────────────────────────────────

const getConfidenceType = confidence => {
  if (confidence >= 0.9) return 'success'
  if (confidence >= 0.7) return 'warning'
  return 'info'
}

const getSourceText = source => {
  const map = {
    tianapi: '天聚数行API',
    database: '数据库（准确）',
    estimated: 'AI估算',
    default: '默认值',
    'baidu-calorie-only': '识别（仅卡路里）'
  }
  return map[source] || source
}

const hasMineral = n => {
  if (!n) return false
  return [n.calcium, n.iron, n.zinc, n.sodium, n.potassium, n.magnesium, n.phosphorus, n.selenium, n.copper, n.manganese]
    .some(v => v != null && v !== 0)
}

const hasVitamin = n => {
  if (!n) return false
  return [n.vitaminA, n.vitaminC, n.vitaminE, n.carotene, n.thiamine, n.riboflavin, n.niacin, n.retinolEquivalent]
    .some(v => v != null && v !== 0)
}

const safeFixed = (val, digits = 1) => {
  if (val == null || isNaN(val)) return '—'
  return Number(val).toFixed(digits)
}

const formatTime = time => {
  if (!time) return ''
  try {
    return new Date(time).toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    })
  } catch {
    return String(time)
  }
}

const formatFullTime = time => {
  if (!time) return ''
  try {
    return new Date(time).toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    })
  } catch {
    return String(time)
  }
}

const getHistoryText = resultJson => {
  try {
    const result = JSON.parse(resultJson)
    const names = result.foods?.map(f => f.name).join('、') || '未知食物'
    return `识别了 ${names}`
  } catch {
    return '识别记录'
  }
}

const parseRecognitionResult = resultJson => {
  try {
    return JSON.parse(resultJson)
  } catch {
    return { foods: [] }
  }
}

const goToHome = () => router.push('/')

// ─── 生命周期 ─────────────────────────────────────────────────────

onMounted(() => {
  loadHistory()
})

onBeforeUnmount(() => {
  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value)
    previewUrl.value = null
  }
  uploadRef.value?.clearFiles?.()
  selectedFile.value = null
  recognitionResult.value = null
  history.value = []
  foodName.value = ''
  isRecognizing.value = false
  Object.keys(expandedHistory).forEach(key => delete expandedHistory[key])
})
</script>

<style scoped>
.food-recognition-view {
  min-height: 100vh;
  background: #FAFAFA;
  padding: 0 24px 40px;
  font-family: 'Inter', sans-serif;
}

/* 顶部导航栏 */
.page-topbar {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 0;
  border-bottom: 1px solid #E2E8F0;
  margin-bottom: 24px;
}

.topbar-title h2 {
  font-size: 24px;
  margin: 0;
  color: #0F172A;
  font-family: 'Calistoga', serif;
}

.topbar-title p {
  font-size: 13px;
  color: #64748B;
  margin: 2px 0 0 0;
}

.page-content {
  max-width: 1200px;
  margin: 0 auto;
}

.disclaimer-alert {
  margin-bottom: 20px;
}

/* 输入区域 — 双栏 */
.input-area {
  display: grid;
  grid-template-columns: 1fr 240px;
  gap: 20px;
  margin-bottom: 24px;
}

.input-card {
  border: 1px solid #E2E8F0;
  border-radius: 12px;
  box-shadow: 0 4px 6px -1px rgba(0,0,0,0.07), 0 2px 4px -2px rgba(0,0,0,0.07);
  background: #FFFFFF;
}

.input-card :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
}

.input-section,
.upload-section {
  min-height: 120px;
}

.input-tip {
  margin-top: 8px;
  font-size: 12px;
  color: #64748B;
}

.upload-demo {
  width: 100%;
}

.upload-demo :deep(.el-upload-dragger) {
  border: 2px dashed #E2E8F0;
  border-radius: 12px;
  background: #FAFAFA;
}

.upload-demo :deep(.el-upload-dragger:hover) {
  border-color: #0052FF;
  background: #FFFFFF;
}

.preview-section {
  margin-top: 16px;
  text-align: center;
}

.preview-image-wrapper {
  position: relative;
  display: inline-block;
  width: 100%;
}

.preview-image {
  max-width: 100%;
  max-height: 260px;
  border-radius: 12px;
  border: 1px solid #E2E8F0;
  box-shadow: 0 4px 6px -1px rgba(0,0,0,0.07), 0 2px 4px -2px rgba(0,0,0,0.07);
}

.delete-image-btn {
  position: absolute;
  top: 8px;
  right: 8px;
  z-index: 10;
}

/* 快捷识别侧栏 */
.quick-card {
  border: 1px solid #E2E8F0;
  border-radius: 12px;
  box-shadow: 0 4px 6px -1px rgba(0,0,0,0.07), 0 2px 4px -2px rgba(0,0,0,0.07);
  background: #FFFFFF;
  height: fit-content;
}

.quick-card :deep(.el-card__header) {
  padding: 14px 16px;
  border-bottom: 1px solid #E2E8F0;
}

.quick-card h4 {
  margin: 0;
  font-size: 14px;
  font-family: 'Calistoga', serif;
  color: #0F172A;
}

.quick-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.quick-tags .el-tag {
  cursor: pointer;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.quick-tags .el-tag:hover {
  transform: translateY(-1px);
  box-shadow: 0 1px 2px rgba(0,0,0,0.05);
}

/* 加载状态 */
.loading-card {
  border: 1px solid #E2E8F0;
  border-radius: 12px;
  box-shadow: 0 4px 6px -1px rgba(0,0,0,0.07), 0 2px 4px -2px rgba(0,0,0,0.07);
  margin-bottom: 24px;
  background: #FFFFFF;
}

.loading-content {
  text-align: center;
  padding: 48px 24px;
}

.loading-icon {
  font-size: 48px;
  color: #0052FF;
  animation: rotate 1s linear infinite;
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.loading-content h3 {
  margin: 16px 0 8px 0;
  color: #0F172A;
  font-family: 'Calistoga', serif;
}

.loading-content p {
  margin: 0;
  color: #64748B;
}

/* 空状态 */
.empty-card {
  border: 1px solid #E2E8F0;
  border-radius: 12px;
  margin-bottom: 24px;
  background: #FFFFFF;
}

.empty-content {
  text-align: center;
  padding: 60px 24px;
}

.empty-content h3 {
  margin: 20px 0 8px 0;
  color: #64748B;
  font-size: 18px;
  font-family: 'Calistoga', serif;
}

.empty-content p {
  margin: 0;
  color: #64748B;
  font-size: 14px;
}

/* 识别结果区域 */
.result-area {
  margin-bottom: 32px;
}

.result-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.result-bar h3 {
  margin: 0;
  font-size: 18px;
  color: #0F172A;
  font-family: 'Calistoga', serif;
}

.result-bar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.recognition-time {
  font-size: 12px;
  color: #64748B;
}

/* 结果卡片网格 — 多食物时并排 */
.result-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(480px, 1fr));
  gap: 20px;
}

.food-result-card {
  border: 1px solid #E2E8F0;
  border-radius: 12px;
  box-shadow: 0 4px 6px -1px rgba(0,0,0,0.07), 0 2px 4px -2px rgba(0,0,0,0.07);
  background: #FFFFFF;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.food-result-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 10px 15px -3px rgba(0,0,0,0.07), 0 4px 6px -4px rgba(0,0,0,0.07);
}

.food-result-card :deep(.el-card__body) {
  padding: 20px;
}

.food-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #E2E8F0;
}

.food-name-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
}

.food-header h4 {
  margin: 0;
  font-size: 18px;
  color: #0F172A;
  font-family: 'Calistoga', serif;
}

/* 核心营养横向展示 */
.core-nutrition {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 10px;
  margin-bottom: 16px;
}

.core-item {
  text-align: center;
  padding: 12px 6px;
  background: #F1F5F9;
  border: 1px solid #E2E8F0;
  border-radius: 8px;
}

.energy-item {
  background: rgba(0,82,255,0.06);
  border: 1px solid #0052FF;
  box-shadow: 0 1px 2px rgba(0,0,0,0.05);
}

.core-label {
  font-size: 12px;
  color: #64748B;
  margin-bottom: 4px;
}

.core-value {
  font-size: 18px;
  font-weight: bold;
  color: #0F172A;
  font-family: 'Calistoga', serif;
}

.core-value .unit {
  font-size: 11px;
  font-weight: normal;
  color: #64748B;
}

/* 折叠面板 */
.detail-collapse {
  margin-bottom: 12px;
  border: none;
}

.detail-collapse :deep(.el-collapse-item__header) {
  font-size: 13px;
  font-weight: 600;
  color: #0F172A;
  height: 36px;
  line-height: 36px;
  font-family: 'Inter', sans-serif;
}

.detail-collapse :deep(.el-collapse-item__wrap) {
  border: none;
}

.nutrition-grid {
  display: grid;
  gap: 8px;
}

.mineral-grid {
  grid-template-columns: repeat(5, 1fr);
}

.nutrition-item-sm {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 8px 4px;
  background: #F1F5F9;
  border: 1px solid #E2E8F0;
  border-radius: 8px;
}

.label-sm {
  font-size: 11px;
  color: #64748B;
  margin-bottom: 2px;
}

.value-sm {
  font-size: 13px;
  font-weight: 600;
  color: #0F172A;
}

.value-sm em {
  font-style: normal;
  font-size: 10px;
  font-weight: normal;
  color: #64748B;
}

.data-source {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #64748B;
  padding-top: 8px;
  border-top: 1px solid #E2E8F0;
}

/* 历史区域 — 卡片网格 */
.history-area {
  margin-top: 8px;
}

.history-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.history-bar h3 {
  margin: 0;
  font-size: 18px;
  color: #0F172A;
  font-family: 'Calistoga', serif;
}

.history-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 16px;
}

.history-card-item {
  border: 1px solid #E2E8F0;
  border-radius: 12px;
  box-shadow: 0 4px 6px -1px rgba(0,0,0,0.07), 0 2px 4px -2px rgba(0,0,0,0.07);
  background: #FFFFFF;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.history-card-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 15px -3px rgba(0,0,0,0.07), 0 4px 6px -4px rgba(0,0,0,0.07);
}

.history-card-item :deep(.el-card__body) {
  padding: 16px;
}

.history-summary {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.history-left {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
}

.history-text {
  font-size: 14px;
  color: #0F172A;
  cursor: pointer;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.history-text:hover {
  color: #0052FF;
}

.history-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.history-time {
  font-size: 12px;
  color: #64748B;
  white-space: nowrap;
}

.expand-icon {
  cursor: pointer;
  color: #64748B;
  font-size: 16px;
}

.expand-icon:hover {
  color: #0052FF;
}

/* 历史详情 */
.history-detail {
  margin-top: 12px;
  padding: 14px;
  background: #F1F5F9;
  border-radius: 8px;
  border-left: 4px solid #0052FF;
}

.detail-info {
  margin-bottom: 12px;
}

.detail-row {
  display: flex;
  align-items: center;
  margin-bottom: 6px;
  font-size: 13px;
}

.detail-label {
  font-weight: 600;
  color: #0F172A;
  margin-right: 8px;
  min-width: 65px;
}

.history-image-section {
  margin-bottom: 12px;
}

.history-image-section h5,
.result-detail h5 {
  margin: 0 0 8px 0;
  font-size: 13px;
  color: #0F172A;
  font-weight: 600;
  font-family: 'Calistoga', serif;
}

.history-image-wrapper {
  text-align: center;
  background: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 8px;
  padding: 8px;
}

.history-image {
  max-width: 100%;
  max-height: 180px;
  border-radius: 4px;
  object-fit: contain;
}

.history-food-item {
  background: #FFFFFF;
  padding: 10px;
  border: 1px solid #E2E8F0;
  border-radius: 8px;
  margin-bottom: 8px;
}

.history-food-item:last-child {
  margin-bottom: 0;
}

.food-name-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.food-name-row strong {
  font-size: 14px;
  color: #0F172A;
}

.nutrition-mini-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 6px;
  font-size: 12px;
  color: #0F172A;
}

.nutrition-mini-grid span {
  padding: 3px 6px;
  background: #F1F5F9;
  border: 1px solid #E2E8F0;
  border-radius: 6px;
  text-align: center;
}

.data-source-mini {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: #64748B;
  margin-top: 4px;
}

/* 响应式 */
@media (max-width: 768px) {
  .food-recognition-view {
    padding: 0 12px 24px;
  }

  .page-topbar {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .input-area {
    grid-template-columns: 1fr;
  }

  .result-cards {
    grid-template-columns: 1fr;
  }

  .core-nutrition {
    grid-template-columns: repeat(3, 1fr);
  }

  .mineral-grid {
    grid-template-columns: repeat(3, 1fr);
  }

  .history-grid {
    grid-template-columns: 1fr;
  }

  .nutrition-mini-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
