<template>
  <div ref="messageContainer" class="message-list-container">
    <!-- 欢迎消息 -->
    <div v-if="messages.length === 0" class="welcome-message">
      <div class="welcome-icon">
        <el-icon :size="48">
          <ChatDotRound />
        </el-icon>
      </div>
      <h2>👋 你好！我是你的AI营养师</h2>
      <p>我可以帮你：</p>
      <ul class="feature-list">
        <li>📊 分析食物营养成分</li>
        <li>🍽️ 制定个性化饮食计划</li>
        <li>💪 提供营养饮食建议</li>
        <li>🥗 推荐适合的食谱</li>
      </ul>
      <p class="start-tip">点击下方快捷按钮或直接输入问题开始吧！</p>
    </div>

    <!-- 消息列表 -->
    <div v-else class="messages">
      <div
        v-for="message in messages"
        :key="message.id"
        :class="['message-item', `message-${message.role}`]"
      >
        <!-- 用户消息 -->
        <div v-if="message.role === 'user'" class="message-content">
          <div class="message-avatar">
            <el-avatar :size="36">
              <img
                v-if="userAvatar"
                :src="userAvatar"
                referrerpolicy="no-referrer"
                style="width: 100%; height: 100%; object-fit: cover"
              />
              <el-icon v-else><User /></el-icon>
            </el-avatar>
          </div>
          <div class="message-bubble user-bubble">
            <div class="message-text">
              {{ message.content }}
            </div>
            <div class="message-time">
              {{ formatTime(message.timestamp) }}
            </div>
          </div>
        </div>

        <!-- AI消息 -->
        <div v-else class="message-content">
          <div class="message-avatar">
            <el-avatar :size="36" class="ai-avatar">
              <el-icon><Cpu /></el-icon>
            </el-avatar>
          </div>
          <div class="message-bubble ai-bubble">
            <!-- 加载中 -->
            <div v-if="message.loading" class="typing-indicator">
              <span />
              <span />
              <span />
              <div v-if="waitingText" class="waiting-hint">{{ waitingText }}</div>
            </div>
            <!-- 消息内容 -->
            <div v-else class="message-text">
              <!-- 流式显示 -->
              <template v-if="message.id === streamingMessageId && streamingContent">
                <div v-html="renderMarkdown(streamingContent)" />
                <span class="streaming-cursor">|</span>
              </template>
              <!-- 普通显示 -->
              <div v-else v-html="renderMarkdown(message.content)" />
            </div>
            <div v-if="!message.loading" class="message-actions">
              <span class="message-time">{{ formatTime(message.timestamp) }}</span>
              <div class="action-buttons">
                <el-button link size="small" title="复制" @click="copyMessage(message.content)">
                  <el-icon><DocumentCopy /></el-icon>
                </el-button>
                <el-button link size="small" title="重新生成" @click="regenerate(message)">
                  <el-icon><Refresh /></el-icon>
                </el-button>
                <el-button
                  link
                  size="small"
                  :title="message.favorite ? '取消收藏' : '收藏'"
                  @click="toggleFavorite(message)"
                >
                  <el-icon>
                    <component :is="message.favorite ? StarFilled : Star" />
                  </el-icon>
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 滚动到底部按钮 -->
    <transition name="fade">
      <div v-if="showScrollButton" class="scroll-bottom-btn" @click="scrollToBottom">
        <el-button circle>
          <el-icon><ArrowDown /></el-icon>
        </el-button>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import { ElMessage } from 'element-plus'
import {
  ChatDotRound,
  User,
  Cpu,
  DocumentCopy,
  Refresh,
  ArrowDown,
  Star,
  StarFilled
} from '@element-plus/icons-vue'

const props = defineProps({
  messages: {
    type: Array,
    required: true,
    default: () => []
  },
  // 流式显示相关
  streamingMessageId: {
    type: String,
    default: null
  },
  streamingContent: {
    type: String,
    default: ''
  },
  waitingText: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['regenerate', 'favorite', 'unfavorite'])

const authStore = useAuthStore()
const messageContainer = ref(null)
const showScrollButton = ref(false)

// 用户头像
const userAvatar = computed(() => authStore.user?.avatar || '')

// 格式化时间
const formatTime = timestamp => {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  const now = new Date()
  const diff = now - date

  // 小于1分钟
  if (diff < 60000) {
    return '刚刚'
  }
  // 小于1小时
  if (diff < 3600000) {
    return `${Math.floor(diff / 60000)}分钟前`
  }
  // 小于24小时
  if (diff < 86400000) {
    return `${Math.floor(diff / 3600000)}小时前`
  }
  // 显示日期
  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 渲染Markdown
const renderMarkdown = content => {
  if (!content) return ''

  // 配置marked
  marked.setOptions({
    breaks: true,
    gfm: true
  })

  // 转换Markdown并清理HTML
  const html = marked.parse(content)
  return DOMPurify.sanitize(html)
}

// 复制消息
const copyMessage = content => {
  navigator.clipboard
    .writeText(content)
    .then(() => {
      ElMessage.success('已复制到剪贴板')
    })
    .catch(() => {
      ElMessage.error('复制失败')
    })
}

// 重新生成
const regenerate = message => {
  emit('regenerate', message)
}

// 收藏/取消收藏
const toggleFavorite = message => {
  if (message.favorite) {
    emit('unfavorite', message.id)
  } else {
    emit('favorite', message.id)
  }
}

// 滚动到底部
const scrollToBottom = (smooth = true) => {
  nextTick(() => {
    if (messageContainer.value) {
      messageContainer.value.scrollTo({
        top: messageContainer.value.scrollHeight,
        behavior: smooth ? 'smooth' : 'auto'
      })
    }
  })
}

// 监听滚动
const handleScroll = () => {
  if (!messageContainer.value) return

  const { scrollTop, scrollHeight, clientHeight } = messageContainer.value
  const isNearBottom = scrollHeight - scrollTop - clientHeight < 200

  showScrollButton.value = !isNearBottom
}

// 监听消息变化，自动滚动
watch(
  () => props.messages,
  () => {
    nextTick(() => {
      // 如果用户在底部或接近底部，自动滚动
      if (!showScrollButton.value) {
        scrollToBottom(true)
      }
    })
  },
  { deep: true }
)

// 初始化
onMounted(() => {
  if (messageContainer.value) {
    messageContainer.value.addEventListener('scroll', handleScroll)
    scrollToBottom(false)
  }
})

onUnmounted(() => {
  if (messageContainer.value) {
    messageContainer.value.removeEventListener('scroll', handleScroll)
  }
})

// 暴露方法
defineExpose({
  scrollToBottom
})
</script>

<style scoped>
.message-list-container {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: #fdfbf7;
  position: relative;
}

/* 欢迎消息 */
.welcome-message {
  max-width: 600px;
  margin: 60px auto;
  text-align: center;
  animation: fadeInUp 0.5s ease-out;
}

.welcome-icon {
  color: #2d5da1;
  margin-bottom: 20px;
}

.welcome-message h2 {
  font-size: 24px;
  color: #2d2d2d;
  margin-bottom: 16px;
  font-family: 'Kalam', 'ZCOOL KuaiLe', cursive;
}

.welcome-message > p {
  color: #2d2d2d;
  margin-bottom: 16px;
  font-size: 16px;
  font-family: 'Patrick Hand', cursive;
}

.feature-list {
  list-style: none;
  padding: 0;
  margin: 20px 0;
  text-align: left;
  display: inline-block;
}

.feature-list li {
  padding: 10px 0;
  font-size: 15px;
  color: #2d2d2d;
  font-family: 'Patrick Hand', cursive;
}

.start-tip {
  margin-top: 30px;
  color: #e5e0d8;
  font-size: 14px;
  font-family: 'Patrick Hand', cursive;
}

/* 消息列表 */
.messages {
  max-width: 900px;
  margin: 0 auto;
}

.message-item {
  margin-bottom: 24px;
  animation: fadeIn 0.3s ease-out;
}

.message-content {
  display: flex;
  gap: 12px;
}

.message-user .message-content {
  flex-direction: row-reverse;
}

/* 头像 */
.message-avatar {
  flex-shrink: 0;
}

.ai-avatar {
  background: #2d5da1;
}

/* 消息气泡 */
.message-bubble {
  max-width: 70%;
  padding: 12px 16px;
  border-radius: 255px 15px 225px 15px / 15px 225px 15px 255px;
  position: relative;
}

.user-bubble {
  background: #2d5da1;
  color: white;
  border-radius: 15px 225px 15px 255px / 255px 15px 225px 15px;
}

.ai-bubble {
  background: #fdfbf7;
  border: 2px solid #2d2d2d;
  border-radius: 255px 15px 225px 15px / 15px 225px 15px 255px;
  box-shadow: 4px 4px 0px 0px #2d2d2d;
}

/* 消息文本 */
.message-text {
  line-height: 1.6;
  word-wrap: break-word;
  font-family: 'Patrick Hand', cursive;
}

.user-bubble .message-text {
  color: white;
}

.ai-bubble .message-text {
  color: #2d2d2d;
}

/* Markdown样式 */
.ai-bubble .message-text :deep(p) {
  margin: 8px 0;
}

.ai-bubble .message-text :deep(h1),
.ai-bubble .message-text :deep(h2),
.ai-bubble .message-text :deep(h3) {
  margin: 16px 0 8px 0;
  font-weight: 600;
  font-family: 'Kalam', 'ZCOOL KuaiLe', cursive;
}

.ai-bubble .message-text :deep(ul),
.ai-bubble .message-text :deep(ol) {
  margin: 8px 0;
  padding-left: 24px;
}

.ai-bubble .message-text :deep(code) {
  background: #fdfbf7;
  padding: 2px 6px;
  border: 1px solid #2d2d2d;
  border-radius: 185px 10px 155px 10px / 10px 155px 10px 185px;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 0.9em;
}

.ai-bubble .message-text :deep(pre) {
  background: #fdfbf7;
  padding: 12px;
  border: 2px solid #2d2d2d;
  border-radius: 185px 10px 155px 10px / 10px 155px 10px 185px;
  overflow-x: auto;
  margin: 8px 0;
  box-shadow: 3px 3px 0px 0px rgba(45, 45, 45, 0.1);
}

.ai-bubble .message-text :deep(blockquote) {
  border-left: 4px solid #2d5da1;
  padding-left: 12px;
  margin: 8px 0;
  color: #2d2d2d;
}

/* 流式显示光标 */
.streaming-cursor {
  display: inline-block;
  width: 2px;
  height: 1em;
  background-color: #2d5da1;
  margin-left: 2px;
  animation: blink 1s step-end infinite;
  vertical-align: text-bottom;
}

@keyframes blink {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0;
  }
}

/* 输入指示器 */
.typing-indicator {
  display: flex;
  gap: 6px;
  padding: 8px 0;
  flex-wrap: wrap;
  align-items: center;
}

.waiting-hint {
  width: 100%;
  font-size: 12px;
  color: #e5e0d8;
  margin-top: 6px;
  animation: fadeInHint 0.3s ease;
  font-family: 'Patrick Hand', cursive;
}

@keyframes fadeInHint {
  from { opacity: 0; transform: translateY(-4px); }
  to { opacity: 1; transform: translateY(0); }
}

.typing-indicator span {
  width: 8px;
  height: 8px;
  background: #2d5da1;
  border-radius: 50%;
  animation: typing 1.4s infinite;
}

.typing-indicator span:nth-child(2) {
  animation-delay: 0.2s;
}

.typing-indicator span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes typing {
  0%,
  60%,
  100% {
    transform: translateY(0);
    opacity: 0.7;
  }
  30% {
    transform: translateY(-10px);
    opacity: 1;
  }
}

/* 消息操作 */
.message-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px dashed #e5e0d8;
}

.message-time {
  font-size: 12px;
  color: #e5e0d8;
  font-family: 'Patrick Hand', cursive;
}

.user-bubble .message-time {
  color: rgba(255, 255, 255, 0.8);
  text-align: right;
  display: block;
  margin-top: 4px;
}

.action-buttons {
  display: flex;
  gap: 4px;
}

.action-buttons .el-button {
  color: #e5e0d8;
}

.action-buttons .el-button:hover {
  color: #2d5da1;
}

/* 滚动到底部按钮 */
.scroll-bottom-btn {
  position: absolute;
  bottom: 20px;
  right: 20px;
  z-index: 10;
}

.scroll-bottom-btn .el-button {
  box-shadow: 4px 4px 0px 0px #2d2d2d;
}

/* 动画 */
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.fade-enter-active,
.fade-leave-active {
  transition:
    opacity 0.3s,
    transform 0.3s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(10px);
}

/* 滚动条样式 */
.message-list-container::-webkit-scrollbar {
  width: 6px;
}

.message-list-container::-webkit-scrollbar-track {
  background: transparent;
}

.message-list-container::-webkit-scrollbar-thumb {
  background: #e5e0d8;
  border-radius: 3px;
}

.message-list-container::-webkit-scrollbar-thumb:hover {
  background: #2d2d2d;
}
</style>
