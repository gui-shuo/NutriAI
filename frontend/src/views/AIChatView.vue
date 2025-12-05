<template>
  <div class="ai-chat-view">
    <!-- 顶部标题栏 -->
    <div class="chat-header">
      <div class="header-left">
        <el-button :icon="ArrowLeft" @click="goToHome" text>
          返回首页
        </el-button>
        <el-divider direction="vertical" />
        <h1 class="header-title">
          <el-icon class="title-icon"><ChatDotRound /></el-icon>
          AI营养师
        </h1>
        <el-tag size="small" effect="plain">智能对话</el-tag>
      </div>
      <div class="header-right">
        <el-button :icon="FolderOpened" circle @click.stop="handleShowHistory" title="历史记录" />
        <el-button :icon="Star" circle @click.stop="handleShowFavorites" title="收藏" />
        <el-button :icon="Delete" circle @click.stop="handleClearHistory" title="清空对话" />
        <el-button :icon="Download" circle @click.stop="handleExport" title="导出对话" />
        <el-button :icon="Setting" circle @click.stop="handleShowSettings" title="设置" />
      </div>
    </div>

    <!-- 主体内容 -->
    <div class="chat-body">
      <!-- 消息列表 -->
      <MessageList
        ref="messageListRef"
        :messages="messages"
        @regenerate="handleRegenerate"
        @favorite="handleFavorite"
        @unfavorite="handleUnfavorite"
      />

      <!-- 快捷操作 -->
      <QuickActions
        :disabled="isLoading"
        :expandable="true"
        :default-expanded="messages.length === 0"
        @action="handleQuickAction"
      />

      <!-- 输入区域 -->
      <ChatInput
        ref="chatInputRef"
        :disabled="isLoading"
        :loading="isLoading"
        @send="handleSend"
        @file-select="handleFileSelect"
      />
    </div>

    <!-- 设置对话框 -->
    <el-dialog
      v-model="showSettings"
      title="聊天设置"
      width="500px"
      :append-to-body="true"
      :close-on-click-modal="false"
      :destroy-on-close="false"
    >
      <el-form label-width="120px">
        <el-form-item label="AI模型">
          <el-select v-model="settings.model" style="width: 100%">
            <el-option label="通义千问" value="qwen-max" />
            <el-option label="通义千问Plus" value="qwen-plus" />
            <el-option label="通义千问Turbo" value="qwen-turbo" />
          </el-select>
        </el-form-item>
        <el-form-item label="温度参数">
          <el-slider v-model="settings.temperature" :min="0" :max="1" :step="0.1" show-input />
          <span class="setting-tip">控制回答的随机性，值越大回答越发散</span>
        </el-form-item>
        <el-form-item label="最大字数">
          <el-input-number v-model="settings.maxTokens" :min="500" :max="4000" :step="100" />
          <span class="setting-tip">单次回复的最大字数</span>
        </el-form-item>
        <el-form-item label="上下文记忆">
          <el-switch v-model="settings.keepContext" />
          <span class="setting-tip">是否保持对话上下文</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showSettings = false">取消</el-button>
        <el-button type="primary" @click="handleSaveSettings">保存</el-button>
      </template>
    </el-dialog>

    <!-- 历史记录对话框 -->
    <el-dialog
      v-model="showHistory"
      title="历史记录"
      width="700px"
      :append-to-body="true"
      :close-on-click-modal="false"
      :destroy-on-close="false"
    >
      <div class="history-list">
        <el-empty v-if="historyList.length === 0" description="暂无历史记录" />
        <div
          v-else
          v-for="item in historyList"
          :key="item.id"
          class="history-item"
          @click="loadHistoryConversation(item)"
        >
          <div class="history-header">
            <span class="history-title">{{ item.title || '未命名对话' }}</span>
            <span class="history-time">{{ formatTime(item.timestamp) }}</span>
          </div>
          <div class="history-preview">{{ item.preview }}</div>
          <div class="history-actions">
            <el-button
              link
              size="small"
              type="danger"
              @click.stop="deleteHistory(item.id)"
            >
              删除
            </el-button>
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- 收藏对话框 -->
    <el-dialog
      v-model="showFavorites"
      title="我的收藏"
      width="700px"
      :append-to-body="true"
      :close-on-click-modal="false"
      :destroy-on-close="false"
    >
      <div class="favorites-list">
        <el-empty v-if="favoriteMessages.length === 0" description="暂无收藏" />
        <div
          v-else
          v-for="msg in favoriteMessages"
          :key="msg.id"
          class="favorite-item"
        >
          <div class="favorite-content" v-html="renderMarkdown(msg.content)"></div>
          <div class="favorite-footer">
            <span class="favorite-time">{{ formatTime(msg.timestamp) }}</span>
            <div class="favorite-actions">
              <el-button link size="small" @click="copyMessage(msg.content)">
                <el-icon><DocumentCopy /></el-icon> 复制
              </el-button>
              <el-button link size="small" type="danger" @click="handleUnfavorite(msg)">
                <el-icon><Delete /></el-icon> 取消收藏
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ChatDotRound,
  Delete,
  Download,
  FolderOpened,
  Setting,
  Star,
  DocumentCopy,
  ArrowLeft
} from '@element-plus/icons-vue'
import MessageList from '@/components/chat/MessageList.vue'
import ChatInput from '@/components/chat/ChatInput.vue'
import QuickActions from '@/components/chat/QuickActions.vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'

// 路由
const router = useRouter()

// 组件引用
const messageListRef = ref(null)
const chatInputRef = ref(null)

// 状态
const messages = ref([])
const historyList = ref([])
const favoritesList = ref([]) // 收藏列表
const currentHistoryId = ref(null) // 当前会话ID
const isLoading = ref(false)
const showHistory = ref(false)
const showFavorites = ref(false)
const showSettings = ref(false)

// 设置
const settings = reactive({
  model: 'qwen-max',
  temperature: 0.7,
  maxTokens: 2000,
  keepContext: true,
  autoSave: true
})

// 收藏消息计算属性（从收藏列表中获取）
const favoriteMessages = computed(() => {
  return favoritesList.value
})

// 生成消息ID
const generateMessageId = () => {
  return `msg_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
}

// 发送消息
const handleSend = async ({ text, file }) => {
  if (!text && !file) {
    return
  }

  // 创建用户消息
  const userMessage = {
    id: generateMessageId(),
    role: 'user',
    content: text,
    timestamp: Date.now(),
    file: file ? {
      name: file.name,
      size: file.size,
      type: file.type
    } : null
  }

  messages.value.push(userMessage)

  // 清空输入框
  chatInputRef.value?.clear()

  // 创建AI消息（加载中）
  const aiMessage = {
    id: generateMessageId(),
    role: 'assistant',
    content: '',
    timestamp: Date.now(),
    loading: true
  }

  messages.value.push(aiMessage)
  isLoading.value = true

  try {
    // 调用真实后端API
    const token = localStorage.getItem('token')
    
    if (!token) {
      console.error('❌ 未找到token，用户未登录')
      ElMessage.error('请先登录')
      const index = messages.value.findIndex(m => m.id === aiMessage.id)
      if (index > -1) {
        messages.value.splice(index, 1)
      }
      return
    }

    console.log('🚀 开始调用AI API...')
    console.log('📝 请求参数:', {
      message: text.substring(0, 50) + (text.length > 50 ? '...' : ''),
      model: settings.model,
      temperature: settings.temperature,
      maxTokens: settings.maxTokens,
      keepContext: settings.keepContext
    })

    const startTime = Date.now()
    const response = await fetch('http://localhost:8080/api/ai/chat', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({
        message: text,
        model: settings.model,
        temperature: settings.temperature,
        maxTokens: settings.maxTokens,
        keepContext: settings.keepContext
      })
    })

    const duration = Date.now() - startTime
    console.log(`⏱️ API响应时间: ${duration}ms`)

    if (!response.ok) {
      console.error(`❌ HTTP错误: ${response.status} ${response.statusText}`)
      throw new Error(`HTTP ${response.status}: ${response.statusText}`)
    }

    const data = await response.json()
    console.log('📦 API响应数据:', data)
    
    if (data.code === 200) {
      // 提取AI回复
      const aiResponse = data.data.message || data.data
      
      if (!aiResponse || aiResponse.trim() === '') {
        console.warn('⚠️ AI回复为空')
        throw new Error('AI回复为空，请重试')
      }
      
      console.log('✅ AI回复成功，长度:', aiResponse.length)
      console.log('📝 AI回复内容:', aiResponse.substring(0, 100))
      
      // 找到messages数组中的AI消息并更新（确保Vue响应式更新）
      const index = messages.value.findIndex(m => m.id === aiMessage.id)
      if (index > -1) {
        messages.value[index] = {
          ...messages.value[index],
          content: aiResponse,
          loading: false
        }
        console.log('✅ 消息已更新到数组索引:', index)
      } else {
        console.error('❌ 未找到消息在数组中')
      }
    } else {
      console.error('❌ API返回错误:', data.message)
      throw new Error(data.message || '请求失败')
    }
  } catch (error) {
    console.error('❌ 发送消息失败:', error)
    
    // 详细的错误信息
    let errorMessage = '发送失败'
    if (error.message.includes('Failed to fetch')) {
      errorMessage = '无法连接到服务器，请检查后端是否启动'
      console.error('💡 提示: 请确保后端服务运行在 http://localhost:8080')
    } else if (error.message.includes('401')) {
      errorMessage = '认证失败，请重新登录'
      console.error('💡 提示: token可能已过期')
    } else if (error.message.includes('500')) {
      errorMessage = 'AI服务异常，请检查API Key配置'
      console.error('💡 提示: 检查后端日志，确认TONGYI_API_KEY是否正确配置')
    } else {
      errorMessage = `发送失败：${error.message}`
    }
    
    ElMessage.error(errorMessage)
    
    // 移除失败的AI消息
    const index = messages.value.findIndex(m => m.id === aiMessage.id)
    if (index > -1) {
      messages.value.splice(index, 1)
    }
  } finally {
    isLoading.value = false
  }
}

// 模拟AI响应（实际开发中替换为真实API调用）
const simulateAIResponse = async (aiMessage, userInput) => {
  return new Promise((resolve) => {
    // 模拟网络延迟
    setTimeout(() => {
      // 根据用户输入生成不同的回复
      let response = ''
      
      if (userInput.includes('苹果') || userInput.includes('营养')) {
        response = `## 苹果的营养成分分析

**基本信息**（每100g）：
- 🔥 能量：53 kcal
- 🥤 水分：85%
- 🍎 膳食纤维：1.2g

**主要营养素**：
1. **碳水化合物**：13.7g
   - 主要是天然果糖
   - 提供快速能量
2. **维生素C**：4mg
   - 增强免疫力
   - 抗氧化作用
3. **钾**：119mg
   - 调节血压
   - 维持心脏健康

**健康益处**：
- ✅ 低热量，适合减肥
- ✅ 富含膳食纤维，促进消化
- ✅ 含有多种抗氧化物质
- ✅ 有助于控制血糖

**食用建议**：
- 每天1-2个为宜
- 饭前或两餐之间食用最佳
- 连皮一起吃营养更全面（需清洗干净）

需要我帮你制定包含苹果的饮食计划吗？ 🍎`
      } else if (userInput.includes('计划') || userInput.includes('饮食')) {
        response = `## 健康饮食计划建议

我可以根据你的具体需求为你制定个性化的饮食计划。

**请告诉我以下信息**：

1. **目标**：减脂 / 增肌 / 维持健康？
2. **时长**：7天 / 14天 / 30天？
3. **饮食偏好**：有无忌口或过敏食物？
4. **运动情况**：轻度 / 中度 / 高强度？

**我可以提供**：
- 📅 详细的每日三餐安排
- 📊 精确的营养素配比
- 🛒 食材采购清单
- 👨‍🍳 简单易做的烹饪方法

请补充你的信息，我会为你量身定制！ 💪`
      } else if (userInput.includes('能帮') || userInput.includes('做什么')) {
        response = `## 我能为你提供的服务

你好！我是你的AI营养师，我可以帮你：

### 📊 营养分析
- 分析食物的营养成分
- 评估每日营养摄入
- 对比不同食物的营养价值

### 🍽️ 饮食计划
- 制定个性化饮食方案
- 提供减肥/增肌/健康饮食建议
- 生成食材采购清单

### 💡 健康建议
- 解答营养相关问题
- 提供健康饮食tips
- 推荐适合的食谱

### 🔍 食物识别
- 上传食物图片识别
- 估算食物热量
- 营养成分分析

有什么我可以帮助你的吗？😊`
      } else {
        response = `我理解你的问题了。

${userInput}

这是一个很好的问题！基于你的需求，我建议：

1. **了解基础营养知识**
   - 三大营养素的作用和配比
   - 如何计算每日所需热量

2. **关注食物质量**
   - 选择天然、新鲜的食材
   - 减少加工食品的摄入

3. **保持饮食多样性**
   - 每天摄入多种颜色的蔬果
   - 适量摄入优质蛋白

如果你想了解更详细的信息，可以：
- 点击快捷按钮选择具体话题
- 直接问我更具体的问题
- 上传食物图片让我分析

我随时准备帮助你！🌟`
      }

      // 更新AI消息
      aiMessage.loading = false
      aiMessage.content = response
      aiMessage.timestamp = Date.now()

      resolve()
    }, 1500)
  })
}

// 快捷操作
const handleQuickAction = (content) => {
  handleSend({ text: content })
}

// 重新生成
const handleRegenerate = async (message) => {
  ElMessage.info('正在重新生成...')
  
  // 找到这条消息的上一条用户消息
  const messageIndex = messages.value.findIndex(m => m.id === message.id)
  if (messageIndex > 0) {
    const userMessage = messages.value[messageIndex - 1]
    if (userMessage.role === 'user') {
      // 移除当前AI消息
      messages.value.splice(messageIndex, 1)
      
      // 重新发送
      await handleSend({ text: userMessage.content })
    }
  }
}

// 文件选择
const handleFileSelect = (file) => {
  console.log('文件已选择:', file)
  ElMessage.success(`文件 ${file.name} 已选择`)
}

// 清空历史（开启新对话）
const handleClearHistory = async () => {
  if (messages.value.length === 0) {
    ElMessage.info('当前没有对话记录')
    return
  }

  try {
    await ElMessageBox.confirm(
      '确定要清空当前对话吗？这将开启一个新的对话。',
      '开启新对话',
      {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'warning',
        customClass: 'ai-chat-delete-confirm',
        dangerouslyUseHTMLString: false,
        showClose: true,
        closeOnClickModal: false,
        closeOnPressEscape: true,
        buttonSize: 'default'
      }
    )

    // 先保存当前对话到历史记录
    if (settings.autoSave && messages.value.length > 0) {
      await saveHistoryToDatabase()
    }

    // 清空消息和会话ID，开启新对话
    messages.value = []
    currentHistoryId.value = null
    ElMessage.success('已开启新对话')
  } catch {
    // 用户取消操作
  }
}

// 返回首页
const goToHome = () => {
  router.push('/')
}

// 导出对话
const handleExport = () => {
  if (messages.value.length === 0) {
    ElMessage.info('当前没有对话记录可导出')
    return
  }

  try {
    // 生成导出内容
    let exportContent = '# AI营养师对话记录\n\n'
    exportContent += `导出时间：${new Date().toLocaleString('zh-CN')}\n\n`
    exportContent += '---\n\n'

    messages.value.forEach((msg, index) => {
      const role = msg.role === 'user' ? '👤 用户' : '🤖 AI营养师'
      const time = new Date(msg.timestamp).toLocaleString('zh-CN')
      
      exportContent += `### ${role} - ${time}\n\n`
      exportContent += `${msg.content}\n\n`
      
      if (msg.favorite) {
        exportContent += `⭐ 已收藏\n\n`
      }
      
      exportContent += '---\n\n'
    })

    // 创建下载
    const blob = new Blob([exportContent], { type: 'text/markdown;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `AI营养师对话_${Date.now()}.md`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)

    ElMessage.success('对话记录已导出')
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('导出失败，请重试')
  }
}

// 显示历史记录
const handleShowHistory = () => {
  console.log('📖 打开历史记录')
  loadHistoryList()
  showHistory.value = true
}

// 显示收藏
const handleShowFavorites = async () => {
  console.log('⭐ 打开收藏列表')
  await loadFavoritesList()
  showFavorites.value = true
}

// 显示设置
const handleShowSettings = () => {
  console.log('⚙️ 打开设置')
  showSettings.value = true
}

// 保存设置
const handleSaveSettings = () => {
  // 保存设置到本地存储
  localStorage.setItem('aiChatSettings', JSON.stringify(settings))
  showSettings.value = false
  
  console.log('💾 用户设置已保存:', {
    model: settings.model,
    temperature: settings.temperature,
    maxTokens: settings.maxTokens,
    keepContext: settings.keepContext
  })
  
  ElMessage.success({
    message: '设置已保存！下次发送消息时生效',
    duration: 3000
  })
}

// 加载设置
const loadSettings = () => {
  try {
    const saved = localStorage.getItem('aiChatSettings')
    if (saved) {
      Object.assign(settings, JSON.parse(saved))
    }
  } catch (error) {
    console.error('加载设置失败:', error)
  }
}

// 收藏消息
const handleFavorite = async (messageId) => {
  const index = messages.value.findIndex(m => m.id === messageId)
  if (index > -1) {
    const message = messages.value[index]
    
    try {
      const token = localStorage.getItem('token')
      const response = await fetch('http://localhost:8080/api/ai-chat/favorite', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          messageContent: message.content,
          messageRole: message.role
        })
      })
      
      const data = await response.json()
      if (data.code === 200) {
        messages.value[index].favorite = true
        messages.value[index].favoriteId = data.data.id
        ElMessage.success('已收藏')
      }
    } catch (error) {
      console.error('收藏失败:', error)
      ElMessage.error('收藏失败')
    }
  }
}

// 取消收藏
const handleUnfavorite = async (msg) => {
  try {
    const token = localStorage.getItem('token')
    const favoriteId = msg.id
    
    const response = await fetch(
      `http://localhost:8080/api/ai-chat/favorite/${favoriteId}`,
      {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${token}`
        }
      }
    )
    
    const data = await response.json()
    if (data.code === 200) {
      ElMessage.success('已取消收藏')
      // 重新加载收藏列表
      await loadFavoritesList()
    } else {
      ElMessage.error('取消收藏失败')
    }
  } catch (error) {
    console.error('取消收藏失败:', error)
    ElMessage.error('取消收藏失败')
  }
}

// 复制消息
const copyMessage = (content) => {
  navigator.clipboard.writeText(content).then(() => {
    ElMessage.success('已复制到剪贴板')
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}

// 渲染Markdown
const renderMarkdown = (content) => {
  if (!content) return ''
  marked.setOptions({
    breaks: true,
    gfm: true
  })
  const html = marked.parse(content)
  return DOMPurify.sanitize(html)
}

// 格式化时间
const formatTime = (timestamp) => {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  const now = new Date()
  const diff = now - date

  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  
  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 生成对话标题
const generateConversationTitle = (firstMessage) => {
  if (!firstMessage) return '未命名对话'
  return firstMessage.substring(0, 30) + (firstMessage.length > 30 ? '...' : '')
}

// 保存历史记录到数据库（一个会话只保存/更新一次）
const saveHistoryToDatabase = async () => {
  if (messages.value.length === 0) return
  
  try {
    const token = localStorage.getItem('token')
    if (!token) return
    
    // 生成标题（取第一条用户消息的前20个字符）
    const firstUserMessage = messages.value.find(m => m.role === 'user')
    const title = firstUserMessage ? 
      (firstUserMessage.content.substring(0, 20) + (firstUserMessage.content.length > 20 ? '...' : '')) :
      '新对话'
    
    // 如果当前会话已有ID，则更新；否则创建新记录
    const response = await fetch('http://localhost:8080/api/ai-chat/history', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        id: currentHistoryId.value, // 如果有ID则更新
        title: title,
        messages: JSON.stringify(messages.value)
      })
    })
    
    const data = await response.json()
    if (data.code === 200) {
      // 保存会话ID，后续更新使用
      if (!currentHistoryId.value) {
        currentHistoryId.value = data.data.id
      }
      console.log('✅ 历史记录已保存到数据库', currentHistoryId.value)
    }
  } catch (error) {
    console.error('保存历史记录失败:', error)
  }
}

// 保存当前对话到历史（已改为自动保存到数据库）
const saveToHistory = async () => {
  await saveHistoryToDatabase()
  // 重新加载历史记录列表
  await loadHistoryList()
}

// 加载历史记录列表
const loadHistoryList = async () => {
  try {
    const token = localStorage.getItem('token')
    if (!token) return
    
    const response = await fetch(
      'http://localhost:8080/api/ai-chat/history/all',
      {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      }
    )
    
    const data = await response.json()
    if (data.code === 200) {
      // 转换数据格式
      historyList.value = data.data.map(item => ({
        id: item.id.toString(),
        title: item.title,
        messages: JSON.parse(item.messages),
        timestamp: new Date(item.createdAt).getTime()
      }))
    }
  } catch (error) {
    console.error('加载历史记录失败:', error)
  }
}

// 加载收藏列表
const loadFavoritesList = async () => {
  try {
    const token = localStorage.getItem('token')
    if (!token) return
    
    const response = await fetch(
      'http://localhost:8080/api/ai-chat/favorite/all',
      {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      }
    )
    
    const data = await response.json()
    if (data.code === 200) {
      // 转换数据格式
      favoritesList.value = data.data.map(item => ({
        id: item.id.toString(),
        content: item.messageContent,
        role: item.messageRole,
        timestamp: new Date(item.createdAt).getTime(),
        favorite: true
      }))
    }
  } catch (error) {
    console.error('加载收藏列表失败:', error)
  }
}

// 加载历史对话
const loadHistoryConversation = (item) => {
  messages.value = JSON.parse(JSON.stringify(item.messages))
  currentHistoryId.value = item.id // 设置当前会话ID
  showHistory.value = false
  ElMessage.success('历史记录已加载')
}

// 删除历史记录
const deleteHistory = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除这条历史记录吗？', '确认删除', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning',
      customClass: 'ai-chat-delete-confirm'
    })

    const token = localStorage.getItem('token')
    const response = await fetch(
      `http://localhost:8080/api/ai-chat/history/${id}`,
      {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${token}`
        }
      }
    )
    
    const data = await response.json()
    if (data.code === 200) {
      ElMessage.success('已删除')
      await loadHistoryList()
    } else {
      ElMessage.error('删除失败')
    }
  } catch {
    // 用户取消
  }
}

// 初始化
onMounted(() => {
  loadSettings()
  // loadFromLocalStorage() // 注释掉：每次打开都是全新对话，不加载历史
  loadHistoryList()
})

// 清理（已移至下方的 onUnmounted）

// 键盘快捷键
const handleKeydown = (e) => {
  // Ctrl/Cmd + K: 清空对话
  if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
    e.preventDefault()
    handleClearHistory()
  }
  
  // Ctrl/Cmd + E: 导出对话
  if ((e.ctrlKey || e.metaKey) && e.key === 'e') {
    e.preventDefault()
    handleExport()
  }
  
  // Ctrl/Cmd + H: 打开历史记录
  if ((e.ctrlKey || e.metaKey) && e.key === 'h') {
    e.preventDefault()
    showHistory.value = true
  }
}

// 监听消息变化，自动保存到数据库（防抖）
watch(
  () => messages.value,
  () => {
    // 延迟保存，避免频繁调用API
    if (messages.value.length > 0) {
      clearTimeout(saveTimer.value)
      saveTimer.value = setTimeout(() => {
        saveHistoryToDatabase()
      }, 5000) // 5秒后保存（延长时间，减少保存频率）
    }
  },
  { deep: true }
)

// 保存定时器
const saveTimer = ref(null)

onMounted(() => {
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  
  // 清除保存定时器
  if (saveTimer.value) {
    clearTimeout(saveTimer.value)
  }
  
  // 保存当前对话到数据库
  if (settings.autoSave && messages.value.length > 0) {
    saveHistoryToDatabase()
  }
  
  // 不再在组件卸载时清理遮罩层，让 Element Plus 自己管理
})
</script>

<style scoped>
.ai-chat-view {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}

/* 顶部标题栏 */
.chat-header {
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
  color: #409eff;
  font-size: 24px;
}

.header-right {
  display: flex;
  gap: 8px;
  position: relative;
  z-index: 10;
}

.header-right .el-button {
  position: relative;
  z-index: 10;
}

/* 主体内容 */
.chat-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 设置提示 */
.setting-tip {
  display: block;
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

/* 历史记录列表 */
.history-list {
  max-height: 500px;
  overflow-y: auto;
}

.history-item {
  padding: 16px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.history-item:hover {
  border-color: #409eff;
  background: #f0f9ff;
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.history-title {
  font-weight: 600;
  color: #303133;
}

.history-time {
  font-size: 12px;
  color: #909399;
}

.history-preview {
  color: #606266;
  font-size: 14px;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.history-actions {
  display: flex;
  justify-content: flex-end;
}

/* 收藏列表 */
.favorites-list {
  max-height: 500px;
  overflow-y: auto;
}

.favorite-item {
  padding: 16px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  margin-bottom: 12px;
  background: white;
}

.favorite-content {
  color: #303133;
  line-height: 1.6;
  margin-bottom: 12px;
}

.favorite-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.favorite-time {
  font-size: 12px;
  color: #909399;
}

.favorite-actions {
  display: flex;
  gap: 8px;
}

/* 响应式 */
@media (max-width: 768px) {
  .chat-header {
    padding: 0 16px;
  }
  
  .header-title {
    font-size: 18px;
  }
  
  .header-right .el-button {
    padding: 8px;
  }
}
</style>

<style lang="scss">
// 删除确认框样式（全局 - 简洁风格）
.el-message-box.ai-chat-delete-confirm {
  width: 440px !important;
  max-width: 90vw !important;
  border-radius: 8px !important;
  padding: 0 !important;
  background: #ffffff !important;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12) !important;
  overflow: hidden !important;
  
  .el-message-box__header {
    position: relative !important;
    padding: 20px 20px 16px 20px !important;
    background: #ffffff !important;
    border-bottom: none !important;
    display: flex !important;
    align-items: center !important;
    justify-content: space-between !important;
  }
  
  .el-message-box__title {
    font-size: 18px !important;
    font-weight: 600 !important;
    color: #1f2937 !important;
    flex: 1 !important;
    line-height: 24px !important;
  }
  
  .el-message-box__headerbtn {
    position: relative !important;
    top: auto !important;
    right: auto !important;
    width: 20px !important;
    height: 20px !important;
    padding: 0 !important;
    margin: 0 0 0 10px !important;
    flex-shrink: 0 !important;
    
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
  z-index: 2000 !important;
}

// 确保对话框正确显示
.el-dialog {
  z-index: 2001 !important;
}

// 确保MessageBox居中显示
.el-overlay .el-message-box {
  position: fixed !important;
  top: 50% !important;
  left: 50% !important;
  transform: translate(-50%, -50%) !important;
  z-index: 2001 !important;
}
</style>
