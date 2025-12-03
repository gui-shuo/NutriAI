# Sprint 8 - WebSocket客户端开发文档

## 📋 概述

本文档说明如何在前端Vue 3项目中使用WebSocket客户端实现实时AI聊天功能。

**完成日期**: 2025-12-03  
**技术栈**: Vue 3 + Composition API + Element Plus

---

## ✅ 已完成的功能

### 1. WebSocket连接管理

**文件**: `src/services/websocket.js`

**核心功能**:
- ✅ WebSocket连接建立和关闭
- ✅ 连接状态管理（连接中、已连接、重连中、断开、错误）
- ✅ 自动断线重连（指数退避策略）
- ✅ 心跳检测（30秒间隔）
- ✅ 消息队列（连接建立前缓存）

**类**: `AIWebSocketClient`

**主要方法**:
```javascript
// 连接WebSocket
await connect(token)

// 发送消息
sendMessage(message, keepContext)

// 发送心跳
sendHeartbeat()

// 关闭连接
close(autoReconnect)

// 注册事件
on(eventName, handler)
```

### 2. 消息收发

**支持的消息类型**:

| 消息类型 | 方向 | 说明 |
|----------|------|------|
| `chat` | 客户端 → 服务端 | 发送聊天消息 |
| `ping` | 客户端 → 服务端 | 心跳检测 |
| `connection` | 服务端 → 客户端 | 连接确认 |
| `start` | 服务端 → 客户端 | AI开始响应 |
| `chunk` | 服务端 → 客户端 | AI响应片段 |
| `complete` | 服务端 → 客户端 | AI响应完成 |
| `pong` | 服务端 → 客户端 | 心跳响应 |
| `error` | 服务端 → 客户端 | 错误消息 |

**消息格式**:

```javascript
// 客户端发送聊天消息
{
  type: 'chat',
  message: '你好',
  keepContext: true
}

// 服务端响应chunk
{
  type: 'chunk',
  content: '你好！'
}
```

### 3. 断线重连

**重连策略**:
- 最大重连次数: 5次
- 重连延迟: 指数退避（1秒、2秒、4秒、8秒、16秒）
- 自动重连: 仅在非正常关闭时触发（code !== 1000）

**实现逻辑**:
```javascript
attemptReconnect(token) {
  if (this.reconnectAttempts >= this.maxReconnectAttempts) {
    // 达到最大次数，放弃重连
    return
  }
  
  this.reconnectAttempts++
  const delay = this.reconnectDelay * Math.pow(2, this.reconnectAttempts - 1)
  
  setTimeout(() => {
    this.connect(token)
  }, delay)
}
```

### 4. 打字效果实现

**文件**: `src/components/chat/TypingEffect.vue`

**功能**:
- ✅ 流式接收时实时显示（无延迟）
- ✅ 非流式时显示打字机效果
- ✅ 光标闪烁动画
- ✅ 可配置打字速度

**使用示例**:
```vue
<TypingEffect
  :text="message.content"
  :streaming="message.streaming"
  :speed="30"
  @complete="onTypingComplete"
/>
```

**参数说明**:
- `text`: 要显示的文本
- `streaming`: 是否正在流式接收
- `speed`: 打字速度（毫秒/字），默认30ms
- `@complete`: 打字完成事件

---

## 🏗️ 项目结构

```
frontend/src/
├── services/
│   └── websocket.js              # WebSocket客户端服务
├── composables/
│   └── useAIWebSocket.js         # WebSocket Composable Hook
├── components/chat/
│   ├── TypingEffect.vue          # 打字效果组件
│   ├── MessageList.vue           # 消息列表（需更新）
│   ├── ChatInput.vue             # 输入框
│   └── QuickActions.vue          # 快捷操作
└── views/
    ├── AIChatView.vue            # 原HTTP实现（待迁移）
    └── AIChatViewWebSocket.vue   # 新WebSocket实现
```

---

## 🚀 快速开始

### 1. 安装依赖

项目已包含所需依赖，无需额外安装。

### 2. 配置WebSocket URL

**文件**: `.env` 或 `.env.production`

```bash
# 开发环境
VITE_WS_URL=ws://localhost:8080/api/ws/ai/chat

# 生产环境
VITE_WS_URL=wss://your-domain.com/api/ws/ai/chat
```

### 3. 在组件中使用

```vue
<script setup>
import { useAIWebSocket } from '@/composables/useAIWebSocket'

// 使用WebSocket Hook
const {
  isConnected,
  isReceiving,
  currentResponse,
  connect,
  sendMessage,
  on
} = useAIWebSocket()

// 连接WebSocket
onMounted(async () => {
  await connect()
  
  // 注册事件处理器
  on('onStreamChunk', (message) => {
    console.log('收到chunk:', message.content)
  })
  
  on('onStreamComplete', (message) => {
    console.log('响应完成:', message.fullContent)
  })
})

// 发送消息
const handleSend = (text) => {
  sendMessage(text, true)
}
</script>
```

---

## 📚 API文档

### AIWebSocketClient

#### 属性

| 属性 | 类型 | 说明 |
|------|------|------|
| `ws` | WebSocket | WebSocket实例 |
| `status` | Ref<string> | 连接状态 |
| `isConnected` | Ref<boolean> | 是否已连接 |
| `isReceiving` | Ref<boolean> | 是否正在接收响应 |
| `currentResponse` | Ref<string> | 当前累积的响应内容 |

#### 方法

##### connect(token)

连接WebSocket

**参数**:
- `token` (string): JWT Token

**返回**: Promise<void>

**示例**:
```javascript
const token = localStorage.getItem('token')
await client.connect(token)
```

##### sendMessage(message, keepContext)

发送聊天消息

**参数**:
- `message` (string): 消息内容
- `keepContext` (boolean): 是否保持上下文，默认true

**示例**:
```javascript
client.sendMessage('你好', true)
```

##### sendHeartbeat()

发送心跳消息

**示例**:
```javascript
client.sendHeartbeat()
```

##### close(autoReconnect)

关闭连接

**参数**:
- `autoReconnect` (boolean): 是否允许自动重连，默认false

**示例**:
```javascript
client.close(false)
```

##### on(event, handler)

注册事件处理器

**事件列表**:
- `onOpen`: 连接打开
- `onClose`: 连接关闭
- `onError`: 连接错误
- `onMessage`: 接收到消息（通用）
- `onConnectionConfirm`: 连接确认
- `onStreamStart`: AI开始响应
- `onStreamChunk`: 接收响应片段
- `onStreamComplete`: 响应完成
- `onPong`: 心跳响应
- `onAIError`: AI错误

**示例**:
```javascript
client.on('onStreamChunk', (message) => {
  console.log('Chunk:', message.content)
})
```

---

### useAIWebSocket Hook

#### 返回值

| 属性/方法 | 类型 | 说明 |
|-----------|------|------|
| `isConnected` | Ref<boolean> | 是否已连接 |
| `isReceiving` | Ref<boolean> | 是否正在接收 |
| `status` | Ref<string> | 连接状态 |
| `currentResponse` | Ref<string> | 当前响应内容 |
| `currentMessageId` | Ref<string> | 当前消息ID |
| `connect()` | Function | 连接WebSocket |
| `disconnect()` | Function | 断开连接 |
| `sendMessage(msg, ctx)` | Function | 发送消息 |
| `sendHeartbeat()` | Function | 发送心跳 |
| `on(event, handler)` | Function | 注册事件 |
| `off(event)` | Function | 移除事件 |
| `isReady()` | Function | 检查是否就绪 |

**使用示例**:
```vue
<script setup>
import { useAIWebSocket } from '@/composables/useAIWebSocket'

const {
  isConnected,
  currentResponse,
  connect,
  sendMessage,
  on
} = useAIWebSocket()

onMounted(async () => {
  await connect()
  
  on('onStreamComplete', (msg) => {
    console.log('完成:', msg.fullContent)
  })
})
</script>
```

---

## 🔄 消息流程

### 完整对话流程

```
客户端                          服务端
  │                              │
  │──────connect()───────────────>│
  │                              │
  │<─────connection confirm──────│
  │                              │
  │──────chat message────────────>│
  │                              │
  │<─────start───────────────────│
  │<─────chunk 1─────────────────│
  │<─────chunk 2─────────────────│
  │<─────chunk 3─────────────────│
  │      ...                     │
  │<─────chunk N─────────────────│
  │<─────complete────────────────│
  │                              │
  │──────ping────────────────────>│
  │<─────pong────────────────────│
  │                              │
```

### 时序图

```
用户操作 → 发送消息
         ↓
    sendMessage()
         ↓
    WebSocket.send()
         ↓
    服务端接收
         ↓
    AI模型处理
         ↓
    流式输出chunk
         ↓
    onStreamChunk事件
         ↓
    currentResponse累积
         ↓
    界面实时显示
         ↓
    complete事件
         ↓
    保存到消息列表
```

---

## 🎨 状态管理

### 连接状态

```javascript
export const ConnectionStatus = {
  DISCONNECTED: 'disconnected',   // 未连接
  CONNECTING: 'connecting',       // 连接中
  CONNECTED: 'connected',         // 已连接
  RECONNECTING: 'reconnecting',   // 重连中
  ERROR: 'error'                  // 错误
}
```

### 状态转换

```
DISCONNECTED
    ↓ connect()
CONNECTING
    ↓ onopen
CONNECTED
    ↓ onclose (code !== 1000)
RECONNECTING
    ↓ 重连成功
CONNECTED
    ↓ 重连失败
ERROR
```

---

## 🧪 测试方法

### 1. 基础连接测试

```vue
<script setup>
import { useAIWebSocket } from '@/composables/useAIWebSocket'

const { connect, isConnected } = useAIWebSocket()

onMounted(async () => {
  await connect()
  console.log('连接状态:', isConnected.value)
})
</script>
```

### 2. 消息收发测试

```javascript
// 发送测试消息
sendMessage('你好')

// 监听响应
on('onStreamChunk', (msg) => {
  console.log('Chunk:', msg.content)
})

on('onStreamComplete', (msg) => {
  console.log('完整内容:', msg.fullContent)
})
```

### 3. 断线重连测试

```javascript
// 方法1: 手动关闭连接
client.close(true)  // 允许自动重连

// 方法2: 模拟网络断开
// 关闭后端服务，观察前端重连行为
```

### 4. 心跳测试

```javascript
// 查看控制台日志
// 每30秒应该看到：
// "💓 心跳已发送"
// "📨 [pong] ..."
```

---

## ⚠️ 注意事项

### 1. Token管理

**重要**: 确保Token有效且未过期

```javascript
// 检查Token
const token = localStorage.getItem('token')
if (!token) {
  ElMessage.error('请先登录')
  return
}

// 连接时使用
await connect()
```

### 2. 内存泄漏

**注意**: WebSocket是全局单例，组件卸载时不要断开连接

```javascript
// ❌ 错误
onUnmounted(() => {
  disconnect()  // 不要这样做
})

// ✅ 正确
// 让WebSocket保持连接，只在应用退出时关闭
```

### 3. 消息队列

**说明**: 连接建立前的消息会自动缓存

```javascript
// 即使未连接，也可以发送消息
sendMessage('你好')  // 消息会加入队列

// 连接建立后自动发送
await connect()  // 队列中的消息会自动发送
```

### 4. 事件清理

**建议**: 组件卸载时清理事件处理器

```javascript
onUnmounted(() => {
  off('onStreamChunk')
  off('onStreamComplete')
})
```

---

## 🐛 故障排查

### 问题1: 连接失败（1006错误）

**原因**: 
- Token无效或过期
- 后端未启动
- URL路径错误

**解决**:
```javascript
// 检查Token
const token = localStorage.getItem('token')
console.log('Token:', token ? '存在' : '不存在')

// 检查URL
console.log('WebSocket URL:', WS_BASE_URL)
// 应该是: ws://localhost:8080/api/ws/ai/chat
```

### 问题2: 无法接收消息

**检查**:
1. 是否注册了事件处理器
2. 事件名称是否正确
3. 检查浏览器Console日志

```javascript
// 调试代码
on('onMessage', (msg) => {
  console.log('收到任意消息:', msg)
})
```

### 问题3: 重连失败

**原因**: 达到最大重连次数

**解决**: 刷新页面或手动重连

```javascript
// 手动重连
await connect()
```

### 问题4: 打字效果卡顿

**原因**: 打字速度过慢

**解决**: 调整打字速度参数

```vue
<TypingEffect
  :text="content"
  :speed="10"  <!-- 减小数值加快速度 -->
/>
```

---

## 📈 性能优化建议

### 1. 消息批量处理

**建议**: 累积多个chunk后一次性更新UI

```javascript
let chunkBuffer = []
let updateTimer = null

on('onStreamChunk', (msg) => {
  chunkBuffer.push(msg.content)
  
  if (!updateTimer) {
    updateTimer = setTimeout(() => {
      currentResponse.value += chunkBuffer.join('')
      chunkBuffer = []
      updateTimer = null
    }, 50)  // 50ms批量更新一次
  }
})
```

### 2. 减少重渲染

**建议**: 使用`v-memo`或`v-once`优化列表渲染

```vue
<div
  v-for="message in messages"
  :key="message.id"
  v-memo="[message.content, message.timestamp]"
>
  <!-- 消息内容 -->
</div>
```

### 3. 虚拟滚动

**建议**: 消息列表很长时使用虚拟滚动

```bash
npm install vue-virtual-scroller
```

---

## 📖 参考资料

- [WebSocket API - MDN](https://developer.mozilla.org/zh-CN/docs/Web/API/WebSocket)
- [Vue 3 Composition API](https://cn.vuejs.org/guide/extras/composition-api-faq.html)
- [Sprint 8 后端WebSocket文档](./Sprint8-WebSocket服务端开发文档.md)
- [Sprint 8 测试报告](./Sprint8-WebSocket服务端测试报告.md)

---

## ✅ 下一步任务

### Sprint 8剩余工作

- [ ] 更新MessageList组件以支持流式显示
- [ ] 迁移AIChatView使用WebSocket
- [ ] 实现历史记录查询
- [ ] 实现消息收藏
- [ ] 实现复制/导出功能
- [ ] 实现文件上传

### 测试计划

- [ ] 单元测试（WebSocket客户端）
- [ ] 集成测试（端到端对话）
- [ ] 性能测试（大量消息）
- [ ] 压力测试（并发连接）

---

**文档版本**: 1.0  
**最后更新**: 2025-12-03 21:00  
**作者**: NutriAI Team
