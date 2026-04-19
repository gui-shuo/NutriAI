/**
 * NutriAI API 服务层
 * 所有后端接口端点
 */
import { get, post, put, del, uploadFile } from '../utils/request'

// ==================== 认证 ====================
export const authApi = {
  login: (data) => post('/auth/login', data),
  merchantLogin: (data) => post('/auth/merchant-login', data),
  wxLogin: (data) => post('/auth/wx-login', data),
  register: (data) => post('/auth/register', data),
  refresh: (data) => post('/auth/refresh', data),
  logout: () => post('/auth/logout'),
  getCaptcha: () => get('/auth/captcha'),
  sendEmailCode: (data) => post('/auth/send-email-code', data),
  forgotPassword: (data) => post('/auth/forgot-password', data),
  resetPassword: (data) => post('/auth/reset-password', data),
}

// ==================== 用户 ====================
export const userApi = {
  getProfile: () => get('/user/profile'),
  updateProfile: (data) => put('/user/profile', data),
  changePassword: (data) => put('/user/password', data),
  uploadAvatar: (filePath) => uploadFile({ url: '/user/avatar', filePath }),
  getHealthProfile: () => get('/user/health-profile'),
  updateHealthProfile: (data) => put('/user/health-profile', data),
  getAddresses: () => get('/addresses'),
  addAddress: (data) => post('/addresses', data),
  updateAddress: (id, data) => put(`/addresses/${id}`, data),
  deleteAddress: (id) => del(`/addresses/${id}`),
  setDefaultAddress: (id) => put(`/addresses/${id}/default`),
}

// ==================== 会员 ====================
export const memberApi = {
  getPermissions: () => get('/member/permissions'),
  getInfo: () => get('/member/info'),
  getGrowthRecords: (params) => get('/member/growth-records', params),
  signIn: () => post('/member/sign-in'),
  getSignInCalendar: () => get('/member/sign-in/calendar'),
  generateInvitation: () => get('/member/invitation/generate'),
  getInvitationRecords: () => get('/member/invitation/records'),
}

// ==================== VIP ====================
export const vipApi = {
  getPlans: () => get('/vip/plans'),
  createOrder: (data) => post('/vip/orders', data),
  getStatus: () => get('/vip/status'),
  getOrders: () => get('/vip/orders'),
}

// ==================== 营养餐/餐品 ====================
export const mealApi = {
  getList: (params) => get('/meals', params),
  getDetail: (id) => get(`/meals/${id}`),
  getCategories: () => get('/meals/categories'),
  getRecommended: () => get('/meals/recommended'),
  search: (params) => get('/meals/search', params),
  // 营养餐订单
  createOrder: (data) => post('/meals/orders', data),
  getOrders: (params) => get('/meals/orders', params),
  getOrderDetail: (orderNo) => get(`/meals/orders/${orderNo}`),
  cancelOrder: (orderNo) => post(`/meals/orders/${orderNo}/cancel`),
}

// ==================== 商城/商品 ====================
export const productApi = {
  getList: (params) => get('/products', params),
  search: (params) => get('/products/search', params),
  getRecommended: () => get('/products/recommended'),
  getDetail: (id) => get(`/products/${id}`),
  getCategories: () => get('/products/categories'),
  createOrder: (data) => post('/products/orders', data),
  getOrders: (params) => get('/products/orders', params),
  getOrderDetail: (orderNo) => get(`/products/orders/${orderNo}`),
  cancelOrder: (orderNo) => post(`/products/orders/${orderNo}/cancel`),
  confirmReceive: (orderNo) => post(`/products/orders/${orderNo}/confirm-receive`),
}

// ==================== 购物车 ====================
export const cartApi = {
  getCart: () => get('/cart'),
  getCount: () => get('/cart/count'),
  addItem: (data) => post('/cart/add', data),
  updateQuantity: (id, quantity) => put(`/cart/${id}/quantity`, { quantity }),
  toggleSelect: (id, selected) => put(`/cart/${id}/select`, { selected }),
  selectAll: (selected) => put('/cart/select-all', { selected }),
  removeItem: (id) => del(`/cart/${id}`),
  batchDelete: (ids) => del('/cart/batch', { ids }),
  clearCart: () => del('/cart/clear'),
}

// ==================== 优惠券 ====================
export const couponApi = {
  getMyCoupons: (status) => get('/coupons/my', status ? { status } : {}),
  claimCoupon: (couponId) => post(`/coupons/${couponId}/claim`),
  claimByCode: (code) => post('/coupons/claim-by-code', { code }),
  getAvailable: (type) => get('/coupons/available', { type: type || 'ALL' }),
}

// ==================== 通知/公告 ====================
export const announcementApi = {
  getList: (params) => get('/announcements', params),
  getUnreadCount: () => get('/announcements/unread-count'),
  markRead: (id) => post(`/announcements/${id}/read`),
  markAllRead: () => post('/announcements/read-all'),
}

// ==================== 反馈 ====================
export const feedbackApi = {
  submit: (data) => post('/feedback', data),
  getMyFeedback: () => get('/feedback/my'),
}

// ==================== 公共配置 ====================
export const publicApi = {
  getConfig: () => get('/public/config'),
  healthCheck: () => get('/health'),
}

// ==================== 门店/商家 ====================
export const merchantApi = {
  /** 获取附近门店（按距离排序） */
  getNearby: (latitude, longitude, limit = 10) =>
    get('/merchants/nearby', { latitude, longitude, limit }),
  /** 获取所有营业中门店 */
  getActive: () => get('/merchants/active'),
  /** 获取门店详情（可选带距离） */
  getDetail: (id, latitude, longitude) =>
    get(`/merchants/${id}`, latitude ? { latitude, longitude } : {}),
}

// ==================== 食物 ====================
export const foodApi = {
  search: (params) => get('/food/search', params),
  getDetail: (id) => get(`/food/${id}`),
  getCategories: () => get('/food/categories'),
}

// ==================== 食物记录 ====================
export const foodRecordApi = {
  create: (data) => post('/food/records', data),
  getList: (params) => get('/food/records', params),
  getDetail: (id) => get(`/food/records/${id}`),
  delete: (id) => del(`/food/records/${id}`),
  getStats: (params) => get('/food/stats', params),
  uploadPhoto: (filePath) => uploadFile({ url: '/food/photo', filePath }),
}

// ==================== 商家端 ====================
export const merchantOrderApi = {
  getOrders: (params) => get('/merchant/orders', params),
  getDetail: (orderNo) => get(`/merchant/orders/${orderNo}`),
  acceptOrder: (orderNo) => post(`/merchant/orders/${orderNo}/accept`),
  markReady: (orderNo) => post(`/merchant/orders/${orderNo}/ready`),
  verifyPickup: (pickupCode) => post('/merchant/orders/verify-pickup', { pickupCode }),
  getStats: () => get('/merchant/orders/stats'),
}

export const merchantStoreApi = {
  getMyStore: () => get('/merchant/store'),
  updateStore: (data) => put('/merchant/store', data),
  updateBusinessStatus: (status) => put('/merchant/store/business-status', { status }),
}

export const merchantMessageApi = {
  getConversations: () => get('/merchant/messages/conversations'),
  getHistory: (params) => get('/merchant/messages/history', params),
  send: (data) => post('/merchant/messages/send', data),
  markRead: (data) => post('/merchant/messages/read', data),
  getUnreadCount: () => get('/merchant/messages/unread-count'),
}

export const userMessageApi = {
  getConversations: () => get('/user/messages/conversations'),
  getHistory: (params) => get('/user/messages/history', params),
  send: (data) => post('/user/messages/send', data),
  markRead: (data) => post('/user/messages/read', data),
  getUnreadCount: () => get('/user/messages/unread-count'),
}
