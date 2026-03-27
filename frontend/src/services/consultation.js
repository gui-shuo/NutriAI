import api from './api'

/**
 * 营养师咨询服务API
 */

// === 营养师 ===

/** 获取所有营养师列表 */
export const getNutritionists = () => api.get('/consultation/nutritionists')

/** 获取在线营养师列表 */
export const getOnlineNutritionists = () => api.get('/consultation/nutritionists/online')

/** 获取营养师详情 */
export const getNutritionistDetail = (id) => api.get(`/consultation/nutritionists/${id}`)

// === 咨询订单 ===

/**
 * 创建咨询订单
 * @param {number} nutritionistId 营养师ID
 * @param {string} description 咨询描述
 * @param {string} consultationType 咨询类型: TEXT/VIDEO
 */
export const createConsultation = (nutritionistId, description, consultationType = 'TEXT') =>
  api.post('/consultation/orders', { nutritionistId, description, consultationType })

/** 模拟支付咨询订单 */
export const simulatePayConsultation = (orderNo) =>
  api.post(`/consultation/orders/${orderNo}/simulate-pay`)

/** 发送咨询消息 */
export const sendConsultationMessage = (orderNo, content) =>
  api.post(`/consultation/orders/${orderNo}/messages`, { content, role: 'user' })

/** 模拟营养师回复 */
export const simulateNutritionistReply = (orderNo) =>
  api.post(`/consultation/orders/${orderNo}/reply`)

/**
 * 完成咨询
 * @param {string} orderNo
 * @param {number} rating 评分1-5
 * @param {string} review 评价
 */
export const completeConsultation = (orderNo, rating, review) =>
  api.post(`/consultation/orders/${orderNo}/complete`, { rating, review })

/** 模拟退款 */
export const simulateRefundConsultation = (orderNo) =>
  api.post(`/consultation/orders/${orderNo}/simulate-refund`)

/** 获取咨询订单历史 */
export const getConsultationHistory = (page = 0, size = 10) =>
  api.get('/consultation/orders', { params: { page, size } })

/** 获取活跃咨询 */
export const getActiveConsultations = () => api.get('/consultation/orders/active')
