<template>
  <div class="food-photo-upload">
    <div
      class="upload-area"
      :class="{ 'is-dragover': isDragOver, 'has-image': modelValue }"
      @click="triggerFileInput"
      @drop.prevent="handleDrop"
      @dragover.prevent="isDragOver = true"
      @dragleave="isDragOver = false"
    >
      <input
        ref="fileInputRef"
        type="file"
        accept="image/*"
        style="display: none"
        @change="handleFileChange"
      />

      <div v-if="!modelValue && !uploading" class="upload-hint">
        <el-icon class="upload-icon">
          <Plus />
        </el-icon>
        <div class="upload-text">
          <p>点击或拖拽上传食物照片</p>
          <p class="upload-tip">支持 JPG、PNG 格式，最大 10MB</p>
        </div>
      </div>

      <div v-else-if="uploading" class="upload-loading">
        <el-icon class="is-loading">
          <Loading />
        </el-icon>
        <p>上传中... {{ uploadProgress }}%</p>
      </div>

      <div v-else class="image-preview">
        <el-image :src="modelValue" fit="cover" class="preview-image">
          <template #error>
            <div class="image-error">
              <el-icon><Picture /></el-icon>
            </div>
          </template>
        </el-image>
        <div class="image-overlay">
          <el-button type="danger" circle @click.stop="handleRemove">
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Plus, Loading, Picture, Delete } from '@element-plus/icons-vue'
import { uploadFoodPhoto, uploadAndRecognize } from '@/services/foodRecord'
import message from '@/utils/message'

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  autoRecognize: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'recognized'])

const fileInputRef = ref()
const isDragOver = ref(false)
const uploading = ref(false)
const uploadProgress = ref(0)

// 触发文件选择
const triggerFileInput = () => {
  fileInputRef.value?.click()
}

// 处理文件选择
const handleFileChange = event => {
  const file = event.target.files?.[0]
  if (file) {
    validateAndUpload(file)
  }
  // 清空input，允许重复选择同一文件
  event.target.value = ''
}

// 处理拖拽上传
const handleDrop = event => {
  isDragOver.value = false
  const file = event.dataTransfer?.files?.[0]
  if (file) {
    validateAndUpload(file)
  }
}

// 验证并上传文件
const validateAndUpload = file => {
  // 验证文件类型
  const allowedTypes = ['image/jpeg', 'image/png', 'image/jpg', 'image/gif', 'image/webp', 'image/bmp']
  if (!allowedTypes.includes(file.type)) {
    message.error('只支持 JPG、PNG、GIF、WebP、BMP 格式的图片')
    return
  }

  // 验证文件大小（10MB）
  const maxSize = 10 * 1024 * 1024
  if (file.size > maxSize) {
    message.error('图片大小不能超过 10MB')
    return
  }

  uploadFile(file)
}

// 上传文件
const uploadFile = async file => {
  uploading.value = true
  uploadProgress.value = 0

  const onProgress = (percent) => {
    uploadProgress.value = percent
  }

  try {
    if (props.autoRecognize) {
      // 上传并识别模式：上传照片后自动调用AI识别营养信息
      const response = await uploadAndRecognize(file, onProgress)
      if (response.data.code === 200) {
        const result = response.data.data
        emit('update:modelValue', result.imageUrl || '')
        emit('recognized', result)
        message.success(response.data.message || '识别成功')
      } else {
        message.error(response.data.message || '识别失败')
      }
    } else {
      // 纯上传模式
      const response = await uploadFoodPhoto(file, onProgress)
      if (response.data.code === 200) {
        const photoUrl = response.data.data
        emit('update:modelValue', photoUrl)
        message.success('照片上传成功')
      } else {
        message.error(response.data.message || '上传失败')
      }
    }
  } catch (error) {
    console.error('上传失败:', error)
    message.error('上传失败，请稍后重试')
  } finally {
    uploading.value = false
    uploadProgress.value = 0
  }
}

// 删除图片
const handleRemove = () => {
  emit('update:modelValue', '')
}
</script>

<style scoped lang="scss">
.food-photo-upload {
  .upload-area {
    width: 100%;
    min-height: 200px;
    border: 2px dashed #dcdfe6;
    border-radius: 8px;
    cursor: pointer;
    transition: all 0.3s;
    overflow: hidden;
    position: relative;

    &:hover,
    &.is-dragover {
      border-color: #667eea;
      background: rgba(102, 126, 234, 0.05);
    }

    &.has-image {
      border-style: solid;
      min-height: 300px;
    }

    .upload-hint,
    .upload-loading {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      height: 200px;
      color: #909399;

      .upload-icon {
        font-size: 48px;
        margin-bottom: 16px;
      }

      .upload-text {
        text-align: center;

        p {
          margin: 4px 0;
        }

        .upload-tip {
          font-size: 12px;
          color: #c0c4cc;
        }
      }
    }

    .upload-loading {
      .el-icon {
        font-size: 48px;
        margin-bottom: 16px;
      }
    }

    .image-preview {
      position: relative;
      width: 100%;
      height: 300px;

      .preview-image {
        width: 100%;
        height: 100%;
      }

      .image-error {
        display: flex;
        align-items: center;
        justify-content: center;
        width: 100%;
        height: 100%;
        background: #f5f7fa;
        color: #909399;

        .el-icon {
          font-size: 48px;
        }
      }

      .image-overlay {
        position: absolute;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        background: rgba(0, 0, 0, 0.5);
        display: flex;
        align-items: center;
        justify-content: center;
        opacity: 0;
        transition: opacity 0.3s;

        &:hover {
          opacity: 1;
        }
      }
    }
  }
}
</style>
