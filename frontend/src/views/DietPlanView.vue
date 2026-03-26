<template>
  <div class="diet-plan-view">
    <!-- 顶部标题栏 -->
    <div class="plan-header">
      <div class="header-left">
        <el-button :icon="ArrowLeft" text @click="goToHome"> 返回首页 </el-button>
        <el-divider direction="vertical" />
        <h1 class="header-title">
          <el-icon class="title-icon">
            <Calendar />
          </el-icon>
          AI饮食计划生成
        </h1>
        <el-tag type="success" size="small"> 智能定制 </el-tag>
      </div>
      <div class="header-right">
        <el-button :icon="Clock" @click="showHistory"> 历史记录 </el-button>
        <el-button v-if="generatedPlan" :icon="Download" @click="handleExportPlan">
          导出计划
        </el-button>
        <el-button :icon="Refresh" @click="handleReset"> 重新设置 </el-button>
      </div>
    </div>

    <!-- 主体内容 -->
    <div class="plan-body">
      <!-- 左侧：参数设置表单 -->
      <el-card v-if="!generatedPlan" class="param-card">
        <template #header>
          <div class="card-header">
            <el-icon><Setting /></el-icon>
            <span>个性化设置</span>
          </div>
        </template>

        <el-form
          ref="formRef"
          :model="formData"
          :rules="formRules"
          label-width="120px"
          label-position="left"
        >
          <!-- 基本信息 -->
          <div class="form-section">
            <h3 class="section-title">基本信息</h3>

            <el-form-item label="计划天数" prop="days" required>
              <el-radio-group v-model="formData.days">
                <el-radio-button :value="7"> 7天计划 </el-radio-button>
                <el-radio-button :value="14"> 14天计划 </el-radio-button>
              </el-radio-group>
            </el-form-item>

            <el-form-item label="目标" prop="goal" required>
              <el-select v-model="formData.goal" placeholder="请选择目标" style="width: 100%">
                <el-option label="🔥 减脂塑形" value="lose_weight" />
                <el-option label="💪 增肌强体" value="gain_muscle" />
                <el-option label="❤️ 维持健康" value="maintain" />
              </el-select>
            </el-form-item>

            <el-form-item label="运动强度">
              <el-select
                v-model="formData.exerciseLevel"
                placeholder="请选择运动强度"
                style="width: 100%"
              >
                <el-option label="轻度运动（每周1-3天）" value="low" />
                <el-option label="中度运动（每周3-5天）" value="medium" />
                <el-option label="高强度运动（每周6-7天）" value="high" />
              </el-select>
            </el-form-item>
          </div>

          <!-- 个人数据 -->
          <div class="form-section">
            <h3 class="section-title">个人数据（可选）</h3>

            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="性别">
                  <el-radio-group v-model="formData.gender">
                    <el-radio value="male"> 男 </el-radio>
                    <el-radio value="female"> 女 </el-radio>
                  </el-radio-group>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="年龄">
                  <el-input-number
                    v-model="formData.age"
                    :min="10"
                    :max="100"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="身高 (cm)">
                  <el-input-number
                    v-model="formData.height"
                    :min="100"
                    :max="250"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="体重 (kg)">
                  <el-input-number
                    v-model="formData.weight"
                    :min="30"
                    :max="200"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
            </el-row>

            <el-form-item label="每日热量">
              <el-input-number
                v-model="formData.dailyCalories"
                :min="1000"
                :max="5000"
                :step="100"
                style="width: 100%"
              />
              <span class="form-tip">留空则由AI自动计算</span>
            </el-form-item>
          </div>

          <!-- 饮食偏好 -->
          <div class="form-section">
            <h3 class="section-title">饮食偏好</h3>

            <el-form-item label="饮食偏好">
              <el-input
                v-model="formData.preferences"
                type="textarea"
                :rows="3"
                placeholder="例如：喜欢清淡口味、不吃辛辣、素食主义等"
              />
            </el-form-item>

            <el-form-item label="过敏/忌口">
              <el-input
                v-model="formData.allergies"
                type="textarea"
                :rows="2"
                placeholder="例如：海鲜过敏、不吃香菜、对牛奶过敏等"
              />
            </el-form-item>
          </div>

          <!-- 生成按钮 -->
          <el-form-item>
            <el-button
              type="primary"
              size="large"
              :loading="isGenerating"
              style="width: 100%"
              @click="handleGenerate"
            >
              <el-icon v-if="!isGenerating">
                <MagicStick />
              </el-icon>
              {{ isGenerating ? '正在生成中...' : '开始生成饮食计划' }}
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 右侧/全屏：生成的计划 -->
      <el-card v-if="generatedPlan" class="result-card">
        <template #header>
          <div class="card-header">
            <div>
              <el-icon><Document /></el-icon>
              <span>{{ generatedPlan.title }}</span>
            </div>
            <div class="header-actions">
              <el-tag type="success"> {{ generatedPlan.days }}天计划 </el-tag>
              <el-button
                :type="generatedPlan.isFavorite ? 'warning' : 'default'"
                :icon="generatedPlan.isFavorite ? StarFilled : Star"
                @click="toggleFavorite"
              >
                {{ generatedPlan.isFavorite ? '已收藏' : '收藏' }}
              </el-button>
              <el-button type="success" :icon="Edit" @click="openModifyDialog">
                修改建议
              </el-button>
              <el-button
                type="primary"
                :icon="Download"
                :loading="isExportingPdf"
                @click="handleExportPdf"
              >
                导出PDF
              </el-button>
              <el-button type="info" :icon="Close" @click="handleClosePlan"> 关闭 </el-button>
            </div>
          </div>
        </template>

        <!-- Markdown渲染内容 -->
        <div class="markdown-content" v-html="renderedMarkdown" />
      </el-card>

      <!-- 生成中的加载状态 -->
      <el-card v-if="isGenerating" class="loading-card">
        <div class="loading-content">
          <el-icon class="loading-icon">
            <Loading />
          </el-icon>
          <h3>AI正在为您生成个性化饮食计划...</h3>
          <p>{{ getEstimatedTimeText() }}</p>
          <el-progress :percentage="Math.floor(progress)" :stroke-width="8" />
          <p style="margin-top: 12px; font-size: 14px; color: #909399">
            正在逐天生成详细计划，请耐心等待...
          </p>
          <el-button
            v-if="currentTaskId"
            type="danger"
            plain
            style="margin-top: 16px"
            @click="handleCancelGenerate"
          >
            取消生成
          </el-button>
          <p v-else style="margin-top: 16px; color: #909399; font-size: 14px">
            正在创建任务，请稍候...
          </p>
        </div>
      </el-card>
    </div>

    <!-- 历史记录抽屉 -->
    <el-drawer v-model="historyDrawerVisible" title="历史记录" direction="rtl" size="400px">
      <template #header>
        <div
          style="display: flex; align-items: center; justify-content: space-between; width: 100%"
        >
          <span style="font-size: 16px; font-weight: 600">历史记录</span>
          <el-button
            :type="showFavoritesOnly ? 'warning' : 'default'"
            size="small"
            :icon="showFavoritesOnly ? StarFilled : Star"
            @click="toggleFavoritesFilter"
          >
            {{ showFavoritesOnly ? '仅收藏' : '全部' }}
          </el-button>
        </div>
      </template>
      <div v-loading="historyLoading">
        <el-empty
          v-if="!historyLoading && historyList.length === 0"
          :description="showFavoritesOnly ? '暂无收藏记录' : '暂无历史记录'"
        />

        <el-timeline v-else>
          <el-timeline-item
            v-for="item in historyList"
            :key="item.planId"
            :timestamp="new Date(item.createdAt).toLocaleString('zh-CN')"
            placement="top"
          >
            <el-card class="history-item" shadow="hover">
              <div class="history-item-content" @click="loadHistoryDetail(item.planId)">
                <h4>
                  {{ item.title }}
                  <el-icon v-if="item.isFavorite" style="color: #e6a23c; margin-left: 4px"
                    ><StarFilled
                  /></el-icon>
                </h4>
                <p>
                  <el-tag size="small" type="primary"> {{ item.days }}天 </el-tag>
                  <el-tag size="small" style="margin-left: 8px">
                    {{ getGoalText(item.goal) }}
                  </el-tag>
                </p>
              </div>
              <div class="history-item-actions">
                <el-button
                  type="danger"
                  :icon="Delete"
                  size="small"
                  circle
                  title="删除记录"
                  @click.stop="confirmDeleteHistory(item.planId)"
                />
              </div>
            </el-card>
          </el-timeline-item>
        </el-timeline>
      </div>
    </el-drawer>

    <!-- 修改建议对话框 -->
    <el-dialog
      v-model="modifyDialogVisible"
      title="提出修改建议"
      width="560px"
      :close-on-click-modal="false"
    >
      <el-form>
        <el-form-item label="修改建议">
          <el-input
            v-model="modifySuggestion"
            type="textarea"
            :rows="5"
            placeholder="请描述您希望对饮食计划做的修改，例如：&#10;- 把第3天的午餐换成素食&#10;- 减少主食比例，增加蛋白质&#10;- 去掉所有海鲜相关的食材&#10;- 早餐增加水果"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="modifyDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="isModifying" @click="submitModifySuggestion">
          {{ isModifying ? 'AI修改中...' : '提交修改' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Calendar,
  Download,
  Refresh,
  Setting,
  MagicStick,
  Document,
  Loading,
  Clock,
  ArrowLeft,
  Close,
  Delete,
  Star,
  StarFilled,
  Edit
} from '@element-plus/icons-vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import api from '@/services/api.js'

// 路由
const router = useRouter()

// 表单引用
const formRef = ref(null)

// 表单数据
const formData = reactive({
  days: 7,
  goal: 'maintain',
  exerciseLevel: 'medium',
  gender: null,
  age: null,
  height: null,
  weight: null,
  dailyCalories: null,
  preferences: '',
  allergies: ''
})

// 表单验证规则
const formRules = {
  days: [{ required: true, message: '请选择计划天数', trigger: 'change' }],
  goal: [{ required: true, message: '请选择目标', trigger: 'change' }]
}

// 状态
const isGenerating = ref(false)
const generatedPlan = ref(null)
const progress = ref(0)
const isExportingPdf = ref(false)
const currentTaskId = ref(null)
const pollInterval = ref(null)
const progressTimer = ref(null)
const historyDrawerVisible = ref(false)
const historyList = ref([])
const historyLoading = ref(false)
const modifyDialogVisible = ref(false)
const modifySuggestion = ref('')
const isModifying = ref(false)
const showFavoritesOnly = ref(false)

// 配置marked
marked.setOptions({
  breaks: true,
  gfm: true,
  headerIds: true,
  mangle: false
})

// 渲染Markdown（带XSS防护）
const renderedMarkdown = computed(() => {
  if (!generatedPlan.value || !generatedPlan.value.markdownContent) {
    return ''
  }

  // 1. 使用marked将Markdown转换为HTML
  const html = marked.parse(generatedPlan.value.markdownContent)

  // 2. 使用DOMPurify清理HTML，防止XSS攻击
  const cleanHtml = DOMPurify.sanitize(html, {
    ALLOWED_TAGS: [
      'h1',
      'h2',
      'h3',
      'h4',
      'h5',
      'h6',
      'p',
      'br',
      'hr',
      'strong',
      'em',
      'u',
      'del',
      'ul',
      'ol',
      'li',
      'blockquote',
      'pre',
      'code',
      'table',
      'thead',
      'tbody',
      'tr',
      'th',
      'td',
      'a',
      'img'
    ],
    ALLOWED_ATTR: ['href', 'src', 'alt', 'title', 'class', 'id'],
    ALLOW_DATA_ATTR: false
  })

  return cleanHtml
})

// 生成饮食计划（异步）
const handleGenerate = async () => {
  try {
    await formRef.value.validate()

    isGenerating.value = true
    progress.value = 0

    const { data } = await api.post('/diet-plan/generate', formData)

    if (data.code === 200) {
      currentTaskId.value = data.data.taskId
      localStorage.setItem('currentTaskId', currentTaskId.value)
      ElMessage.success('任务已创建，开始生成...')
      startPolling()
    } else {
      throw new Error(data.message || '创建任务失败')
    }
  } catch (error) {
    if (error?.response?.status === 401) {
      ElMessage.error('登录已过期，请重新登录')
      return
    }
    ElMessage.error('创建任务失败: ' + (error.response?.data?.message || error.message))
    isGenerating.value = false
    progress.value = 0
  }
}

// 清理所有定时器
const clearTimers = () => {
  if (pollInterval.value) {
    clearInterval(pollInterval.value)
    pollInterval.value = null
  }
  if (progressTimer.value) {
    clearInterval(progressTimer.value)
    progressTimer.value = null
  }
}

// 开始轮询任务状态
const startPolling = () => {
  clearTimers()

  const estimatedSeconds = formData.days * 40
  const progressStep = 100 / estimatedSeconds

  // 模拟进度条
  progressTimer.value = setInterval(() => {
    if (progress.value < 95) {
      progress.value += progressStep
    }
  }, 1000)

  // 轮询任务状态（每2秒查询一次）
  pollInterval.value = setInterval(async () => {
    try {
      const { data } = await api.get(`/diet-plan/task/${currentTaskId.value}/status`)

      if (data.code === 200) {
        const status = data.data

        if (status.progress) {
          progress.value = Math.max(progress.value, status.progress)
        }

        if (status.status === 'completed') {
          clearTimers()
          progress.value = 100
          await loadPlanDetail(status.planId)
          isGenerating.value = false
          currentTaskId.value = null
          localStorage.removeItem('currentTaskId')
          ElMessage.success('饮食计划生成成功！')
          setTimeout(() => window.scrollTo({ top: 0, behavior: 'smooth' }), 100)
        } else if (status.status === 'failed') {
          clearTimers()
          isGenerating.value = false
          currentTaskId.value = null
          localStorage.removeItem('currentTaskId')
          ElMessage.error('生成失败: ' + (status.errorMessage || '未知错误'))
        } else if (status.status === 'cancelled') {
          clearTimers()
          isGenerating.value = false
          currentTaskId.value = null
          localStorage.removeItem('currentTaskId')
          ElMessage.info('任务已取消')
        }
      }
    } catch (error) {
      // 静默处理轮询错误，避免刷屏
    }
  }, 2000)
}

// 加载计划详情
const loadPlanDetail = async planId => {
  try {
    const { data } = await api.get(`/diet-plan/history/${planId}`)
    if (data.code === 200 && data.data) {
      generatedPlan.value = data.data
    } else {
      ElMessage.error(data.message || '加载计划详情失败')
    }
  } catch (error) {
    ElMessage.error('加载计划详情失败: ' + (error.response?.data?.message || error.message))
  }
}

// 获取目标文本
const getGoalText = goal => {
  const goalMap = {
    lose_weight: '减脂塑形',
    gain_muscle: '增肌强壮',
    maintain: '健康维持'
  }
  return goalMap[goal] || goal
}

// 获取预计时间文本
const getEstimatedTimeText = () => {
  const days = formData.days || 7
  const minutes = Math.ceil((days * 40) / 60)
  return `预计需要 ${minutes} 分钟（生成 ${days} 天计划）`
}

// 导出计划为Markdown
const handleExportPlan = () => {
  if (!generatedPlan.value) {
    return
  }

  try {
    const content = generatedPlan.value.markdownContent
    const blob = new Blob([content], { type: 'text/markdown;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `${generatedPlan.value.title}_${Date.now()}.md`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)

    ElMessage.success('饮食计划已导出')
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('导出失败')
  }
}

// 导出计划为PDF
const handleExportPdf = async () => {
  if (!generatedPlan.value) {
    ElMessage.warning('请先生成饮食计划')
    return
  }

  isExportingPdf.value = true

  try {
    const response = await api.get(`/diet-plan/export-pdf/${generatedPlan.value.planId}`, {
      responseType: 'blob'
    })

    // 检查返回的是否为JSON错误(非PDF)
    if (response.data.type && response.data.type.includes('application/json')) {
      const text = await response.data.text()
      const errorData = JSON.parse(text)
      if (errorData.code === 403) {
        ElMessage.warning('此功能仅限黄金会员使用')
        return
      }
      throw new Error(errorData.message || '导出失败')
    }

    const blob = response.data
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${generatedPlan.value.title}.pdf`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)

    ElMessage.success('PDF导出成功！')
  } catch (error) {
    if (error?.response?.status === 404) {
      ElMessage.error('计划不存在或已过期，请重新生成')
    } else {
      ElMessage.error('PDF导出失败: ' + (error.response?.data?.message || error.message))
    }
  } finally {
    isExportingPdf.value = false
  }
}

// 重置
const handleReset = () => {
  ElMessageBox.confirm('确定要重新设置吗？当前的计划内容将被清空。', '重新设置', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
    center: true,
    customClass: 'custom-message-box'
  })
    .then(() => {
      // 清空生成的计划
      generatedPlan.value = null
      progress.value = 0
      isGenerating.value = false
      currentTaskId.value = null

      // 手动重置表单数据到初始值
      Object.assign(formData, {
        days: 7,
        goal: 'maintain',
        exerciseLevel: 'medium',
        gender: null,
        age: null,
        height: null,
        weight: null,
        dailyCalories: null,
        preferences: '',
        allergies: ''
      })

      // 清除表单验证状态
      if (formRef.value) {
        formRef.value.clearValidate()
      }

      // 清空localStorage
      localStorage.removeItem('currentTaskId')
      localStorage.removeItem('generatingPlan')

      ElMessage.success('已重置')

      // 滚动到顶部
      window.scrollTo({ top: 0, behavior: 'smooth' })
    })
    .catch(() => {
      // 取消重置
    })
}

// 关闭详细计划
const handleClosePlan = () => {
  generatedPlan.value = null
  ElMessage.info('已关闭饮食计划')
}

// 返回首页
const goToHome = () => {
  if (isGenerating.value) {
    ElMessageBox.confirm(
      '饮食计划正在生成中，返回首页后生成将在后台继续进行，您可以稍后在历史记录中查看。',
      '提示',
      {
        confirmButtonText: '确定返回',
        cancelButtonText: '继续等待',
        type: 'warning',
        center: true,
        customClass: 'custom-message-box'
      }
    )
      .then(() => {
        router.push('/')
      })
      .catch(() => {
        // 用户选择继续等待
      })
  } else {
    router.push('/')
  }
}

// 显示历史记录
const showHistory = async () => {
  historyDrawerVisible.value = true
  historyLoading.value = true

  try {
    const url = showFavoritesOnly.value
      ? '/diet-plan/favorites?page=1&size=20'
      : '/diet-plan/history?page=1&size=20'
    const { data } = await api.get(url)

    if (data.code === 200) {
      historyList.value = data.data.content || []
    } else {
      ElMessage.error(data.message || '加载历史记录失败')
      historyList.value = []
    }
  } catch (error) {
    ElMessage.error('加载历史记录失败: ' + (error.response?.data?.message || error.message))
    historyList.value = []
  } finally {
    historyLoading.value = false
  }
}

// 加载历史记录详情
const loadHistoryDetail = async planId => {
  try {
    await loadPlanDetail(planId)
    historyDrawerVisible.value = false
    setTimeout(() => window.scrollTo({ top: 0, behavior: 'smooth' }), 100)
  } catch (error) {
    ElMessage.error('加载失败')
  }
}

// 确认删除历史记录
const confirmDeleteHistory = planId => {
  ElMessageBox.confirm('确定要删除这条饮食计划记录吗？', '删除确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
    center: true,
    customClass: 'custom-message-box'
  })
    .then(() => {
      deleteHistory(planId)
    })
    .catch(() => {
      // 取消删除
    })
}

// 删除历史记录
const deleteHistory = async planId => {
  try {
    const { data } = await api.delete(`/diet-plan/${planId}`)

    if (data.code === 200) {
      // 直接从列表中移除，而非重新加载整个列表
      historyList.value = historyList.value.filter(item => {
        const itemId = item.planId || item.id
        return itemId !== planId
      })
      ElMessage.success('删除成功')
    } else {
      throw new Error(data.message)
    }
  } catch (error) {
    ElMessage.error('删除失败: ' + (error.response?.data?.message || error.message))
  }
}

// 取消生成
const handleCancelGenerate = () => {
  if (!currentTaskId.value) {
    ElMessage.warning('没有正在进行的任务')
    return
  }

  const taskIdToCancel = currentTaskId.value

  ElMessageBox.confirm(
    '确定要取消当前的饮食计划生成吗？取消后将在当前天数生成完成后停止（约30-40秒）。',
    '确认取消',
    {
      confirmButtonText: '确定取消',
      cancelButtonText: '继续生成',
      type: 'warning',
      center: true,
      customClass: 'custom-message-box'
    }
  )
    .then(async () => {
      try {
        const { data } = await api.post(`/diet-plan/task/${taskIdToCancel}/cancel`)

        if (data.code === 200) {
          clearTimers()
          isGenerating.value = false
          progress.value = 0
          currentTaskId.value = null
          localStorage.removeItem('currentTaskId')
          ElMessage.success('已取消生成')
        } else {
          ElMessage.error('取消失败: ' + data.message)
        }
      } catch (error) {
        ElMessage.error('取消失败: ' + (error.response?.data?.message || error.message))
      }
    })
    .catch(() => {})
}

// 初始化
onMounted(async () => {
  const savedTaskId = localStorage.getItem('currentTaskId')

  if (savedTaskId) {
    currentTaskId.value = savedTaskId
    isGenerating.value = true

    try {
      const { data } = await api.get(`/diet-plan/task/${savedTaskId}/status`)

      if (data.code === 200 && data.data) {
        const status = data.data

        if (status.progress) {
          progress.value = status.progress
        }

        if (status.status === 'completed') {
          await loadPlanDetail(status.planId)
          isGenerating.value = false
          currentTaskId.value = null
          localStorage.removeItem('currentTaskId')
          return
        } else if (status.status === 'failed' || status.status === 'cancelled') {
          isGenerating.value = false
          currentTaskId.value = null
          localStorage.removeItem('currentTaskId')
          ElMessage.error(status.status === 'failed' ? '任务已失败' : '任务已取消')
          return
        }
      }
    } catch (error) {
      // 恢复失败时静默处理
    }

    ElMessage.info('检测到未完成的任务，继续生成...')
    startPolling()
  }
})

// 组件卸载前清理
onBeforeUnmount(() => {
  clearTimers()
})

// 收藏/取消收藏
const toggleFavorite = async () => {
  if (!generatedPlan.value) return

  try {
    const { data } = await api.post(`/diet-plan/${generatedPlan.value.planId}/favorite`)

    if (data.code === 200) {
      generatedPlan.value.isFavorite = data.data.isFavorite
      ElMessage.success(data.data.isFavorite ? '已收藏' : '已取消收藏')
    }
  } catch (error) {
    ElMessage.error('操作失败: ' + (error.response?.data?.message || error.message))
  }
}

// 切换收藏筛选
const toggleFavoritesFilter = () => {
  showFavoritesOnly.value = !showFavoritesOnly.value
  showHistory()
}

// 打开修改建议对话框
const openModifyDialog = () => {
  if (!generatedPlan.value) return
  modifySuggestion.value = ''
  modifyDialogVisible.value = true
}

// 提交修改建议
const submitModifySuggestion = async () => {
  if (!modifySuggestion.value.trim()) {
    ElMessage.warning('请输入修改建议')
    return
  }

  isModifying.value = true

  try {
    const { data } = await api.post(`/diet-plan/${generatedPlan.value.planId}/modify`, {
      suggestion: modifySuggestion.value.trim()
    })

    if (data.code === 200) {
      modifyDialogVisible.value = false
      currentTaskId.value = data.data.taskId
      localStorage.setItem('currentTaskId', currentTaskId.value)
      isGenerating.value = true
      progress.value = 0
      generatedPlan.value = null
      ElMessage.success('修改任务已创建，正在生成修改版计划...')
      startPolling()
    } else {
      throw new Error(data.message || '创建修改任务失败')
    }
  } catch (error) {
    ElMessage.error('创建修改任务失败: ' + (error.response?.data?.message || error.message))
  } finally {
    isModifying.value = false
  }
}
</script>

<style scoped>
.diet-plan-view {
  min-height: 100vh;
  background: #f5f7fa;
  display: flex;
  flex-direction: column;
}

/* 顶部标题栏 */
.plan-header {
  height: 60px;
  background: white;
  border-bottom: 1px solid #e4e7ed;
  padding: 0 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-shrink: 0;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-title {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.title-icon {
  color: #67c23a;
  font-size: 24px;
}

.header-right {
  display: flex;
  gap: 8px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

/* 主体内容 */
.plan-body {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}

.param-card,
.result-card,
.loading-card {
  max-width: 1200px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  justify-content: space-between;
}

/* 表单 */
.form-section {
  margin-bottom: 32px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 16px;
  padding-left: 12px;
  border-left: 3px solid #67c23a;
}

.form-tip {
  display: block;
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.preview-content {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

/* 历史记录样式 */
.history-item {
  transition: all 0.3s;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.history-item-content {
  flex: 1;
  cursor: pointer;
}

.history-item-content:hover {
  color: #409eff;
}

.history-item-actions {
  flex-shrink: 0;
}

.history-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.history-item h4 {
  margin: 0 0 8px 0;
  font-size: 16px;
  color: #303133;
}

.history-item p {
  margin: 0;
  display: flex;
  align-items: center;
}

/* Markdown内容 */
.markdown-content {
  line-height: 1.8;
  color: #303133;
  font-size: 15px;
}

.markdown-content :deep(h1) {
  font-size: 28px;
  font-weight: 600;
  margin: 24px 0 16px;
  padding-bottom: 8px;
  border-bottom: 2px solid #67c23a;
  color: #67c23a;
}

.markdown-content :deep(h2) {
  font-size: 24px;
  font-weight: 600;
  margin: 20px 0 12px;
  color: #409eff;
}

.markdown-content :deep(h3) {
  font-size: 20px;
  font-weight: 600;
  margin: 16px 0 8px;
  color: #606266;
}

.markdown-content :deep(p) {
  margin: 12px 0;
}

.markdown-content :deep(ul),
.markdown-content :deep(ol) {
  margin: 12px 0;
  padding-left: 24px;
}

.markdown-content :deep(li) {
  margin: 8px 0;
}

.markdown-content :deep(strong) {
  color: #409eff;
  font-weight: 600;
}

.markdown-content :deep(code) {
  background: #f5f7fa;
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 0.9em;
  color: #e6a23c;
}

.markdown-content :deep(pre) {
  background: #f5f7fa;
  padding: 16px;
  border-radius: 8px;
  overflow-x: auto;
  margin: 16px 0;
}

.markdown-content :deep(blockquote) {
  border-left: 4px solid #67c23a;
  padding-left: 16px;
  margin: 16px 0;
  color: #606266;
  background: #f0f9ff;
  padding: 12px 16px;
  border-radius: 4px;
}

.markdown-content :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 16px 0;
}

.markdown-content :deep(th),
.markdown-content :deep(td) {
  border: 1px solid #dcdfe6;
  padding: 12px;
  text-align: left;
}

.markdown-content :deep(th) {
  background: #f5f7fa;
  font-weight: 600;
}

.markdown-content :deep(tr:hover) {
  background: #fafafa;
}

/* 加载状态 */
.loading-card {
  min-height: 400px;
}

.loading-content {
  text-align: center;
  padding: 60px 20px;
}

.loading-icon {
  font-size: 48px;
  color: #67c23a;
  animation: rotate 2s linear infinite;
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
  margin: 24px 0 12px;
  color: #303133;
}

.loading-content p {
  color: #909399;
  margin-bottom: 24px;
}

/* 响应式 */
@media (max-width: 768px) {
  .plan-header {
    padding: 0 16px;
  }

  .plan-body {
    padding: 16px;
  }

  .header-title {
    font-size: 18px;
  }
}
</style>

<style>
/* 自定义MessageBox样式 - 全局样式 */
.custom-message-box {
  width: 420px !important;
  max-width: 90vw !important;
  background: white !important;
  border-radius: 8px !important;
  position: fixed !important;
  top: 50% !important;
  left: 50% !important;
  transform: translate(-50%, -50%) !important;
  margin: 0 !important;
}

.custom-message-box .el-message-box__header {
  padding: 20px 20px 10px !important;
  display: flex !important;
  align-items: center !important;
  justify-content: space-between !important;
}

.custom-message-box .el-message-box__title {
  font-size: 18px !important;
  font-weight: 600 !important;
  flex: 1 !important;
}

.custom-message-box .el-message-box__headerbtn {
  position: static !important;
  top: auto !important;
  right: auto !important;
  margin-left: 12px !important;
}

.custom-message-box .el-message-box__content {
  padding: 10px 20px !important;
  color: #606266 !important;
  font-size: 14px !important;
  line-height: 1.6 !important;
}

.custom-message-box .el-message-box__btns {
  padding: 10px 20px 20px !important;
  text-align: center !important;
}
</style>
