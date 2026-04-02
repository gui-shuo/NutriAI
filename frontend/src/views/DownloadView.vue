<template>
  <div class="download-page">
    <!-- Hero -->
    <section class="download-hero">
      <div class="hero-content">
        <div class="app-icon">🥗</div>
        <h1>NutriAI 智膳</h1>
        <p class="subtitle">AI驱动的智能营养饮食规划平台</p>
        <div v-if="latestVersion" class="latest-info">
          <el-button type="success" size="large" @click="downloadVersion(latestVersion)" :loading="downloading">
            <el-icon><Download /></el-icon>
            下载最新版 v{{ latestVersion.versionName }}
          </el-button>
          <div class="version-meta">
            <span>{{ formatSize(latestVersion.fileSize) }}</span>
            <span>·</span>
            <span>{{ formatDate(latestVersion.createdAt) }}</span>
            <span>·</span>
            <span>{{ latestVersion.downloadCount }} 次下载</span>
          </div>
        </div>
        <div v-else class="no-version">
          <el-empty description="暂无可下载版本，敬请期待" :image-size="80" />
        </div>
        <div class="alt-access">
          <p>📱 也可以通过 <a href="/h5/" target="_blank">H5移动版</a> 直接在浏览器中使用</p>
        </div>
      </div>
    </section>

    <!-- Update Log -->
    <section v-if="latestVersion && latestVersion.description" class="update-log">
      <h2>📋 更新日志</h2>
      <div class="log-content">
        <pre>{{ latestVersion.description }}</pre>
      </div>
    </section>

    <!-- Version History -->
    <section class="version-history">
      <h2>📦 历史版本</h2>
      <div v-if="versions.length > 0" class="version-list">
        <div v-for="v in versions" :key="v.id" class="version-card" :class="{ latest: v.isLatest }">
          <div class="version-left">
            <div class="version-name">
              v{{ v.versionName }}
              <el-tag v-if="v.isLatest" type="success" size="small">最新</el-tag>
            </div>
            <div class="version-desc" v-if="v.description">{{ v.description }}</div>
            <div class="version-info">
              <span>{{ formatSize(v.fileSize) }}</span>
              <span>·</span>
              <span>{{ formatDate(v.createdAt) }}</span>
              <span>·</span>
              <span>{{ v.downloadCount }} 次下载</span>
            </div>
          </div>
          <div class="version-right">
            <el-button type="primary" size="small" @click="downloadVersion(v)" :loading="downloading">
              <el-icon><Download /></el-icon> 下载
            </el-button>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无历史版本" />
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Download } from '@element-plus/icons-vue'
import { appVersionApi } from '@/services/appVersion'
import { ElMessage } from 'element-plus'

const latestVersion = ref(null)
const versions = ref([])
const downloading = ref(false)

const loadData = async () => {
  try {
    const [latestRes, listRes] = await Promise.all([
      appVersionApi.getLatest(),
      appVersionApi.getList()
    ])
    latestVersion.value = latestRes.data?.data
    const page = listRes.data?.data
    versions.value = page?.content || page || []
  } catch (e) {
    console.error('Failed to load versions', e)
  }
}

const downloadVersion = async (version) => {
  downloading.value = true
  try {
    const res = await appVersionApi.getDownloadUrl(version.id)
    const url = res.data?.data
    if (url) {
      window.open(url, '_blank')
      setTimeout(loadData, 1000)
    }
  } catch (e) {
    ElMessage.error('下载失败')
  } finally {
    downloading.value = false
  }
}

const formatSize = (bytes) => {
  if (!bytes) return '未知大小'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return d.toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' })
}

onMounted(loadData)
</script>

<style scoped>
.download-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
  font-family: 'Inter', sans-serif;
  background: #FAFAFA;
  min-height: 100vh;
}

.download-hero {
  text-align: center;
  padding: 60px 20px 40px;
  background: #FFFFFF;
  border-radius: 16px;
  border: 1px solid #E2E8F0;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -2px rgba(0, 0, 0, 0.1);
  margin-bottom: 30px;
  position: relative;
}

.app-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.download-hero h1 {
  font-size: 32px;
  font-family: 'Calistoga', 'Inter', sans-serif;
  color: #0F172A;
  margin: 0 0 8px;
}

.subtitle {
  color: #0F172A;
  font-size: 16px;
  margin-bottom: 24px;
  opacity: 0.6;
}

.latest-info {
  margin-top: 20px;
}

.latest-info :deep(.el-button--success) {
  background: linear-gradient(135deg, #10B981, #34D399);
  border: none;
  border-radius: 12px;
  box-shadow: 0 4px 6px -1px rgba(16, 185, 129, 0.25);
  font-family: 'Inter', sans-serif;
  font-weight: 600;
  transition: box-shadow 0.2s, transform 0.2s;
}

.latest-info :deep(.el-button--success:hover) {
  transform: translateY(-1px);
  box-shadow: 0 10px 15px -3px rgba(16, 185, 129, 0.3);
}

.version-meta {
  margin-top: 12px;
  color: #0F172A;
  opacity: 0.5;
  font-size: 13px;
  display: flex;
  gap: 6px;
  justify-content: center;
}

.alt-access {
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #E2E8F0;
  color: #0F172A;
  font-size: 14px;
}
.alt-access a {
  color: #10B981;
  font-weight: 600;
  text-decoration: none;
}
.alt-access a:hover {
  text-decoration: underline;
  text-decoration-color: #34D399;
}

.update-log, .version-history {
  margin-bottom: 30px;
}

.update-log h2, .version-history h2 {
  font-size: 20px;
  font-family: 'Calistoga', 'Inter', sans-serif;
  color: #0F172A;
  margin-bottom: 16px;
}

.log-content {
  background: #F1F5F9;
  border-radius: 12px;
  border: 1px solid #E2E8F0;
  box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
  padding: 16px;
}

.log-content pre {
  margin: 0;
  white-space: pre-wrap;
  font-family: 'JetBrains Mono', monospace;
  color: #0F172A;
  font-size: 14px;
  line-height: 1.6;
}

.version-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.version-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  background: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 12px;
  box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
  transition: box-shadow 0.2s, transform 0.2s;
}

.version-card:hover {
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -2px rgba(0, 0, 0, 0.1);
  transform: translateY(-1px);
}

.version-card.latest {
  border-color: #10B981;
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.03), rgba(77, 124, 255, 0.06));
  box-shadow: 0 4px 6px -1px rgba(16, 185, 129, 0.1), 0 2px 4px -2px rgba(16, 185, 129, 0.1);
}

.version-name {
  font-weight: 600;
  font-size: 16px;
  font-family: 'Calistoga', 'Inter', sans-serif;
  color: #0F172A;
  display: flex;
  align-items: center;
  gap: 8px;
}

.version-desc {
  color: #0F172A;
  opacity: 0.6;
  font-size: 13px;
  margin-top: 4px;
  max-width: 500px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.version-info {
  margin-top: 6px;
  color: #0F172A;
  opacity: 0.45;
  font-size: 12px;
  display: flex;
  gap: 6px;
}
</style>
