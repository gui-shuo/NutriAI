<script setup>
/**
 * 商家端 - 消息页面
 * 会话列表 + 聊天详情
 */
import { ref, computed, onMounted, nextTick } from 'vue'
import NavBar from '../../components/NavBar.vue'
import MerchantTabBar from '../../components/MerchantTabBar.vue'
import { merchantMessageApi } from '../../services/api'

const conversations = ref([])
const loading = ref(false)
const chatMode = ref(false)

// 聊天状态
const currentChat = ref(null)
const messages = ref([])
const inputText = ref('')
const chatLoading = ref(false)
const chatPage = ref(0)
const chatHasMore = ref(true)
const sending = ref(false)

async function fetchConversations() {
  loading.value = true
  try {
    const res = await merchantMessageApi.getConversations()
    conversations.value = res || []
  } catch (e) {
    console.error('获取会话失败', e)
  } finally {
    loading.value = false
  }
}

function openChat(conv) {
  currentChat.value = conv
  chatMode.value = true
  chatPage.value = 0
  messages.value = []
  chatHasMore.value = true
  fetchMessages()
  // 标记已读
  merchantMessageApi.markRead({ userId: conv.userId }).catch(() => {})
}

function closeChat() {
  chatMode.value = false
  currentChat.value = null
  fetchConversations()
}

async function fetchMessages() {
  if (chatLoading.value) return
  chatLoading.value = true
  try {
    const res = await merchantMessageApi.getHistory({
      userId: currentChat.value.userId,
      page: chatPage.value,
      size: 30,
    })
    if (res) {
      const list = (res.content || []).reverse()
      if (chatPage.value === 0) {
        messages.value = list
        await nextTick()
        scrollToBottom()
      } else {
        messages.value = [...list, ...messages.value]
      }
      chatHasMore.value = list.length >= 30
    }
  } catch (e) {
    console.error('获取消息失败', e)
  } finally {
    chatLoading.value = false
  }
}

async function sendMessage() {
  const text = inputText.value.trim()
  if (!text || sending.value) return
  sending.value = true
  try {
    const msg = await merchantMessageApi.send({
      userId: currentChat.value.userId,
      content: text,
    })
    if (msg) {
      messages.value.push(msg)
      inputText.value = ''
      await nextTick()
      scrollToBottom()
    }
  } catch (e) {
    uni.showToast({ title: e.message || '发送失败', icon: 'none' })
  } finally {
    sending.value = false
  }
}

function scrollToBottom() {
  // 使用scroll-into-view
}

function formatTime(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const now = new Date()
  const isToday = d.toDateString() === now.toDateString()
  const h = String(d.getHours()).padStart(2, '0')
  const m = String(d.getMinutes()).padStart(2, '0')
  if (isToday) return `${h}:${m}`
  return `${d.getMonth() + 1}/${d.getDate()} ${h}:${m}`
}

function loadMoreMessages() {
  if (!chatHasMore.value || chatLoading.value) return
  chatPage.value++
  fetchMessages()
}

onMounted(() => {
  fetchConversations()
})
</script>

<template>
  <view class="page">
    <!-- 会话列表模式 -->
    <template v-if="!chatMode">
      <NavBar>
        <template #center>
          <text class="nav-title">消息</text>
        </template>
      </NavBar>

      <scroll-view scroll-y class="conv-list" :enhanced="true" :show-scrollbar="false">
        <view v-if="loading" class="loading-wrap">
          <u-loading-icon />
        </view>

        <u-empty v-else-if="conversations.length === 0" text="暂无消息" mode="message" />

        <view
          v-for="conv in conversations"
          :key="conv.conversationId"
          class="conv-item"
          @tap="openChat(conv)"
        >
          <u-avatar icon="account" size="44" shape="circle" bgColor="#f0f0f0" />
          <view class="conv-item__info">
            <view class="conv-item__top">
              <text class="conv-item__name">用户 #{{ conv.userId }}</text>
              <text class="conv-item__time">{{ formatTime(conv.createdAt) }}</text>
            </view>
            <text class="conv-item__msg">{{ conv.content }}</text>
          </view>
          <view v-if="!conv.isRead && conv.senderType === 'USER'" class="conv-item__dot" />
        </view>

        <view style="height: 140rpx;" />
      </scroll-view>
    </template>

    <!-- 聊天模式 -->
    <template v-else>
      <NavBar showBack @click:left="closeChat">
        <template #left>
          <view class="nav-back" @tap="closeChat">
            <u-icon name="arrow-left" size="20" color="#1a1c1a" />
          </view>
        </template>
        <template #center>
          <text class="nav-title">用户 #{{ currentChat?.userId }}</text>
        </template>
      </NavBar>

      <!-- 消息列表 -->
      <scroll-view
        scroll-y
        class="chat-list"
        :enhanced="true"
        :show-scrollbar="false"
        @scrolltoupper="loadMoreMessages"
      >
        <view v-if="chatLoading && chatPage > 0" class="loading-wrap">
          <u-loading-icon size="20" />
        </view>

        <view
          v-for="msg in messages"
          :key="msg.id"
          class="msg-row"
          :class="{ 'msg-row--self': msg.senderType === 'MERCHANT' }"
        >
          <u-avatar
            v-if="msg.senderType === 'USER'"
            icon="account"
            size="36"
            shape="circle"
            bgColor="#f0f0f0"
          />
          <view class="msg-bubble" :class="{ 'msg-bubble--self': msg.senderType === 'MERCHANT' }">
            <text class="msg-bubble__text">{{ msg.content }}</text>
          </view>
          <u-avatar
            v-if="msg.senderType === 'MERCHANT'"
            icon="bag"
            size="36"
            shape="circle"
            bgColor="rgba(10, 110, 44, 0.1)"
          />
        </view>

        <view style="height: 20rpx;" />
      </scroll-view>

      <!-- 输入框 -->
      <view class="input-bar">
        <input
          class="input-bar__field"
          v-model="inputText"
          placeholder="输入消息..."
          confirm-type="send"
          @confirm="sendMessage"
        />
        <view class="input-bar__send" :class="{ 'input-bar__send--active': inputText.trim() }" @tap="sendMessage">
          <u-icon name="arrow-up" size="20" :color="inputText.trim() ? '#fff' : '#999'" />
        </view>
      </view>
    </template>

    <MerchantTabBar v-if="!chatMode" :current="1" />
  </view>
</template>

<style lang="scss" scoped>
@import '../../styles/design-system.scss';

.page {
  min-height: 100vh;
  background: #ffffff;
  overflow-x: hidden;
  width: 100%;
  display: flex;
  flex-direction: column;
}

.nav-title {
  font-size: 36rpx;
  font-weight: 800;
  color: $on-surface;
}

.nav-back {
  width: 64rpx;
  height: 64rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

// 会话列表
.conv-list {
  flex: 1;
  height: calc(100vh - 88px);
}

.conv-item {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 24rpx 32rpx;
  position: relative;

  & + & {
    border-top: 1rpx solid rgba(0, 0, 0, 0.04);
  }

  &__info {
    flex: 1;
    min-width: 0;
  }

  &__top {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 6rpx;
  }

  &__name {
    font-size: 28rpx;
    font-weight: 600;
    color: $on-surface;
  }

  &__time {
    font-size: 22rpx;
    color: $on-surface-variant;
  }

  &__msg {
    font-size: 26rpx;
    color: $on-surface-variant;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    display: block;
  }

  &__dot {
    width: 16rpx;
    height: 16rpx;
    border-radius: 50%;
    background: #ff4d4f;
    flex-shrink: 0;
  }
}

// 聊天
.chat-list {
  flex: 1;
  padding: 16rpx 24rpx;
  height: calc(100vh - 88px - 110rpx);
}

.msg-row {
  display: flex;
  align-items: flex-start;
  gap: 12rpx;
  margin-bottom: 20rpx;

  &--self {
    flex-direction: row-reverse;
  }
}

.msg-bubble {
  max-width: 65%;
  padding: 16rpx 24rpx;
  border-radius: 20rpx 20rpx 20rpx 4rpx;
  background: $surface-container-low;

  &--self {
    background: $primary;
    border-radius: 20rpx 20rpx 4rpx 20rpx;

    .msg-bubble__text {
      color: #ffffff;
    }
  }

  &__text {
    font-size: 28rpx;
    color: $on-surface;
    word-break: break-all;
    line-height: 1.5;
  }
}

// 输入栏
.input-bar {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 12rpx 24rpx;
  padding-bottom: calc(12rpx + env(safe-area-inset-bottom));
  background: #ffffff;
  border-top: 1rpx solid rgba(0, 0, 0, 0.06);

  &__field {
    flex: 1;
    height: 72rpx;
    background: $surface-container-low;
    border-radius: $radius-full;
    padding: 0 24rpx;
    font-size: 28rpx;
    color: $on-surface;
  }

  &__send {
    width: 72rpx;
    height: 72rpx;
    border-radius: 50%;
    background: $surface-container;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;

    &--active {
      background: $primary;
    }
  }
}

.loading-wrap {
  display: flex;
  justify-content: center;
  padding: 32rpx;
}
</style>
