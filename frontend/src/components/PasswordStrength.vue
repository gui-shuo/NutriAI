<template>
  <div v-if="password" class="password-strength">
    <div class="strength-bar">
      <div class="strength-fill" :class="strengthClass" :style="{ width: strengthWidth }" />
    </div>
    <div class="strength-text">
      <span :class="strengthClass">{{ strengthText }}</span>
      <span class="strength-tips">{{ strengthTips }}</span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  password: {
    type: String,
    default: ''
  }
})

// 计算密码强度
const strength = computed(() => {
  const pwd = props.password
  if (!pwd) return 0

  let score = 0

  // 长度评分
  if (pwd.length >= 6) score += 1
  if (pwd.length >= 10) score += 1
  if (pwd.length >= 14) score += 1

  // 包含小写字母
  if (/[a-z]/.test(pwd)) score += 1

  // 包含大写字母
  if (/[A-Z]/.test(pwd)) score += 1

  // 包含数字
  if (/\d/.test(pwd)) score += 1

  // 包含特殊字符
  if (/[!@#$%^&*(),.?":{}|<>]/.test(pwd)) score += 1

  return score
})

// 强度等级
const strengthLevel = computed(() => {
  const s = strength.value
  if (s <= 2) return 'weak'
  if (s <= 4) return 'medium'
  if (s <= 6) return 'strong'
  return 'very-strong'
})

// 强度样式类
const strengthClass = computed(() => {
  return `strength-${strengthLevel.value}`
})

// 强度宽度
const strengthWidth = computed(() => {
  const s = strength.value
  return `${Math.min((s / 7) * 100, 100)}%`
})

// 强度文本
const strengthText = computed(() => {
  const level = strengthLevel.value
  const textMap = {
    weak: '弱',
    medium: '中',
    strong: '强',
    'very-strong': '非常强'
  }
  return textMap[level] || ''
})

// 强度提示
const strengthTips = computed(() => {
  const pwd = props.password
  if (!pwd) return ''

  const tips = []

  if (pwd.length < 6) {
    tips.push('至少6个字符')
  }
  if (!/[a-z]/.test(pwd)) {
    tips.push('需要小写字母')
  }
  if (!/[A-Z]/.test(pwd)) {
    tips.push('需要大写字母')
  }
  if (!/\d/.test(pwd)) {
    tips.push('需要数字')
  }

  return tips.length > 0 ? `建议: ${tips.join('、')}` : '密码强度良好'
})
</script>

<style scoped lang="scss">
.password-strength {
  margin-top: 8px;

  .strength-bar {
    height: 6px;
    background-color: #64748B;
    border: 1px solid #E2E8F0;
    border-radius: 12px;
    overflow: hidden;

    .strength-fill {
      height: 100%;
      transition: all 0.3s ease;
      border-radius: 12px;

      &.strength-weak {
        background-color: #EF4444;
      }

      &.strength-medium {
        background-color: #e6a23c;
      }

      &.strength-strong {
        background-color: #67c23a;
      }

      &.strength-very-strong {
        background-color: #10B981;
      }
    }
  }

  .strength-text {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: 4px;
    font-family: 'Inter', sans-serif;
    font-size: 12px;

    span {
      &.strength-weak {
        color: #EF4444;
      }

      &.strength-medium {
        color: #e6a23c;
      }

      &.strength-strong {
        color: #67c23a;
      }

      &.strength-very-strong {
        color: #10B981;
      }
    }

    .strength-tips {
      color: #0F172A;
      opacity: 0.6;
    }
  }
}
</style>
