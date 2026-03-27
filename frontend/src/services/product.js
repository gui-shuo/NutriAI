import api from './api'

/**
 * 营养产品商城服务API
 */

// === 产品 ===

/**
 * 获取产品列表
 * @param {string} category 分类（可选）
 * @param {number} page
 * @param {number} size
 */
export const getProducts = (category, page = 0, size = 12) =>
  api.get('/products', { params: { category, page, size } })

/** 搜索产品 */
export const searchProducts = (keyword, page = 0, size = 12) =>
  api.get('/products/search', { params: { keyword, page, size } })

/** 获取推荐产品 */
export const getRecommendedProducts = () => api.get('/products/recommended')

/** 获取产品详情 */
export const getProductDetail = (id) => api.get(`/products/${id}`)

/** 获取产品分类 */
export const getProductCategories = () => api.get('/products/categories')

// === 订单 ===

/**
 * 创建产品订单
 * @param {Array} items [{productId, quantity}]
 * @param {string} receiverName
 * @param {string} receiverPhone
 * @param {string} receiverAddress
 * @param {string} remark
 */
export const createProductOrder = (items, receiverName, receiverPhone, receiverAddress, remark) =>
  api.post('/products/orders', { items, receiverName, receiverPhone, receiverAddress, remark })

/** 模拟支付 */
export const simulatePayProduct = (orderNo) =>
  api.post(`/products/orders/${orderNo}/simulate-pay`)

/** 模拟发货 */
export const simulateShipProduct = (orderNo) =>
  api.post(`/products/orders/${orderNo}/simulate-ship`)

/** 确认收货 */
export const confirmReceiveProduct = (orderNo) =>
  api.post(`/products/orders/${orderNo}/confirm-receive`)

/** 模拟退款 */
export const simulateRefundProduct = (orderNo) =>
  api.post(`/products/orders/${orderNo}/simulate-refund`)

/** 获取订单历史 */
export const getProductOrderHistory = (page = 0, size = 10) =>
  api.get('/products/orders', { params: { page, size } })

// === 分类常量 ===

export const ProductCategories = {
  SUPPLEMENT: { code: 'SUPPLEMENT', name: '膳食补充剂', icon: '💊' },
  PROTEIN: { code: 'PROTEIN', name: '蛋白粉', icon: '🥤' },
  VITAMIN: { code: 'VITAMIN', name: '维生素', icon: '🌞' },
  ORGANIC: { code: 'ORGANIC', name: '有机食品', icon: '🌿' },
  HEALTH_FOOD: { code: 'HEALTH_FOOD', name: '健康食品', icon: '🥗' },
  EQUIPMENT: { code: 'EQUIPMENT', name: '健康器材', icon: '⚖️' }
}
