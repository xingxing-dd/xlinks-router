<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import {
  createPlan,
  deletePlan,
  listModels,
  listPlans,
  updatePlan,
  updatePlanStatus,
  updatePlanVisible,
} from '@/api/admin'
import { useToastStore } from '@/stores/toast'
import { formatBooleanFlag, formatDateTime, formatStatus, summarizeJsonArray } from '@/utils/format'

const toastStore = useToastStore()

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const dialogMode = ref('create')
const currentId = ref(null)
const modelOptions = ref([])
const records = ref([])

const filters = reactive({
  planName: '',
  status: '',
  visible: '',
})

const page = reactive({
  page: 1,
  pageSize: 10,
  total: 0,
})

const form = reactive({
  planName: '',
  price: '',
  durationDays: 30,
  dailyQuota: '',
  totalQuota: '',
  selectedModels: [],
  status: 1,
  visible: 1,
  payUrl: '',
  payLinkStatus: 1,
  remark: '',
})

const pageCount = computed(() => Math.max(1, Math.ceil((page.total || 0) / page.pageSize)))

const resetForm = () => {
  Object.assign(form, {
    planName: '',
    price: '',
    durationDays: 30,
    dailyQuota: '',
    totalQuota: '',
    selectedModels: [],
    status: 1,
    visible: 1,
    payUrl: '',
    payLinkStatus: 1,
    remark: '',
  })
}

const parseAllowedModels = (value) => {
  if (!value) {
    return []
  }
  try {
    const parsed = JSON.parse(value)
    return Array.isArray(parsed) ? parsed : []
  } catch (error) {
    return []
  }
}

const selectAllModels = () => {
  form.selectedModels = Array.from(new Set(modelOptions.value.map((item) => item.modelCode).filter(Boolean)))
}

const clearSelectedModels = () => {
  form.selectedModels = []
}

const loadPlans = async () => {
  loading.value = true
  try {
    const data = await listPlans({
      page: page.page,
      pageSize: page.pageSize,
      planName: filters.planName,
      status: filters.status,
      visible: filters.visible,
    })
    records.value = data.records || []
    page.total = data.total || 0
  } catch (error) {
    toastStore.push(error.message || '加载套餐列表失败', 'error')
  } finally {
    loading.value = false
  }
}

const loadModelOptions = async () => {
  try {
    const data = await listModels({ page: 1, pageSize: 200, status: 1 })
    modelOptions.value = data.records || []
  } catch (error) {
    toastStore.push(error.message || '加载模型选项失败', 'error')
  }
}

const openCreate = () => {
  dialogMode.value = 'create'
  currentId.value = null
  resetForm()
  dialogVisible.value = true
}

const openEdit = (record) => {
  dialogMode.value = 'edit'
  currentId.value = record.id
  Object.assign(form, {
    planName: record.planName || '',
    price: record.price ?? '',
    durationDays: record.durationDays ?? 30,
    dailyQuota: record.dailyQuota ?? '',
    totalQuota: record.totalQuota ?? '',
    selectedModels: parseAllowedModels(record.allowedModels),
    status: record.status ?? 1,
    visible: record.visible ?? 1,
    payUrl: record.payUrl || '',
    payLinkStatus: record.payLinkStatus ?? 1,
    remark: record.remark || '',
  })
  dialogVisible.value = true
}

const buildPayload = () => ({
  planName: form.planName.trim(),
  price: Number(form.price),
  durationDays: Number(form.durationDays),
  dailyQuota: Number(form.dailyQuota),
  totalQuota: Number(form.totalQuota),
  allowedModels: form.selectedModels.length ? JSON.stringify(form.selectedModels) : null,
  status: Number(form.status),
  visible: Number(form.visible),
  payUrl: form.payUrl,
  payLinkStatus: form.payUrl ? Number(form.payLinkStatus) : undefined,
  remark: form.remark,
})

const handleSubmit = async () => {
  if (!form.planName.trim()) {
    toastStore.push('请填写套餐名称', 'warning')
    return
  }
  if (Number.isNaN(Number(form.price)) || Number(form.price) < 0) {
    toastStore.push('请输入有效的套餐价格', 'warning')
    return
  }
  if (Number.isNaN(Number(form.durationDays)) || Number(form.durationDays) < 1) {
    toastStore.push('套餐时长至少为 1 天', 'warning')
    return
  }
  if (Number.isNaN(Number(form.dailyQuota)) || Number(form.dailyQuota) < 0) {
    toastStore.push('请输入有效的每日额度', 'warning')
    return
  }
  if (Number.isNaN(Number(form.totalQuota)) || Number(form.totalQuota) < Number(form.dailyQuota)) {
    toastStore.push('总额度不能小于每日额度', 'warning')
    return
  }

  submitting.value = true
  try {
    const payload = buildPayload()
    if (dialogMode.value === 'create') {
      await createPlan(payload)
      toastStore.push('套餐创建成功', 'success')
    } else {
      await updatePlan(currentId.value, payload)
      toastStore.push('套餐更新成功', 'success')
    }
    dialogVisible.value = false
    await loadPlans()
  } catch (error) {
    toastStore.push(error.message || '保存套餐失败', 'error')
  } finally {
    submitting.value = false
  }
}

const handleToggleStatus = async (record) => {
  try {
    await updatePlanStatus(record.id, Number(record.status) === 1 ? 0 : 1)
    toastStore.push('套餐状态已更新', 'success')
    await loadPlans()
  } catch (error) {
    toastStore.push(error.message || '更新套餐状态失败', 'error')
  }
}

const handleToggleVisible = async (record) => {
  try {
    await updatePlanVisible(record.id, Number(record.visible) === 1 ? 0 : 1)
    toastStore.push('套餐展示状态已更新', 'success')
    await loadPlans()
  } catch (error) {
    toastStore.push(error.message || '更新套餐展示状态失败', 'error')
  }
}

const handleDelete = async (record) => {
  if (!window.confirm(`确认删除套餐「${record.planName}」吗？`)) {
    return
  }
  try {
    await deletePlan(record.id)
    toastStore.push('套餐已删除', 'success')
    if (records.value.length === 1 && page.page > 1) {
      page.page -= 1
    }
    await loadPlans()
  } catch (error) {
    toastStore.push(error.message || '删除套餐失败', 'error')
  }
}

const resetFilters = async () => {
  Object.assign(filters, { planName: '', status: '', visible: '' })
  page.page = 1
  await loadPlans()
}

const changePage = async (nextPage) => {
  if (nextPage < 1 || nextPage > pageCount.value) {
    return
  }
  page.page = nextPage
  await loadPlans()
}

onMounted(async () => {
  await Promise.all([loadPlans(), loadModelOptions()])
})
</script>

<template>
  <div class="p-6 space-y-6">
    <div class="flex flex-col gap-3 lg:flex-row lg:items-end lg:justify-between">
      <div>
        <h1 class="text-2xl font-bold text-slate-900">套餐管理</h1>
        <p class="text-slate-500">维护客户端可购买套餐、支持模型范围、展示状态与第三方支付链接。</p>
      </div>
      <button class="btn-primary" @click="openCreate">新增套餐</button>
    </div>

    <div class="card">
      <div class="card-body grid gap-4 md:grid-cols-4">
        <div>
          <label class="text-sm text-slate-500">套餐名称</label>
          <input v-model.trim="filters.planName" class="input mt-2" placeholder="搜索套餐名称" />
        </div>
        <div>
          <label class="text-sm text-slate-500">状态</label>
          <select v-model="filters.status" class="input mt-2">
            <option value="">全部</option>
            <option :value="1">启用</option>
            <option :value="0">停用</option>
          </select>
        </div>
        <div>
          <label class="text-sm text-slate-500">展示状态</label>
          <select v-model="filters.visible" class="input mt-2">
            <option value="">全部</option>
            <option :value="1">显示</option>
            <option :value="0">隐藏</option>
          </select>
        </div>
        <div class="flex items-end justify-end gap-3">
          <button class="btn-primary h-11" @click="page.page = 1; loadPlans()">搜索</button>
          <button class="btn-outline h-11" @click="resetFilters">重置</button>
        </div>
      </div>
    </div>

    <div class="card">
      <div class="card-header">
        <div>
          <h2 class="card-title">套餐列表</h2>
          <p class="text-sm text-slate-400 mt-1">共 {{ page.total }} 条记录</p>
        </div>
        <button class="btn-outline" :disabled="loading" @click="loadPlans">{{ loading ? '刷新中...' : '刷新' }}</button>
      </div>
      <div class="card-body">
        <div class="table-wrap">
          <table class="table min-w-[1080px]">
            <thead>
              <tr>
                <th>套餐名称</th>
                <th>价格</th>
                <th>时长</th>
                <th>每日额度</th>
                <th>总额度</th>
                <th>支持模型</th>
                <th>展示状态</th>
                <th>状态</th>
                <th>支付链接</th>
                <th>更新时间</th>
                <th class="text-right">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="!records.length && !loading">
                <td colspan="11" class="empty-state">暂无套餐数据</td>
              </tr>
              <tr v-for="record in records" :key="record.id">
                <td>
                  <div class="font-medium text-slate-800">{{ record.planName }}</div>
                  <div class="text-xs text-slate-400 mt-1">ID: {{ record.id }}</div>
                </td>
                <td>￥{{ record.price ?? 0 }}</td>
                <td>{{ record.durationDays }} 天</td>
                <td>${{ record.dailyQuota ?? 0 }}</td>
                <td>${{ record.totalQuota ?? 0 }}</td>
                <td class="max-w-[220px] break-words">{{ summarizeJsonArray(record.allowedModels) }}</td>
                <td>
                  <span class="badge" :class="Number(record.visible) === 1 ? 'badge-success' : 'badge-warning'">
                    {{ formatBooleanFlag(record.visible, '显示', '隐藏') }}
                  </span>
                </td>
                <td>
                  <span class="badge" :class="Number(record.status) === 1 ? 'badge-success' : 'badge-danger'">
                    {{ formatStatus(record.status) }}
                  </span>
                </td>
                <td class="max-w-[220px] break-all">{{ record.payUrl || '-' }}</td>
                <td>{{ formatDateTime(record.updatedAt) }}</td>
                <td>
                  <div class="flex items-center justify-end gap-2">
                    <button class="btn-outline" @click="openEdit(record)">编辑</button>
                    <button class="btn-outline" @click="handleToggleVisible(record)">
                      {{ Number(record.visible) === 1 ? '隐藏' : '显示' }}
                    </button>
                    <button class="btn-outline" @click="handleToggleStatus(record)">
                      {{ Number(record.status) === 1 ? '停用' : '启用' }}
                    </button>
                    <button class="btn-danger" @click="handleDelete(record)">删除</button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="mt-4 flex items-center justify-between text-sm text-slate-500">
          <span>第 {{ page.page }} / {{ pageCount }} 页</span>
          <div class="flex gap-2">
            <button class="btn-outline" :disabled="page.page <= 1" @click="changePage(page.page - 1)">上一页</button>
            <button class="btn-outline" :disabled="page.page >= pageCount" @click="changePage(page.page + 1)">下一页</button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="dialogVisible" class="fixed inset-0 z-50 flex items-center justify-center px-4">
      <div class="absolute inset-0 bg-slate-900/50" @click="dialogVisible = false"></div>
      <div class="modal-panel max-w-5xl">
        <div class="flex items-center justify-between gap-4">
          <div>
            <h3 class="text-lg font-semibold text-slate-800">{{ dialogMode === 'create' ? '新增套餐' : '编辑套餐' }}</h3>
            <p class="text-sm text-slate-400 mt-1">可配置每日额度、总额度、可用模型与独立支付链接。</p>
          </div>
          <button class="btn-text" @click="dialogVisible = false">关闭</button>
        </div>

        <div class="mt-6 grid gap-4 md:grid-cols-2">
          <div>
            <label class="text-sm text-slate-500">套餐名称</label>
            <input v-model.trim="form.planName" class="input mt-2" placeholder="请输入套餐名称" />
          </div>
          <div>
            <label class="text-sm text-slate-500">套餐价格（元）</label>
            <input v-model.number="form.price" type="number" min="0" step="0.01" class="input mt-2" placeholder="59.90" />
          </div>
          <div>
            <label class="text-sm text-slate-500">套餐时长（天）</label>
            <input v-model.number="form.durationDays" type="number" min="1" class="input mt-2" placeholder="30" />
          </div>
          <div>
            <label class="text-sm text-slate-500">每日额度上限</label>
            <input v-model.number="form.dailyQuota" type="number" min="0" step="0.01" class="input mt-2" placeholder="30" />
          </div>
          <div>
            <label class="text-sm text-slate-500">总额度上限</label>
            <input v-model.number="form.totalQuota" type="number" min="0" step="0.01" class="input mt-2" placeholder="900" />
          </div>
          <div>
            <label class="text-sm text-slate-500">状态</label>
            <select v-model.number="form.status" class="input mt-2">
              <option :value="1">启用</option>
              <option :value="0">停用</option>
            </select>
          </div>
          <div>
            <label class="text-sm text-slate-500">展示状态</label>
            <select v-model.number="form.visible" class="input mt-2">
              <option :value="1">显示</option>
              <option :value="0">隐藏</option>
            </select>
          </div>
          <div>
            <label class="text-sm text-slate-500">支付链接状态</label>
            <select v-model.number="form.payLinkStatus" class="input mt-2" :disabled="!form.payUrl">
              <option :value="1">启用</option>
              <option :value="0">停用</option>
            </select>
          </div>
          <div class="md:col-span-2">
            <label class="text-sm text-slate-500">第三方支付链接</label>
            <input v-model.trim="form.payUrl" class="input mt-2" placeholder="填写后将同步到第三方支付链接表" />
          </div>
          <div class="md:col-span-2">
            <label class="text-sm text-slate-500">可用模型范围</label>
            <div class="mt-2 rounded-2xl border border-slate-200 bg-slate-50 p-4">
              <div v-if="modelOptions.length" class="mb-3 flex items-center justify-end gap-2">
                <button type="button" class="btn-outline" @click="selectAllModels">全选</button>
                <button type="button" class="btn-outline" @click="clearSelectedModels">全不选</button>
              </div>
              <div v-if="modelOptions.length" class="grid gap-3 md:grid-cols-3">
                <label
                  v-for="model in modelOptions"
                  :key="model.id"
                  class="flex items-start gap-3 rounded-xl border border-slate-200 bg-white px-3 py-3 text-sm text-slate-700"
                >
                  <input v-model="form.selectedModels" :value="model.modelCode" type="checkbox" class="mt-1 accent-primary" />
                  <span>
                    <span class="block font-medium text-slate-800">{{ model.modelName }}</span>
                    <span class="block text-xs text-slate-400 mt-1">{{ model.modelCode }}</span>
                  </span>
                </label>
              </div>
              <p v-else class="text-sm text-slate-400">暂无可选模型，请先在模型资源中心启用标准模型。</p>
            </div>
            <p class="text-xs text-slate-400 mt-2">
              已选择：{{ form.selectedModels.length ? form.selectedModels.join(', ') : '尚未选择模型' }}
            </p>
          </div>
          <div class="md:col-span-2">
            <label class="text-sm text-slate-500">备注</label>
            <textarea v-model.trim="form.remark" class="input mt-2 min-h-24" placeholder="可填写运营备注"></textarea>
          </div>
        </div>

        <div class="mt-6 flex justify-end gap-3">
          <button class="btn-outline" @click="dialogVisible = false">取消</button>
          <button class="btn-primary" :disabled="submitting" @click="handleSubmit">
            {{ submitting ? '提交中...' : dialogMode === 'create' ? '确认新增' : '确认保存' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
