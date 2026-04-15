import { request, uploadFile } from '@/utils/request'

// ============ Auth ============
export const authApi = {
  getCaptcha: () => request({ url: '/auth/captcha' }),
  login: (data: any) => request({ url: '/auth/login', method: 'POST', data }),
  register: (data: any) => request({ url: '/auth/register', method: 'POST', data }),
  sendEmailCode: (email: string) => request({ url: '/auth/send-email-code', method: 'POST', data: { email } }),
  checkUsername: (username: string) => request({ url: `/auth/check-username?username=${username}`, showError: false }),
  checkEmail: (email: string) => request({ url: `/auth/check-email?email=${email}`, showError: false }),
  forgotPassword: (data: any) => request({ url: '/auth/forgot-password', method: 'POST', data }),
  resetPassword: (data: any) => request({ url: '/auth/reset-password', method: 'POST', data }),
  wxLogin: (code: string) => request({ url: '/auth/wx-login', method: 'POST', data: { code } })
}

// ============ User / Profile ============
export const userApi = {
  getProfile: () => request({ url: '/user/profile' }),
  updateProfile: (data: any) => request({ url: '/user/profile', method: 'PUT', data }),
  uploadAvatar: (filePath: string) => uploadFile({ url: '/user/avatar', filePath, name: 'file' }),
  changePassword: (data: any) => request({ url: '/user/password', method: 'PUT', data }),
  deleteAccount: (password: string) => request({ url: '/user/delete-account', method: 'POST', data: { password } }),
  sendEmailCode: () => request({ url: '/user/email-code/send', method: 'POST' }),
  changePhone: (data: any) => request({ url: '/user/phone', method: 'PUT', data })
}

// ============ Food Records ============
export const foodApi = {
  getRecords: (params: any) => request({ url: '/food/records', data: params }),
  getRecord: (id: number) => request({ url: `/food/records/${id}` }),
  createRecord: (data: any) => request({ url: '/food/records', method: 'POST', data }),
  deleteRecord: (id: number) => request({ url: `/food/records/${id}`, method: 'DELETE' }),
  getStats: (date: string) => request({ url: `/food/stats?date=${date}` }),
  photoRecognize: (filePath: string) => uploadFile({ url: '/food-recognition/recognize-by-image', filePath, name: 'image' }),
  recognizeByName: (name: string) => request({ url: `/food-recognition/recognize-by-name?foodName=${encodeURIComponent(name)}`, method: 'POST' })
}

// ============ AI Diet Plan ============
export const dietPlanApi = {
  generate: (data: any) => request({ url: '/diet-plan/generate', method: 'POST', data, showLoading: true, loadingText: 'AI生成中...' }),
  getPlans: (params?: any) => request({ url: '/diet-plan/history', data: params }),
  getPlan: (id: number) => request({ url: `/diet-plan/${id}` })
}

// ============ Consultation ============
export const consultationApi = {
  getNutritionists: () => request({ url: '/consultation/nutritionists' }),
  getOnlineNutritionists: () => request({ url: '/consultation/nutritionists/online' }),
  getNutritionist: (id: number) => request({ url: `/consultation/nutritionists/${id}` }),
  createOrder: (data: any) => request({ url: '/consultation/orders', method: 'POST', data }),
  getOrders: (params?: any) => request({ url: '/consultation/orders', data: params }),
  getOrderDetail: (orderNo: string) => request({ url: `/consultation/orders/${orderNo}` }),
  getActiveOrders: () => request({ url: '/consultation/orders/active' }),
  sendMessage: (orderNo: string, data: any) => request({ url: `/consultation/orders/${orderNo}/messages`, method: 'POST', data }),
  completeOrder: (orderNo: string, data: any) => request({ url: `/consultation/orders/${orderNo}/complete`, method: 'POST', data }),
  simulatePay: (orderNo: string) => request({ url: `/consultation/orders/${orderNo}/simulate-pay`, method: 'POST' }),
  simulateRefund: (orderNo: string) => request({ url: `/consultation/orders/${orderNo}/simulate-refund`, method: 'POST' }),
  getImConfig: (orderNo: string) => request({ url: `/im/config/consultation/${orderNo}` })
}

// ============ Product Shop ============
export const productApi = {
  getProducts: (params?: any) => request({ url: '/products', data: params }),
  search: (keyword: string) => request({ url: `/products/search?keyword=${keyword}` }),
  getRecommended: () => request({ url: '/products/recommended' }),
  getProduct: (id: number) => request({ url: `/products/${id}` }),
  getCategories: () => request({ url: '/products/categories' }),
  createOrder: (data: any) => request({ url: '/products/orders', method: 'POST', data }),
  getOrders: (params?: any) => request({ url: '/products/orders', data: params }),
  simulatePay: (orderNo: string) => request({ url: `/products/orders/${orderNo}/simulate-pay`, method: 'POST' }),
  confirmReceive: (orderNo: string) => request({ url: `/products/orders/${orderNo}/confirm-receive`, method: 'POST' }),
  simulateRefund: (orderNo: string) => request({ url: `/products/orders/${orderNo}/simulate-refund`, method: 'POST' })
}

// ============ Community ============
export const communityApi = {
  getFeed: (params?: any) => request({ url: '/community/feed', data: params }),
  getPost: (id: number) => request({ url: `/community/posts/${id}` }),
  getUserPosts: (userId: number, params?: any) => request({ url: `/community/user/${userId}/posts`, data: params }),
  createPost: (data: any) => request({ url: '/community/posts', method: 'POST', data }),
  deletePost: (id: number) => request({ url: `/community/posts/${id}`, method: 'DELETE' }),
  uploadMedia: (filePath: string) => uploadFile({ url: '/community/upload', filePath, name: 'file' }),
  getComments: (postId: number, params?: any) => request({ url: `/community/posts/${postId}/comments`, data: params }),
  addComment: (postId: number, data: any) => request({ url: `/community/posts/${postId}/comments`, method: 'POST', data }),
  deleteComment: (id: number) => request({ url: `/community/comments/${id}`, method: 'DELETE' }),
  toggleLike: (data: any) => request({ url: '/community/like', method: 'POST', data }),
  toggleFollow: (userId: number) => request({ url: `/community/follow/${userId}`, method: 'POST' }),
  getFollowStatus: (userId: number) => request({ url: `/community/follow/${userId}/status` })
}

// ============ Member / VIP ============
export const memberApi = {
  getInfo: () => request({ url: '/member/info' }),
  getPermissions: () => request({ url: '/member/permissions' }),
  signIn: () => request({ url: '/member/sign-in', method: 'POST' }),
  getSignInCalendar: (params?: any) => request({ url: '/member/sign-in/calendar', data: params }),
  getGrowthRecords: (params?: any) => request({ url: '/member/growth-records', data: params }),
  generateInvitation: () => request({ url: '/member/invitation/generate' }),
  getInvitationRecords: (params?: any) => request({ url: '/member/invitation/records', data: params })
}

export const vipApi = {
  getPlans: () => request({ url: '/vip/plans' }),
  createOrder: (data: any) => request({ url: '/vip/orders', method: 'POST', data }),
  simulatePay: (orderNo: string) => request({ url: `/vip/orders/${orderNo}/simulate-pay`, method: 'POST' }),
  simulateRefund: (orderNo: string) => request({ url: `/vip/orders/${orderNo}/simulate-refund`, method: 'POST' }),
  getOrderStatus: (orderNo: string) => request({ url: `/vip/orders/${orderNo}/status` }),
  getVipStatus: () => request({ url: '/vip/status' }),
  getOrders: (params?: any) => request({ url: '/vip/orders', data: params })
}

// ============ Recipes ============
export const recipeApi = {
  getList: (params?: any) => request({ url: '/recipes', data: params }),
  getDetail: (id: number | string) => request({ url: `/recipes/${id}` }),
  toggleFavorite: (id: number | string) => request({ url: `/recipes/${id}/favorite`, method: 'POST' }),
  rate: (id: number | string, data: { score: number; comment?: string }) => request({ url: `/recipes/${id}/rate`, method: 'POST', data })
}

// ============ Meal Plans ============
export const mealPlanApi = {
  getList: (params?: any) => request({ url: '/meal-plans', data: params }),
  getDetail: (id: number | string) => request({ url: `/meal-plans/${id}` }),
  toggleFavorite: (id: number | string) => request({ url: `/meal-plans/${id}/favorite`, method: 'POST' }),
  getFeatured: () => request({ url: '/meal-plans/featured' }),
  getMyFavorites: (params?: any) => request({ url: '/meal-plans/my-favorites', data: params }),
  getTags: () => request({ url: '/meal-plans/tags' }),
  searchByTag: (tag: string, params?: any) => request({ url: '/meal-plans/by-tag', data: { tag, ...params } }),
  getRecommendations: () => request({ url: '/meal-plans/recommendations' }),
  // Follow & Track
  follow: (id: number | string) => request({ url: `/meal-plans/${id}/follow`, method: 'POST' }),
  unfollow: (id: number | string) => request({ url: `/meal-plans/${id}/follow`, method: 'DELETE' }),
  getMyFollows: () => request({ url: '/meal-plans/my-follows' }),
  getProgress: (id: number | string) => request({ url: `/meal-plans/${id}/progress` }),
  // Check-in
  checkin: (data: { followId: number; dayNumber: number; mealType: string }) =>
    request({ url: '/meal-plans/checkin', method: 'POST', data }),
  uncheckin: (followId: number, dayNumber: number, mealType: string) =>
    request({ url: `/meal-plans/checkin?followId=${followId}&dayNumber=${dayNumber}&mealType=${mealType}`, method: 'DELETE' }),
  // Ratings
  rate: (id: number | string, data: { rating: number; review?: string }) =>
    request({ url: `/meal-plans/${id}/rate`, method: 'POST', data }),
  getRatings: (id: number | string, params?: any) => request({ url: `/meal-plans/${id}/ratings`, data: params }),
}

// ============ Meal Ordering (营养餐) ============
export const mealApi = {
  getList: (params?: any) => request({ url: '/meals', data: params }),
  getDetail: (id: number | string) => request({ url: `/meals/${id}` }),
  getCategories: () => request({ url: '/meals/categories' }),
  getRecommended: () => request({ url: '/meals/recommended' }),
  createOrder: (data: any) => request({ url: '/meals/orders', method: 'POST', data }),
  getOrders: (params?: any) => request({ url: '/meals/orders', data: params }),
  getOrderDetail: (orderNo: string) => request({ url: `/meals/orders/${orderNo}` }),
  cancelOrder: (orderNo: string) => request({ url: `/meals/orders/${orderNo}/cancel`, method: 'POST' }),
  simulatePay: (orderNo: string) => request({ url: `/meals/orders/${orderNo}/simulate-pay`, method: 'POST' })
}

// ============ Admin APIs ============
export const adminApi = {
  // Meal management
  getMeals: (params?: any) => request({ url: '/admin/shop/meals', data: params }),
  createMeal: (data: any) => request({ url: '/admin/shop/meals', method: 'POST', data }),
  updateMeal: (id: number, data: any) => request({ url: `/admin/shop/meals/${id}`, method: 'PUT', data }),
  deleteMeal: (id: number) => request({ url: `/admin/shop/meals/${id}`, method: 'DELETE' }),
  toggleMealStatus: (id: number) => request({ url: `/admin/shop/meals/${id}/toggle-status`, method: 'PUT' }),
  // Product management
  getProducts: (params?: any) => request({ url: '/admin/shop/products', data: params }),
  createProduct: (data: any) => request({ url: '/admin/shop/products', method: 'POST', data }),
  updateProduct: (id: number, data: any) => request({ url: `/admin/shop/products/${id}`, method: 'PUT', data }),
  deleteProduct: (id: number) => request({ url: `/admin/shop/products/${id}`, method: 'DELETE' }),
  toggleProductStatus: (id: number) => request({ url: `/admin/shop/products/${id}/toggle-status`, method: 'PUT' }),
  // Order management
  getOrders: (params?: any) => request({ url: '/admin/shop/orders', data: params }),
  updateOrderStatus: (orderNo: string, data: any) => request({ url: `/admin/shop/orders/${orderNo}/status`, method: 'PUT', data }),
  // Dashboard
  getDashboard: () => request({ url: '/admin/shop/dashboard' }),
  // Upload
  uploadImage: (filePath: string) => uploadFile({ url: '/admin/shop/upload', filePath, name: 'file' })
}

// ============ Feedback ============
export const feedbackApi = {
  submit: (data: any) => request({ url: '/feedback', method: 'POST', data }),
  getMyFeedbacks: (params?: any) => request({ url: '/feedback/my', data: params })
}

// ============ Addresses ============
export const addressApi = {
  getList: () => request({ url: '/addresses' }),
  add: (data: any) => request({ url: '/addresses', method: 'POST', data }),
  update: (id: number, data: any) => request({ url: `/addresses/${id}`, method: 'PUT', data }),
  remove: (id: number) => request({ url: `/addresses/${id}`, method: 'DELETE' }),
  setDefault: (id: number) => request({ url: `/addresses/${id}/default`, method: 'PUT' })
}

// ============ Social Auth ============
export const socialAuthApi = {
  getWechatAuthUrl: (state = 'login') => request({ url: `/auth/social/wechat/auth-url?state=${state}` }),
  getQqAuthUrl: (state = 'login') => request({ url: `/auth/social/qq/auth-url?state=${state}` }),
  wechatLogin: (code: string) => request({ url: '/auth/social/wechat/login', method: 'POST', data: { code, provider: 'wechat' } }),
  qqLogin: (code: string, state = '') => request({ url: '/auth/social/qq/login', method: 'POST', data: { code, provider: 'qq', state } }),
  qqTokenLogin: (accessToken: string) => request({ url: '/auth/social/qq/token-login', method: 'POST', data: { access_token: accessToken } }),
  getBindInfo: () => request({ url: '/auth/social/bindinfo' }),
  bindWechat: (code: string) => request({ url: '/auth/social/bind/wechat', method: 'POST', data: { code } }),
  bindQq: (code: string, state = '') => request({ url: '/auth/social/bind/qq', method: 'POST', data: { code, state } }),
  bindQqToken: (accessToken: string) => request({ url: '/auth/social/bind/qq-token', method: 'POST', data: { access_token: accessToken } }),
  unbindWechat: () => request({ url: '/auth/social/unbind/wechat', method: 'DELETE' }),
  unbindQq: () => request({ url: '/auth/social/unbind/qq', method: 'DELETE' }),
  bindEmail: (email: string, code: string) => request({ url: '/auth/social/bind-email', method: 'POST', data: { email, code } }),
  sendMergeCode: (email: string) => request({ url: '/auth/social/merge/send-code', method: 'POST', data: { email } }),
  mergeToEmail: (email: string, code: string) => request({ url: '/auth/social/merge/confirm', method: 'POST', data: { email, code } }),
}

// ============ Announcements ============
export const announcementApi = {
  getList: (params?: any) => request({ url: '/announcements', data: params }),
  getUnreadCount: () => request({ url: '/announcements/unread-count' }),
  markRead: (id: number) => request({ url: `/announcements/${id}/read`, method: 'POST' }),
  markAllRead: () => request({ url: '/announcements/read-all', method: 'POST' }),
}

// ============ Constants ============
export const PostCategories = [
  { value: '饮食打卡', label: '🍽️ 饮食打卡' },
  { value: '食谱分享', label: '📖 食谱分享' },
  { value: '健身日记', label: '💪 健身日记' },
  { value: '营养知识', label: '📚 营养知识' },
  { value: '减脂心得', label: '🔥 减脂心得' },
  { value: '问答交流', label: '❓ 问答交流' }
]

export const MealTypes = [
  { value: 'BREAKFAST', label: '早餐', icon: '🌅' },
  { value: 'LUNCH', label: '午餐', icon: '☀️' },
  { value: 'DINNER', label: '晚餐', icon: '🌙' },
  { value: 'SNACK', label: '加餐', icon: '🍪' }
]
