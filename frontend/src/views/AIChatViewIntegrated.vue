<template>
  <div class="ai-chat-view">
    <!-- 顶部标题栏 -->
    <div class="chat-header">
      <div class="header-left">
        <h1 class="header-title">
          <el-icon class="title-icon"><ChatDotRound /></el-icon>
          AI营养师
        </h1>
        <el-tag 
          :type="statusTagType" 
          size="small" 
          effect="plain"
        >
          {{ statusText }}
        </el-tag>
      </div>
      <div class="header-right">
        <el-button :icon="FolderOpened" circle @click="showHistory = true" title="历史记录" />
        <el-button :icon="Star" circle @click="showFavorites = true" title="收藏" />
        <el-button :icon="Delete" circle @click="handleClearHistory" title="清空对话" />
        <el-button :icon="Download" circle @click="handleExport" title="导出对话" />
        <el-button :icon="Setting" circle @click="showSettings = true" title="设置" />
      </div>
    </div>

    <!-- 主体内容 -->
    <div class="chat-body">
      <!-- 消息列表 -->
      <MessageList
        ref="messageListRef"
        :messages="messages"
        :streaming-message-id="currentMessageId"
        :streaming-content="currentResponse"
        @regenerate="handleRegenerate"
        @favorite="handleFavorite"
        @unfavorite="handleUnfavorite"
      />

      <!-- 快捷操作 -->
      <QuickActions
        :disabled="isLoading || !isConnected"
        :expandable="true"
        :default-expanded="messages.length === 0"
        @action="handleQuickAction"
      />

      <!-- 输入区域 -->
      <ChatInput
        ref="chatInputRef"
        :disabled="isLoading || !isConnected"
        :loading="isLoading"
        @send="handleSend"
        @file-select="handleFileSelect"
      />
    </div>

    <!-- 历史记录对话框 -->
    <el-dialog
      v-model="showHistory"
      title="历史记录"
      width="700px"
      :append-to-body="true"
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

    <!-- 设置对话框 -->
    <el-dialog
      v-model="showSettings"
      title="聊天设置"
      width="500px"
    >
      <el-form label-width="120px">
        <el-form-item label="连接状态">
          <el-tag :type="statusTagType">{{ statusText }}</el-tag>
          <el-button 
            v-if="!isConnected" 
            type="primary" 
            size="small" 
            @click="reconnect"
            style="margin-left: 10px"
          >
            重新连接
          </el-button>
        </el-form-item>
        <el-divider />
        <el-form-item label="上下文记忆">
          <el-switch v-model="settings.keepContext" />
          <span class="setting-tip">是否保持对话上下文</span>
        </el-form-item>
        <el-form-item label="自动保存">
          <el-switch v-model="settings.autoSave" />
          <span class="setting-tip">自动保存聊天记录到本地</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showSettings = false">取消</el-button>
        <el-button type="primary" @click="handleSaveSettings">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ChatDotRound,
  Delete,
  Download,
  Setting,
  FolderOpened,
  Star,
  DocumentCopy
} from '@element-plus/icons-vue'
import MessageList from '@/components/chat/MessageList.vue'
import ChatInput from '@/components/chat/ChatInput.vue'
import QuickActions from '@/components/chat/QuickActions.vue'
import { useAIWebSocket } from '@/composables/useAIWebSocket'
import { ConnectionStatus } from '@/services/websocket'
import { marked } from 'marked'
import DOMPurify from 'dompurify'

// WebSocket Hook
const {
  isConnected,
  isReceiving,
  status,
  currentResponse,
  currentMessageId,
  connect,
  disconnect,
  sendMessage,
  on,
  setCurrentMessageId,
  clearCurrentResponse
} = useAIWebSocket()

// 组件引用
const messageListRef = ref(null)
const chatInputRef = ref(null)

// 状态
const messages = ref([])
const isLoading = ref(false)
const showSettings = ref(false)
const showHistory = ref(false)
const showFavorites = ref(false)

// 历史记录和收藏
const historyList = ref([])
const favoriteMessages = computed(() => {
  return messages.value.filter(m => m.favorite && m.role === 'assistant')
})

// 设置
const settings = reactive({
  keepContext: true,
  autoSave: true
})

// 连接状态文本
const statusText = computed(() => {
  switch (status.value) {
    case ConnectionStatus.CONNECTED:
      return '● 已连接'
    case ConnectionStatus.CONNECTING:
      return '○ 连接中...'
    case ConnectionStatus.RECONNECTING:
      return '○ 重连中...'
    case ConnectionStatus.DISCONNECTED:
      return '○ 未连接'
    case ConnectionStatus.ERROR:
      return '○ 连接错误'
    default:
      return '○ 未知状态'
  }
})

// 状态标签类型
const statusTagType = computed(() => {
  switch (status.value) {
    case ConnectionStatus.CONNECTED:
      return 'success'
    case ConnectionStatus.CONNECTING:
    case ConnectionStatus.RECONNECTING:
      return 'warning'
    case ConnectionStatus.ERROR:
      return 'danger'
    default:
      return 'info'
  }
})

// 生成消息ID
const generateMessageId = () => {
  return `msg_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
}

// 生成对话标题
const generateConversationTitle = (firstMessage) => {
  if (!firstMessage) return '未命名对话'
  return firstMessage.substring(0, 30) + (firstMessage.length > 30 ? '...' : '')
}

// 发送消息
const handleSend = async ({ text, file }) => {
  if (!text && !file) {
    return
  }

  if (!isConnected.value) {
    ElMessage.warning('WebSocket未连接，请等待连接建立')
    return
  }

  // 处理文件上传
  if (file) {
    await handleFileUpload(file, text)
    return
  }

  // 创建用户消息
  const userMessage = {
    id: generateMessageId(),
    role: 'user',
    content: text,
    timestamp: Date.now()
  }

  messages.value.push(userMessage)

  // 清空输入框
  chatInputRef.value?.clear()

  // 创建AI消息（流式接收）
  const aiMessage = {
    id: generateMessageId(),
    role: 'assistant',
    content: '',
    timestamp: Date.now(),
    streaming: true
  }

  messages.value.push(aiMessage)
  isLoading.value = true

  // 设置当前消息ID
  setCurrentMessageId(aiMessage.id)

  // 发送WebSocket消息
  sendMessage(text, settings.keepContext)
}

// 文件上传处理
const handleFileUpload = async (file, message) => {
  try {
    // 检查文件类型
    const allowedTypes = ['text/plain', 'application/pdf']
    if (!allowedTypes.includes(file.type)) {
      ElMessage.error('仅支持TXT和PDF文件')
      return
    }

    // 检查文件大小（限制5MB）
    if (file.size > 5 * 1024 * 1024) {
      ElMessage.error('文件大小不能超过5MB')
      return
    }

    ElMessage.info('正在上传文件...')

    // 读取文件内容
    const fileContent = await readFileContent(file)
    
    // 构建带文件内容的消息
    const fullMessage = message 
      ? `${message}\n\n【文件内容】：\n${fileContent.substring(0, 2000)}${fileContent.length > 2000 ? '...(内容过长已截断)' : ''}`
      : `请帮我分析这个文件的内容：\n${fileContent.substring(0, 2000)}${fileContent.length > 2000 ? '...(内容过长已截断)' : ''}`

    // 创建用户消息
    const userMessage = {
      id: generateMessageId(),
      role: 'user',
      content: message || '上传了文件',
      timestamp: Date.now(),
      file: {
        name: file.name,
        size: file.size,
        type: file.type
      }
    }

    messages.value.push(userMessage)
    chatInputRef.value?.clear()

    // 创建AI消息
    const aiMessage = {
      id: generateMessageId(),
      role: 'assistant',
      content: '',
      timestamp: Date.now(),
      streaming: true
    }

    messages.value.push(aiMessage)
    isLoading.value = true
    setCurrentMessageId(aiMessage.id)

    // 发送包含文件内容的消息
    sendMessage(fullMessage, settings.keepContext)

    ElMessage.success('文件已上传')
  } catch (error) {
    console.error('文件上传失败:', error)
    ElMessage.error('文件上传失败')
  }
}

// 读取文件内容
const readFileContent = (file) => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    
    reader.onload = (e) => {
      resolve(e.target.result)
    }
    
    reader.onerror = reject
    
    if (file.type === 'application/pdf') {
      // PDF需要特殊处理（这里简化处理）
      reader.readAsText(file)
    } else {
      reader.readAsText(file)
    }
  })
}

// 快捷操作
const handleQuickAction = (content) => {
  handleSend({ text: content })
}

// 重新生成
const handleRegenerate = async (message) => {
  ElMessage.info('正在重新生成...')
  
  const messageIndex = messages.value.findIndex(m => m.id === message.id)
  if (messageIndex > 0) {
    const userMessage = messages.value[messageIndex - 1]
    if (userMessage.role === 'user') {
      messages.value.splice(messageIndex, 1)
      await handleSend({ text: userMessage.content })
    }
  }
}

// 收藏消息
const handleFavorite = (message) => {
  const index = messages.value.findIndex(m => m.id === message.id)
  if (index > -1) {
    messages.value[index].favorite = true
    ElMessage.success('已收藏')
    saveToLocalStorage()
  }
}

// 取消收藏
const handleUnfavorite = (message) => {
  const index = messages.value.findIndex(m => m.id === message.id)
  if (index > -1) {
    messages.value[index].favorite = false
    ElMessage.success('已取消收藏')
    saveToLocalStorage()
  }
}

// 文件选择
const handleFileSelect = (file) => {
  // 文件已在handleSend中处理
}

// 清空历史
const handleClearHistory = async () => {
  if (messages.value.length === 0) {
    ElMessage.info('当前没有对话记录')
    return
  }

  try {
    await ElMessageBox.confirm(
      '确定要清空所有对话记录吗？删除后将无法恢复。',
      '确认删除',
      {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    // 先保存到历史记录
    if (settings.autoSave && messages.value.length > 0) {
      saveCurrentConversation()
    }

    messages.value = []
    ElMessage.success('对话记录已清空')
  } catch {
    // 用户取消操作
  }
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

    messages.value.forEach((msg) => {
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

// 保存当前对话
const saveCurrentConversation = () => {
  if (messages.value.length === 0) return

  const firstUserMessage = messages.value.find(m => m.role === 'user')
  const conversation = {
    id: `conv_${Date.now()}`,
    title: generateConversationTitle(firstUserMessage?.content),
    preview: firstUserMessage?.content?.substring(0, 50) || '',
    messages: JSON.parse(JSON.stringify(messages.value)),
    timestamp: Date.now()
  }

  const history = JSON.parse(localStorage.getItem('chatHistory') || '[]')
  history.unshift(conversation)
  
  // 最多保存50条历史记录
  if (history.length > 50) {
    history.length = 50
  }
  
  localStorage.setItem('chatHistory', JSON.stringify(history))
}

// 加载历史记录列表
const loadHistoryList = () => {
  try {
    const history = JSON.parse(localStorage.getItem('chatHistory') || '[]')
    historyList.value = history
  } catch (error) {
    console.error('加载历史记录失败:', error)
    historyList.value = []
  }
}

// 加载历史对话
const loadHistoryConversation = (item) => {
  messages.value = JSON.parse(JSON.stringify(item.messages))
  showHistory.value = false
  ElMessage.success('历史记录已加载')
}

// 删除历史记录
const deleteHistory = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除这条历史记录吗？', '确认删除', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    })

    const history = JSON.parse(localStorage.getItem('chatHistory') || '[]')
    const index = history.findIndex(h => h.id === id)
    if (index > -1) {
      history.splice(index, 1)
      localStorage.setItem('chatHistory', JSON.stringify(history))
      loadHistoryList()
      ElMessage.success('已删除')
    }
  } catch {
    // 用户取消
  }
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
      Object.assign(settings, JSON.parse(saved))
    }
  } catch (error) {
    console.error('加载设置失败:', error)
  }
}

// 保存到本地存储
const saveToLocalStorage = () => {
  if (settings.autoSave && messages.value.length > 0) {
    try {
      localStorage.setItem('currentChat', JSON.stringify(messages.value))
    } catch (error) {
      console.error('保存失败:', error)
    }
  }
}

// 从本地存储加载
const loadFromLocalStorage = () => {
  try {
    const saved = localStorage.getItem('currentChat')
    if (saved) {
      messages.value = JSON.parse(saved)
    }
  } catch (error) {
    console.error('加载失败:', error)
  }
}

// 重新连接
const reconnect = async () => {
  try {
    await connect()
    ElMessage.success('重新连接成功')
  } catch (error) {
    ElMessage.error('重新连接失败')
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

// 设置WebSocket事件处理器
const setupWebSocketHandlers = () => {
  on('onConnectionConfirm', (message) => {
    console.log('✅ 连接确认:', message)
  })

  on('onStreamStart', () => {
    console.log('🎬 AI开始响应')
  })

  on('onStreamChunk', (message) => {
    // currentResponse会自动更新
  })

  on('onStreamComplete', (message) => {
    console.log('✅ AI响应完成')
    isLoading.value = false

    const index = messages.value.findIndex(m => m.id === currentMessageId.value)
    if (index > -1) {
      messages.value[index] = {
        ...messages.value[index],
        content: message.fullContent,
        streaming: false,
        timestamp: Date.now()
      }
    }

    clearCurrentResponse()
    saveToLocalStorage()
  })

  on('onAIError', (message) => {
    console.error('❌ AI错误:', message.message)
    isLoading.value = false

    const index = messages.value.findIndex(m => m.id === currentMessageId.value)
    if (index > -1) {
      messages.value.splice(index, 1)
    }

    clearCurrentResponse()
  })

  on('onClose', (event) => {
    if (event.code !== 1000) {
      ElMessage.warning('WebSocket连接已断开，正在尝试重连...')
    }
  })

  on('onError', () => {
    isLoading.value = false
  })
}

// 监听消息变化，自动保存
watch(
  () => messages.value,
  () => {
    saveToLocalStorage()
  },
  { deep: true }
)

// 初始化
onMounted(async () => {
  console.log('🔧 AIChatView mounted')
  
  loadSettings()
  loadFromLocalStorage()
  loadHistoryList()
  setupWebSocketHandlers()
  
  try {
    await connect()
  } catch (error) {
    console.error('初始化连接失败:', error)
  }
})

// 清理
onUnmounted(() => {
  console.log('🧹 AIChatView unmounted')
  
  // 保存当前对话
  if (settings.autoSave && messages.value.length > 0) {
    saveCurrentConversation()
  }
})

// 键盘快捷键
const handleKeydown = (e) => {
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
    showHistory.value = true
  }
}

onMounted(() => {
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.ai-chat-view {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}

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
}

.chat-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 历史记录 */
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

.setting-tip {
  display: block;
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
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
