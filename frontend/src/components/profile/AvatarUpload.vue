<template>
  <div class="avatar-upload">
    <div
      class="upload-area"
      :class="{ 'is-dragover': isDragOver }"
      @drop.prevent="handleDrop"
      @dragover.prevent="isDragOver = true"
      @dragleave.prevent="isDragOver = false"
      @click="triggerFileInput"
    >
      <!-- 已上传的头像 -->
      <div v-if="avatarUrl" class="avatar-preview">
        <el-image :src="avatarUrl" fit="cover" class="avatar-image" />
        <div class="avatar-mask">
          <el-icon class="icon-camera"><Camera /></el-icon>
          <span>更换头像</span>
        </div>
      </div>

      <!-- 上传提示 -->
      <div v-else class="upload-placeholder">
        <el-icon class="icon-upload"><Plus /></el-icon>
        <div class="upload-text">
          <p>点击或拖拽上传头像</p>
          <p class="upload-hint">支持 JPG、PNG 格式，文件小于 2MB</p>
        </div>
      </div>

      <!-- 隐藏的文件输入框 -->
      <input
        ref="fileInputRef"
        type="file"
        accept="image/jpeg,image/png,image/jpg"
        style="display: none"
        @change="handleFileChange"
      />
    </div>

    <!-- 上传进度 -->
    <el-progress
      v-if="uploading"
      :percentage="uploadProgress"
      class="upload-progress"
    />

    <!-- 图片裁剪对话框 -->
    <ImageCropper
      v-model="showCropper"
      :image-file="selectedFile"
      @confirm="handleCroppedImage"
    />
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { Plus, Camera } from '@element-plus/icons-vue'
import ImageCropper from './ImageCropper.vue'
import api from '@/services/api'
import message from '@/utils/message'

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['update:modelValue', 'uploaded'])

const fileInputRef = ref()
const isDragOver = ref(false)
const uploading = ref(false)
const uploadProgress = ref(0)
const showCropper = ref(false)
const selectedFile = ref(null)

const avatarUrl = computed(() => props.modelValue)

// 触发文件选择
const triggerFileInput = () => {
  fileInputRef.value?.click()
}

// 处理文件选择
const handleFileChange = (event) => {
  const file = event.target.files?.[0]
  if (file) {
    validateAndUpload(file)
  }
  // 清空input值，允许重复选择同一文件
  event.target.value = ''
}

// 处理拖拽上传
const handleDrop = (event) => {
  isDragOver.value = false
  const file = event.dataTransfer?.files?.[0]
  if (file) {
    validateAndUpload(file)
  }
}

// 验证并打开裁剪器
const validateAndUpload = (file) => {
  // 验证文件类型
  const allowedTypes = ['image/jpeg', 'image/png', 'image/jpg']
  if (!allowedTypes.includes(file.type)) {
    message.error('只支持 JPG、PNG 格式的图片')
    return
  }

  // 验证文件大小（10MB，裁剪前可以大一些）
  const maxSize = 10 * 1024 * 1024
  if (file.size > maxSize) {
    message.error('图片大小不能超过 10MB')
    return
  }

  // 打开裁剪器
  selectedFile.value = file
  showCropper.value = true
}

// 处理裁剪后的图片
const handleCroppedImage = (file) => {
  uploadFile(file)
}

// 上传文件
const uploadFile = async (file) => {
  uploading.value = true
  uploadProgress.value = 0

  try {
    const formData = new FormData()
    formData.append('file', file)

    const response = await api.post('/user/avatar', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      },
      onUploadProgress: (progressEvent) => {
        uploadProgress.value = Math.round(
          (progressEvent.loaded * 100) / progressEvent.total
        )
      }
    })

    if (response.data.code === 200) {
      const avatarUrl = response.data.data
      emit('update:modelValue', avatarUrl)
      emit('uploaded', avatarUrl)
      message.success('头像上传成功')
    } else {
      message.error(response.data.message || '上传失败')
    }
  } catch (error) {
    console.error('上传失败:', error)
    
    let errorMessage = '上传失败'
    
    if (error.response) {
      const { status, data } = error.response
      if (data?.message) {
        errorMessage = data.message
      } else if (status === 400) {
        errorMessage = '文件格式不正确或文件过大'
      } else if (status === 401) {
        errorMessage = '登录已过期，请重新登录'
      } else if (status === 413) {
        errorMessage = '文件过大，请选择小于2MB的图片'
      } else if (status === 500) {
        errorMessage = '服务器错误，请稍后重试'
      } else {
        errorMessage = `上传失败 (${status})`
      }
    } else if (error.request) {
      errorMessage = '网络连接失败，请检查网络后重试'
    } else {
      errorMessage = error.message || '请求配置错误'
    }
    
    message.error(errorMessage)
  } finally {
    uploading.value = false
    uploadProgress.value = 0
  }
}
</script>

<style scoped lang="scss">
.avatar-upload {
  .upload-area {
    width: 150px;
    height: 150px;
    border: 2px dashed #dcdfe6;
    border-radius: 12px;
    cursor: pointer;
    position: relative;
    overflow: hidden;
    transition: all 0.3s;

    &:hover {
      border-color: #667eea;
      
      .avatar-mask {
        opacity: 1;
      }
    }

    &.is-dragover {
      border-color: #667eea;
      background: rgba(102, 126, 234, 0.05);
    }

    .avatar-preview {
      width: 100%;
      height: 100%;
      position: relative;

      .avatar-image {
        width: 100%;
        height: 100%;
      }

      .avatar-mask {
        position: absolute;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        background: rgba(0, 0, 0, 0.6);
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        color: white;
        opacity: 0;
        transition: opacity 0.3s;

        .icon-camera {
          font-size: 32px;
          margin-bottom: 8px;
        }

        span {
          font-size: 14px;
        }
      }
    }

    .upload-placeholder {
      width: 100%;
      height: 100%;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      color: #909399;

      .icon-upload {
        font-size: 48px;
        color: #c0c4cc;
        margin-bottom: 12px;
      }

      .upload-text {
        text-align: center;

        p {
          margin: 0;
          font-size: 14px;
          line-height: 1.5;

          &.upload-hint {
            font-size: 12px;
            color: #c0c4cc;
            margin-top: 4px;
          }
        }
      }
    }
  }

  .upload-progress {
    margin-top: 12px;
    width: 150px;
  }
}
</style>
