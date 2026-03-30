/**
 * AI聊天WebSocket客户端
 * 提供WebSocket连接管理、消息收发、断线重连等功能
 *
 * @author NutriAI Team
 * @date 2025-12-03
 */

import { ref } from 'vue'
import { ElMessage } from 'element-plus'

// WebSocket URL配置——动态构建，支持 http/https 自动切换 ws/wss
function getWsBaseUrl() {
  const env = import.meta.env.VITE_WS_BASE_URL || import.meta.env.VITE_WS_URL
  if (env && (env.startsWith('ws://') || env.startsWith('wss://'))) {
    return env
  }
  // 根据当前页面协议自动构建WebSocket URL
  const loc = window.location
  const protocol = loc.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${protocol}//${loc.host}/api/ws/ai/chat`
}
const WS_BASE_URL = getWsBaseUrl()

// 消息类型
export const MessageType = {
  CHAT: 'chat',
  PING: 'ping',
  CONNECTION: 'connection',
  START: 'start',
  CHUNK: 'chunk',
  COMPLETE: 'complete',
  PONG: 'pong',
  ERROR: 'error'
}

// WebSocket连接状态
export const ConnectionStatus = {
  DISCONNECTED: 'disconnected',
  CONNECTING: 'connecting',
  CONNECTED: 'connected',
  RECONNECTING: 'reconnecting',
  ERROR: 'error'
}

/**
 * AI WebSocket 客户端类
 */
export class AIWebSocketClient {
  constructor() {
    // WebSocket实例
    this.ws = null

    // 连接状态
    this.status = ref(ConnectionStatus.DISCONNECTED)
    this.isConnected = ref(false)
    this.isReceiving = ref(false)

    // 重连配置
    this.reconnectAttempts = 0
    this.maxReconnectAttempts = 5
    this.reconnectDelay = 1000 // 初始延迟1秒
    this.reconnectTimer = null

    // 心跳配置
    this.heartbeatInterval = 15000 // 15秒（CDN可能30秒超时，保持活跃）
    this.heartbeatTimer = null

    // 当前响应累积内容
    this.currentResponse = ref('')

    // 消息队列（在连接建立前缓存）
    this.messageQueue = []

    // 事件处理器
    this.eventHandlers = {
      onOpen: null,
      onClose: null,
      onError: null,
      onMessage: null,
      onConnectionConfirm: null,
      onStreamStart: null,
      onStreamChunk: null,
      onStreamComplete: null,
      onPong: null,
      onAIError: null
    }

    // 自动重连开关
    this.autoReconnect = true

    console.log('🔧 AIWebSocketClient 初始化完成')
  }

  /**
   * 连接WebSocket
   * @param {string} token - JWT Token
   * @returns {Promise<void>}
   */
  connect(token) {
    return new Promise((resolve, reject) => {
      if (!token) {
        const error = new Error('Token不能为空')
        console.error('❌', error.message)
        reject(error)
        return
      }

      if (this.ws && this.ws.readyState === WebSocket.OPEN) {
        console.log('✅ WebSocket已连接，无需重复连接')
        resolve()
        return
      }

      try {
        // 重置自动重连
        this.autoReconnect = true

        // 构建WebSocket URL（注意：路径已包含/api前缀）
        const wsUrl = `${WS_BASE_URL}?token=${token}`
        console.log('🔌 正在连接WebSocket:', wsUrl.replace(/token=.*/, 'token=***'))

        this.status.value = ConnectionStatus.CONNECTING
        this.ws = new WebSocket(wsUrl)

        // 连接打开
        this.ws.onopen = () => {
          console.log('✅ WebSocket连接成功')
          this.status.value = ConnectionStatus.CONNECTED
          this.isConnected.value = true
          this.reconnectAttempts = 0

          // 启动心跳
          this.startHeartbeat()

          // 发送队列中的消息
          this.flushMessageQueue()

          // 触发回调
          this.eventHandlers.onOpen?.()

          resolve()
        }

        // 接收消息
        this.ws.onmessage = event => {
          try {
            const message = JSON.parse(event.data)
            this.handleMessage(message)
          } catch (error) {
            console.error('❌ 消息解析失败:', error, event.data)
          }
        }

        // 连接错误
        this.ws.onerror = error => {
          console.error('❌ WebSocket错误:', error)
          this.status.value = ConnectionStatus.ERROR
          this.eventHandlers.onError?.(error)
        }

        // 连接关闭
        this.ws.onclose = event => {
          console.log(`🔴 WebSocket关闭 (代码: ${event.code}, 原因: ${event.reason || '无'})`)
          this.isConnected.value = false
          this.isReceiving.value = false
          this.stopHeartbeat()

          // 触发回调
          this.eventHandlers.onClose?.(event)

          // 尝试重连（如果不是正常关闭）
          if (event.code !== 1000 && this.autoReconnect) {
            this.attemptReconnect(token)
          } else {
            this.status.value = ConnectionStatus.DISCONNECTED
          }
        }
      } catch (error) {
        console.error('❌ 连接失败:', error)
        this.status.value = ConnectionStatus.ERROR
        reject(error)
      }
    })
  }

  /**
   * 处理接收到的消息
   * @param {Object} message - 服务端消息
   */
  handleMessage(message) {
    console.log(`📨 [${message.type}]`, message)

    // 触发通用消息回调
    this.eventHandlers.onMessage?.(message)

    // 根据消息类型处理
    switch (message.type) {
      case MessageType.CONNECTION:
        // 连接确认
        console.log(`✅ 连接确认: 用户ID=${message.userId}`)
        this.eventHandlers.onConnectionConfirm?.(message)
        break

      case MessageType.START:
        // AI开始响应
        console.log('🎬 AI开始响应')
        this.isReceiving.value = true
        this.currentResponse.value = ''
        this.eventHandlers.onStreamStart?.(message)
        break

      case MessageType.CHUNK:
        // AI响应片段
        if (message.content) {
          this.currentResponse.value += message.content
          this.eventHandlers.onStreamChunk?.(message)
        }
        break

      case MessageType.COMPLETE:
        // AI响应完成
        console.log(`✅ AI响应完成 (字数: ${this.currentResponse.value.length})`)
        this.isReceiving.value = false
        this.eventHandlers.onStreamComplete?.({
          ...message,
          fullContent: this.currentResponse.value
        })
        this.currentResponse.value = ''
        break

      case MessageType.PONG:
        // 心跳响应
        console.log('💓 心跳正常')
        this.eventHandlers.onPong?.(message)
        break

      case MessageType.ERROR:
        // 错误消息
        console.error('❌ 服务端错误:', message.message)
        this.eventHandlers.onAIError?.(message)
        ElMessage.error(message.message || '服务端错误')
        break

      default:
        console.warn('⚠️ 未知消息类型:', message.type)
    }
  }

  /**
   * 发送聊天消息
   * @param {string} message - 消息内容
   * @param {boolean} keepContext - 是否保持上下文
   */
  sendMessage(message, keepContext = true) {
    if (!message || !message.trim()) {
      console.warn('⚠️ 消息内容为空')
      return
    }

    const chatMessage = {
      type: MessageType.CHAT,
      message: message.trim(),
      keepContext
    }

    this.send(chatMessage)
    console.log('📤 已发送消息:', message.substring(0, 50) + (message.length > 50 ? '...' : ''))
  }

  /**
   * 发送心跳
   */
  sendHeartbeat() {
    const pingMessage = {
      type: MessageType.PING
    }
    this.send(pingMessage)
  }

  /**
   * 发送消息（通用方法）
   * @param {Object} message - 消息对象
   */
  send(message) {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
      console.warn('⚠️ WebSocket未连接，消息已加入队列')
      this.messageQueue.push(message)
      return
    }

    try {
      this.ws.send(JSON.stringify(message))
    } catch (error) {
      console.error('❌ 发送消息失败:', error)
      throw error
    }
  }

  /**
   * 发送队列中的消息
   */
  flushMessageQueue() {
    if (this.messageQueue.length === 0) return

    console.log(`📤 发送队列中的${this.messageQueue.length}条消息`)
    while (this.messageQueue.length > 0) {
      const message = this.messageQueue.shift()
      this.send(message)
    }
  }

  /**
   * 启动心跳
   */
  startHeartbeat() {
    this.stopHeartbeat()

    this.heartbeatTimer = setInterval(() => {
      if (this.isConnected.value) {
        this.sendHeartbeat()
      }
    }, this.heartbeatInterval)

    console.log('💓 心跳已启动')
  }

  /**
   * 停止心跳
   */
  stopHeartbeat() {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer)
      this.heartbeatTimer = null
      console.log('💔 心跳已停止')
    }
  }

  /**
   * 尝试重连
   * @param {string} token - JWT Token (will be refreshed from localStorage)
   */
  attemptReconnect(token) {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.error(`❌ 重连失败，已达到最大尝试次数 (${this.maxReconnectAttempts})`)
      this.status.value = ConnectionStatus.ERROR
      ElMessage.error('WebSocket连接失败，请刷新页面重试')
      return
    }

    this.reconnectAttempts++
    this.status.value = ConnectionStatus.RECONNECTING

    const delay = this.reconnectDelay * Math.pow(2, this.reconnectAttempts - 1) // 指数退避
    console.log(`🔄 ${delay}ms后尝试第${this.reconnectAttempts}次重连...`)

    this.reconnectTimer = setTimeout(() => {
      // 重连时重新获取最新 token（可能已刷新）
      const freshToken = localStorage.getItem('token') || token
      if (!freshToken) {
        console.error('❌ Token已失效，停止重连')
        this.status.value = ConnectionStatus.ERROR
        return
      }
      console.log(`🔄 开始第${this.reconnectAttempts}次重连`)
      this.connect(freshToken).catch(error => {
        console.error('❌ 重连失败:', error)
      })
    }, delay)
  }

  /**
   * 关闭连接
   * @param {boolean} autoReconnect - 是否允许自动重连
   */
  close(autoReconnect = false) {
    this.autoReconnect = autoReconnect

    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }

    this.stopHeartbeat()

    if (this.ws) {
      console.log('🔴 主动关闭WebSocket连接')
      this.ws.close(1000, '正常关闭')
      this.ws = null
    }

    this.isConnected.value = false
    this.isReceiving.value = false
    this.status.value = ConnectionStatus.DISCONNECTED
  }

  /**
   * 注册事件处理器
   * @param {string} event - 事件名称
   * @param {Function} handler - 处理函数
   */
  on(event, handler) {
    if (Object.prototype.hasOwnProperty.call(this.eventHandlers, event)) {
      this.eventHandlers[event] = handler
    } else {
      console.warn(`⚠️ 未知事件: ${event}`)
    }
  }

  /**
   * 移除事件处理器
   * @param {string} event - 事件名称
   */
  off(event) {
    if (Object.prototype.hasOwnProperty.call(this.eventHandlers, event)) {
      this.eventHandlers[event] = null
    }
  }

  /**
   * 获取连接状态
   * @returns {string}
   */
  getStatus() {
    return this.status.value
  }

  /**
   * 检查是否已连接
   * @returns {boolean}
   */
  isReady() {
    return this.isConnected.value && this.ws && this.ws.readyState === WebSocket.OPEN
  }
}

/**
 * 创建WebSocket客户端实例（单例）
 */
let instance = null

export function createWebSocketClient() {
  if (!instance) {
    instance = new AIWebSocketClient()
  }
  return instance
}

export function getWebSocketClient() {
  return instance
}

export default {
  AIWebSocketClient,
  createWebSocketClient,
  getWebSocketClient,
  MessageType,
  ConnectionStatus
}
