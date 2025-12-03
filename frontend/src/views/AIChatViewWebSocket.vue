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
      </el-form>
      <template #footer>
        <el-button @click="showSettings = false">取消</el-button>
        <el-button type="primary" @click="handleSaveSettings">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ChatDotRound,
  Delete,
  Download,
  Setting
} from '@element-plus/icons-vue'
import MessageList from '@/components/chat/MessageList.vue'
import ChatInput from '@/components/chat/ChatInput.vue'
import QuickActions from '@/components/chat/QuickActions.vue'
import { useAIWebSocket } from '@/composables/useAIWebSocket'
import { ConnectionStatus } from '@/services/websocket'

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

// 设置
const settings = reactive({
  keepContext: true
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

// 发送消息
const handleSend = async ({ text, file }) => {
  if (!text && !file) {
    return
  }

  if (!isConnected.value) {
    ElMessage.warning('WebSocket未连接，请等待连接建立')
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
  ElMessage.info('文件上传功能即将推出')
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
    // 生成导出内容
    let exportContent = '# AI营养师对话记录\n\n'
    exportContent += `导出时间：${new Date().toLocaleString('zh-CN')}\n\n`
    exportContent += '---\n\n'

    messages.value.forEach((msg) => {
      const role = msg.role === 'user' ? '👤 用户' : '🤖 AI营养师'
      const time = new Date(msg.timestamp).toLocaleString('zh-CN')
      
      exportContent += `### ${role} - ${time}\n\n`
      exportContent += `${msg.content}\n\n`
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

// 重新连接
const reconnect = async () => {
  try {
    await connect()
    ElMessage.success('重新连接成功')
  } catch (error) {
    ElMessage.error('重新连接失败')
  }
}

// 设置WebSocket事件处理器
const setupWebSocketHandlers = () => {
  // 连接确认
  on('onConnectionConfirm', (message) => {
    console.log('✅ 连接确认:', message)
    ElMessage.success('WebSocket已连接')
  })

  // 流开始
  on('onStreamStart', () => {
    console.log('🎬 AI开始响应')
  })

  // 流片段
  on('onStreamChunk', (message) => {
    // currentResponse会自动更新，MessageList会监听并显示
  })

  // 流完成
  on('onStreamComplete', (message) => {
    console.log('✅ AI响应完成')
    isLoading.value = false

    // 更新消息列表中的AI消息
    const index = messages.value.findIndex(m => m.id === currentMessageId.value)
    if (index > -1) {
      messages.value[index] = {
        ...messages.value[index],
        content: message.fullContent,
        streaming: false,
        timestamp: Date.now()
      }
    }

    // 清除当前响应
    clearCurrentResponse()
  })

  // 错误处理
  on('onAIError', (message) => {
    console.error('❌ AI错误:', message.message)
    isLoading.value = false

    // 移除失败的AI消息
    const index = messages.value.findIndex(m => m.id === currentMessageId.value)
    if (index > -1) {
      messages.value.splice(index, 1)
    }

    clearCurrentResponse()
  })

  // 连接关闭
  on('onClose', (event) => {
    if (event.code !== 1000) {
      ElMessage.warning('WebSocket连接已断开，正在尝试重连...')
    }
  })

  // 连接错误
  on('onError', () => {
    isLoading.value = false
  })
}

// 初始化
onMounted(async () => {
  console.log('🔧 AIChatView mounted')
  
  // 加载设置
  loadSettings()
  
  // 设置事件处理器
  setupWebSocketHandlers()
  
  // 连接WebSocket
  try {
    await connect()
  } catch (error) {
    console.error('初始化连接失败:', error)
  }
})

// 清理
onUnmounted(() => {
  console.log('🧹 AIChatView unmounted')
  // 注意：不要断开WebSocket，因为是全局单例
})

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
