<template>
  <div class="system-config">
    <h2 class="page-title">系统配置</h2>

    <!-- 配置分类标签 -->
    <el-card class="category-card">
      <el-tabs v-model="activeCategory" @tab-change="handleCategoryChange">
        <el-tab-pane label="全部配置" name="all" />
        <el-tab-pane label="AI配置" name="AI" />
        <el-tab-pane label="系统配置" name="系统" />
        <el-tab-pane label="用户配置" name="用户" />
        <el-tab-pane label="安全配置" name="安全" />
        <el-tab-pane label="通知配置" name="通知" />
      </el-tabs>
    </el-card>

    <!-- 配置列表 -->
    <el-card class="config-card" v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>配置列表</span>
          <el-button type="primary" @click="handleCreate">
            <el-icon><Plus /></el-icon>
            添加配置
          </el-button>
        </div>
      </template>
      <el-table :data="configList" stripe>
        <el-table-column prop="configKey" label="配置键" width="250" />
        <el-table-column prop="configValue" label="配置值" width="200">
          <template #default="{ row }">
            <el-tag v-if="row.configType === 'boolean'" :type="row.configValue === 'true' ? 'success' : 'info'">
              {{ row.configValue }}
            </el-tag>
            <span v-else>{{ row.configValue }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="configType" label="类型" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ row.configType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        <el-table-column prop="category" label="分类" width="100" />
        <el-table-column label="公开" width="80">
          <template #default="{ row }">
            <el-icon v-if="row.isPublic" color="#67c23a"><Check /></el-icon>
            <el-icon v-else color="#909399"><Close /></el-icon>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">
              编辑
            </el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 编辑配置对话框 -->
    <el-dialog
      v-model="editDialogVisible"
      :title="isCreate ? '创建配置' : '编辑配置'"
      width="700px"
    >
      <el-form :model="editForm" label-width="120px" :rules="rules" ref="formRef">
        <!-- 创建时显示配置选项选择器 -->
        <el-form-item label="选择配置项" v-if="isCreate">
          <el-select 
            v-model="selectedOption" 
            placeholder="从预定义配置中选择"
            filterable
            @change="handleOptionSelect"
            style="width: 100%"
          >
            <el-option
              v-for="option in configOptions"
              :key="option.key"
              :label="`${option.name} (${option.key})`"
              :value="option.key"
            >
              <div style="display: flex; flex-direction: column;">
                <span style="font-weight: bold;">{{ option.name }}</span>
                <span style="font-size: 12px; color: #909399;">{{ option.description }}</span>
              </div>
            </el-option>
          </el-select>
          <div style="margin-top: 8px; font-size: 12px; color: #909399;">
            或者手动输入自定义配置键
          </div>
        </el-form-item>
        
        <el-form-item label="配置键" prop="configKey">
          <el-input v-model="editForm.configKey" :disabled="!isCreate" placeholder="例如: ai.model" />
        </el-form-item>
        
        <el-form-item label="配置名称" prop="configName" v-if="isCreate">
          <el-input v-model="editForm.configName" placeholder="配置的显示名称" />
        </el-form-item>
        
        <el-form-item label="配置值" prop="configValue">
          <!-- 根据valueType显示不同的输入控件 -->
          <el-select 
            v-if="currentOption && currentOption.valueType === 'select'" 
            v-model="editForm.configValue"
            style="width: 100%"
          >
            <el-option
              v-for="opt in currentOption.options"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
          <el-input-number 
            v-else-if="editForm.configType === 'number'" 
            v-model.number="editForm.configValue"
            style="width: 100%"
          />
          <el-switch
            v-else-if="editForm.configType === 'boolean'"
            v-model="editForm.configValue"
            active-value="true"
            inactive-value="false"
          />
          <el-input 
            v-else 
            v-model="editForm.configValue"
            :placeholder="currentOption ? `默认值: ${currentOption.defaultValue}` : ''"
          />
        </el-form-item>
        
        <el-form-item label="类型" prop="configType">
          <el-select v-model="editForm.configType" :disabled="!isCreate" style="width: 100%">
            <el-option label="字符串" value="string" />
            <el-option label="数字" value="number" />
            <el-option label="布尔值" value="boolean" />
            <el-option label="JSON" value="json" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="描述" prop="description">
          <el-input v-model="editForm.description" type="textarea" :rows="3" placeholder="配置项的详细说明" />
        </el-form-item>
        
        <el-form-item label="分类" prop="category">
          <el-select v-model="editForm.category" style="width: 100%">
            <el-option label="AI配置" value="AI" />
            <el-option label="系统配置" value="系统" />
            <el-option label="用户配置" value="用户" />
            <el-option label="安全配置" value="安全" />
            <el-option label="通知配置" value="通知" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="是否公开">
          <el-switch v-model="editForm.isPublic" />
          <span class="form-tip">公开后前端可直接访问</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Close, Plus } from '@element-plus/icons-vue'

const loading = ref(false)
const configList = ref([])
const configOptions = ref([])
const selectedOption = ref('')
const activeCategory = ref('all')
const editDialogVisible = ref(false)
const isCreate = ref(false)
const formRef = ref(null)

const editForm = reactive({
  configKey: '',
  configName: '',
  configValue: '',
  configType: 'string',
  description: '',
  category: 'system',
  isPublic: false
})

// 当前选中的配置选项
const currentOption = computed(() => {
  return configOptions.value.find(opt => opt.key === editForm.configKey)
})

const rules = {
  configKey: [{ required: true, message: '请输入配置键', trigger: 'blur' }],
  configValue: [{ required: true, message: '请输入配置值', trigger: 'blur' }],
  configType: [{ required: true, message: '请选择类型', trigger: 'change' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }]
}

// 加载配置选项
const loadConfigOptions = async () => {
  try {
    const token = localStorage.getItem('token')
    const response = await fetch('http://localhost:8080/api/admin/config/options', {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    })
    
    const data = await response.json()
    if (data.code === 200) {
      configOptions.value = data.data
      console.log('配置选项加载成功:', configOptions.value.length)
    }
  } catch (error) {
    console.error('加载配置选项失败:', error)
  }
}

// 选择配置选项
const handleOptionSelect = (key) => {
  const option = configOptions.value.find(opt => opt.key === key)
  if (option) {
    editForm.configKey = option.key
    editForm.configName = option.name
    editForm.configValue = option.defaultValue
    editForm.configType = option.valueType === 'select' ? 'string' : option.valueType
    editForm.description = option.description
    editForm.category = option.category
    editForm.isPublic = false
  }
}

// 加载配置列表
const loadConfigs = async () => {
  loading.value = true
  try {
    const token = localStorage.getItem('token')
    const url = activeCategory.value === 'all'
      ? 'http://localhost:8080/api/admin/config'
      : `http://localhost:8080/api/admin/config?category=${activeCategory.value}`
    
    const response = await fetch(url, {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    })
    
    const data = await response.json()
    if (data.code === 200) {
      configList.value = data.data
    }
  } catch (error) {
    console.error('加载配置失败:', error)
    ElMessage.error('加载配置失败')
  } finally {
    loading.value = false
  }
}

// 分类切换
const handleCategoryChange = () => {
  loadConfigs()
}

// 创建配置
const handleCreate = () => {
  isCreate.value = true
  Object.assign(editForm, {
    configKey: '',
    configValue: '',
    configType: 'string',
    description: '',
    category: 'system',
    isPublic: false
  })
  editDialogVisible.value = true
}

// 编辑配置
const handleEdit = (row) => {
  isCreate.value = false
  Object.assign(editForm, {
    ...row,
    configValue: String(row.configValue)
  })
  editDialogVisible.value = true
}

// 保存配置
const handleSave = async () => {
  try {
    await formRef.value.validate()
    
    const token = localStorage.getItem('token')
    
    if (isCreate.value) {
      // 创建新配置
      const response = await fetch('http://localhost:8080/api/admin/config', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(editForm)
      })
      
      const data = await response.json()
      if (data.code === 200) {
        ElMessage.success('创建成功')
        editDialogVisible.value = false
        loadConfigs()
      } else {
        ElMessage.error(data.message || '创建失败')
      }
    } else {
      // 更新配置
      const response = await fetch(
        `http://localhost:8080/api/admin/config/${editForm.configKey}`,
        {
          method: 'PUT',
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({ value: editForm.configValue })
        }
      )
      
      const data = await response.json()
      if (data.code === 200) {
        ElMessage.success('更新成功')
        editDialogVisible.value = false
        loadConfigs()
      } else {
        ElMessage.error(data.message || '更新失败')
      }
    }
  } catch (error) {
    console.error('保存失败:', error)
  }
}

// 删除配置
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除配置 "${row.configKey}" 吗？`,
      '确认删除',
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
    
    const token = localStorage.getItem('token')
    const response = await fetch(
      `http://localhost:8080/api/admin/config/${encodeURIComponent(row.configKey)}`,
      {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${token}`
        }
      }
    )
    
    const data = await response.json()
    if (data.code === 200) {
      ElMessage.success('删除成功')
      loadConfigs()
    } else {
      ElMessage.error(data.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

onMounted(() => {
  loadConfigs()
  loadConfigOptions()
})
</script>

<style scoped>
.system-config {
  padding: 0;
  position: relative;
}

.page-title {
  margin: 0 0 24px 0;
  font-size: 24px;
  font-weight: 500;
  color: #262626;
}

.category-card {
  margin-bottom: 16px;
}

.config-card {
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 500;
  font-size: 16px;
}

.form-tip {
  margin-left: 8px;
  font-size: 12px;
  color: #909399;
}
</style>
