# Sprint 8 - 完整功能使用指南

## 📋 功能概览

**文件**: `src/views/AIChatViewIntegrated.vue`

本文档说明Sprint 8已实现的所有功能及其使用方法。

---

## ✅ 已实现的功能

### 1. WebSocket实时通信 ✅

**功能**:
- 实时连接状态显示
- 自动断线重连
- 流式AI响应
- 心跳检测

**使用方法**:
- 页面自动连接WebSocket
- 顶部显示连接状态（已连接/连接中/断开）
- 连接断开时自动尝试重连（最多5次）

**状态指示器**:
```vue
<el-tag :type="statusTagType">
  {{ statusText }}
</el-tag>
```

- 🟢 **已连接**: 绿色标签
- 🟡 **连接中/重连中**: 黄色标签
- 🔴 **连接错误**: 红色标签

---

### 2. 历史记录查询 ✅

**功能**:
- 自动保存对话到本地
- 查看历史对话列表
- 加载历史对话
- 删除历史记录

**使用方法**:

#### 2.1 自动保存

对话自动保存到 `localStorage`，包括：
- 当前对话（实时保存）
- 历史对话列表（清空或关闭时保存）

**保存逻辑**:
```javascript
// 实时保存当前对话
const saveToLocalStorage = () => {
  if (settings.autoSave && messages.value.length > 0) {
    localStorage.setItem('currentChat', JSON.stringify(messages.value))
  }
}

// 保存到历史记录
const saveCurrentConversation = () => {
  const conversation = {
    id: `conv_${Date.now()}`,
    title: generateConversationTitle(firstUserMessage?.content),
    preview: firstUserMessage?.content?.substring(0, 50),
    messages: messages.value,
    timestamp: Date.now()
  }
  
  const history = JSON.parse(localStorage.getItem('chatHistory') || '[]')
  history.unshift(conversation)
  
  // 最多保存50条
  if (history.length > 50) {
    history.length = 50
  }
  
  localStorage.setItem('chatHistory', JSON.stringify(history))
}
```

#### 2.2 查看历史记录

**步骤**:
1. 点击顶部工具栏 📁 按钮
2. 打开历史记录对话框
3. 查看所有历史对话

**界面**:
```vue
<el-dialog v-model="showHistory" title="历史记录">
  <div class="history-list">
    <div v-for="item in historyList" :key="item.id" class="history-item">
      <div class="history-header">
        <span class="history-title">{{ item.title }}</span>
        <span class="history-time">{{ formatTime(item.timestamp) }}</span>
      </div>
      <div class="history-preview">{{ item.preview }}</div>
    </div>
  </div>
</el-dialog>
```

#### 2.3 加载历史对话

**步骤**:
1. 在历史记录列表中点击任意对话
2. 自动加载该对话的所有消息
3. 对话框关闭

**代码**:
```javascript
const loadHistoryConversation = (item) => {
  messages.value = JSON.parse(JSON.stringify(item.messages))
  showHistory.value = false
  ElMessage.success('历史记录已加载')
}
```

#### 2.4 删除历史记录

**步骤**:
1. 点击历史记录项的"删除"按钮
2. 确认删除
3. 记录从列表中移除

**代码**:
```javascript
const deleteHistory = async (id) => {
  await ElMessageBox.confirm('确定要删除这条历史记录吗？', '确认删除')
  
  const history = JSON.parse(localStorage.getItem('chatHistory') || '[]')
  const index = history.findIndex(h => h.id === id)
  if (index > -1) {
    history.splice(index, 1)
    localStorage.setItem('chatHistory', JSON.stringify(history))
    loadHistoryList()
    ElMessage.success('已删除')
  }
}
```

---

### 3. 消息收藏 ✅

**功能**:
- 收藏AI回复
- 查看收藏列表
- 取消收藏
- 复制收藏内容

**使用方法**:

#### 3.1 收藏消息

**步骤**:
1. 在AI消息下方找到 ⭐ 按钮
2. 点击收藏
3. 按钮变为实心星 ⭐ (已收藏)

**代码**:
```javascript
const handleFavorite = (message) => {
  const index = messages.value.findIndex(m => m.id === message.id)
  if (index > -1) {
    messages.value[index].favorite = true
    ElMessage.success('已收藏')
    saveToLocalStorage()
  }
}
```

#### 3.2 查看收藏列表

**步骤**:
1. 点击顶部工具栏 ⭐ 按钮
2. 打开收藏对话框
3. 查看所有收藏的消息

**界面**:
```vue
<el-dialog v-model="showFavorites" title="我的收藏">
  <div class="favorites-list">
    <div v-for="msg in favoriteMessages" :key="msg.id" class="favorite-item">
      <div class="favorite-content" v-html="renderMarkdown(msg.content)"></div>
      <div class="favorite-footer">
        <span class="favorite-time">{{ formatTime(msg.timestamp) }}</span>
        <div class="favorite-actions">
          <el-button @click="copyMessage(msg.content)">复制</el-button>
          <el-button type="danger" @click="handleUnfavorite(msg)">取消收藏</el-button>
        </div>
      </div>
    </div>
  </div>
</el-dialog>
```

#### 3.3 取消收藏

**步骤**:
1. 在消息列表中点击实心星按钮
2. 或在收藏列表中点击"取消收藏"
3. 消息从收藏中移除

**代码**:
```javascript
const handleUnfavorite = (message) => {
  const index = messages.value.findIndex(m => m.id === message.id)
  if (index > -1) {
    messages.value[index].favorite = false
    ElMessage.success('已取消收藏')
    saveToLocalStorage()
  }
}
```

#### 3.4 收藏列表过滤

**实现**:
```javascript
const favoriteMessages = computed(() => {
  return messages.value.filter(m => m.favorite && m.role === 'assistant')
})
```

---

### 4. 复制/导出功能 ✅

**功能**:
- 复制单条消息
- 导出完整对话（Markdown格式）
- 包含时间戳和收藏标记

**使用方法**:

#### 4.1 复制单条消息

**步骤**:
1. 找到消息下方的 📄 复制按钮
2. 点击复制
3. 内容已复制到剪贴板

**代码**:
```javascript
const copyMessage = (content) => {
  navigator.clipboard.writeText(content).then(() => {
    ElMessage.success('已复制到剪贴板')
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}
```

#### 4.2 导出完整对话

**步骤**:
1. 点击顶部工具栏 ⬇️ 导出按钮
2. 自动下载Markdown文件
3. 文件名: `AI营养师对话_时间戳.md`

**导出格式**:
```markdown
# AI营养师对话记录

导出时间：2025-12-03 21:00:00

---

### 👤 用户 - 2025-12-03 20:30:00

你好，请介绍一下苹果的营养成分

---

### 🤖 AI营养师 - 2025-12-03 20:30:02

## 苹果的营养成分分析

**基本信息**（每100g）：
- 🔥 能量：53 kcal
- 🥤 水分：85%
...

⭐ 已收藏

---
```

**代码**:
```javascript
const handleExport = () => {
  if (messages.value.length === 0) {
    ElMessage.info('当前没有对话记录可导出')
    return
  }

  let exportContent = '# AI营养师对话记录\n\n'
  exportContent += `导出时间：${new Date().toLocaleString('zh-CN')}\n\n---\n\n`

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
}
```

---

### 5. 文件上传 ✅

**功能**:
- 支持TXT和PDF文件
- 文件大小限制5MB
- 自动读取文件内容
- 将内容发送给AI分析

**使用方法**:

#### 5.1 上传文件

**步骤**:
1. 点击输入框的 📎 按钮
2. 选择TXT或PDF文件
3. 可选：输入附加说明
4. 发送

**支持的文件类型**:
- ✅ `.txt` - 文本文件
- ✅ `.pdf` - PDF文档

**限制**:
- 最大文件大小: 5MB
- 内容截断: 2000字符（避免消息过长）

#### 5.2 文件处理流程

```javascript
const handleFileUpload = async (file, message) => {
  // 1. 检查文件类型
  const allowedTypes = ['text/plain', 'application/pdf']
  if (!allowedTypes.includes(file.type)) {
    ElMessage.error('仅支持TXT和PDF文件')
    return
  }

  // 2. 检查文件大小
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('文件大小不能超过5MB')
    return
  }

  // 3. 读取文件内容
  const fileContent = await readFileContent(file)
  
  // 4. 构建消息
  const fullMessage = message 
    ? `${message}\n\n【文件内容】：\n${fileContent.substring(0, 2000)}`
    : `请帮我分析这个文件的内容：\n${fileContent.substring(0, 2000)}`

  // 5. 发送到AI
  sendMessage(fullMessage, settings.keepContext)
}
```

#### 5.3 文件读取

**代码**:
```javascript
const readFileContent = (file) => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    
    reader.onload = (e) => {
      resolve(e.target.result)
    }
    
    reader.onerror = reject
    
    // TXT文件直接读取
    // PDF文件简化处理（实际可使用pdf.js）
    reader.readAsText(file)
  })
}
```

#### 5.4 使用示例

**场景1: 分析营养成分表**
```
上传：nutrition.txt
内容：苹果 100g 含...
AI分析：这份营养成分表显示...
```

**场景2: 分析体检报告**
```
上传：report.pdf
附加说明：请帮我分析这份体检报告
AI分析：根据您的体检报告...
```

---

## 🎨 界面功能

### 顶部工具栏

| 按钮 | 图标 | 功能 | 快捷键 |
|------|------|------|--------|
| 历史记录 | 📁 | 查看历史对话 | Ctrl+H |
| 收藏 | ⭐ | 查看收藏列表 | - |
| 清空 | 🗑️ | 清空当前对话 | Ctrl+K |
| 导出 | ⬇️ | 导出为Markdown | Ctrl+E |
| 设置 | ⚙️ | 打开设置对话框 | - |

### 消息操作按钮

**用户消息**:
- 无操作按钮

**AI消息**:
- 📄 **复制**: 复制消息内容
- 🔄 **重新生成**: 重新生成回答
- ⭐ **收藏**: 收藏/取消收藏

---

## ⚙️ 设置选项

### 连接设置

- **连接状态**: 显示当前WebSocket连接状态
- **重新连接**: 手动触发重连

### 对话设置

- **上下文记忆**: 是否保持对话上下文（默认开启）
- **自动保存**: 是否自动保存聊天记录（默认开启）

**配置代码**:
```javascript
const settings = reactive({
  keepContext: true,
  autoSave: true
})
```

---

## 💾 数据存储

### localStorage结构

**当前对话**:
```javascript
localStorage.setItem('currentChat', JSON.stringify(messages.value))
```

**历史记录**:
```javascript
{
  "chatHistory": [
    {
      "id": "conv_1733234567890",
      "title": "询问苹果营养成分",
      "preview": "你好，请介绍一下苹果的营养成分",
      "messages": [...],
      "timestamp": 1733234567890
    }
  ]
}
```

**设置**:
```javascript
{
  "aiChatSettings": {
    "keepContext": true,
    "autoSave": true
  }
}
```

### 数据持久化

**自动保存时机**:
1. 消息变化时（watch监听）
2. 清空对话时
3. 组件卸载时

**手动保存时机**:
1. 点击保存按钮（设置对话框）
2. 清空当前对话时

---

## 🔄 状态管理

### 连接状态

```javascript
const status = ref(ConnectionStatus.DISCONNECTED)

// 状态枚举
ConnectionStatus = {
  DISCONNECTED: 'disconnected',
  CONNECTING: 'connecting',
  CONNECTED: 'connected',
  RECONNECTING: 'reconnecting',
  ERROR: 'error'
}
```

### 消息状态

```javascript
const message = {
  id: 'msg_xxx',
  role: 'user' | 'assistant',
  content: '消息内容',
  timestamp: 1733234567890,
  favorite: false,          // 是否收藏
  streaming: false,         // 是否正在流式接收
  loading: false           // 是否加载中
}
```

---

## 📱 响应式设计

### 桌面端

- 完整功能展示
- 多按钮工具栏
- 大号对话框

### 移动端

- 简化工具栏
- 小号按钮
- 适配对话框大小

**媒体查询**:
```css
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
```

---

## 🎯 使用流程示例

### 完整对话流程

1. **打开页面**
   - 自动连接WebSocket
   - 加载上次对话（如果有）
   - 显示连接状态

2. **开始对话**
   - 输入消息或点击快捷按钮
   - 发送消息
   - 实时接收AI响应

3. **管理消息**
   - 收藏重要回答
   - 重新生成不满意的回答
   - 复制需要的内容

4. **保存记录**
   - 自动保存到本地
   - 手动导出为Markdown
   - 清空时保存到历史记录

5. **查看历史**
   - 打开历史记录对话框
   - 选择历史对话加载
   - 继续之前的话题

---

## ⌨️ 键盘快捷键

| 快捷键 | 功能 |
|--------|------|
| `Ctrl + K` | 清空当前对话 |
| `Ctrl + E` | 导出对话 |
| `Ctrl + H` | 打开历史记录 |
| `Enter` | 发送消息（输入框） |

**实现代码**:
```javascript
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
```

---

## 🔍 常见问题

### Q1: 历史记录最多保存多少条？

**A**: 最多保存50条历史记录，超过后自动删除最早的记录。

### Q2: 收藏的消息保存在哪里？

**A**: 收藏信息保存在消息的 `favorite` 属性中，随对话一起保存到 localStorage。

### Q3: 文件上传支持哪些格式？

**A**: 目前支持 TXT 和 PDF 文件，大小限制5MB。

### Q4: 导出的文件是什么格式？

**A**: Markdown格式（.md文件），可用任何Markdown编辑器打开。

### Q5: 清空对话会丢失数据吗？

**A**: 如果开启了"自动保存"，清空前会自动保存到历史记录。

---

## 🚀 下一步优化

### 建议功能

- [ ] 历史记录搜索
- [ ] 收藏分类/标签
- [ ] 批量导出历史记录
- [ ] PDF文件完整解析（使用pdf.js）
- [ ] 图片文件上传支持
- [ ] 对话分享（生成链接）
- [ ] 云端同步

---

## 📚 相关文档

- [Sprint8-WebSocket服务端开发文档.md](./Sprint8-WebSocket服务端开发文档.md)
- [Sprint8-WebSocket客户端开发文档.md](./Sprint8-WebSocket客户端开发文档.md)
- [Sprint8-前端集成指南.md](./Sprint8-前端集成指南.md)

---

**文档版本**: 1.0  
**最后更新**: 2025-12-03 21:10  
**作者**: NutriAI Team
