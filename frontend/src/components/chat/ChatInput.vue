<template>
  <div class="chat-input-container">
    <div class="input-wrapper">
      <!-- 文件上传按钮 -->
      <el-upload
        ref="uploadRef"
        :show-file-list="false"
        :before-upload="handleFileSelect"
        :accept="acceptedFileTypes"
        :disabled="disabled"
      >
        <el-button
          :icon="Paperclip"
          circle
          :disabled="disabled"
          title="上传文件"
        />
      </el-upload>

      <!-- 输入框 -->
      <el-input
        v-model="inputText"
        type="textarea"
        :rows="1"
        :autosize="{ minRows: 1, maxRows: 4 }"
        placeholder="输入消息... (Shift+Enter换行, Enter发送)"
        :disabled="disabled"
        @keydown.enter="handleKeyDown"
        resize="none"
        class="message-input"
      />

      <!-- 发送按钮 -->
      <el-button
        type="primary"
        :icon="loading ? Loading : Promotion"
        circle
        :disabled="!canSend"
        :loading="loading"
        @click="handleSend"
        title="发送消息"
      />
    </div>

    <!-- 已选择的文件 -->
    <transition name="slide-up">
      <div v-if="selectedFile" class="selected-file">
        <div class="file-info">
          <el-icon class="file-icon"><Document /></el-icon>
          <span class="file-name">{{ selectedFile.name }}</span>
          <span class="file-size">{{ formatFileSize(selectedFile.size) }}</span>
        </div>
        <el-button
          :icon="Close"
          link
          size="small"
          @click="clearFile"
        />
      </div>
    </transition>

    <!-- 字数统计 -->
    <div v-if="showCharCount" class="char-count" :class="{ warning: isOverLimit }">
      {{ inputText.length }} / {{ maxLength }}
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Paperclip,
  Promotion,
  Loading,
  Document,
  Close
} from '@element-plus/icons-vue'

const props = defineProps({
  disabled: {
    type: Boolean,
    default: false
  },
  loading: {
    type: Boolean,
    default: false
  },
  maxLength: {
    type: Number,
    default: 2000
  },
  maxFileSize: {
    type: Number,
    default: 10 * 1024 * 1024 // 10MB
  },
  acceptedFileTypes: {
    type: String,
    default: '.txt,.pdf,.doc,.docx'
  }
})

const emit = defineEmits(['send', 'fileSelect'])

const inputText = ref('')
const selectedFile = ref(null)
const uploadRef = ref(null)

// 是否可以发送
const canSend = computed(() => {
  return (inputText.value.trim() || selectedFile.value) && !props.disabled && !props.loading
})

// 是否显示字数统计
const showCharCount = computed(() => {
  return inputText.value.length > props.maxLength * 0.8
})

// 是否超出限制
const isOverLimit = computed(() => {
  return inputText.value.length > props.maxLength
})

// 处理键盘事件
const handleKeyDown = (event) => {
  // Shift + Enter 换行
  if (event.shiftKey && event.key === 'Enter') {
    return
  }
  
  // Enter 发送
  if (event.key === 'Enter') {
    event.preventDefault()
    handleSend()
  }
}

// 发送消息
const handleSend = () => {
  const text = inputText.value.trim()
  
  if (!canSend.value) {
    return
  }
  
  if (text.length > props.maxLength) {
    ElMessage.warning(`消息长度不能超过${props.maxLength}个字符`)
    return
  }
  
  // 发送消息
  emit('send', {
    text,
    file: selectedFile.value
  })
  
  // 清空输入
  inputText.value = ''
  selectedFile.value = null
}

// 处理文件选择
const handleFileSelect = (file) => {
  // 检查文件大小
  if (file.size > props.maxFileSize) {
    ElMessage.error(`文件大小不能超过${formatFileSize(props.maxFileSize)}`)
    return false
  }
  
  // 检查文件类型
  const fileExt = '.' + file.name.split('.').pop().toLowerCase()
  if (!props.acceptedFileTypes.includes(fileExt)) {
    ElMessage.error('不支持的文件类型')
    return false
  }
  
  selectedFile.value = file
  emit('fileSelect', file)
  
  return false // 阻止自动上传
}

// 清除文件
const clearFile = () => {
  selectedFile.value = null
}

// 格式化文件大小
const formatFileSize = (bytes) => {
  if (bytes === 0) return '0 B'
  
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

// 清空输入
const clear = () => {
  inputText.value = ''
  selectedFile.value = null
}

// 聚焦输入框
const focus = () => {
  // 这里需要获取el-input的内部输入框
  const textarea = document.querySelector('.message-input textarea')
  if (textarea) {
    textarea.focus()
  }
}

// 暴露方法
defineExpose({
  clear,
  focus
})
</script>

<style scoped>
.chat-input-container {
  padding: 16px 20px;
  background: white;
  border-top: 1px solid #e4e7ed;
  position: relative;
}

.input-wrapper {
  display: flex;
  align-items: flex-end;
  gap: 12px;
}

.message-input {
  flex: 1;
}

.message-input :deep(.el-textarea__inner) {
  padding: 12px 16px;
  border-radius: 20px;
  border: 1px solid #dcdfe6;
  transition: all 0.3s;
  font-size: 14px;
  line-height: 1.5;
}

.message-input :deep(.el-textarea__inner):focus {
  border-color: #409eff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.1);
}

.message-input :deep(.el-textarea__inner):disabled {
  background-color: #f5f7fa;
  cursor: not-allowed;
}

/* 选中的文件 */
.selected-file {
  margin-top: 12px;
  padding: 12px 16px;
  background: #f5f7fa;
  border-radius: 8px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  animation: slideUp 0.3s ease-out;
}

.file-info {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.file-icon {
  color: #409eff;
  font-size: 20px;
}

.file-name {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
}

.file-size {
  font-size: 12px;
  color: #909399;
}

/* 字数统计 */
.char-count {
  position: absolute;
  bottom: 24px;
  right: 80px;
  font-size: 12px;
  color: #909399;
  background: white;
  padding: 2px 8px;
  border-radius: 4px;
}

.char-count.warning {
  color: #f56c6c;
}

/* 动画 */
@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.slide-up-enter-active,
.slide-up-leave-active {
  transition: all 0.3s ease-out;
}

.slide-up-enter-from {
  opacity: 0;
  transform: translateY(10px);
}

.slide-up-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

/* 响应式 */
@media (max-width: 768px) {
  .chat-input-container {
    padding: 12px 16px;
  }
  
  .input-wrapper {
    gap: 8px;
  }
}
</style>
