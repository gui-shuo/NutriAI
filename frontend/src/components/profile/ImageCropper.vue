<template>
  <el-dialog
    v-model="visible"
    title="裁剪头像"
    width="600px"
    @close="handleClose"
  >
    <div class="cropper-container">
      <div class="preview-area">
        <canvas
          ref="canvasRef"
          class="cropper-canvas"
          @mousedown="startDrag"
          @mousemove="onDrag"
          @mouseup="endDrag"
          @mouseleave="endDrag"
        ></canvas>
      </div>

      <!-- 裁剪控制 -->
      <div class="cropper-controls">
        <div class="control-item">
          <span>尺寸：</span>
          <el-radio-group v-model="cropSize" @change="updateCanvas">
            <el-radio-button :value="200">200x200</el-radio-button>
            <el-radio-button :value="300">300x300</el-radio-button>
            <el-radio-button :value="400">400x400</el-radio-button>
          </el-radio-group>
        </div>

        <div class="control-item">
          <span>缩放：</span>
          <el-slider
            v-model="scale"
            :min="0.5"
            :max="3"
            :step="0.1"
            style="width: 200px"
            @input="updateCanvas"
          />
        </div>
      </div>

      <!-- 预览 -->
      <div class="preview-result">
        <div class="preview-title">预览效果</div>
        <canvas ref="previewCanvasRef" class="preview-canvas"></canvas>
      </div>
    </div>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" @click="handleConfirm" :loading="uploading">
        确认上传
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  imageFile: {
    type: File,
    default: null
  }
})

const emit = defineEmits(['update:modelValue', 'confirm'])

const visible = ref(false)
const uploading = ref(false)
const canvasRef = ref()
const previewCanvasRef = ref()

const cropSize = ref(300)
const scale = ref(1)
const imageData = ref(null)
const isDragging = ref(false)
const dragStart = ref({ x: 0, y: 0 })
const imagePosition = ref({ x: 0, y: 0 })

watch(() => props.modelValue, (val) => {
  visible.value = val
  if (val && props.imageFile) {
    loadImage(props.imageFile)
  }
})

watch(visible, (val) => {
  emit('update:modelValue', val)
})

// 加载图片
const loadImage = (file) => {
  const reader = new FileReader()
  reader.onload = (e) => {
    const img = new Image()
    img.onload = () => {
      imageData.value = img
      imagePosition.value = { x: 0, y: 0 }
      scale.value = 1
      nextTick(() => {
        updateCanvas()
      })
    }
    img.src = e.target.result
  }
  reader.readAsDataURL(file)
}

// 更新画布
const updateCanvas = () => {
  if (!canvasRef.value || !imageData.value) return

  try {
    const canvas = canvasRef.value
    const ctx = canvas.getContext('2d')
  
  // 设置画布大小
  canvas.width = 400
  canvas.height = 400

  // 清空画布
  ctx.fillStyle = '#f0f0f0'
  ctx.fillRect(0, 0, canvas.width, canvas.height)

  // 绘制图片
  const scaledWidth = imageData.value.width * scale.value
  const scaledHeight = imageData.value.height * scale.value
  
  ctx.drawImage(
    imageData.value,
    imagePosition.value.x,
    imagePosition.value.y,
    scaledWidth,
    scaledHeight
  )

  // 绘制裁剪框
  const cropX = (canvas.width - cropSize.value) / 2
  const cropY = (canvas.height - cropSize.value) / 2

  // 暗色遮罩
  ctx.fillStyle = 'rgba(0, 0, 0, 0.5)'
  ctx.fillRect(0, 0, canvas.width, cropY)
  ctx.fillRect(0, cropY + cropSize.value, canvas.width, canvas.height - cropY - cropSize.value)
  ctx.fillRect(0, cropY, cropX, cropSize.value)
  ctx.fillRect(cropX + cropSize.value, cropY, canvas.width - cropX - cropSize.value, cropSize.value)

    // 裁剪框边框
    ctx.strokeStyle = '#fff'
    ctx.lineWidth = 2
    ctx.strokeRect(cropX, cropY, cropSize.value, cropSize.value)

    // 更新预览
    updatePreview()
  } catch (error) {
    console.error('画布更新失败:', error)
  }
}

// 更新预览
const updatePreview = () => {
  if (!previewCanvasRef.value || !imageData.value) return

  try {
    const previewCanvas = previewCanvasRef.value
    const previewCtx = previewCanvas.getContext('2d')
    
    previewCanvas.width = cropSize.value
    previewCanvas.height = cropSize.value

    const canvas = canvasRef.value
    if (!canvas) return
    
    const cropX = (canvas.width - cropSize.value) / 2
    const cropY = (canvas.height - cropSize.value) / 2

    // 从主画布复制裁剪区域
    const mainCtx = canvas.getContext('2d')
    const croppedImageData = mainCtx.getImageData(cropX, cropY, cropSize.value, cropSize.value)
    previewCtx.putImageData(croppedImageData, 0, 0)
  } catch (error) {
    console.error('预览更新失败:', error)
  }
}

// 拖拽开始
const startDrag = (e) => {
  isDragging.value = true
  dragStart.value = {
    x: e.offsetX - imagePosition.value.x,
    y: e.offsetY - imagePosition.value.y
  }
}

// 拖拽中
const onDrag = (e) => {
  if (!isDragging.value) return
  
  imagePosition.value = {
    x: e.offsetX - dragStart.value.x,
    y: e.offsetY - dragStart.value.y
  }
  
  updateCanvas()
}

// 拖拽结束
const endDrag = () => {
  isDragging.value = false
}

// 确认裁剪
const handleConfirm = async () => {
  if (!previewCanvasRef.value) return

  uploading.value = true
  try {
    // 将canvas转换为blob
    const blob = await new Promise((resolve) => {
      previewCanvasRef.value.toBlob(resolve, 'image/jpeg', 0.9)
    })

    // 创建File对象
    const file = new File([blob], 'avatar.jpg', { type: 'image/jpeg' })
    
    emit('confirm', file)
    handleClose()
  } catch (error) {
    console.error('裁剪失败:', error)
  } finally {
    uploading.value = false
  }
}

// 关闭对话框
const handleClose = () => {
  visible.value = false
  imageData.value = null
  scale.value = 1
  imagePosition.value = { x: 0, y: 0 }
}
</script>

<style scoped lang="scss">
.cropper-container {
  .preview-area {
    display: flex;
    justify-content: center;
    margin-bottom: 20px;

    .cropper-canvas {
      border: 1px solid #dcdfe6;
      border-radius: 8px;
      cursor: move;
    }
  }

  .cropper-controls {
    margin-bottom: 20px;

    .control-item {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 16px;

      span {
        font-size: 14px;
        color: #606266;
        min-width: 50px;
      }
    }
  }

  .preview-result {
    text-align: center;

    .preview-title {
      font-size: 14px;
      color: #606266;
      margin-bottom: 12px;
    }

    .preview-canvas {
      border: 1px solid #dcdfe6;
      border-radius: 8px;
    }
  }
}
</style>
