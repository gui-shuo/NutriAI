<template>
  <div class="address-manager">
    <div class="section-header">
      <h2>📍 收货地址管理</h2>
      <el-button type="primary" @click="openForm()">
        <el-icon><Plus /></el-icon>
        新增地址
      </el-button>
    </div>
    <p class="section-desc">管理您的收货地址，最多保存 20 个地址</p>

    <!-- 地址列表 -->
    <div v-loading="loading" class="address-list">
      <el-empty v-if="!loading && !addresses.length" description="暂无收货地址，快去添加一个吧" />
      <div
        v-for="addr in addresses"
        :key="addr.id"
        class="address-card"
        :class="{ default: addr.isDefault }"
      >
        <div class="card-main">
          <div class="card-top">
            <span class="receiver-name">{{ addr.receiverName }}</span>
            <span class="receiver-phone">{{ addr.receiverPhone }}</span>
            <el-tag v-if="addr.isDefault" type="success" size="small" effect="dark">默认</el-tag>
            <el-tag v-if="addr.label" size="small" effect="plain">{{ addr.label }}</el-tag>
          </div>
          <div class="card-address">
            {{ addr.province }}{{ addr.city }}{{ addr.district }}{{ addr.detailAddress }}
          </div>
        </div>
        <div class="card-actions">
          <el-button text type="primary" size="small" @click="openForm(addr)">
            <el-icon><Edit /></el-icon> 编辑
          </el-button>
          <el-button
            v-if="!addr.isDefault"
            text
            type="warning"
            size="small"
            @click="handleSetDefault(addr.id)"
          >
            <el-icon><Star /></el-icon> 设为默认
          </el-button>
          <el-popconfirm title="确定删除该地址吗？" @confirm="handleDelete(addr.id)">
            <template #reference>
              <el-button text type="danger" size="small">
                <el-icon><Delete /></el-icon> 删除
              </el-button>
            </template>
          </el-popconfirm>
        </div>
      </div>
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="formVisible"
      :title="editingId ? '编辑地址' : '新增地址'"
      width="520px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="收货人" prop="receiverName">
          <el-input v-model="form.receiverName" placeholder="请输入收货人姓名" maxlength="20" />
        </el-form-item>
        <el-form-item label="手机号" prop="receiverPhone">
          <el-input v-model="form.receiverPhone" placeholder="请输入手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="所在地区" prop="region">
          <el-cascader
            v-model="form.region"
            :options="regionOptions"
            :props="{ expandTrigger: 'hover' }"
            placeholder="请选择省/市/区"
            clearable
            filterable
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="详细地址" prop="detailAddress">
          <el-input
            v-model="form.detailAddress"
            type="textarea"
            :rows="2"
            placeholder="街道、门牌号等详细地址"
            maxlength="200"
          />
        </el-form-item>
        <el-form-item label="标签">
          <el-radio-group v-model="form.label">
            <el-radio-button value="">无</el-radio-button>
            <el-radio-button value="家">🏠 家</el-radio-button>
            <el-radio-button value="公司">🏢 公司</el-radio-button>
            <el-radio-button value="学校">🎓 学校</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="默认地址">
          <el-switch v-model="form.isDefault" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Plus, Edit, Delete, Star } from '@element-plus/icons-vue'
import { regionData } from 'element-china-area-data'
import { getAddresses, addAddress, updateAddress, deleteAddress, setDefaultAddress } from '@/services/address'
import message from '@/utils/message'

const regionOptions = regionData

const loading = ref(false)
const addresses = ref([])
const formVisible = ref(false)
const saving = ref(false)
const editingId = ref(null)
const formRef = ref(null)

const emptyForm = () => ({
  receiverName: '',
  receiverPhone: '',
  region: [],
  detailAddress: '',
  label: '',
  isDefault: false
})

const form = ref(emptyForm())

const rules = {
  receiverName: [{ required: true, message: '请输入收货人姓名', trigger: 'blur' }],
  receiverPhone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  region: [{ required: true, message: '请选择所在地区', trigger: 'change', type: 'array' }],
  detailAddress: [{ required: true, message: '请输入详细地址', trigger: 'blur' }]
}

// 根据行政代码查找对应的label名称
function findRegionLabels(codes) {
  if (!codes || codes.length === 0) return { province: '', city: '', district: '' }
  const result = { province: '', city: '', district: '' }
  const provinceNode = regionOptions.find(p => p.value === codes[0])
  if (provinceNode) {
    result.province = provinceNode.label
    if (codes[1] && provinceNode.children) {
      const cityNode = provinceNode.children.find(c => c.value === codes[1])
      if (cityNode) {
        result.city = cityNode.label
        if (codes[2] && cityNode.children) {
          const districtNode = cityNode.children.find(d => d.value === codes[2])
          if (districtNode) result.district = districtNode.label
        }
      }
    }
  }
  return result
}

// 根据省市区名称反查行政代码
function findRegionCodes(province, city, district) {
  for (const p of regionOptions) {
    if (p.label === province) {
      if (!p.children) return [p.value]
      for (const c of p.children) {
        if (c.label === city) {
          if (!district || !c.children) return [p.value, c.value]
          for (const d of c.children) {
            if (d.label === district) return [p.value, c.value, d.value]
          }
          return [p.value, c.value]
        }
      }
      return [p.value]
    }
  }
  return []
}

async function fetchAddresses() {
  loading.value = true
  try {
    const res = await getAddresses()
    if (res.data.code === 200) {
      addresses.value = res.data.data || []
    }
  } catch {
    message.error('获取地址列表失败')
  } finally {
    loading.value = false
  }
}

function openForm(addr) {
  if (addr) {
    editingId.value = addr.id
    form.value = {
      receiverName: addr.receiverName,
      receiverPhone: addr.receiverPhone,
      region: findRegionCodes(addr.province, addr.city, addr.district),
      detailAddress: addr.detailAddress,
      label: addr.label || '',
      isDefault: addr.isDefault
    }
  } else {
    editingId.value = null
    form.value = emptyForm()
  }
  formVisible.value = true
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    const labels = findRegionLabels(form.value.region)
    const data = {
      receiverName: form.value.receiverName,
      receiverPhone: form.value.receiverPhone,
      province: labels.province,
      city: labels.city,
      district: labels.district,
      detailAddress: form.value.detailAddress,
      label: form.value.label,
      isDefault: form.value.isDefault
    }
    if (editingId.value) {
      await updateAddress(editingId.value, data)
      message.success('地址已更新')
    } else {
      await addAddress(data)
      message.success('地址已添加')
    }
    formVisible.value = false
    fetchAddresses()
  } catch (e) {
    message.error(e?.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function handleSetDefault(id) {
  try {
    await setDefaultAddress(id)
    message.success('已设为默认地址')
    fetchAddresses()
  } catch {
    message.error('操作失败')
  }
}

async function handleDelete(id) {
  try {
    await deleteAddress(id)
    message.success('地址已删除')
    fetchAddresses()
  } catch {
    message.error('删除失败')
  }
}

onMounted(fetchAddresses)
</script>

<style scoped lang="scss">
.address-manager {
  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 8px;

    h2 {
      font-size: 22px;
      font-weight: 700;
      margin: 0;
      font-family: 'Calistoga', serif;
      color: #0F172A;
    }
  }

  .section-desc {
    color: #64748B;
    font-size: 14px;
    margin: 0 0 20px;
  }
}

.address-list {
  min-height: 200px;
}

.address-card {
  background: #FAFAFA;
  border: 1px solid #E2E8F0;
  border-radius: 12px;
  padding: 16px 20px;
  margin-bottom: 12px;
  transition: all 0.3s;

  &:hover {
    box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -2px rgba(0, 0, 0, 0.1);
    border-color: #0F172A;
  }

  &.default {
    border-color: #0052FF;
    background: #F1F5F9;
  }

  .card-main {
    .card-top {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 8px;

      .receiver-name {
        font-weight: 600;
        font-size: 16px;
        font-family: 'Calistoga', serif;
      }

      .receiver-phone {
        color: #0F172A;
        font-size: 14px;
      }
    }

    .card-address {
      color: #0F172A;
      font-size: 14px;
      line-height: 1.5;
    }
  }

  .card-actions {
    margin-top: 12px;
    display: flex;
    gap: 4px;
    border-top: 1px solid #E2E8F0;
    padding-top: 10px;
  }
}
</style>
