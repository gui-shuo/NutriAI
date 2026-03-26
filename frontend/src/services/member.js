import api from './api'

/**
 * 会员服务API
 */

/** 获取会员信息 */
export const getMemberInfo = () => api.get('/member/info')

/** 获取会员权限概要（AI配额、VIP状态、功能权限等） */
export const getMemberPermissions = () => api.get('/member/permissions')

/**
 * 每日签到（返回获得成长值，0表示已签到）
 */
export const dailySignIn = () => api.post('/member/sign-in')

/**
 * 获取本月签到日期列表（日数字数组，如 [1,5,8]）
 */
export const getSignInCalendar = () => api.get('/member/sign-in/calendar')

/**
 * 获取成长值记录（分页）
 * @param {number} page
 * @param {number} size
 */
export const getGrowthRecords = (page = 0, size = 200) =>
  api.get('/member/growth-records', { params: { page, size } })

/** 生成邀请链接 */
export const generateInvitationLink = () => api.get('/member/invitation/generate')

/**
 * 查询邀请记录（分页）
 * @param {number} page
 * @param {number} size
 */
export const getInvitationRecords = (page = 0, size = 10) =>
  api.get('/member/invitation/records', { params: { page, size } })

// ========== VIP充值 ==========

/** 获取所有 VIP 套餐 */
export const getVipPlans = () => api.get('/vip/plans')

/**
 * 创建充值订单
 * @param {number} planId 套餐ID
 * @returns {{ orderNo, payUrl, expireTime, ... }}
 */
export const createVipOrder = planId => api.post('/vip/orders', { planId, paymentMethod: 'ALIPAY' })

/**
 * 查询订单支付状态（前端轮询）
 * @param {string} orderNo
 */
export const queryVipOrderStatus = orderNo => api.get(`/vip/orders/${orderNo}/status`)

/** 获取当前用户 VIP 状态 */
export const getVipStatus = () => api.get('/vip/status')

/**
 * 获取订单历史
 * @param {number} page
 * @param {number} size
 */
export const getVipOrderHistory = (page = 0, size = 10) =>
  api.get('/vip/orders', { params: { page, size } })

// ========== 常量 ==========

export const MemberLevels = {
  ROOKIE: { code: 'ROOKIE', name: '新手会员', color: '#95a5a6', order: 1 },
  BRONZE: { code: 'BRONZE', name: '青铜会员', color: '#cd7f32', order: 2 },
  SILVER: { code: 'SILVER', name: '白银会员', color: '#c0c0c0', order: 3 },
  GOLD: { code: 'GOLD', name: '黄金会员', color: '#ffd700', order: 4 },
  PLATINUM: { code: 'PLATINUM', name: '铂金会员', color: '#e5e4e2', order: 5 }
}

export const GrowthTypes = {
  SIGN_IN: { name: '每日签到', color: '#67c23a' },
  FOOD_RECORD: { name: '记录饮食', color: '#409eff' },
  AI_CHAT: { name: 'AI咨询', color: '#e6a23c' },
  INVITATION: { name: '邀请好友', color: '#f56c6c' },
  LEVEL_UP: { name: '等级提升', color: '#909399' },
  SYSTEM_REWARD: { name: '系统奖励', color: '#ff69b4' }
}
