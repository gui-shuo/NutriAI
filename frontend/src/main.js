import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { ElMessage } from 'element-plus'
import App from './App.vue'
import router from './router'

// 样式导入
import './styles/main.scss'
import 'element-plus/theme-chalk/dark/css-vars.css'

// Patch ElMessage globally — all imports share the same object reference,
// so every ElMessage call across all files will get showClose + grouping.
;['success', 'error', 'warning', 'info'].forEach(type => {
  const orig = ElMessage[type]
  ElMessage[type] = (options) => {
    if (typeof options === 'string') options = { message: options }
    return orig({ showClose: true, grouping: true, offset: 0, ...options })
  }
})

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)

app.mount('#app')
