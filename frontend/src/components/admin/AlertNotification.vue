<template>
  <div class="alert-notification">
    <!-- 告警图标 -->
    <el-badge
      :value="unreadCount"
      :hidden="unreadCount === 0"
      class="alert-badge"
    >
      <el-button
        :icon="Bell"
        circle
        :type="unreadCount > 0 ? 'danger' : 'default'"
        @click="drawerVisible = true"
      />
    </el-badge>

    <!-- 告警抽屉 -->
    <el-drawer
      v-model="drawerVisible"
      title="系统告警"
      direction="rtl"
      size="400px"
    >
      <div class="alert-list">
        <el-empty
          v-if="alerts.length === 0"
          description="暂无告警"
        />

        <div
          v-for="alert in alerts"
          :key="alert.id"
          class="alert-item"
          :class="{ unread: !alert.read }"
        >
          <div class="alert-header">
            <el-tag
              :type="
                alert.level === 'error' ? 'danger' : alert.level === 'warning' ? 'warning' : 'info'
              "
              size="small"
            >
              {{ levelMap[alert.level] }}
            </el-tag>
            <span class="alert-time">{{ formatTime(alert.time) }}</span>
          </div>
          <div class="alert-title">
            {{ alert.title }}
          </div>
          <div class="alert-content">
            {{ alert.content }}
          </div>
          <div class="alert-actions">
            <el-button
              v-if="!alert.read"
              type="primary"
              link
              size="small"
              @click="markAsRead(alert.id)"
            >
              标记已读
            </el-button>
            <el-button
              type="danger"
              link
              size="small"
              @click="deleteAlert(alert.id)"
            >
              删除
            </el-button>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button
          :disabled="alerts.length === 0"
          @click="clearAll"
        >
          清空所有
        </el-button>
        <el-button
          type="primary"
          :disabled="unreadCount === 0"
          @click="markAllAsRead"
        >
          全部已读
        </el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElNotification } from 'element-plus'
import { Bell } from '@element-plus/icons-vue'

const drawerVisible = ref(false)
const alerts = ref([])
const ws = ref(null)
const reconnectAttempts = ref(0)
const maxReconnectAttempts = 3
const isConnecting = ref(false)

const levelMap = {
  info: '信息',
  warning: '警告',
  error: '错误'
}

const unreadCount = computed(() => {
  return alerts.value.filter(alert => !alert.read).length
})

// 连接WebSocket
const connectWebSocket = () => {
  if (
    ws.value &&
    (ws.value.readyState === WebSocket.CONNECTING || ws.value.readyState === WebSocket.OPEN)
  ) {
    console.log('⚠️ WebSocket已连接或正在连接中，跳过重复连接')
    return
  }

  if (reconnectAttempts.value >= maxReconnectAttempts) {
    console.log('❌ 已达到最大重连次数，停止重连')
    return
  }

  if (isConnecting.value) {
    console.log('⚠️ 正在连接中，跳过重复连接')
    return
  }

  const token = localStorage.getItem('token')
  if (!token) {
    console.error('❌ 未找到token，无法建立WebSocket连接')
    return
  }

  try {
    isConnecting.value = true
    // 注意：WebSocket URL需要包含 /api 前缀（与后端context-path一致）
    const wsUrl = `ws://localhost:8080/api/ws/admin/alerts?token=${token}`
    console.log('🔌 正在连接WebSocket:', wsUrl)

    ws.value = new WebSocket(wsUrl)

    ws.value.onopen = () => {
      console.log('✅ WebSocket连接成功')
      isConnecting.value = false
      reconnectAttempts.value = 0 // 重置重连计数
    }

    ws.value.onmessage = event => {
      try {
        const alert = JSON.parse(event.data)
        handleNewAlert(alert)
      } catch (error) {
        console.error('解析告警消息失败:', error)
      }
    }

    ws.value.onerror = () => {
      console.error('❌ WebSocket连接错误')
      isConnecting.value = false
    }

    ws.value.onclose = event => {
      console.log('🔌 WebSocket连接关闭', event.code)
      isConnecting.value = false

      // 只在非正常关闭时重连
      if (event.code !== 1000 && reconnectAttempts.value < maxReconnectAttempts) {
        reconnectAttempts.value++
        console.log(`⏳ 将在5秒后进行第${reconnectAttempts.value}次重连...`)
        setTimeout(() => {
          connectWebSocket()
        }, 5000)
      }
    }
  } catch (error) {
    console.error('WebSocket连接失败:', error)
    isConnecting.value = false
  }
}

// 处理新告警
const handleNewAlert = alert => {
  const newAlert = {
    id: Date.now(),
    ...alert,
    read: false,
    time: new Date()
  }

  alerts.value.unshift(newAlert)

  // 显示通知
  ElNotification({
    title: alert.title,
    message: alert.content,
    type: alert.level === 'error' ? 'error' : alert.level === 'warning' ? 'warning' : 'info',
    duration: 5000,
    position: 'top-right'
  })

  // 播放提示音（可选）
  playNotificationSound()
}

// 播放提示音（可选，需要音频文件）
const playNotificationSound = () => {
  // 暂时禁用音频提示，避免404错误
  // 如需启用，请在public目录下添加notification.mp3文件
  /*
  try {
    const audio = new Audio('/notification.mp3')
    audio.volume = 0.3
    audio.play().catch(() => {
      // 忽略播放失败
    })
  } catch (error) {
    // 忽略错误
  }
  */
}

// 标记为已读
const markAsRead = id => {
  const alert = alerts.value.find(a => a.id === id)
  if (alert) {
    alert.read = true
  }
}

// 全部标记为已读
const markAllAsRead = () => {
  alerts.value.forEach(alert => {
    alert.read = true
  })
  ElMessage.success('已全部标记为已读')
}

// 删除告警
const deleteAlert = id => {
  const index = alerts.value.findIndex(a => a.id === id)
  if (index !== -1) {
    alerts.value.splice(index, 1)
  }
}

// 清空所有
const clearAll = () => {
  alerts.value = []
  ElMessage.success('已清空所有告警')
}

// 格式化时间
const formatTime = time => {
  const now = new Date()
  const alertTime = new Date(time)
  const diff = now - alertTime

  if (diff < 60000) {
    return '刚刚'
  } else if (diff < 3600000) {
    return `${Math.floor(diff / 60000)}分钟前`
  } else if (diff < 86400000) {
    return `${Math.floor(diff / 3600000)}小时前`
  } else {
    return alertTime.toLocaleString('zh-CN')
  }
}

// 模拟告警（用于测试）
const simulateAlert = () => {
  const types = ['info', 'warning', 'error']
  const titles = ['系统通知', '性能警告', '错误告警']
  const contents = ['系统运行正常', 'CPU使用率超过80%', '数据库连接失败']

  const randomType = types[Math.floor(Math.random() * types.length)]
  const randomIndex = types.indexOf(randomType)

  handleNewAlert({
    level: randomType,
    title: titles[randomIndex],
    content: contents[randomIndex]
  })
}

// 使用全局标志避免重复连接
const isGlobalConnected = ref(false)
const globalConnectionKey = 'ws_admin_alerts_connected'

onMounted(() => {
  // 检查是否已经有全局连接
  const alreadyConnected = sessionStorage.getItem(globalConnectionKey)

  if (!alreadyConnected) {
    // 延迟连接WebSocket，避免路由切换时重复连接
    setTimeout(() => {
      connectWebSocket()
      sessionStorage.setItem(globalConnectionKey, 'true')
      isGlobalConnected.value = true
    }, 1000)
  } else {
    console.log('⚠️ WebSocket已在其他组件实例中连接，跳过重复连接')
  }

  // 开发环境下，每60秒模拟一个告警（降低频率）
  if (import.meta.env.DEV && !alreadyConnected) {
    const intervalId = setInterval(simulateAlert, 60000)
    // 保存intervalId以便清理
    onUnmounted(() => {
      clearInterval(intervalId)
    })
  }
})

onUnmounted(() => {
  // 只在真正离开管理后台时关闭连接
  // 路由切换时不关闭
  const isLeavingAdmin = !window.location.pathname.startsWith('/admin')

  if (isLeavingAdmin && ws.value) {
    console.log('🔌 离开管理后台，关闭WebSocket连接')
    ws.value.close(1000, 'Leaving admin') // 正常关闭
    ws.value = null
    sessionStorage.removeItem(globalConnectionKey)
    reconnectAttempts.value = maxReconnectAttempts // 防止重连
  } else {
    console.log('⚠️ 路由切换，保持WebSocket连接')
  }
})
</script>

<style scoped>
.alert-notification {
  display: inline-block;
}

.alert-badge {
  cursor: pointer;
}

.alert-list {
  height: calc(100vh - 120px);
  overflow-y: auto;
}

.alert-item {
  padding: 16px;
  border-bottom: 1px solid #f0f0f0;
  transition: background-color 0.3s;
}

.alert-item:hover {
  background-color: #f5f5f5;
}

.alert-item.unread {
  background-color: #e6f7ff;
}

.alert-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.alert-time {
  font-size: 12px;
  color: #999;
}

.alert-title {
  font-size: 14px;
  font-weight: 500;
  color: #262626;
  margin-bottom: 4px;
}

.alert-content {
  font-size: 13px;
  color: #666;
  margin-bottom: 8px;
  line-height: 1.5;
}

.alert-actions {
  display: flex;
  gap: 8px;
}
</style>
