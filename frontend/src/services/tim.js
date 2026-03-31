import TencentCloudChat from '@tencentcloud/chat'

/**
 * 腾讯云即时通信IM服务
 * 封装TIM SDK初始化、登录、消息监听等功能
 * 仅用于营养师咨询系统的实时消息推送
 */

let chat = null
let currentUserId = null
let messageCallback = null
let readyPromise = null

/**
 * 初始化TIM SDK
 * @param {number} sdkAppId
 */
export function initTim(sdkAppId) {
  if (chat) return chat

  chat = TencentCloudChat.create({
    SDKAppID: sdkAppId
  })

  // 设置日志级别（生产环境关闭）
  chat.setLogLevel(1) // 0=详细 1=警告 2=错误 3=无

  // 监听SDK就绪
  readyPromise = new Promise(resolve => {
    chat.on(TencentCloudChat.EVENT.SDK_READY, () => {
      console.log('[TIM] SDK就绪')
      resolve()
    })
  })

  // 监听SDK未就绪
  chat.on(TencentCloudChat.EVENT.SDK_NOT_READY, () => {
    console.warn('[TIM] SDK未就绪')
  })

  // 监听被踢下线
  chat.on(TencentCloudChat.EVENT.KICKED_OUT, event => {
    console.warn('[TIM] 被踢下线:', event.data.type)
  })

  // 监听收到消息
  chat.on(TencentCloudChat.EVENT.MESSAGE_RECEIVED, event => {
    const messages = event.data
    if (messageCallback) {
      messages.forEach(msg => {
        try {
          messageCallback(msg)
        } catch (e) {
          console.error('[TIM] 消息回调异常:', e)
        }
      })
    }
  })

  return chat
}

/**
 * 登录TIM
 * @param {string} userId IM用户ID (如 user_123)
 * @param {string} userSig 后端生成的UserSig
 */
export async function loginTim(userId, userSig) {
  if (!chat) throw new Error('TIM未初始化，请先调用initTim')

  try {
    const res = await chat.login({ userID: userId, userSig })
    currentUserId = userId
    console.log('[TIM] 登录成功:', userId)

    // 等待SDK就绪
    await readyPromise
    return res
  } catch (e) {
    console.error('[TIM] 登录失败:', e)
    throw e
  }
}

/**
 * 登出TIM
 */
export async function logoutTim() {
  if (!chat) return
  try {
    await chat.logout()
    console.log('[TIM] 已登出')
  } catch (e) {
    console.warn('[TIM] 登出异常:', e)
  }
  currentUserId = null
  messageCallback = null
}

/**
 * 销毁TIM实例
 */
export function destroyTim() {
  if (chat) {
    chat.destroy()
    chat = null
    currentUserId = null
    messageCallback = null
    readyPromise = null
  }
}

/**
 * 注册消息接收回调
 * @param {function} callback 回调函数，参数为TIM消息对象
 */
export function onMessageReceived(callback) {
  messageCallback = callback
}

/**
 * 移除消息接收回调
 */
export function offMessageReceived() {
  messageCallback = null
}

/**
 * 发送C2C文本消息
 * @param {string} toUserId 对方IM用户ID
 * @param {string} text 消息文本
 * @param {string} orderNo 关联的咨询订单号
 */
export async function sendTextMessage(toUserId, text, orderNo) {
  if (!chat || !currentUserId) {
    throw new Error('TIM未登录')
  }

  const message = chat.createTextMessage({
    to: toUserId,
    conversationType: TencentCloudChat.TYPES.CONV_C2C,
    payload: { text },
    cloudCustomData: orderNo ? JSON.stringify({ orderNo }) : ''
  })

  try {
    const res = await chat.sendMessage(message)
    console.log('[TIM] 消息发送成功')
    return res
  } catch (e) {
    console.error('[TIM] 消息发送失败:', e)
    throw e
  }
}

/**
 * 获取TIM SDK实例
 */
export function getTimInstance() {
  return chat
}

/**
 * 获取当前登录用户ID
 */
export function getCurrentUserId() {
  return currentUserId
}

/**
 * 检查TIM是否已登录
 */
export function isTimLoggedIn() {
  return !!currentUserId && !!chat
}

/**
 * 从TIM消息对象中提取orderNo
 * @param {object} msg TIM消息对象
 * @returns {string|null}
 */
export function getOrderNoFromMessage(msg) {
  try {
    if (msg.cloudCustomData) {
      const data = JSON.parse(msg.cloudCustomData)
      return data.orderNo || null
    }
  } catch {
    // ignore
  }
  return null
}

/**
 * 从TIM消息对象中提取文本内容
 * @param {object} msg TIM消息对象
 * @returns {string}
 */
export function getTextFromMessage(msg) {
  if (msg.type === TencentCloudChat.TYPES.MSG_TEXT) {
    return msg.payload?.text || ''
  }
  return '[不支持的消息类型]'
}
