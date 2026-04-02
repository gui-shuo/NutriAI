<template>
  <div class="quick-actions-container">
    <div class="actions-header">
      <h3>快捷操作</h3>
      <el-button
        v-if="expandable"
        :icon="expanded ? ArrowUp : ArrowDown"
        link
        size="small"
        @click="toggleExpand"
      >
        {{ expanded ? '收起' : '展开' }}
      </el-button>
    </div>

    <transition name="expand">
      <div v-show="expanded" class="actions-content">
        <!-- 常用问题 -->
        <div class="action-section">
          <h4 class="section-title">
            <el-icon><QuestionFilled /></el-icon>
            常用问题
          </h4>
          <div class="action-buttons">
            <el-button
              v-for="question in commonQuestions"
              :key="question.id"
              class="action-btn"
              :disabled="disabled"
              @click="handleQuickAction(question.content)"
            >
              <span class="btn-icon">{{ question.icon }}</span>
              <span class="btn-text">{{ question.label }}</span>
            </el-button>
          </div>
        </div>

        <!-- 食物分析 -->
        <div class="action-section">
          <h4 class="section-title">
            <el-icon><Apple /></el-icon>
            食物分析
          </h4>
          <div class="action-buttons">
            <el-button
              v-for="analysis in foodAnalysis"
              :key="analysis.id"
              class="action-btn"
              :disabled="disabled"
              @click="handleQuickAction(analysis.content)"
            >
              <span class="btn-icon">{{ analysis.icon }}</span>
              <span class="btn-text">{{ analysis.label }}</span>
            </el-button>
          </div>
        </div>

        <!-- 饮食计划 -->
        <div class="action-section">
          <h4 class="section-title">
            <el-icon><Calendar /></el-icon>
            饮食计划
          </h4>
          <div class="action-buttons">
            <el-button
              v-for="plan in dietPlans"
              :key="plan.id"
              class="action-btn"
              :disabled="disabled"
              @click="handleQuickAction(plan.content)"
            >
              <span class="btn-icon">{{ plan.icon }}</span>
              <span class="btn-text">{{ plan.label }}</span>
            </el-button>
          </div>
        </div>

        <!-- 营养建议 -->
        <div class="action-section">
          <h4 class="section-title">
            <el-icon><MagicStick /></el-icon>
            营养建议
          </h4>
          <div class="action-buttons">
            <el-button
              v-for="advice in healthAdvice"
              :key="advice.id"
              class="action-btn"
              :disabled="disabled"
              @click="handleQuickAction(advice.content)"
            >
              <span class="btn-icon">{{ advice.icon }}</span>
              <span class="btn-text">{{ advice.label }}</span>
            </el-button>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import {
  QuestionFilled,
  Apple,
  Calendar,
  MagicStick,
  ArrowUp,
  ArrowDown
} from '@element-plus/icons-vue'

const props = defineProps({
  disabled: {
    type: Boolean,
    default: false
  },
  expandable: {
    type: Boolean,
    default: true
  },
  defaultExpanded: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['action'])

const expanded = ref(props.defaultExpanded)

// 常用问题
const commonQuestions = [
  { id: 1, icon: '👋', label: '你能帮我做什么？', content: '你能帮我做什么？' },
  { id: 2, icon: '📊', label: '如何查看营养数据？', content: '如何查看我的营养摄入数据？' },
  { id: 3, icon: '🎯', label: '如何设置饮食目标？', content: '如何设置我的饮食目标？' },
  { id: 4, icon: '💡', label: '营养饮食建议', content: '给我一些营养饮食的建议' }
]

// 食物分析
const foodAnalysis = [
  { id: 1, icon: '🍎', label: '分析苹果', content: '帮我分析一下苹果的营养成分' },
  { id: 2, icon: '🥗', label: '分析沙拉', content: '帮我分析一下蔬菜沙拉的营养价值' },
  { id: 3, icon: '🍗', label: '分析鸡胸肉', content: '鸡胸肉有什么营养？适合减肥吃吗？' },
  { id: 4, icon: '🥛', label: '对比牛奶', content: '纯牛奶和酸奶哪个更好？' }
]

// 饮食计划
const dietPlans = [
  { id: 1, icon: '📅', label: '今日饮食', content: '帮我制定今天的饮食计划' },
  { id: 2, icon: '🗓️', label: '一周计划', content: '帮我制定一周的营养饮食计划' },
  { id: 3, icon: '💪', label: '增肌计划', content: '帮我制定增肌饮食计划' },
  { id: 4, icon: '⚖️', label: '减脂计划', content: '帮我制定减脂饮食计划' }
]

// 营养建议
const healthAdvice = [
  { id: 1, icon: '🏃', label: '运动建议', content: '给我一些适合的运动建议' },
  { id: 2, icon: '💧', label: '饮水提醒', content: '每天应该喝多少水？' },
  { id: 3, icon: '😴', label: '睡眠建议', content: '如何改善睡眠质量？' },
  { id: 4, icon: '🧘', label: '减压方法', content: '有什么减压的方法推荐吗？' }
]

// 切换展开/收起
const toggleExpand = () => {
  expanded.value = !expanded.value
}

// 处理快捷操作
const handleQuickAction = content => {
  emit('action', content)
}

// 暴露方法
defineExpose({
  expand: () => {
    expanded.value = true
  },
  collapse: () => {
    expanded.value = false
  }
})
</script>

<style scoped>
.quick-actions-container {
  padding: 16px 20px;
  background: #fdfbf7;
  border-top: 2px dashed #2d2d2d;
}

.actions-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.actions-header h3 {
  font-size: 16px;
  color: #2d2d2d;
  margin: 0;
  font-family: 'Kalam', 'ZCOOL KuaiLe', cursive;
}

.actions-content {
  overflow: hidden;
}

.action-section {
  margin-bottom: 20px;
}

.action-section:last-child {
  margin-bottom: 0;
}

.section-title {
  font-size: 14px;
  color: #2d2d2d;
  margin: 0 0 12px 0;
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 500;
  font-family: 'Patrick Hand', cursive;
}

.section-title .el-icon {
  color: #2d5da1;
}

.action-buttons {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 8px;
}

.action-btn {
  width: 100%;
  height: auto;
  padding: 12px 16px;
  border: 2px solid #2d2d2d;
  border-radius: 255px 15px 225px 15px / 15px 225px 15px 255px;
  background: #fdfbf7;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  gap: 8px;
  text-align: left;
  justify-content: flex-start;
}

.action-btn:hover:not(:disabled) {
  border-color: #2d2d2d;
  background: #fff9c4;
  transform: translate(-2px, -2px);
  box-shadow: 4px 4px 0px 0px #2d2d2d;
}

.action-btn:active:not(:disabled) {
  transform: translate(0, 0);
  box-shadow: none;
}

.action-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-icon {
  font-size: 20px;
  flex-shrink: 0;
}

.btn-text {
  font-size: 13px;
  color: #2d2d2d;
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-family: 'Patrick Hand', cursive;
}

.action-btn:hover:not(:disabled) .btn-text {
  color: #2d5da1;
}

/* 展开/收起动画 */
.expand-enter-active,
.expand-leave-active {
  transition: all 0.3s ease-out;
  max-height: 1000px;
}

.expand-enter-from,
.expand-leave-to {
  max-height: 0;
  opacity: 0;
}

/* 响应式 */
@media (max-width: 1200px) {
  .action-buttons {
    grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  }
}

@media (max-width: 768px) {
  .quick-actions-container {
    padding: 12px 16px;
  }

  .action-buttons {
    grid-template-columns: repeat(2, 1fr);
    gap: 6px;
  }

  .action-btn {
    padding: 10px 12px;
  }

  .btn-text {
    font-size: 12px;
  }
}

@media (max-width: 480px) {
  .action-buttons {
    grid-template-columns: 1fr;
  }
}
</style>
