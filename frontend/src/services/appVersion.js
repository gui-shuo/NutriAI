import api from './api'

/**
 * APP版本管理 API
 */
export const appVersionApi = {
  // 获取最新版本（公开）
  getLatest(platform = 'android') {
    return api.get('/app-versions/latest', { params: { platform } })
  },

  // 获取版本列表（公开）
  getList(platform = 'android', page = 0, size = 20) {
    return api.get('/app-versions/list', { params: { platform, page, size } })
  },

  // 获取下载链接
  getDownloadUrl(id) {
    return api.get(`/app-versions/download/${id}`)
  },

  // 上传新版本（管理员）
  upload(formData, onProgress) {
    return api.post('/app-versions/admin/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 300000, // 5 min for large APK
      onUploadProgress: onProgress
    })
  },

  // 设置为最新版本（管理员）
  setLatest(id) {
    return api.put(`/app-versions/admin/${id}/set-latest`)
  },

  // 更新版本信息（管理员）
  update(id, description) {
    return api.put(`/app-versions/admin/${id}`, null, { params: { description } })
  },

  // 删除版本（管理员）
  delete(id) {
    return api.delete(`/app-versions/admin/${id}`)
  }
}
