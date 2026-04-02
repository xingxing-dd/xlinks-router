<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import {
  createPayLink,
  deletePayLink,
  listPayLinks,
  listPlans,
  updatePayLink,
  updatePayLinkStatus,
} from '@/api/admin'
import { useToastStore } from '@/stores/toast'
import { formatBooleanFlag, formatDateTime, formatStatus } from '@/utils/format'

const toastStore = useToastStore()

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const dialogMode = ref('create')
const currentRecord = ref(null)
const records = ref([])
const planOptions = ref([])

const filters = reactive({
  targetId: '',
  planName: '',
  status: '',
})

const page = reactive({
  page: 1,
  pageSize: 10,
  total: 0,
})

const form = reactive({
  targetId: '',
  payUrl: '',
  status: 1,
  remark: '',
})

const pageCount = computed(() => Math.max(1, Math.ceil((page.total || 0) / page.pageSize)))
const summary = computed(() => ({
  total: page.total,
  enabled: records.value.filter((item) => Number(item.status) === 1).length,
  hiddenPlans: records.value.filter((item) => Number(item.planVisible) === 0).length,
}))

const resetForm = () => {
  Object.assign(form, {
    targetId: '',
    payUrl: '',
    status: 1,
    remark: '',
  })
}

const loadPlans = async () => {
  try {
    const data = await listPlans({ page: 1, pageSize: 200 })
    planOptions.value = data.records || []
  } catch (error) {
    toastStore.push(error.message || '加载套餐选项失败', 'error')
  }
}

const loadPayLinks = async () => {
  loading.value = true
  try {
    const data = await listPayLinks({
      page: page.page,
      pageSize: page.pageSize,
      targetId: filters.targetId,
      planName: filters.planName,
      status: filters.status,
    })
    records.value = data.records || []
    page.total = data.total || 0
  } catch (error) {
    toastStore.push(error.message || '加载支付链接失败', 'error')
  } finally {
    loading.value = false
  }
}

const openCreate = () => {
  dialogMode.value = 'create'
  currentRecord.value = null
  resetForm()
  dialogVisible.value = true
}

const openEdit = (record) => {
  dialogMode.value = 'edit'
  currentRecord.value = record
  Object.assign(form, {
    targetId: record.targetId,
    payUrl: record.payUrl || '',
    status: Number(record.status ?? 1),
    remark: record.remark || '',
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!form.targetId) {
    toastStore.push('请选择套餐', 'warning')
    return
  }
  if (!form.payUrl.trim()) {
    toastStore.push('请输入支付链接', 'warning')
    return
  }

  submitting.value = true
  try {
    if (dialogMode.value === 'create') {
      await createPayLink({
        targetId: Number(form.targetId),
        payUrl: form.payUrl.trim(),
        status: Number(form.status),
        remark: form.remark,
      })
      toastStore.push('支付链接创建成功', 'success')
    } else {
      await updatePayLink(currentRecord.value.id, {
        payUrl: form.payUrl.trim(),
        status: Number(form.status),
        remark: form.remark,
      })
      toastStore.push('支付链接更新成功', 'success')
    }
    dialogVisible.value = false
    await loadPayLinks()
  } catch (error) {
    toastStore.push(error.message || '保存支付链接失败', 'error')
  } finally {
    submitting.value = false
  }
}

const handleToggleStatus = async (record) => {
  try {
    await updatePayLinkStatus(record.id, Number(record.status) === 1 ? 0 : 1)
    toastStore.push('支付链接状态已更新', 'success')
    await loadPayLinks()
  } catch (error) {
    toastStore.push(error.message || '更新支付链接状态失败', 'error')
  }
}

const handleDelete = async (record) => {
  if (!window.confirm(`确认删除支付链接“${record.planName || record.targetId}”吗？`)) {
    return
  }
  try {
    await deletePayLink(record.id)
    toastStore.push('支付链接已删除', 'success')
    if (records.value.length === 1 && page.page > 1) {
      page.page -= 1
    }
    await loadPayLinks()
  } catch (error) {
    toastStore.push(error.message || '删除支付链接失败', 'error')
  }
}

const resetFilters = async () => {
  Object.assign(filters, {
    targetId: '',
    planName: '',
    status: '',
  })
  page.page = 1
  await loadPayLinks()
}

const changePage = async (nextPage) => {
  if (nextPage < 1 || nextPage > pageCount.value) {
    return
  }
  page.page = nextPage
  await loadPayLinks()
}

onMounted(async () => {
  await Promise.all([loadPlans(), loadPayLinks()])
})
</script>

<template>
  <div class="p-6 space-y-6">
    <div class="flex flex-col gap-3 lg:flex-row lg:items-end lg:justify-between">
      <div>
        <h1 class="text-2xl font-bold text-slate-900">支付链接管理</h1>
        <p class="text-slate-500">集中维护套餐专属的第三方支付跳转链接，便于渠道投放和活动运营。</p>
      </div>
      <div class="flex items-center gap-3">
        <div class="rounded-2xl border border-slate-200 bg-white px-4 py-3 shadow-sm">
          <div class="text-xs text-slate-400">当前页启用链接</div>
          <div class="mt-2 text-2xl font-semibold text-slate-900">{{ summary.enabled }}</div>
        </div>
        <button class="btn-primary" @click="openCreate">新增支付链接</button>
      </div>
    </div>

    <div class="card">
      <div class="card-body grid gap-4 md:grid-cols-4">
        <div>
          <label class="text-sm text-slate-500">套餐</label>
          <select v-model="filters.targetId" class="input mt-2">
            <option value="">全部</option>
            <option v-for="plan in planOptions" :key="plan.id" :value="plan.id">{{ plan.planName }}</option>
          </select>
        </div>
        <div>
          <label class="text-sm text-slate-500">套餐名称关键字</label>
          <input v-model.trim="filters.planName" class="input mt-2" placeholder="支持模糊搜索" />
        </div>
        <div>
          <label class="text-sm text-slate-500">状态</label>
          <select v-model="filters.status" class="input mt-2">
            <option value="">全部</option>
            <option :value="1">启用</option>
            <option :value="0">停用</option>
          </select>
        </div>
        <div class="flex items-end justify-end gap-3">
          <button class="btn-primary h-11" @click="page.page = 1; loadPayLinks()">搜索</button>
          <button class="btn-outline h-11" @click="resetFilters">重置</button>
        </div>
      </div>
    </div>

    <div class="card">
      <div class="card-header">
        <div>
          <h2 class="card-title">支付链接列表</h2>
          <p class="text-sm text-slate-400 mt-1">共 {{ page.total }} 条记录，当前页隐藏套餐 {{ summary.hiddenPlans }} 个</p>
        </div>
        <button class="btn-outline" :disabled="loading" @click="loadPayLinks">{{ loading ? '刷新中...' : '刷新' }}</button>
      </div>
      <div class="card-body">
        <div class="table-wrap">
          <table class="table min-w-[1240px]">
            <thead>
              <tr>
                <th>套餐</th>
                <th>支付链接</th>
                <th>链接状态</th>
                <th>套餐状态</th>
                <th>展示状态</th>
                <th>备注</th>
                <th>更新时间</th>
                <th class="text-right">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="!records.length && !loading">
                <td colspan="8" class="empty-state">暂无支付链接数据</td>
              </tr>
              <tr v-for="record in records" :key="record.id">
                <td>
                  <div class="font-medium text-slate-800">{{ record.planName || '-' }}</div>
                  <div class="text-xs text-slate-400 mt-1">套餐 ID：{{ record.targetId }}</div>
                </td>
                <td class="font-mono text-xs text-slate-600 break-all max-w-[320px]">{{ record.payUrl }}</td>
                <td>
                  <span class="badge" :class="Number(record.status) === 1 ? 'badge-success' : 'badge-danger'">
                    {{ formatStatus(record.status) }}
                  </span>
                </td>
                <td>
                  <span class="badge" :class="Number(record.planStatus) === 1 ? 'badge-success' : 'badge-danger'">
                    {{ formatStatus(record.planStatus) }}
                  </span>
                </td>
                <td>
                  <span class="badge" :class="Number(record.planVisible) === 1 ? 'badge-success' : 'badge-warning'">
                    {{ formatBooleanFlag(record.planVisible, '显示', '隐藏') }}
                  </span>
                </td>
                <td class="max-w-[220px] break-words">{{ record.remark || '-' }}</td>
                <td>{{ formatDateTime(record.updatedAt) }}</td>
                <td>
                  <div class="flex items-center justify-end gap-2">
                    <button class="btn-outline" @click="openEdit(record)">编辑</button>
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
      <div class="modal-panel max-w-2xl">
        <div class="flex items-center justify-between gap-4">
          <div>
            <h3 class="text-lg font-semibold text-slate-800">{{ dialogMode === 'create' ? '新增支付链接' : '编辑支付链接' }}</h3>
            <p class="text-sm text-slate-400 mt-1">绑定套餐专属跳转链接，方便用户从活动页直接进入购买流程。</p>
          </div>
          <button class="btn-text" @click="dialogVisible = false">关闭</button>
        </div>

        <div class="mt-6 grid gap-4 md:grid-cols-2">
          <div class="md:col-span-2">
            <label class="text-sm text-slate-500">套餐</label>
            <select v-model.number="form.targetId" class="input mt-2" :disabled="dialogMode === 'edit'">
              <option value="">请选择套餐</option>
              <option v-for="plan in planOptions" :key="plan.id" :value="plan.id">{{ plan.planName }}</option>
            </select>
          </div>
          <div class="md:col-span-2">
            <label class="text-sm text-slate-500">支付链接</label>
            <input v-model.trim="form.payUrl" class="input mt-2" placeholder="请输入第三方支付跳转链接" />
          </div>
          <div>
            <label class="text-sm text-slate-500">状态</label>
            <select v-model.number="form.status" class="input mt-2">
              <option :value="1">启用</option>
              <option :value="0">停用</option>
            </select>
          </div>
          <div class="md:col-span-2">
            <label class="text-sm text-slate-500">备注</label>
            <textarea v-model.trim="form.remark" class="input mt-2 min-h-24" placeholder="填写投放渠道、活动说明或补充信息"></textarea>
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
