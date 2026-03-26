<template>
  <div class="ai-chat-view">
    <!-- 顶部标题栏 -->
    <div class="chat-header">
      <div class="header-left">
        <el-button :icon="ArrowLeft" text @click="goToHome"> 返回首页 </el-button>
        <el-divider direction="vertical" />
        <h1 class="header-title">
          <el-icon class="title-icon">
            <ChatDotRound />
          </el-icon>
          AI营养师
        </h1>
        <el-tag size="small" effect="plain"> 智能对话 </el-tag>
      </div>
      <div class="header-right">
        <el-button :icon="FolderOpened" circle title="历史记录" @click.stop="handleShowHistory" />
        <el-button :icon="Star" circle title="收藏" @click.stop="handleShowFavorites" />
        <el-button :icon="Delete" circle title="清空对话" @click.stop="handleClearHistory" />
        <el-button :icon="Download" circle title="导出对话" @click.stop="handleExport" />
        <el-button :icon="Setting" circle title="设置" @click.stop="handleShowSettings" />
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
            <el-option label="Qwen3.5-Flash（推荐）" value="qwen3.5-flash" />
            <el-option label="通义千问Max" value="qwen-max" />
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
        <el-button @click="showSettings = false"> 取消 </el-button>
        <el-button type="primary" @click="handleSaveSettings"> 保存 </el-button>
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
          v-for="item in historyList"
          v-else
          :key="item.id"
          class="history-item"
          @click="loadHistoryConversation(item)"
        >
          <div class="history-header">
            <span class="history-title">{{ item.title || '未命名对话' }}</span>
            <span class="history-time">{{ formatTime(item.timestamp) }}</span>
          </div>
          <div class="history-preview">
            {{ item.preview }}
          </div>
          <div class="history-actions">
            <el-button link size="small" type="danger" @click.stop="deleteHistory(item.id)">
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
        <div v-for="msg in favoriteMessages" v-else :key="msg.id" class="favorite-item">
          <div class="favorite-content" v-html="renderMarkdown(msg.content)" />
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
import api from '@/services/api'

// 路由
const router = useRouter()

// 组件引用
const messageListRef = ref(null)
const chatInputRef = ref(null)

// 状态
const messages = ref([])
const historyList = ref([])
const favoritesList = ref([])
const currentHistoryId = ref(null)
const isLoading = ref(false)
const showHistory = ref(false)
const showFavorites = ref(false)
const showSettings = ref(false)

// 保存定时器
const saveTimer = ref(null)

// 设置
const settings = reactive({
  model: 'qwen3.5-flash',
  temperature: 0.7,
  maxTokens: 2000,
  keepContext: true,
  autoSave: true
})

// 收藏消息计算属性
const favoriteMessages = computed(() => {
  return favoritesList.value
})

// 生成消息ID
const generateMessageId = () => {
  return `msg_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
}

// 发送消息
const handleSend = async ({ text, file }) => {
  if (!text && !file) return

  // 如果有文件但没有文本，生成描述文本
  let messageText = text
  if (file && !text) {
    messageText = `请分析这个文件：${file.name}`
  }

  // 创建用户消息
  const userMessage = {
    id: generateMessageId(),
    role: 'user',
    content: messageText,
    timestamp: Date.now(),
    file: file ? { name: file.name, size: file.size, type: file.type } : null
  }

  messages.value.push(userMessage)
  chatInputRef.value?.clear()

  // 创建AI消息占位
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
    let aiResponse = ''

    // 如果有文件附件，先上传文件
    if (file) {
      try {
        const formData = new FormData()
        formData.append('file', file)
        const uploadRes = await api.post('/ai/upload', formData, {
          headers: { 'Content-Type': 'multipart/form-data' },
          timeout: 60000
        })
        if (uploadRes.data?.code === 200 && uploadRes.data?.data) {
          // 将文件URL加入消息，让AI分析
          messageText = `${messageText}\n\n[附件: ${file.name}](${uploadRes.data.data})`
        }
      } catch (uploadErr) {
        console.warn('文件上传失败，将只发送文本消息:', uploadErr)
      }
    }

    // 调用AI聊天API
    const response = await api.post(
      '/ai/chat',
      {
        message: messageText,
        model: settings.model,
        temperature: settings.temperature,
        maxTokens: settings.maxTokens,
        keepContext: settings.keepContext
      },
      { timeout: 180000 }
    )

    const data = response.data
    if (data.code === 200) {
      aiResponse = data.data?.message || data.data
      if (!aiResponse || (typeof aiResponse === 'string' && !aiResponse.trim())) {
        throw new Error('AI回复为空，请重试')
      }
    } else {
      throw new Error(data.message || '请求失败')
    }

    // 更新AI消息
    const index = messages.value.findIndex(m => m.id === aiMessage.id)
    if (index > -1) {
      messages.value[index] = {
        ...messages.value[index],
        content: aiResponse,
        loading: false,
        timestamp: Date.now()
      }
    }
  } catch (error) {
    console.error('发送消息失败:', error)

    let errorMessage = '发送失败'
    const status = error.response?.status
    const msg = error.response?.data?.message || error.message

    if (error.code === 'ECONNABORTED' || error.message?.includes('timeout')) {
      errorMessage = 'AI响应超时，请稍后重试'
    } else if (!error.response && error.message?.includes('Network')) {
      errorMessage = '无法连接到服务器，请检查网络'
    } else if (status === 401 || status === 403) {
      errorMessage = '认证失败，请重新登录'
    } else if (status === 429) {
      errorMessage = '请求过于频繁，请稍后再试'
    } else if (status >= 500) {
      errorMessage = 'AI服务异常，请稍后重试'
    } else if (msg) {
      errorMessage = msg
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

// 快捷操作
const handleQuickAction = content => {
  handleSend({ text: content })
}

// 重新生成
const handleRegenerate = async message => {
  const messageIndex = messages.value.findIndex(m => m.id === message.id)
  if (messageIndex > 0) {
    const userMessage = messages.value[messageIndex - 1]
    if (userMessage.role === 'user') {
      messages.value.splice(messageIndex, 1)
      await handleSend({ text: userMessage.content })
    }
  }
}

// 文件选择
const handleFileSelect = file => {
  ElMessage.info(`已选择文件: ${file.name}`)
}

// 清空历史（开启新对话）
const handleClearHistory = async () => {
  if (messages.value.length === 0) {
    ElMessage.info('当前没有对话记录')
    return
  }

  try {
    await ElMessageBox.confirm('确定要清空当前对话吗？这将开启一个新的对话。', '开启新对话', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning',
      customClass: 'ai-chat-delete-confirm',
      dangerouslyUseHTMLString: false,
      showClose: true,
      closeOnClickModal: false,
      closeOnPressEscape: true,
      buttonSize: 'default'
    })

    // 先保存当前对话到历史记录
    if (settings.autoSave && messages.value.length > 0) {
      await saveHistoryToDatabase()
    }

    // 清除后端对话上下文
    try {
      await api.delete('/ai/clear')
    } catch {
      // 静默处理，不影响前端流程
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
    let exportContent = '# AI营养师对话记录\n\n'
    exportContent += `导出时间：${new Date().toLocaleString('zh-CN')}\n\n`
    exportContent += '---\n\n'

    messages.value.forEach(msg => {
      const role = msg.role === 'user' ? '👤 用户' : '🤖 AI营养师'
      const time = new Date(msg.timestamp).toLocaleString('zh-CN')
      exportContent += `### ${role} - ${time}\n\n`
      exportContent += `${msg.content}\n\n`
      if (msg.favorite) {
        exportContent += `⭐ 已收藏\n\n`
      }
      exportContent += '---\n\n'
    })

    const blob = new Blob([exportContent], { type: 'text/markdown;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `AI营养师对话_${new Date().toISOString().slice(0, 10)}.md`
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
  loadHistoryList()
  showHistory.value = true
}

// 显示收藏
const handleShowFavorites = async () => {
  await loadFavoritesList()
  showFavorites.value = true
}

// 显示设置
const handleShowSettings = () => {
  showSettings.value = true
}

// 保存设置
const handleSaveSettings = () => {
  localStorage.setItem('aiChatSettings', JSON.stringify(settings))
  showSettings.value = false
  ElMessage.success('设置已保存')
}

// 加载设置
const loadSettings = () => {
  try {
    const saved = localStorage.getItem('aiChatSettings')
    if (saved) {
      const parsed = JSON.parse(saved)
      // 仅赋值合法字段，防止注入
      if (parsed.model) settings.model = parsed.model
      if (typeof parsed.temperature === 'number') settings.temperature = parsed.temperature
      if (typeof parsed.maxTokens === 'number') settings.maxTokens = parsed.maxTokens
      if (typeof parsed.keepContext === 'boolean') settings.keepContext = parsed.keepContext
      if (typeof parsed.autoSave === 'boolean') settings.autoSave = parsed.autoSave
    }
  } catch {
    // 忽略解析失败
  }
}

// 收藏消息
const handleFavorite = async messageId => {
  const index = messages.value.findIndex(m => m.id === messageId)
  if (index === -1) return

  const message = messages.value[index]
  try {
    const response = await api.post('/ai-chat/favorite', {
      messageContent: message.content,
      messageRole: message.role
    })
    const data = response.data
    if (data.code === 200) {
      messages.value[index] = {
        ...messages.value[index],
        favorite: true,
        favoriteId: data.data.id
      }
      ElMessage.success('已收藏')
    } else {
      ElMessage.error(data.message || '收藏失败')
    }
  } catch (error) {
    console.error('收藏失败:', error)
    ElMessage.error('收藏失败')
  }
}

// 取消收藏 - 支持从消息列表和收藏列表两种来源
const handleUnfavorite = async msg => {
  // 从消息列表取消收藏时 msg 是 messageId 字符串，从收藏弹窗取消时 msg 是对象
  let favoriteId = null
  let messageId = null

  if (typeof msg === 'string' || typeof msg === 'number') {
    // 从 MessageList 的 unfavorite 事件，参数是 message.id
    messageId = msg
    const message = messages.value.find(m => m.id === messageId)
    favoriteId = message?.favoriteId
    if (!favoriteId) {
      ElMessage.error('未找到收藏记录')
      return
    }
  } else {
    // 从收藏弹窗取消收藏，msg 是收藏对象
    favoriteId = msg.id
  }

  try {
    const response = await api.delete(`/ai-chat/favorite/${favoriteId}`)
    const data = response.data
    if (data.code === 200) {
      ElMessage.success('已取消收藏')

      // 更新消息列表中的收藏状态
      if (messageId) {
        const index = messages.value.findIndex(m => m.id === messageId)
        if (index > -1) {
          messages.value[index] = {
            ...messages.value[index],
            favorite: false,
            favoriteId: null
          }
        }
      } else {
        // 同步更新消息列表中对应收藏的状态
        const msgIndex = messages.value.findIndex(
          m => m.favoriteId && m.favoriteId.toString() === favoriteId.toString()
        )
        if (msgIndex > -1) {
          messages.value[msgIndex] = {
            ...messages.value[msgIndex],
            favorite: false,
            favoriteId: null
          }
        }
      }

      // 如果收藏面板打开，刷新收藏列表
      if (showFavorites.value) {
        await loadFavoritesList()
      }
    } else {
      ElMessage.error(data.message || '取消收藏失败')
    }
  } catch (error) {
    console.error('取消收藏失败:', error)
    ElMessage.error('取消收藏失败')
  }
}

// 复制消息
const copyMessage = content => {
  navigator.clipboard
    .writeText(content)
    .then(() => ElMessage.success('已复制到剪贴板'))
    .catch(() => ElMessage.error('复制失败'))
}

// 渲染Markdown
const renderMarkdown = content => {
  if (!content) return ''
  marked.setOptions({ breaks: true, gfm: true })
  const html = marked.parse(content)
  return DOMPurify.sanitize(html)
}

// 格式化时间
const formatTime = timestamp => {
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

// 保存历史记录到数据库
const saveHistoryToDatabase = async () => {
  if (messages.value.length === 0) return

  try {
    const firstUserMessage = messages.value.find(m => m.role === 'user')
    const title = firstUserMessage
      ? firstUserMessage.content.substring(0, 20) +
        (firstUserMessage.content.length > 20 ? '...' : '')
      : '新对话'

    const response = await api.post('/ai-chat/history', {
      id: currentHistoryId.value,
      title: title,
      messages: JSON.stringify(messages.value)
    })

    const data = response.data
    if (data.code === 200) {
      if (!currentHistoryId.value) {
        currentHistoryId.value = data.data.id
      }
    }
  } catch (error) {
    console.error('保存历史记录失败:', error)
  }
}

// 提取对话预览
const extractPreview = messagesJson => {
  try {
    const msgs = typeof messagesJson === 'string' ? JSON.parse(messagesJson) : messagesJson
    if (!Array.isArray(msgs) || msgs.length === 0) return '暂无内容'
    const lastMsg = msgs[msgs.length - 1]
    const content = lastMsg?.content || ''
    return content.substring(0, 80) + (content.length > 80 ? '...' : '')
  } catch {
    return '暂无内容'
  }
}

// 加载历史记录列表
const loadHistoryList = async () => {
  try {
    const response = await api.get('/ai-chat/history/all')
    const data = response.data
    if (data.code === 200) {
      historyList.value = (data.data || []).map(item => ({
        id: item.id.toString(),
        title: item.title,
        messages: typeof item.messages === 'string' ? JSON.parse(item.messages) : item.messages,
        timestamp: new Date(item.createdAt).getTime(),
        preview: extractPreview(item.messages)
      }))
    }
  } catch (error) {
    console.error('加载历史记录失败:', error)
    ElMessage.error('加载历史记录失败')
  }
}

// 加载收藏列表
const loadFavoritesList = async () => {
  try {
    const response = await api.get('/ai-chat/favorite/all')
    const data = response.data
    if (data.code === 200) {
      favoritesList.value = (data.data || []).map(item => ({
        id: item.id.toString(),
        content: item.messageContent,
        role: item.messageRole,
        timestamp: new Date(item.createdAt).getTime(),
        favorite: true
      }))
    }
  } catch (error) {
    console.error('加载收藏列表失败:', error)
    ElMessage.error('加载收藏列表失败')
  }
}

// 加载历史对话
const loadHistoryConversation = item => {
  // 清除 loading 标志，避免恢复历史时消息仍显示为"正在加载"
  const parsedMessages = JSON.parse(JSON.stringify(item.messages)).map(msg => ({
    ...msg,
    loading: false
  }))
  messages.value = parsedMessages
  currentHistoryId.value = item.id
  showHistory.value = false
  ElMessage.success('历史记录已加载')
}

// 删除历史记录
const deleteHistory = async id => {
  try {
    await ElMessageBox.confirm('确定要删除这条历史记录吗？', '确认删除', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning',
      customClass: 'ai-chat-delete-confirm'
    })

    const response = await api.delete(`/ai-chat/history/${id}`)
    const data = response.data
    if (data.code === 200) {
      // 如果删除的是当前会话，重置会话ID
      if (currentHistoryId.value && currentHistoryId.value.toString() === id.toString()) {
        currentHistoryId.value = null
      }
      ElMessage.success('已删除')
      await loadHistoryList()
    } else {
      ElMessage.error(data.message || '删除失败')
    }
  } catch {
    // 用户取消
  }
}

// 键盘快捷键
const handleKeydown = e => {
  if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
    e.preventDefault()
    handleClearHistory()
  }
  if ((e.ctrlKey || e.metaKey) && e.key === 'e') {
    e.preventDefault()
    handleExport()
  }
  if ((e.ctrlKey || e.metaKey) && e.key === 'h') {
    e.preventDefault()
    handleShowHistory()
  }
}

// 监听消息变化，自动保存到数据库（防抖）
watch(
  () => messages.value,
  () => {
    if (messages.value.length > 0 && !isLoading.value) {
      clearTimeout(saveTimer.value)
      saveTimer.value = setTimeout(() => {
        saveHistoryToDatabase()
      }, 5000)
    }
  },
  { deep: true }
)

// 生命周期
onMounted(() => {
  loadSettings()
  loadHistoryList()
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)

  if (saveTimer.value) {
    clearTimeout(saveTimer.value)
  }

  // 保存当前对话到数据库
  if (settings.autoSave && messages.value.length > 0) {
    saveHistoryToDatabase()
  }
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

// 确保MessageBox居中显示
.el-overlay .el-message-box {
  position: fixed !important;
  top: 50% !important;
  left: 50% !important;
  transform: translate(-50%, -50%) !important;
}
</style>
