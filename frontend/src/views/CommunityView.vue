<template>
  <div class="community-view">
    <!-- 顶部导航 -->
    <nav class="top-nav">
      <div class="nav-inner">
        <div class="nav-left">
          <el-button text @click="router.push('/')">
            <el-icon><ArrowLeft /></el-icon>
            返回首页
          </el-button>
          <h2 class="page-title">🌿 营养圈</h2>
        </div>
        <div class="nav-right">
          <el-button type="primary" round @click="showCreateDialog = true">
            <el-icon><Plus /></el-icon>
            发布动态
          </el-button>
        </div>
      </div>
    </nav>

    <main class="main-area">
      <el-alert type="info" :closable="true" show-icon style="margin-bottom: 12px">
        <template #title>
          社区内容为用户个人观点，不代表平台立场，请自行甄别信息的准确性。
          <router-link to="/legal/disclaimer" style="color:#409eff">详细声明</router-link>
        </template>
      </el-alert>
      <!-- 分类标签 -->
      <div class="category-bar">
        <div
          v-for="cat in allCategories"
          :key="cat.code"
          class="cat-tag"
          :class="{ active: activeCategory === cat.code }"
          @click="switchCategory(cat.code)"
        >
          <span class="cat-icon">{{ cat.icon }}</span>
          <span>{{ cat.name }}</span>
        </div>
      </div>

      <!-- 帖子列表 -->
      <div class="feed-container">
        <div v-if="loading && posts.length === 0" class="loading-box">
          <el-skeleton :rows="4" animated />
        </div>

        <div v-else-if="posts.length === 0" class="empty-box">
          <el-empty description="暂无动态，快来发布第一条吧 🌱" />
        </div>

        <div v-else class="post-list">
          <div
            v-for="post in posts"
            :key="post.id"
            class="post-card"
            :class="{ pinned: post.pinned }"
          >
            <div class="post-header">
              <div class="user-info" @click="viewUserPosts(post.userId)">
                <el-avatar :size="36" :src="post.avatarUrl">
                  {{ (post.username || '').charAt(0) }}
                </el-avatar>
                <div class="user-meta">
                  <span class="username">{{ post.username }}</span>
                  <span class="time">{{ formatTime(post.createdAt) }}</span>
                </div>
              </div>
              <div class="post-tags">
                <el-tag v-if="post.pinned" size="small" type="danger" effect="dark">置顶</el-tag>
                <el-tag size="small" :color="getCategoryColor(post.category)" effect="dark" style="border:none;color:#fff">
                  {{ post.category }}
                </el-tag>
              </div>
            </div>

            <div class="post-content" @click="openPostDetail(post)">
              <p class="content-text">{{ post.content }}</p>

              <!-- 图片网格 -->
              <div v-if="getImages(post.images).length" class="image-grid" :class="'cols-' + Math.min(getImages(post.images).length, 3)">
                <div
                  v-for="(img, idx) in getImages(post.images).slice(0, 9)"
                  :key="idx"
                  class="image-item"
                  @click.stop="previewImages(getImages(post.images), idx)"
                >
                  <el-image :src="img" fit="cover" lazy />
                  <div v-if="idx === 8 && getImages(post.images).length > 9" class="more-overlay">
                    +{{ getImages(post.images).length - 9 }}
                  </div>
                </div>
              </div>

              <!-- 视频 -->
              <div v-if="post.videoUrl" class="video-box">
                <video :src="post.videoUrl" controls preload="metadata" />
              </div>
            </div>

            <div class="post-actions">
              <div class="action-btn" :class="{ active: post.liked }" @click="handleLike(post)">
                <el-icon><component :is="post.liked ? 'StarFilled' : 'Star'" /></el-icon>
                <span>{{ post.likesCount || 0 }}</span>
              </div>
              <div class="action-btn" @click="openPostDetail(post)">
                <el-icon><ChatDotRound /></el-icon>
                <span>{{ post.commentsCount || 0 }}</span>
              </div>
              <div v-if="post.userId === currentUserId" class="action-btn delete-btn" @click.stop="handleDeletePost(post)">
                <el-icon><Delete /></el-icon>
              </div>
            </div>
          </div>
        </div>

        <!-- 加载更多 -->
        <div v-if="hasMore" class="load-more">
          <el-button :loading="loading" text @click="loadMore">加载更多</el-button>
        </div>
      </div>
    </main>

    <!-- 发布动态对话框 -->
    <el-dialog v-model="showCreateDialog" title="发布动态" width="560px" :close-on-click-modal="false" class="create-dialog">
      <el-form label-position="top">
        <el-form-item label="分类">
          <div class="category-selector">
            <div
              v-for="cat in PostCategories"
              :key="cat.code"
              class="cat-option"
              :class="{ active: newPost.category === cat.code }"
              @click="newPost.category = cat.code"
            >
              <span>{{ cat.icon }} {{ cat.name }}</span>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="内容">
          <el-input
            v-model="newPost.content"
            type="textarea"
            :rows="4"
            maxlength="1000"
            show-word-limit
            placeholder="分享你的饮食心得、营养知识..."
          />
        </el-form-item>
        <el-form-item label="图片（最多9张）">
          <div class="upload-grid">
            <div v-for="(img, idx) in newPost.images" :key="idx" class="upload-preview">
              <el-image :src="img" fit="cover" />
              <div class="remove-btn" @click="newPost.images.splice(idx, 1)">
                <el-icon><Close /></el-icon>
              </div>
            </div>
            <div v-if="newPost.images.length < 9" class="upload-btn" @click="triggerImageUpload">
              <el-icon :size="24"><Plus /></el-icon>
              <span>添加图片</span>
            </div>
          </div>
          <input ref="imageInputRef" type="file" accept="image/*" multiple hidden @change="handleImageSelect" />
        </el-form-item>
        <el-form-item label="视频（可选，最多1个）">
          <div v-if="newPost.videoUrl" class="video-preview">
            <video :src="newPost.videoUrl" controls style="max-height:200px;width:100%;border-radius:8px" />
            <el-button type="danger" size="small" @click="newPost.videoUrl = ''">移除视频</el-button>
          </div>
          <el-button v-else :loading="videoUploading" @click="triggerVideoUpload">
            <el-icon><VideoCamera /></el-icon>
            上传视频
          </el-button>
          <input ref="videoInputRef" type="file" accept="video/mp4,video/webm,video/quicktime" hidden @change="handleVideoSelect" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="publishing" @click="publishPost">发布</el-button>
      </template>
    </el-dialog>

    <!-- 帖子详情对话框 -->
    <el-dialog v-model="showDetailDialog" :title="null" width="640px" class="detail-dialog" top="5vh">
      <template v-if="detailPost">
        <div class="detail-header">
          <div class="user-info">
            <el-avatar :size="40" :src="detailPost.avatarUrl">
              {{ (detailPost.username || '').charAt(0) }}
            </el-avatar>
            <div class="user-meta">
              <div class="username-row">
                <span class="username">{{ detailPost.username }}</span>
                <el-tag size="small" :color="getCategoryColor(detailPost.category)" effect="dark" style="border:none;color:#fff;margin-left:8px">
                  {{ detailPost.category }}
                </el-tag>
              </div>
              <span class="time">{{ formatTime(detailPost.createdAt) }}</span>
            </div>
          </div>
          <el-button
            v-if="detailPost.userId !== currentUserId"
            :type="detailPost._following ? 'default' : 'primary'"
            size="small"
            round
            @click="handleFollow(detailPost)"
          >
            {{ detailPost._following ? '已关注' : '+ 关注' }}
          </el-button>
        </div>

        <div class="detail-content">
          <p>{{ detailPost.content }}</p>
          <div v-if="getImages(detailPost.images).length" class="image-grid" :class="'cols-' + Math.min(getImages(detailPost.images).length, 3)">
            <div
              v-for="(img, idx) in getImages(detailPost.images)"
              :key="idx"
              class="image-item"
              @click="previewImages(getImages(detailPost.images), idx)"
            >
              <el-image :src="img" fit="cover" lazy />
            </div>
          </div>
          <div v-if="detailPost.videoUrl" class="video-box">
            <video :src="detailPost.videoUrl" controls preload="metadata" />
          </div>
        </div>

        <div class="detail-actions">
          <div class="action-btn" :class="{ active: detailPost.liked }" @click="handleLikeDetail">
            <el-icon><component :is="detailPost.liked ? 'StarFilled' : 'Star'" /></el-icon>
            <span>{{ detailPost.likesCount || 0 }} 赞</span>
          </div>
          <span class="comment-count">💬 {{ detailPost.commentsCount || 0 }} 条评论</span>
        </div>

        <!-- 评论区 -->
        <div class="comments-section">
          <div class="comment-input">
            <el-input
              v-model="commentContent"
              :placeholder="replyTarget ? `回复 @${replyTarget.username}` : '写评论...'"
              maxlength="500"
              @keyup.enter="submitComment"
            >
              <template #append>
                <el-button :loading="commentSubmitting" @click="submitComment">发送</el-button>
              </template>
            </el-input>
            <el-tag v-if="replyTarget" closable size="small" @close="replyTarget = null" style="margin-top:4px">
              回复 @{{ replyTarget.username }}
            </el-tag>
          </div>

          <div class="comment-list">
            <div v-if="commentsLoading" style="text-align:center;padding:20px">
              <el-skeleton :rows="2" animated />
            </div>
            <div v-else-if="comments.length === 0" class="no-comments">
              还没有评论，快来抢沙发 🛋️
            </div>
            <div v-for="comment in comments" :key="comment.id" class="comment-item">
              <el-avatar :size="28" :src="comment.avatarUrl">
                {{ (comment.username || '').charAt(0) }}
              </el-avatar>
              <div class="comment-body">
                <div class="comment-meta">
                  <span class="comment-user">{{ comment.username }}</span>
                  <span v-if="comment.replyToUsername" class="reply-to">
                    回复 <b>@{{ comment.replyToUsername }}</b>
                  </span>
                  <span class="comment-time">{{ formatTime(comment.createdAt) }}</span>
                </div>
                <p class="comment-text">{{ comment.content }}</p>
                <div class="comment-actions">
                  <span class="act-btn" :class="{ active: comment.liked }" @click="handleCommentLike(comment)">
                    <el-icon size="12"><component :is="comment.liked ? 'StarFilled' : 'Star'" /></el-icon>
                    {{ comment.likesCount || 0 }}
                  </span>
                  <span class="act-btn" @click="setReplyTarget(comment)">回复</span>
                  <span v-if="comment.userId === currentUserId" class="act-btn del" @click="handleDeleteComment(comment)">删除</span>
                </div>
              </div>
            </div>
            <div v-if="commentsHasMore" class="load-more" style="padding:8px 0">
              <el-button text :loading="commentsLoading" @click="loadMoreComments">加载更多评论</el-button>
            </div>
          </div>
        </div>
      </template>
    </el-dialog>

    <!-- 图片预览 -->
    <el-image-viewer
      v-if="previewVisible"
      :url-list="previewList"
      :initial-index="previewIndex"
      @close="previewVisible = false"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft, Plus, Close, Delete, ChatDotRound,
  Star, StarFilled, VideoCamera
} from '@element-plus/icons-vue'
import {
  getFeed, createPost, deletePost, uploadMedia,
  getComments, addComment, deleteComment,
  toggleLike, toggleFollow, getFollowStatus,
  PostCategories
} from '@/services/community'

const router = useRouter()
const authStore = useAuthStore()
const currentUserId = computed(() => authStore.user?.id)

// Feed state
const posts = ref([])
const loading = ref(false)
const currentPage = ref(0)
const hasMore = ref(true)
const activeCategory = ref('')

const allCategories = [
  { code: '', name: '全部', icon: '🔥' },
  ...PostCategories
]

// Create post state
const showCreateDialog = ref(false)
const publishing = ref(false)
const videoUploading = ref(false)
const imageInputRef = ref(null)
const videoInputRef = ref(null)
const newPost = reactive({
  content: '',
  category: '饮食打卡',
  images: [],
  videoUrl: ''
})

// Detail state
const showDetailDialog = ref(false)
const detailPost = ref(null)
const comments = ref([])
const commentsLoading = ref(false)
const commentContent = ref('')
const commentSubmitting = ref(false)
const replyTarget = ref(null)
const commentsPage = ref(0)
const commentsHasMore = ref(true)

// Image preview
const previewVisible = ref(false)
const previewList = ref([])
const previewIndex = ref(0)

function getCategoryColor(cat) {
  const found = PostCategories.find(c => c.code === cat)
  return found ? found.color : '#909399'
}

function getImages(images) {
  if (!images) return []
  if (Array.isArray(images)) return images
  try { return JSON.parse(images) } catch { return [] }
}

function formatTime(t) {
  if (!t) return ''
  const d = new Date(t)
  const now = new Date()
  const diff = (now - d) / 1000
  if (diff < 60) return '刚刚'
  if (diff < 3600) return Math.floor(diff / 60) + '分钟前'
  if (diff < 86400) return Math.floor(diff / 3600) + '小时前'
  if (diff < 604800) return Math.floor(diff / 86400) + '天前'
  return d.toLocaleDateString('zh-CN')
}

// ===== Feed =====
async function loadFeed(reset = false) {
  if (reset) {
    currentPage.value = 0
    posts.value = []
    hasMore.value = true
  }
  loading.value = true
  try {
    const params = { page: currentPage.value, size: 10 }
    if (activeCategory.value) params.category = activeCategory.value
    const res = await getFeed(params)
    const data = res.data?.data || res.data
    const content = data.content || []
    if (reset) {
      posts.value = content
    } else {
      posts.value.push(...content)
    }
    hasMore.value = !data.last
  } catch (e) {
    console.error('加载失败', e)
  } finally {
    loading.value = false
  }
}

function loadMore() {
  currentPage.value++
  loadFeed()
}

function switchCategory(code) {
  activeCategory.value = code
  loadFeed(true)
}

// ===== Create Post =====
function triggerImageUpload() { imageInputRef.value?.click() }
function triggerVideoUpload() { videoInputRef.value?.click() }

async function handleImageSelect(e) {
  const files = Array.from(e.target.files)
  const remaining = 9 - newPost.images.length
  const toUpload = files.slice(0, remaining)
  for (const file of toUpload) {
    try {
      const res = await uploadMedia(file)
      const url = res.data?.data || res.data
      newPost.images.push(url)
    } catch {
      ElMessage.error('图片上传失败')
    }
  }
  e.target.value = ''
}

async function handleVideoSelect(e) {
  const file = e.target.files[0]
  if (!file) return
  if (file.size > 50 * 1024 * 1024) {
    ElMessage.error('视频不能超过50MB')
    return
  }
  videoUploading.value = true
  try {
    const res = await uploadMedia(file)
    newPost.videoUrl = res.data?.data || res.data
  } catch {
    ElMessage.error('视频上传失败')
  } finally {
    videoUploading.value = false
    e.target.value = ''
  }
}

async function publishPost() {
  if (!newPost.content.trim()) {
    ElMessage.warning('请输入内容')
    return
  }
  publishing.value = true
  try {
    await createPost({
      content: newPost.content,
      images: JSON.stringify(newPost.images),
      videoUrl: newPost.videoUrl,
      category: newPost.category
    })
    ElMessage.success('发布成功')
    showCreateDialog.value = false
    newPost.content = ''
    newPost.images = []
    newPost.videoUrl = ''
    loadFeed(true)
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '发布失败')
  } finally {
    publishing.value = false
  }
}

// ===== Like =====
async function handleLike(post) {
  try {
    const res = await toggleLike({ targetType: 'POST', targetId: post.id })
    const liked = res.data?.data?.liked
    post.liked = liked
    post.likesCount = liked ? (post.likesCount || 0) + 1 : Math.max((post.likesCount || 0) - 1, 0)
  } catch {
    ElMessage.error('操作失败')
  }
}

async function handleLikeDetail() {
  if (!detailPost.value) return
  try {
    const res = await toggleLike({ targetType: 'POST', targetId: detailPost.value.id })
    const liked = res.data?.data?.liked
    detailPost.value.liked = liked
    detailPost.value.likesCount = liked
      ? (detailPost.value.likesCount || 0) + 1
      : Math.max((detailPost.value.likesCount || 0) - 1, 0)
    // sync feed
    const fp = posts.value.find(p => p.id === detailPost.value.id)
    if (fp) { fp.liked = liked; fp.likesCount = detailPost.value.likesCount }
  } catch {
    ElMessage.error('操作失败')
  }
}

async function handleCommentLike(comment) {
  try {
    const res = await toggleLike({ targetType: 'COMMENT', targetId: comment.id })
    const liked = res.data?.data?.liked
    comment.liked = liked
    comment.likesCount = liked ? (comment.likesCount || 0) + 1 : Math.max((comment.likesCount || 0) - 1, 0)
  } catch {
    ElMessage.error('操作失败')
  }
}

// ===== Post Detail & Comments =====
async function openPostDetail(post) {
  detailPost.value = { ...post, _following: false }
  showDetailDialog.value = true
  commentsPage.value = 0
  comments.value = []
  commentsHasMore.value = true
  replyTarget.value = null
  commentContent.value = ''
  loadComments(true)
  // check follow
  if (post.userId !== currentUserId.value) {
    try {
      const res = await getFollowStatus(post.userId)
      detailPost.value._following = res.data?.data?.isFollowing
    } catch { /* ignore */ }
  }
}

async function loadComments(reset = false) {
  if (reset) { commentsPage.value = 0; comments.value = []; commentsHasMore.value = true }
  commentsLoading.value = true
  try {
    const res = await getComments(detailPost.value.id, { page: commentsPage.value, size: 20 })
    const data = res.data?.data || res.data
    const content = data.content || []
    if (reset) {
      comments.value = content
    } else {
      comments.value.push(...content)
    }
    commentsHasMore.value = !data.last
  } catch {
    // ignore
  } finally {
    commentsLoading.value = false
  }
}

function loadMoreComments() {
  commentsPage.value++
  loadComments()
}

function setReplyTarget(comment) {
  replyTarget.value = comment
}

async function submitComment() {
  const text = commentContent.value.trim()
  if (!text) return
  commentSubmitting.value = true
  try {
    await addComment(detailPost.value.id, {
      content: text,
      parentId: replyTarget.value?.id || null
    })
    commentContent.value = ''
    replyTarget.value = null
    loadComments(true)
    detailPost.value.commentsCount = (detailPost.value.commentsCount || 0) + 1
    const fp = posts.value.find(p => p.id === detailPost.value.id)
    if (fp) fp.commentsCount = detailPost.value.commentsCount
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '评论失败')
  } finally {
    commentSubmitting.value = false
  }
}

async function handleDeleteComment(comment) {
  try {
    await ElMessageBox.confirm('确定删除该评论？', '提示', { type: 'warning' })
    await deleteComment(comment.id)
    comments.value = comments.value.filter(c => c.id !== comment.id)
    detailPost.value.commentsCount = Math.max((detailPost.value.commentsCount || 0) - 1, 0)
    const fp = posts.value.find(p => p.id === detailPost.value.id)
    if (fp) fp.commentsCount = detailPost.value.commentsCount
  } catch { /* cancel */ }
}

async function handleDeletePost(post) {
  try {
    await ElMessageBox.confirm('确定删除该动态？', '提示', { type: 'warning' })
    await deletePost(post.id)
    posts.value = posts.value.filter(p => p.id !== post.id)
    ElMessage.success('已删除')
  } catch { /* cancel */ }
}

// ===== Follow =====
async function handleFollow(post) {
  try {
    const res = await toggleFollow(post.userId)
    post._following = res.data?.data?.followed
  } catch {
    ElMessage.error('操作失败')
  }
}

// ===== Image Preview =====
function previewImages(list, idx) {
  previewList.value = list
  previewIndex.value = idx
  previewVisible.value = true
}

function viewUserPosts(userId) {
  // future: navigate to user profile
}

onMounted(() => loadFeed(true))
</script>

<style scoped>
.community-view {
  min-height: 100vh;
  background: #f5f7fa;
}

/* 顶部导航 */
.top-nav {
  background: #fff;
  box-shadow: 0 1px 4px rgba(0,0,0,.08);
  position: sticky;
  top: 0;
  z-index: 50;
}
.nav-inner {
  max-width: 800px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  height: 56px;
}
.nav-left {
  display: flex;
  align-items: center;
  gap: 8px;
}
.page-title {
  font-size: 18px;
  font-weight: 600;
  margin: 0;
}

/* 分类标签 */
.category-bar {
  max-width: 800px;
  margin: 16px auto 0;
  padding: 0 16px;
  display: flex;
  gap: 8px;
  overflow-x: auto;
  scrollbar-width: none;
}
.category-bar::-webkit-scrollbar { display: none; }
.cat-tag {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 14px;
  border-radius: 20px;
  background: #fff;
  cursor: pointer;
  white-space: nowrap;
  font-size: 13px;
  color: #606266;
  transition: all .2s;
  border: 1px solid #e4e7ed;
}
.cat-tag:hover { border-color: #22c55e; color: #22c55e; }
.cat-tag.active { background: #22c55e; color: #fff; border-color: #22c55e; }
.cat-icon { font-size: 14px; }

/* Feed */
.feed-container {
  max-width: 800px;
  margin: 16px auto;
  padding: 0 16px 40px;
}
.loading-box, .empty-box { padding: 40px 0; }

/* 帖子卡片 */
.post-card {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 12px;
  transition: box-shadow .2s;
}
.post-card:hover { box-shadow: 0 4px 12px rgba(0,0,0,.06); }
.post-card.pinned { border-left: 3px solid #f56c6c; }

.post-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}
.user-meta {
  display: flex;
  flex-direction: column;
}
.user-meta .username { font-weight: 600; font-size: 14px; color: #303133; }
.user-meta .time { font-size: 12px; color: #909399; }
.post-tags { display: flex; gap: 6px; }

.post-content { cursor: pointer; }
.content-text {
  font-size: 14px;
  line-height: 1.7;
  color: #303133;
  margin: 0 0 10px;
  white-space: pre-wrap;
  word-break: break-word;
}

/* 图片网格 */
.image-grid {
  display: grid;
  gap: 4px;
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 8px;
}
.image-grid.cols-1 { grid-template-columns: 1fr; max-width: 360px; }
.image-grid.cols-2 { grid-template-columns: 1fr 1fr; max-width: 400px; }
.image-grid.cols-3 { grid-template-columns: 1fr 1fr 1fr; }
.image-item { position: relative; aspect-ratio: 1; cursor: pointer; overflow: hidden; }
.image-item .el-image { width: 100%; height: 100%; }
.more-overlay {
  position: absolute; inset: 0; background: rgba(0,0,0,.5);
  display: flex; align-items: center; justify-content: center;
  color: #fff; font-size: 20px; font-weight: 600;
}

.video-box { margin-bottom: 8px; }
.video-box video { width: 100%; max-height: 360px; border-radius: 8px; background: #000; }

/* 操作栏 */
.post-actions {
  display: flex;
  gap: 20px;
  padding-top: 10px;
  border-top: 1px solid #f2f3f5;
}
.action-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #909399;
  cursor: pointer;
  transition: color .2s;
}
.action-btn:hover { color: #22c55e; }
.action-btn.active { color: #f59e0b; }
.action-btn.delete-btn:hover { color: #f56c6c; }

.load-more { text-align: center; padding: 16px 0; }

/* 发布对话框 */
.category-selector { display: flex; flex-wrap: wrap; gap: 8px; }
.cat-option {
  padding: 6px 14px;
  border-radius: 16px;
  border: 1px solid #dcdfe6;
  font-size: 13px;
  cursor: pointer;
  transition: all .2s;
}
.cat-option:hover { border-color: #22c55e; }
.cat-option.active { background: #22c55e; color: #fff; border-color: #22c55e; }

.upload-grid { display: flex; flex-wrap: wrap; gap: 8px; }
.upload-preview {
  width: 80px; height: 80px; border-radius: 8px;
  overflow: hidden; position: relative;
}
.upload-preview .el-image { width: 100%; height: 100%; }
.upload-preview .remove-btn {
  position: absolute; top: 2px; right: 2px;
  width: 20px; height: 20px; border-radius: 50%;
  background: rgba(0,0,0,.6); color: #fff;
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; font-size: 12px;
}
.upload-btn {
  width: 80px; height: 80px; border-radius: 8px;
  border: 1px dashed #dcdfe6; display: flex;
  flex-direction: column; align-items: center;
  justify-content: center; cursor: pointer;
  color: #909399; font-size: 12px; gap: 4px;
  transition: border-color .2s;
}
.upload-btn:hover { border-color: #22c55e; color: #22c55e; }

/* 详情对话框 */
.detail-dialog .detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.detail-dialog .user-info { display: flex; align-items: center; gap: 10px; }
.detail-dialog .user-meta { display: flex; flex-direction: column; }
.username-row { display: flex; align-items: center; }
.detail-dialog .username { font-weight: 600; font-size: 15px; }
.detail-dialog .time { font-size: 12px; color: #909399; }

.detail-content { margin-bottom: 16px; }
.detail-content p { font-size: 15px; line-height: 1.8; white-space: pre-wrap; word-break: break-word; margin: 0 0 12px; }

.detail-actions {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 10px 0;
  border-top: 1px solid #f2f3f5;
  border-bottom: 1px solid #f2f3f5;
  margin-bottom: 16px;
}
.comment-count { font-size: 13px; color: #909399; }

/* 评论区 */
.comments-section { max-height: 400px; overflow-y: auto; }
.comment-input { margin-bottom: 16px; }
.comment-list { display: flex; flex-direction: column; gap: 12px; }
.no-comments { text-align: center; color: #909399; padding: 20px 0; font-size: 13px; }
.comment-item { display: flex; gap: 8px; }
.comment-body { flex: 1; min-width: 0; }
.comment-meta { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.comment-user { font-weight: 600; font-size: 13px; color: #303133; }
.reply-to { font-size: 12px; color: #909399; }
.comment-time { font-size: 12px; color: #c0c4cc; }
.comment-text { font-size: 13px; line-height: 1.6; margin: 4px 0; color: #303133; }
.comment-actions { display: flex; gap: 12px; }
.act-btn {
  font-size: 12px; color: #909399; cursor: pointer;
  display: flex; align-items: center; gap: 2px;
}
.act-btn:hover { color: #22c55e; }
.act-btn.active { color: #f59e0b; }
.act-btn.del:hover { color: #f56c6c; }

@media (max-width: 640px) {
  .nav-inner, .category-bar, .feed-container { padding-left: 12px; padding-right: 12px; }
  .image-grid.cols-3 { grid-template-columns: 1fr 1fr; }
}
</style>
