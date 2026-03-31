import TencentCloudChat from '@tencentcloud/chat'

/**
 * 腾讯云即时通信IM服务 (UniApp版本)
 * 用于H5和APP端的营养师咨询实时消息
 */

let chat: any = null
let currentUserId: string | null = null
let messageCallback: ((msg: any) => void) | null = null
let readyResolve: (() => void) | null = null
let readyPromise: Promise<void> | null = null

/** 初始化TIM SDK */
export function initTim(sdkAppId: number) {
  if (chat) return chat

  chat = TencentCloudChat.create({ SDKAppID: sdkAppId })
  chat.setLogLevel(1)

  readyPromise = new Promise<void>(resolve => {
    readyResolve = resolve
  })

  chat.on(TencentCloudChat.EVENT.SDK_READY, () => {
    console.log('[TIM] SDK就绪')
    readyResolve?.()
  })

  chat.on(TencentCloudChat.EVENT.MESSAGE_RECEIVED, (event: any) => {
    const messages = event.data
    if (messageCallback) {
      messages.forEach((msg: any) => {
        try { messageCallback!(msg) } catch (e) { console.error('[TIM] 消息回调异常:', e) }
      })
    }
  })

  return chat
}

/** 登录TIM */
export async function loginTim(userId: string, userSig: string) {
  if (!chat) throw new Error('TIM未初始化')

  const res = await chat.login({ userID: userId, userSig })
  currentUserId = userId
  console.log('[TIM] 登录成功:', userId)
  await readyPromise
  return res
}

/** 登出TIM */
export async function logoutTim() {
  if (!chat) return
  try { await chat.logout() } catch (e) { /* ignore */ }
  currentUserId = null
  messageCallback = null
}

/** 销毁TIM实例 */
export function destroyTim() {
  if (chat) {
    chat.destroy()
    chat = null
    currentUserId = null
    messageCallback = null
    readyPromise = null
  }
}

/** 注册消息接收回调 */
export function onMessageReceived(callback: (msg: any) => void) {
  messageCallback = callback
}

/** 移除消息接收回调 */
export function offMessageReceived() {
  messageCallback = null
}

/** 从TIM消息提取orderNo */
export function getOrderNoFromMessage(msg: any): string | null {
  try {
    if (msg.cloudCustomData) {
      const data = JSON.parse(msg.cloudCustomData)
      return data.orderNo || null
    }
  } catch { /* ignore */ }
  return null
}

/** 从TIM消息提取文本 */
export function getTextFromMessage(msg: any): string {
  if (msg.type === TencentCloudChat.TYPES.MSG_TEXT) {
    return msg.payload?.text || ''
  }
  return '[不支持的消息类型]'
}

/** 检查TIM是否已登录 */
export function isTimLoggedIn(): boolean {
  return !!currentUserId && !!chat
}
