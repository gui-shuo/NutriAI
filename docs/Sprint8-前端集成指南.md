# Sprint 8 - 前端WebSocket集成指南

## 📋 概述

本文档说明如何在前端（Vue 3 + TypeScript）中集成WebSocket AI聊天功能。

---

## 🔗 WebSocket端点

### 基础信息

- **协议**: WebSocket (ws://)
- **域名**: localhost
- **端口**: 8080
- **Context Path**: `/api` ⚠️ **重要！所有路径都需要加此前缀**

### 端点列表

| 端点 | 完整路径 | 用途 | 认证 |
|------|----------|------|------|
| 测试端点 | `ws://localhost:8080/api/ws/test` | 简单测试 | 否 |
| AI聊天 | `ws://localhost:8080/api/ws/ai/chat?token={JWT}` | AI对话 | 是 |
| SockJS | `ws://localhost:8080/api/ws/ai/chat-sockjs` | 降级支持 | 是 |

---

## 🚀 快速开始

### 1. 安装（如需要）

```bash
# 原生WebSocket不需要额外安装
# 如果使用SockJS，需要安装：
npm install sockjs-client
```

### 2. 创建WebSocket服务

**文件**: `src/services/websocket.ts`

```typescript
import { ref, Ref } from 'vue'

export interface ChatMessage {
  type: 'chat' | 'ping'
  message?: string
  keepContext?: boolean
}

export interface ServerMessage {
  type: 'connection' | 'start' | 'chunk' | 'complete' | 'pong' | 'error'
  content?: string
  message?: string
  userId?: number
  sessionId?: string
  timestamp?: number
}

export class AIWebSocketService {
  private ws: WebSocket | null = null
  private reconnectAttempts = 0
  private maxReconnectAttempts = 5
  private reconnectDelay = 1000
  
  // 响应状态
  public isConnected: Ref<boolean> = ref(false)
  public isReceiving: Ref<boolean> = ref(false)
  public currentResponse: Ref<string> = ref('')
  
  // 回调函数
  public onMessage?: (message: ServerMessage) => void
  public onError?: (error: Event) => void
  public onClose?: (event: CloseEvent) => void
  
  /**
   * 连接WebSocket
   * @param token JWT Token
   */
  connect(token: string): Promise<void> {
    return new Promise((resolve, reject) => {
      try {
        // ⚠️ 注意：路径包含 /api 前缀
        const wsUrl = `ws://localhost:8080/api/ws/ai/chat?token=${token}`
        this.ws = new WebSocket(wsUrl)
        
        this.ws.onopen = () => {
          console.log('✅ WebSocket连接成功')
          this.isConnected.value = true
          this.reconnectAttempts = 0
          resolve()
        }
        
        this.ws.onmessage = (event) => {
          const message: ServerMessage = JSON.parse(event.data)
          this.handleMessage(message)
        }
        
        this.ws.onerror = (error) => {
          console.error('❌ WebSocket错误:', error)
          this.onError?.(error)
          reject(error)
        }
        
        this.ws.onclose = (event) => {
          console.log(`🔴 WebSocket关闭 (代码: ${event.code})`)
          this.isConnected.value = false
          this.isReceiving.value = false
          this.onClose?.(event)
          
          // 如果不是正常关闭，尝试重连
          if (event.code !== 1000 && this.reconnectAttempts < this.maxReconnectAttempts) {
            this.attemptReconnect(token)
          }
        }
      } catch (error) {
        console.error('连接失败:', error)
        reject(error)
      }
    })
  }
  
  /**
   * 处理服务端消息
   */
  private handleMessage(message: ServerMessage) {
    console.log(`📨 [${message.type}]`, message)
    
    switch (message.type) {
      case 'connection':
        console.log(`已连接，用户ID: ${message.userId}`)
        break
        
      case 'start':
        this.isReceiving.value = true
        this.currentResponse.value = ''
        break
        
      case 'chunk':
        this.currentResponse.value += message.content || ''
        break
        
      case 'complete':
        this.isReceiving.value = false
        console.log('✅ 响应完成')
        break
        
      case 'pong':
        console.log('💓 心跳正常')
        break
        
      case 'error':
        console.error('错误:', message.message)
        break
    }
    
    // 触发自定义回调
    this.onMessage?.(message)
  }
  
  /**
   * 发送聊天消息
   * @param message 消息内容
   * @param keepContext 是否保持上下文
   */
  sendMessage(message: string, keepContext = true) {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
      throw new Error('WebSocket未连接')
    }
    
    const chatMessage: ChatMessage = {
      type: 'chat',
      message,
      keepContext
    }
    
    this.ws.send(JSON.stringify(chatMessage))
    console.log('📤 已发送:', message)
  }
  
  /**
   * 发送心跳
   */
  sendHeartbeat() {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
      return
    }
    
    const ping: ChatMessage = { type: 'ping' }
    this.ws.send(JSON.stringify(ping))
  }
  
  /**
   * 关闭连接
   */
  close() {
    if (this.ws) {
      this.ws.close(1000, '正常关闭')
      this.ws = null
    }
  }
  
  /**
   * 尝试重连
   */
  private attemptReconnect(token: string) {
    this.reconnectAttempts++
    console.log(`🔄 尝试重连 (${this.reconnectAttempts}/${this.maxReconnectAttempts})`)
    
    setTimeout(() => {
      this.connect(token).catch((error) => {
        console.error('重连失败:', error)
      })
    }, this.reconnectDelay * this.reconnectAttempts)
  }
}
```

### 3. 在组件中使用

**文件**: `src/views/AIChat.vue`

```vue
<template>
  <div class="ai-chat">
    <div class="chat-header">
      <span :class="['status', isConnected ? 'connected' : 'disconnected']">
        {{ isConnected ? '● 已连接' : '○ 未连接' }}
      </span>
    </div>
    
    <div class="chat-messages">
      <!-- 历史消息 -->
      <div v-for="(msg, index) in messages" :key="index" class="message">
        <div class="message-user">{{ msg.user }}</div>
        <div class="message-content">{{ msg.content }}</div>
      </div>
      
      <!-- AI正在响应 -->
      <div v-if="isReceiving" class="message ai-message">
        <div class="message-user">AI助手</div>
        <div class="message-content">{{ currentResponse }}</div>
        <div class="typing-indicator">正在输入...</div>
      </div>
    </div>
    
    <div class="chat-input">
      <input
        v-model="inputMessage"
        @keyup.enter="sendMessage"
        placeholder="输入消息..."
        :disabled="!isConnected || isReceiving"
      />
      <button 
        @click="sendMessage"
        :disabled="!isConnected || isReceiving || !inputMessage.trim()"
      >
        发送
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { AIWebSocketService, ServerMessage } from '@/services/websocket'

const userStore = useUserStore()
const wsService = new AIWebSocketService()

// 状态
const isConnected = wsService.isConnected
const isReceiving = wsService.isReceiving
const currentResponse = wsService.currentResponse
const inputMessage = ref('')
const messages = ref<Array<{ user: string; content: string }>>([])

// 初始化
onMounted(async () => {
  try {
    // 从store获取token
    const token = userStore.token
    if (!token) {
      console.error('未登录')
      return
    }
    
    // 连接WebSocket
    await wsService.connect(token)
    
    // 设置消息回调
    wsService.onMessage = handleMessage
    
    // 启动心跳
    startHeartbeat()
  } catch (error) {
    console.error('初始化失败:', error)
  }
})

// 清理
onUnmounted(() => {
  wsService.close()
})

// 处理服务端消息
function handleMessage(message: ServerMessage) {
  if (message.type === 'complete') {
    // 响应完成，添加到历史消息
    messages.value.push({
      user: 'AI助手',
      content: currentResponse.value
    })
    currentResponse.value = ''
  }
}

// 发送消息
function sendMessage() {
  if (!inputMessage.value.trim()) return
  
  // 添加用户消息到历史
  messages.value.push({
    user: '我',
    content: inputMessage.value
  })
  
  // 发送到服务端
  wsService.sendMessage(inputMessage.value)
  
  // 清空输入框
  inputMessage.value = ''
}

// 心跳
let heartbeatTimer: number | null = null
function startHeartbeat() {
  heartbeatTimer = window.setInterval(() => {
    wsService.sendHeartbeat()
  }, 30000) // 每30秒一次
}
</script>

<style scoped>
.ai-chat {
  display: flex;
  flex-direction: column;
  height: 100vh;
}

.chat-header {
  padding: 1rem;
  background: #f5f5f5;
  border-bottom: 1px solid #ddd;
}

.status {
  font-size: 0.9rem;
}

.status.connected {
  color: #52c41a;
}

.status.disconnected {
  color: #f5222d;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 1rem;
}

.message {
  margin-bottom: 1rem;
}

.message-user {
  font-weight: bold;
  margin-bottom: 0.5rem;
}

.message-content {
  padding: 0.5rem;
  background: #f0f0f0;
  border-radius: 4px;
}

.ai-message .message-content {
  background: #e6f7ff;
}

.typing-indicator {
  font-size: 0.8rem;
  color: #999;
  margin-top: 0.5rem;
}

.chat-input {
  display: flex;
  padding: 1rem;
  border-top: 1px solid #ddd;
}

.chat-input input {
  flex: 1;
  padding: 0.5rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  margin-right: 0.5rem;
}

.chat-input button {
  padding: 0.5rem 1rem;
  background: #1890ff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.chat-input button:disabled {
  background: #d9d9d9;
  cursor: not-allowed;
}
</style>
```

---

## 🔐 认证说明

### Token获取

```typescript
// 从localStorage获取
const token = localStorage.getItem('token')

// 或从Pinia store获取
import { useUserStore } from '@/stores/user'
const userStore = useUserStore()
const token = userStore.token
```

### Token传递

WebSocket连接时通过URL参数传递：

```typescript
const wsUrl = `ws://localhost:8080/api/ws/ai/chat?token=${token}`
```

---

## 📨 消息格式

### 客户端发送

#### 聊天消息

```json
{
  "type": "chat",
  "message": "你好，请介绍一下你自己",
  "keepContext": true
}
```

#### 心跳消息

```json
{
  "type": "ping"
}
```

### 服务端响应

#### 连接确认

```json
{
  "type": "connection",
  "message": "连接成功",
  "userId": 1,
  "sessionId": "abc-123-def-456"
}
```

#### 开始响应

```json
{
  "type": "start",
  "message": "AI开始响应"
}
```

#### 内容块

```json
{
  "type": "chunk",
  "content": "你好！"
}
```

#### 响应完成

```json
{
  "type": "complete",
  "message": "响应完成"
}
```

#### 心跳响应

```json
{
  "type": "pong",
  "timestamp": 1764765983891
}
```

#### 错误消息

```json
{
  "type": "error",
  "message": "错误描述"
}
```

---

## ⚠️ 常见问题

### Q1: 连接失败，错误代码1006

**原因**: 路径错误或Token无效

**解决**:
1. 确认路径包含`/api`前缀：`ws://localhost:8080/api/ws/ai/chat`
2. 检查Token是否有效
3. 查看浏览器Network标签的WS连接详情

### Q2: 如何调试WebSocket？

**方法**:
1. 打开浏览器开发者工具（F12）
2. 切换到`Network`标签
3. 过滤器选择`WS`
4. 点击连接查看详情：
   - Headers: 请求头和响应头
   - Messages: 收发的消息
   - Frames: 原始数据帧

### Q3: 如何实现断线重连？

**参考上面的`AIWebSocketService`类中的`attemptReconnect`方法**

### Q4: 生产环境配置

**修改WebSocket URL**:

```typescript
// 开发环境
const WS_URL = 'ws://localhost:8080/api/ws/ai/chat'

// 生产环境
const WS_URL = `wss://${window.location.host}/api/ws/ai/chat`
```

注意使用`wss://`协议（WebSocket over TLS）

---

## 📚 参考资料

- [WebSocket API - MDN](https://developer.mozilla.org/zh-CN/docs/Web/API/WebSocket)
- [Vue 3 文档](https://cn.vuejs.org/)
- [Sprint 8 后端开发文档](./Sprint8-WebSocket服务端开发文档.md)

---

**更新日期**: 2025-12-03  
**版本**: 1.0
