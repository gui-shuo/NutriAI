import api from './api'

/**
 * 饮食记录API服务
 */

/**
 * 创建饮食记录
 * @param {Object} data - 饮食记录数据
 * @returns {Promise}
 */
export const createFoodRecord = data => {
  return api.post('/food/records', data)
}

/**
 * 查询饮食记录（分页）
 * @param {Object} params - 查询参数
 * @param {number} params.page - 页码
 * @param {number} params.size - 每页数量
 * @param {string} params.startDate - 开始日期
 * @param {string} params.endDate - 结束日期
 * @param {string} params.mealType - 餐次类型
 * @returns {Promise}
 */
export const getFoodRecords = params => {
  return api.get('/food/records', { params })
}

/**
 * 获取饮食记录详情
 * @param {number} id - 记录ID
 * @returns {Promise}
 */
export const getFoodRecordById = id => {
  return api.get(`/food/records/${id}`)
}

/**
 * 删除饮食记录
 * @param {number} id - 记录ID
 * @returns {Promise}
 */
export const deleteFoodRecord = id => {
  return api.delete(`/food/records/${id}`)
}

/**
 * 获取营养摄入统计
 * @param {string} date - 日期 (YYYY-MM-DD)
 * @returns {Promise}
 */
export const getNutritionStats = date => {
  return api.get('/food/stats', {
    params: date ? { date } : {}
  })
}

/**
 * 上传食物照片
 * @param {File} file - 图片文件
 * @param {Function} onProgress - 上传进度回调
 * @returns {Promise}
 */
export const uploadFoodPhoto = (file, onProgress) => {
  const formData = new FormData()
  formData.append('file', file)

  return api.post('/food/photo', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    onUploadProgress: onProgress
      ? progressEvent => {
          const percent = Math.round(
            (progressEvent.loaded * 100) / (progressEvent.total || progressEvent.loaded)
          )
          onProgress(percent)
        }
      : undefined
  })
}

/**
 * 上传食物照片并自动识别营养信息
 * @param {File} file - 图片文件
 * @param {Function} onProgress - 上传进度回调
 * @returns {Promise}
 */
export const uploadAndRecognize = (file, onProgress) => {
  const formData = new FormData()
  formData.append('file', file)

  return api.post('/food/photo-recognize', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    timeout: 30000,
    onUploadProgress: onProgress
      ? progressEvent => {
          const percent = Math.round(
            (progressEvent.loaded * 100) / (progressEvent.total || progressEvent.loaded)
          )
          onProgress(percent)
        }
      : undefined
  })
}

/**
 * 通过食物名称识别营养信息
 * @param {string} foodName - 食物名称
 * @returns {Promise}
 */
export const recognizeByName = foodName => {
  return api.post('/food/recognize-by-name', null, {
    params: { foodName },
    timeout: 15000
  })
}

/**
 * 餐次类型枚举
 */
export const MealTypes = {
  BREAKFAST: { value: 'BREAKFAST', label: '早餐', color: '#FF9800' },
  LUNCH: { value: 'LUNCH', label: '午餐', color: '#4CAF50' },
  DINNER: { value: 'DINNER', label: '晚餐', color: '#2196F3' },
  SNACK: { value: 'SNACK', label: '加餐', color: '#9C27B0' }
}

/**
 * 获取餐次类型列表
 */
export const getMealTypeList = () => {
  return Object.values(MealTypes)
}

/**
 * 根据value获取餐次类型
 */
export const getMealTypeByValue = value => {
  return MealTypes[value] || null
}
