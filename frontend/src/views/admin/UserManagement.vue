<template>
  <div class="user-management">
    <h2 class="page-title">用户管理</h2>

    <!-- 搜索和筛选 -->
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="关键词">
          <el-input
            v-model="searchForm.keyword"
            placeholder="用户名/邮箱/手机号"
            clearable
            @clear="handleSearch"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="正常" value="ACTIVE" />
            <el-option label="禁用" value="DISABLED" />
          </el-select>
        </el-form-item>
        <el-form-item label="会员等级">
          <el-select
            v-model="searchForm.memberLevel"
            placeholder="全部"
            clearable
            style="width: 140px"
          >
            <el-option label="免费用户" value="FREE" />
            <el-option label="青铜会员" value="BRONZE" />
            <el-option label="白银会员" value="SILVER" />
            <el-option label="黄金会员" value="GOLD" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearchClick">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset"> 重置 </el-button>
          <el-button type="success" @click="handleExport">
            <el-icon><Download /></el-icon>
            导出数据
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 用户列表 -->
    <el-card class="table-card">
      <el-table v-loading="loading" :data="userList" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="email" label="邮箱" width="200" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'">
              {{ row.status === 'ACTIVE' ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="角色" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.role === 'ADMIN'" type="warning"> 管理员 </el-tag>
            <el-tag v-else-if="row.role === 'SUPER_ADMIN'" type="danger"> 超级管理员 </el-tag>
            <el-tag v-else type="info"> 普通用户 </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="会员等级" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.memberLevel === 'GOLD'" type="warning"> 黄金 </el-tag>
            <el-tag v-else-if="row.memberLevel === 'SILVER'" type="info"> 白银 </el-tag>
            <el-tag v-else-if="row.memberLevel === 'BRONZE'" type="success"> 青铜 </el-tag>
            <el-tag v-else> 免费 </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="totalChats" label="总对话数" width="100" />
        <el-table-column prop="createdAt" label="注册时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleView(row)"> 查看 </el-button>
            <el-button type="warning" link size="small" @click="handleEdit(row)"> 编辑 </el-button>
            <el-button
              :type="row.status === 'ACTIVE' ? 'danger' : 'success'"
              link
              size="small"
              @click="handleToggleStatus(row)"
            >
              {{ row.status === 'ACTIVE' ? '禁用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        class="pagination"
        @size-change="handleSearch"
        @current-change="handleSearch"
      />
    </el-card>

    <!-- 用户详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="用户详情" width="600px">
      <el-descriptions v-if="currentUser" :column="2" border>
        <el-descriptions-item label="用户ID">
          {{ currentUser.id }}
        </el-descriptions-item>
        <el-descriptions-item label="用户名">
          {{ currentUser.username }}
        </el-descriptions-item>
        <el-descriptions-item label="邮箱">
          {{ currentUser.email }}
        </el-descriptions-item>
        <el-descriptions-item label="手机号">
          {{ currentUser.phone }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="currentUser.status === 'ACTIVE' ? 'success' : 'danger'">
            {{ currentUser.status === 'ACTIVE' ? '正常' : '禁用' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="角色">
          {{ currentUser.role }}
        </el-descriptions-item>
        <el-descriptions-item label="会员等级">
          {{ currentUser.memberLevel }}
        </el-descriptions-item>
        <el-descriptions-item label="成长值">
          {{ currentUser.growthValue }}
        </el-descriptions-item>
        <el-descriptions-item label="总对话数">
          {{ currentUser.totalChats }}
        </el-descriptions-item>
        <el-descriptions-item label="今日对话">
          {{ currentUser.todayChats }}
        </el-descriptions-item>
        <el-descriptions-item label="注册时间" :span="2">
          {{ formatDate(currentUser.createdAt) }}
        </el-descriptions-item>
        <el-descriptions-item label="最后登录" :span="2">
          {{ formatDate(currentUser.lastLoginTime) }}
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 编辑用户对话框 -->
    <el-dialog v-model="editDialogVisible" title="编辑用户" width="500px">
      <el-form v-if="currentUser" :model="editForm" label-width="100px">
        <el-form-item label="用户名">
          <el-input v-model="currentUser.username" disabled />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="editForm.status">
            <el-option label="正常" value="ACTIVE" />
            <el-option label="禁用" value="DISABLED" />
          </el-select>
        </el-form-item>
        <el-form-item label="会员等级">
          <el-select v-model="editForm.memberLevel">
            <el-option label="免费用户" value="FREE" />
            <el-option label="青铜会员" value="BRONZE" />
            <el-option label="白银会员" value="SILVER" />
            <el-option label="黄金会员" value="GOLD" />
          </el-select>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="editForm.role">
            <el-option label="普通用户" value="USER" />
            <el-option label="管理员" value="ADMIN" />
            <el-option label="超级管理员" value="SUPER_ADMIN" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false"> 取消 </el-button>
        <el-button type="primary" @click="handleSaveEdit"> 保存 </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Download } from '@element-plus/icons-vue'
import {
  getUserList,
  updateUserStatus,
  updateUserMemberLevel,
  updateUserRole
} from '@/services/admin'

const loading = ref(false)
const userList = ref([])
const detailDialogVisible = ref(false)
const editDialogVisible = ref(false)
const currentUser = ref(null)

const searchForm = reactive({
  keyword: '',
  status: '',
  memberLevel: ''
})

const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

const editForm = reactive({
  status: '',
  memberLevel: '',
  role: ''
})

// 加载用户列表
const loadUsers = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      size: pagination.size
    }
    if (searchForm.keyword) params.keyword = searchForm.keyword
    if (searchForm.status) params.status = searchForm.status
    if (searchForm.memberLevel) params.memberLevel = searchForm.memberLevel

    const { data } = await getUserList(params)
    if (data.code === 200) {
      userList.value = data.data.content
      pagination.total = data.data.totalElements
    }
  } catch (error) {
    console.error('加载用户列表失败:', error)
    ElMessage.error('加载用户列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  loadUsers()
}

// 搜索按钮点击
const handleSearchClick = () => {
  pagination.page = 1
  loadUsers()
}

// 重置
const handleReset = () => {
  searchForm.keyword = ''
  searchForm.status = ''
  searchForm.memberLevel = ''
  pagination.page = 1
  loadUsers()
}

// 查看详情
const handleView = row => {
  currentUser.value = row
  detailDialogVisible.value = true
}

// 编辑
const handleEdit = row => {
  currentUser.value = row
  editForm.status = row.status
  editForm.memberLevel = row.memberLevel
  editForm.role = row.role
  editDialogVisible.value = true
}

// 保存编辑
const handleSaveEdit = async () => {
  try {
    const userId = currentUser.value.id
    const promises = []

    if (editForm.status !== currentUser.value.status) {
      promises.push(updateUserStatus(userId, editForm.status))
    }
    if (editForm.memberLevel !== currentUser.value.memberLevel) {
      promises.push(updateUserMemberLevel(userId, editForm.memberLevel))
    }
    if (editForm.role !== currentUser.value.role) {
      promises.push(updateUserRole(userId, editForm.role))
    }

    if (promises.length === 0) {
      ElMessage.info('没有修改的内容')
      editDialogVisible.value = false
      return
    }

    await Promise.all(promises)
    ElMessage.success('保存成功')
    editDialogVisible.value = false
    loadUsers()
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error(error.response?.data?.message || '保存失败')
  }
}

// 切换状态
const handleToggleStatus = async row => {
  try {
    await ElMessageBox.confirm(
      `确定要${row.status === 'ACTIVE' ? '禁用' : '启用'}该用户吗？`,
      '确认操作',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
        center: true,
        customClass: 'custom-message-box',
        showClose: true,
        closeOnClickModal: false
      }
    )

    const newStatus = row.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
    const { data } = await updateUserStatus(row.id, newStatus)
    if (data.code === 200) {
      ElMessage.success('操作成功')
      loadUsers()
    } else {
      ElMessage.error(data.message || '操作失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('操作失败:', error)
      ElMessage.error(`操作失败: ${error.message}`)
    }
  }
}

// 导出数据
const handleExport = () => {
  try {
    // 准备导出数据
    const exportData = userList.value.map(user => ({
      ID: user.id,
      用户名: user.username,
      邮箱: user.email,
      手机号: user.phone || '-',
      状态: user.status === 'ACTIVE' ? '正常' : '禁用',
      角色: user.role,
      会员等级: user.memberLevel,
      成长值: user.growthValue,
      总对话数: user.totalChats,
      今日对话: user.todayChats,
      注册时间: formatDate(user.createdAt)
    }))

    // 转换为CSV
    const headers = Object.keys(exportData[0])
    const csvContent = [
      headers.join(','),
      ...exportData.map(row => headers.map(header => `"${row[header]}"`).join(','))
    ].join('\n')

    // 添加BOM以支持中文
    const BOM = '\uFEFF'
    const blob = new Blob([BOM + csvContent], { type: 'text/csv;charset=utf-8;' })
    const link = document.createElement('a')
    const url = URL.createObjectURL(blob)

    link.setAttribute('href', url)
    link.setAttribute('download', `用户数据_${new Date().toISOString().slice(0, 10)}.csv`)
    link.style.visibility = 'hidden'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)

    ElMessage.success('导出成功')
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('导出失败')
  }
}

// 格式化日期
const formatDate = dateString => {
  if (!dateString) return '-'
  const date = new Date(dateString)
  return date.toLocaleString('zh-CN')
}

onMounted(() => {
  loadUsers()
})
</script>

<style scoped>
.user-management {
  padding: 0;
}

.page-title {
  margin: 0 0 24px 0;
  font-size: 24px;
  font-weight: 500;
  color: #262626;
}

.search-card {
  margin-bottom: 16px;
}

.table-card {
  margin-bottom: 16px;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
