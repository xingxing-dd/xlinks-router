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
import { formatDateTime, formatStatus, summarizeJsonArray } from '@/utils/format'

const toastStore = useToastStore()

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const dialogMode = ref('create')
const currentId = ref(null)
const records = ref([])
const modelOptions = ref([])

const filters = reactive({
  planName: '',
  status: '',
  visible: '',
  accountId: '',
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
  maxPurchaseCount: '',
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
    maxPurchaseCount: '',
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
  form.selectedModels = Array.from(
    new Set(modelOptions.value.map((item) => item.modelCode).filter(Boolean)),
  )
}

const clearSelectedModels = () => {
  form.selectedModels = []
}

const loadModelOptions = async () => {
  try {
    const data = await listModels({ page: 1, pageSize: 200, status: 1 })
    modelOptions.value = data.records || []
  } catch (error) {
    toastStore.push(error.message || '加载模型列表失败', 'error')
  }
}

const loadPlans = async () => {
  loading.value = true
  try {
    const normalizedAccountId = Number(filters.accountId)
    const data = await listPlans({
      page: page.page,
      pageSize: page.pageSize,
      planName: filters.planName,
      status: filters.status,
      visible: filters.visible,
      accountId:
        filters.accountId === '' || Number.isNaN(normalizedAccountId)
          ? undefined
          : normalizedAccountId,
    })
    records.value = data.records || []
    page.total = data.total || 0
  } catch (error) {
    toastStore.push(error.message || '加载套餐列表失败', 'error')
  } finally {
    loading.value = false
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
    maxPurchaseCount: record.maxPurchaseCount ?? '',
    selectedModels: parseAllowedModels(record.allowedModels),
    status: Number(record.status ?? 1),
    visible: Number(record.visible ?? 1),
    payUrl: record.payUrl || '',
    payLinkStatus: Number(record.payLinkStatus ?? 1),
    remark: record.remark || '',
  })
  dialogVisible.value = true
}

const toOptionalInteger = (value) => {
  if (value === '' || value === null || value === undefined) {
    return null
  }
  return Number(value)
}

const buildPayload = () => ({
  planName: form.planName.trim(),
  price: Number(form.price),
  durationDays: Number(form.durationDays),
  dailyQuota: Number(form.dailyQuota),
  totalQuota: Number(form.totalQuota),
  maxPurchaseCount: toOptionalInteger(form.maxPurchaseCount),
  allowedModels: form.selectedModels.length ? JSON.stringify(form.selectedModels) : null,
  status: Number(form.status),
  visible: Number(form.visible),
  payUrl: form.payUrl.trim(),
  payLinkStatus: form.payUrl.trim() ? Number(form.payLinkStatus) : undefined,
  remark: form.remark,
})

const validateForm = () => {
  if (!form.planName.trim()) {
    toastStore.push('请输入套餐名称', 'warning')
    return false
  }
  if (Number.isNaN(Number(form.price)) || Number(form.price) < 0) {
    toastStore.push('套餐价格必须是大于等于 0 的数字', 'warning')
    return false
  }
  if (Number.isNaN(Number(form.durationDays)) || Number(form.durationDays) < 1) {
    toastStore.push('套餐时长至少为 1 天', 'warning')
    return false
  }
  if (Number.isNaN(Number(form.dailyQuota)) || Number(form.dailyQuota) < 0) {
    toastStore.push('每日额度必须是大于等于 0 的数字', 'warning')
    return false
  }
  if (Number.isNaN(Number(form.totalQuota)) || Number(form.totalQuota) < Number(form.dailyQuota)) {
    toastStore.push('总额度必须大于等于每日额度', 'warning')
    return false
  }
  if (form.maxPurchaseCount !== '' && form.maxPurchaseCount !== null && form.maxPurchaseCount !== undefined) {
    const count = Number(form.maxPurchaseCount)
    if (Number.isNaN(count) || count < 1 || !Number.isInteger(count)) {
      toastStore.push('购买次数上限必须是大于等于 1 的整数', 'warning')
      return false
    }
  }
  return true
}

const handleSubmit = async () => {
  if (!validateForm()) {
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
    toastStore.push('可见状态已更新', 'success')
    await loadPlans()
  } catch (error) {
    toastStore.push(error.message || '更新可见状态失败', 'error')
  }
}

const handleDelete = async (record) => {
  if (!window.confirm(`确认删除套餐“${record.planName}”吗？`)) {
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
  Object.assign(filters, { planName: '', status: '', visible: '', accountId: '' })
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
        <p class="text-slate-500">支持配置套餐基础信息、可见性、允许模型和购买次数上限。</p>
      </div>
      <button class="btn-primary" @click="openCreate">新增套餐</button>
    </div>

    <div class="card">
      <div class="card-body grid gap-4 md:grid-cols-5">
        <div>
          <label class="text-sm text-slate-500">套餐名称</label>
          <input v-model.trim="filters.planName" class="input mt-2" placeholder="按套餐名称搜索" />
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
          <label class="text-sm text-slate-500">是否展示</label>
          <select v-model="filters.visible" class="input mt-2">
            <option value="">全部</option>
            <option :value="1">展示</option>
            <option :value="0">隐藏</option>
          </select>
        </div>
        <div>
          <label class="text-sm text-slate-500">账号 ID</label>
          <input
            v-model.number="filters.accountId"
            type="number"
            min="1"
            class="input mt-2"
            placeholder="用于按购买上限过滤"
          />
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
          <table class="table min-w-[1240px]">
            <thead>
              <tr>
                <th>套餐</th>
                <th>价格</th>
                <th>时长</th>
                <th>每日额度</th>
                <th>总额度</th>
                <th>购买次数上限</th>
                <th>可用模型</th>
                <th>展示</th>
                <th>状态</th>
                <th>支付链接</th>
                <th>更新时间</th>
                <th class="text-right">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="!records.length && !loading">
                <td colspan="12" class="empty-state">暂无套餐数据</td>
              </tr>
              <tr v-for="record in records" :key="record.id">
                <td>
                  <div class="font-medium text-slate-800">{{ record.planName }}</div>
                  <div class="text-xs text-slate-400 mt-1">ID: {{ record.id }}</div>
                </td>
                <td>¥{{ record.price ?? 0 }}</td>
                <td>{{ record.durationDays }} 天</td>
                <td>{{ record.dailyQuota ?? 0 }}</td>
                <td>{{ record.totalQuota ?? 0 }}</td>
                <td>{{ record.maxPurchaseCount == null ? '不限制' : record.maxPurchaseCount }}</td>
                <td class="max-w-[220px] break-words">{{ summarizeJsonArray(record.allowedModels) }}</td>
                <td>
                  <span class="badge" :class="Number(record.visible) === 1 ? 'badge-success' : 'badge-warning'">
                    {{ Number(record.visible) === 1 ? '展示' : '隐藏' }}
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
                      {{ Number(record.visible) === 1 ? '隐藏' : '展示' }}
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
            <p class="text-sm text-slate-400 mt-1">支持为空的购买次数上限，空值表示不限制。</p>
          </div>
          <button class="btn-text" @click="dialogVisible = false">关闭</button>
        </div>

        <div class="mt-6 grid gap-4 md:grid-cols-2">
          <div>
            <label class="text-sm text-slate-500">套餐名称</label>
            <input v-model.trim="form.planName" class="input mt-2" placeholder="请输入套餐名称" />
          </div>
          <div>
            <label class="text-sm text-slate-500">价格</label>
            <input v-model.number="form.price" type="number" min="0" step="0.01" class="input mt-2" placeholder="59.90" />
          </div>
          <div>
            <label class="text-sm text-slate-500">时长（天）</label>
            <input v-model.number="form.durationDays" type="number" min="1" class="input mt-2" placeholder="30" />
          </div>
          <div>
            <label class="text-sm text-slate-500">每日额度</label>
            <input v-model.number="form.dailyQuota" type="number" min="0" step="0.01" class="input mt-2" placeholder="100" />
          </div>
          <div>
            <label class="text-sm text-slate-500">总额度</label>
            <input v-model.number="form.totalQuota" type="number" min="0" step="0.01" class="input mt-2" placeholder="3000" />
          </div>
          <div>
            <label class="text-sm text-slate-500">购买次数上限</label>
            <input
              v-model.number="form.maxPurchaseCount"
              type="number"
              min="1"
              class="input mt-2"
              placeholder="留空表示不限制"
            />
          </div>
          <div>
            <label class="text-sm text-slate-500">状态</label>
            <select v-model.number="form.status" class="input mt-2">
              <option :value="1">启用</option>
              <option :value="0">停用</option>
            </select>
          </div>
          <div>
            <label class="text-sm text-slate-500">是否展示</label>
            <select v-model.number="form.visible" class="input mt-2">
              <option :value="1">展示</option>
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
            <label class="text-sm text-slate-500">支付链接</label>
            <input v-model.trim="form.payUrl" class="input mt-2" placeholder="可选，留空则不关联支付链接" />
          </div>
          <div class="md:col-span-2">
            <label class="text-sm text-slate-500">允许模型</label>
            <div class="mt-2 rounded-2xl border border-slate-200 bg-slate-50 p-4">
              <div v-if="modelOptions.length" class="mb-3 flex items-center justify-end gap-2">
                <button type="button" class="btn-outline" @click="selectAllModels">全选</button>
                <button type="button" class="btn-outline" @click="clearSelectedModels">清空</button>
              </div>
              <div v-if="modelOptions.length" class="grid gap-3 md:grid-cols-3">
                <label
                  v-for="model in modelOptions"
                  :key="model.id"
                  class="flex items-start gap-3 rounded-xl border border-slate-200 bg-white px-3 py-3 text-sm text-slate-700"
                >
                  <input
                    v-model="form.selectedModels"
                    :value="model.modelCode"
                    type="checkbox"
                    class="mt-1 accent-primary"
                  />
                  <span>
                    <span class="block font-medium text-slate-800">{{ model.modelName }}</span>
                    <span class="block text-xs text-slate-400 mt-1">{{ model.modelCode }}</span>
                  </span>
                </label>
              </div>
              <p v-else class="text-sm text-slate-400">暂无可选模型，请先在模型管理中维护。</p>
            </div>
            <p class="text-xs text-slate-400 mt-2">
              已选择：{{ form.selectedModels.length ? form.selectedModels.join(', ') : '未限制（表示允许所有模型）' }}
            </p>
          </div>
          <div class="md:col-span-2">
            <label class="text-sm text-slate-500">备注</label>
            <textarea v-model.trim="form.remark" class="input mt-2 min-h-24" placeholder="可选备注"></textarea>
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
