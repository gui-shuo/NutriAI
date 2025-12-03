/**
 * AI WebSocket Composable
 * 封装WebSocket逻辑，提供Vue组合式API
 * 
 * @author NutriAI Team
 * @date 2025-12-03
 */

import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { createWebSocketClient, MessageType } from '@/services/websocket'

/**
 * AI WebSocket Hook
 * @returns {Object}
 */
export function useAIWebSocket() {
  // WebSocket客户端
  const wsClient = createWebSocketClient()
  
  // 状态
  const isConnected = wsClient.isConnected
  const isReceiving = wsClient.isReceiving
  const status = wsClient.status
  const currentResponse = wsClient.currentResponse
  
  // 当前流式响应的消息ID
  const currentMessageId = ref(null)
  
  // 连接WebSocket
  const connect = async () => {
    const token = localStorage.getItem('token')
    
    if (!token) {
      console.error('❌ 未找到token')
      ElMessage.error('请先登录')
      throw new Error('未登录')
    }
    
    try {
      await wsClient.connect(token)
      console.log('✅ WebSocket连接成功')
    } catch (error) {
      console.error('❌ WebSocket连接失败:', error)
      ElMessage.error('连接失败，请刷新页面重试')
      throw error
    }
  }
  
  // 断开连接
  const disconnect = () => {
    wsClient.close(false)
    console.log('🔴 WebSocket已断开')
  }
  
  // 发送消息
  const sendMessage = (message, keepContext = true) => {
    if (!wsClient.isReady()) {
      console.warn('⚠️ WebSocket未连接')
      ElMessage.warning('连接已断开，正在重连...')
      return
    }
    
    wsClient.sendMessage(message, keepContext)
  }
  
  // 发送心跳
  const sendHeartbeat = () => {
    wsClient.sendHeartbeat()
  }
  
  // 注册事件处理器
  const on = (event, handler) => {
    wsClient.on(event, handler)
  }
  
  // 移除事件处理器
  const off = (event) => {
    wsClient.off(event)
  }
  
  // 设置当前消息ID
  const setCurrentMessageId = (id) => {
    currentMessageId.value = id
  }
  
  // 获取当前消息ID
  const getCurrentMessageId = () => {
    return currentMessageId.value
  }
  
  // 清除当前响应
  const clearCurrentResponse = () => {
    wsClient.currentResponse.value = ''
    currentMessageId.value = null
  }
  
  // 检查是否准备就绪
  const isReady = () => {
    return wsClient.isReady()
  }
  
  // 获取状态
  const getStatus = () => {
    return wsClient.getStatus()
  }
  
  // 自动连接（可选）
  const autoConnect = async () => {
    try {
      await connect()
    } catch (error) {
      console.error('自动连接失败:', error)
    }
  }
  
  // 生命周期：组件挂载时自动连接
  onMounted(() => {
    console.log('🔧 useAIWebSocket hook mounted')
  })
  
  // 生命周期：组件卸载时断开连接
  onUnmounted(() => {
    console.log('🧹 useAIWebSocket hook unmounted')
    // 注意：不要在这里断开连接，因为WebSocket应该是全局单例
    // disconnect()
  })
  
  return {
    // 状态
    isConnected,
    isReceiving,
    status,
    currentResponse,
    currentMessageId,
    
    // 方法
    connect,
    disconnect,
    sendMessage,
    sendHeartbeat,
    on,
    off,
    isReady,
    getStatus,
    setCurrentMessageId,
    getCurrentMessageId,
    clearCurrentResponse,
    autoConnect
  }
}

export default useAIWebSocket
