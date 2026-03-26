<template>
  <el-dialog v-model="visible" title="裁剪头像" width="650px" @close="handleClose">
    <div class="cropper-container">
      <div class="preview-area">
        <canvas
          ref="canvasRef"
          class="cropper-canvas"
          @mousedown="startDrag"
          @mousemove="onDrag"
          @mouseup="endDrag"
          @mouseleave="endDrag"
          @wheel.prevent="onWheel"
          @touchstart.prevent="handleTouchStart"
          @touchmove.prevent="handleTouchMove"
          @touchend.prevent="endDrag"
        />
      </div>

      <!-- 裁剪控制 -->
      <div class="cropper-controls">
        <div class="control-item">
          <span>缩放：</span>
          <el-slider
            v-model="scale"
            :min="0.05"
            :max="5"
            :step="0.01"
            style="width: 260px"
            @input="updateCanvas"
          />
          <span class="scale-label">{{ (scale * 100).toFixed(0) }}%</span>
        </div>

        <div class="control-item">
          <span>裁剪框：</span>
          <el-slider
            v-model="cropSize"
            :min="50"
            :max="380"
            :step="1"
            style="width: 260px"
            @input="updateCanvas"
          />
          <span class="scale-label">{{ cropSize }}px</span>
        </div>

        <div class="control-item">
          <el-button size="small" @click="resetTransform">重置</el-button>
          <el-button size="small" @click="fitToCanvas">适应画布</el-button>
        </div>
      </div>

      <!-- 预览 -->
      <div class="preview-result">
        <div class="preview-title">预览效果</div>
        <canvas ref="previewCanvasRef" class="preview-canvas" />
      </div>
    </div>

    <template #footer>
      <el-button @click="handleClose"> 取消 </el-button>
      <el-button type="primary" :loading="uploading" @click="handleConfirm"> 确认上传 </el-button>
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

const cropSize = ref(200)
const scale = ref(1)
const imageData = ref(null)
const isDragging = ref(false)
const dragStart = ref({ x: 0, y: 0 })
const imagePosition = ref({ x: 0, y: 0 })

const CANVAS_SIZE = 400

watch(
  () => props.modelValue,
  val => {
    visible.value = val
    if (val && props.imageFile) {
      loadImage(props.imageFile)
    }
  }
)

watch(visible, val => {
  emit('update:modelValue', val)
})

// 加载图片
const loadImage = file => {
  const reader = new FileReader()
  reader.onload = e => {
    const img = new Image()
    img.onload = () => {
      imageData.value = img
      // 自动适应画布
      const fitScale = Math.min(CANVAS_SIZE / img.width, CANVAS_SIZE / img.height, 1)
      scale.value = fitScale
      imagePosition.value = {
        x: (CANVAS_SIZE - img.width * fitScale) / 2,
        y: (CANVAS_SIZE - img.height * fitScale) / 2
      }
      cropSize.value = Math.min(
        200,
        Math.floor(Math.min(img.width * fitScale, img.height * fitScale))
      )
      nextTick(() => {
        updateCanvas()
      })
    }
    img.src = e.target.result
  }
  reader.readAsDataURL(file)
}

// 适应画布
const fitToCanvas = () => {
  if (!imageData.value) return
  const img = imageData.value
  const fitScale = Math.min(CANVAS_SIZE / img.width, CANVAS_SIZE / img.height, 1)
  scale.value = fitScale
  imagePosition.value = {
    x: (CANVAS_SIZE - img.width * fitScale) / 2,
    y: (CANVAS_SIZE - img.height * fitScale) / 2
  }
  updateCanvas()
}

// 重置变换
const resetTransform = () => {
  scale.value = 1
  imagePosition.value = { x: 0, y: 0 }
  cropSize.value = 200
  updateCanvas()
}

// 更新画布
const updateCanvas = () => {
  if (!canvasRef.value || !imageData.value) return

  try {
    const canvas = canvasRef.value
    const ctx = canvas.getContext('2d')

    // 设置画布大小
    canvas.width = CANVAS_SIZE
    canvas.height = CANVAS_SIZE

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
    ctx.fillRect(
      cropX + cropSize.value,
      cropY,
      canvas.width - cropX - cropSize.value,
      cropSize.value
    )

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

    // 预览始终输出为 200x200
    const previewSize = 200
    previewCanvas.width = previewSize
    previewCanvas.height = previewSize

    const canvas = canvasRef.value
    if (!canvas) return

    const cropX = (canvas.width - cropSize.value) / 2
    const cropY = (canvas.height - cropSize.value) / 2

    // 从主画布复制裁剪区域并缩放到预览尺寸
    const mainCtx = canvas.getContext('2d')
    const croppedImageData = mainCtx.getImageData(cropX, cropY, cropSize.value, cropSize.value)

    const tempCanvas = document.createElement('canvas')
    tempCanvas.width = cropSize.value
    tempCanvas.height = cropSize.value
    tempCanvas.getContext('2d').putImageData(croppedImageData, 0, 0)

    previewCtx.clearRect(0, 0, previewSize, previewSize)
    previewCtx.drawImage(
      tempCanvas,
      0,
      0,
      cropSize.value,
      cropSize.value,
      0,
      0,
      previewSize,
      previewSize
    )
  } catch (error) {
    console.error('预览更新失败:', error)
  }
}

// 拖拽开始
const startDrag = e => {
  isDragging.value = true
  dragStart.value = {
    x: e.offsetX - imagePosition.value.x,
    y: e.offsetY - imagePosition.value.y
  }
}

// 拖拽中
const onDrag = e => {
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

// 鼠标滚轮缩放
const onWheel = e => {
  const delta = e.deltaY > 0 ? -0.05 : 0.05
  const newScale = Math.min(5, Math.max(0.05, scale.value + delta))

  // 以鼠标位置为中心缩放
  const rect = canvasRef.value.getBoundingClientRect()
  const mouseX = e.clientX - rect.left
  const mouseY = e.clientY - rect.top

  const ratio = newScale / scale.value
  imagePosition.value = {
    x: mouseX - (mouseX - imagePosition.value.x) * ratio,
    y: mouseY - (mouseY - imagePosition.value.y) * ratio
  }

  scale.value = newScale
  updateCanvas()
}

// 触摸事件处理
const handleTouchStart = e => {
  if (e.touches.length === 1) {
    const touch = e.touches[0]
    const rect = canvasRef.value.getBoundingClientRect()
    isDragging.value = true
    dragStart.value = {
      x: touch.clientX - rect.left - imagePosition.value.x,
      y: touch.clientY - rect.top - imagePosition.value.y
    }
  }
}

const handleTouchMove = e => {
  if (!isDragging.value || e.touches.length !== 1) return
  const touch = e.touches[0]
  const rect = canvasRef.value.getBoundingClientRect()
  imagePosition.value = {
    x: touch.clientX - rect.left - dragStart.value.x,
    y: touch.clientY - rect.top - dragStart.value.y
  }
  updateCanvas()
}

// 确认裁剪
const handleConfirm = async () => {
  if (!canvasRef.value || !imageData.value) return

  uploading.value = true
  try {
    // 从原图高精度裁剪，输出 200x200
    const outputSize = 200
    const outputCanvas = document.createElement('canvas')
    outputCanvas.width = outputSize
    outputCanvas.height = outputSize
    const outCtx = outputCanvas.getContext('2d')

    // 计算裁剪区域在原图上的位置
    const cropX = (CANVAS_SIZE - cropSize.value) / 2
    const cropY = (CANVAS_SIZE - cropSize.value) / 2

    const srcX = (cropX - imagePosition.value.x) / scale.value
    const srcY = (cropY - imagePosition.value.y) / scale.value
    const srcSize = cropSize.value / scale.value

    outCtx.drawImage(imageData.value, srcX, srcY, srcSize, srcSize, 0, 0, outputSize, outputSize)

    // 将canvas转换为blob
    const blob = await new Promise(resolve => {
      outputCanvas.toBlob(resolve, 'image/jpeg', 0.9)
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
  cropSize.value = 200
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
        min-width: 55px;
      }

      .scale-label {
        min-width: 50px;
        text-align: right;
        font-size: 12px;
        color: #909399;
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
