<template>
  <span class="typing-effect">
    <span class="content">{{ displayedText }}</span>
    <span v-if="isTyping" class="cursor">|</span>
  </span>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted } from 'vue'

const props = defineProps({
  // 完整文本
  text: {
    type: String,
    default: ''
  },
  // 打字速度（毫秒/字）
  speed: {
    type: Number,
    default: 30
  },
  // 是否正在接收流式内容
  streaming: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['complete'])

// 显示的文本
const displayedText = ref('')

// 是否正在打字
const isTyping = ref(false)

// 打字定时器
let typingTimer = null

// 当前显示位置
let currentIndex = 0

/**
 * 开始打字效果
 */
const startTyping = () => {
  if (typingTimer) {
    clearTimeout(typingTimer)
  }
  
  const type = () => {
    if (currentIndex < props.text.length) {
      displayedText.value = props.text.substring(0, currentIndex + 1)
      currentIndex++
      
      typingTimer = setTimeout(type, props.speed)
    } else {
      isTyping.value = false
      emit('complete')
    }
  }
  
  isTyping.value = true
  type()
}

/**
 * 停止打字效果
 */
const stopTyping = () => {
  if (typingTimer) {
    clearTimeout(typingTimer)
    typingTimer = null
  }
  isTyping.value = false
}

/**
 * 立即显示全部文本
 */
const showAll = () => {
  stopTyping()
  displayedText.value = props.text
  currentIndex = props.text.length
  emit('complete')
}

// 监听文本变化
watch(() => props.text, (newText, oldText) => {
  if (props.streaming) {
    // 流式接收时，实时显示新内容（无打字效果，避免延迟）
    displayedText.value = newText
    currentIndex = newText.length
  } else if (newText && newText !== oldText) {
    // 非流式时，显示打字效果
    currentIndex = displayedText.value.length
    startTyping()
  }
}, { immediate: true })

// 监听streaming状态变化
watch(() => props.streaming, (isStreaming) => {
  if (isStreaming) {
    // 开始流式接收，停止打字效果
    stopTyping()
  }
})

// 组件挂载
onMounted(() => {
  if (props.text && !props.streaming) {
    startTyping()
  }
})

// 组件卸载
onUnmounted(() => {
  stopTyping()
})

// 暴露方法
defineExpose({
  startTyping,
  stopTyping,
  showAll
})
</script>

<style scoped>
.typing-effect {
  display: inline;
}

.content {
  white-space: pre-wrap;
  word-break: break-word;
}

.cursor {
  display: inline-block;
  width: 2px;
  height: 1em;
  background-color: currentColor;
  margin-left: 2px;
  animation: blink 1s step-end infinite;
}

@keyframes blink {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0;
  }
}
</style>
