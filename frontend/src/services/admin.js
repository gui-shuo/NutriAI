/**
 * 管理后台 API 服务层
 * 所有请求均通过集中式 axios 实例（自动携带 Token、处理 401 刷新）
 */
import api from './api'

// ==================== 数据看板 ====================

export const getDashboardStats = () => api.get('/admin/dashboard/stats')

export const getUserGrowthTrend = (days = 7) =>
  api.get('/admin/dashboard/user-growth', { params: { days } })

export const getAIUsageTrend = (days = 7) =>
  api.get('/admin/dashboard/ai-usage-trend', { params: { days } })

// ==================== 用户管理 ====================

export const getUserList = (params) => api.get('/admin/users', { params })

export const getUserDetail = (id) => api.get(`/admin/users/${id}`)

export const updateUserStatus = (id, status) =>
  api.put(`/admin/users/${id}/status`, { status })

export const updateUserMemberLevel = (id, memberLevel) =>
  api.put(`/admin/users/${id}/member-level`, { memberLevel })

export const updateUserRole = (id, role) =>
  api.put(`/admin/users/${id}/role`, { role })

// ==================== AI 日志管理 ====================

export const getAILogList = (params) => api.get('/admin/ai-logs', { params })

export const getAILogDetail = (id) => api.get(`/admin/ai-logs/${id}`)

// ==================== 系统配置 ====================

export const getConfigOptions = (category) =>
  api.get('/admin/config/options', { params: category ? { category } : {} })

export const getAllConfigs = (category) =>
  api.get('/admin/config', { params: category ? { category } : {} })

export const updateConfig = (key, value) =>
  api.put(`/admin/config/${encodeURIComponent(key)}`, { value })

export const createConfig = (dto) => api.post('/admin/config', dto)

export const deleteConfig = (key) =>
  api.delete(`/admin/config/${encodeURIComponent(key)}`)

// ==================== 公告管理 ====================

export const getAllAnnouncements = () => api.get('/admin/announcements')

export const createAnnouncement = (dto) => api.post('/admin/announcements', dto)

export const updateAnnouncement = (id, dto) => api.put(`/admin/announcements/${id}`, dto)

export const deleteAnnouncement = (id) => api.delete(`/admin/announcements/${id}`)
