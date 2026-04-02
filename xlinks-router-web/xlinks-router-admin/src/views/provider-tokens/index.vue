<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import {
  createProviderToken,
  deleteProviderToken,
  listProviderTokens,
  listProviders,
  updateProviderToken,
  updateProviderTokenStatus,
} from '@/api/admin'
import { useToastStore } from '@/stores/toast'
import { formatDateTime, formatNullable, formatStatus, normalizeDateTimeInput, toDateTimeLocalValue } from '@/utils/format'

const toastStore = useToastStore()
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const dialogMode = ref('create')
const currentId = ref(null)
const filters = reactive({ providerId: '', tokenStatus: '' })
const page = reactive({ page: 1, pageSize: 10, total: 0 })
const records = ref([])
const providers = ref([])
const form = reactive({
  providerId: '',
  tokenName: '',
  tokenValue: '',
  tokenStatus: 1,
  quotaTotal: '',
  expireTime: '',
  remark: '',
})

const pageCount = computed(() => Math.max(1, Math.ceil((page.total || 0) / page.pageSize)))
const providerNameMap = computed(() => Object.fromEntries(providers.value.map((item) => [item.id, item.providerName])))

const resetForm = () => {
  Object.assign(form, {
    providerId: '',
    tokenName: '',
    tokenValue: '',
    tokenStatus: 1,
    quotaTotal: '',
    expireTime: '',
    remark: '',
  })
}

const loadProviders = async () => {
  try {
    const data = await listProviders({ page: 1, pageSize: 200 })
    providers.value = data.records || []
  } catch (error) {
    toastStore.push(error.message || '加载服务商选项失败', 'error')
  }
}

const loadTokens = async () => {
  loading.value = true
  try {
    const data = await listProviderTokens({
      page: page.page,
      pageSize: page.pageSize,
      providerId: filters.providerId,
      tokenStatus: filters.tokenStatus,
    })
    records.value = data.records || []
    page.total = data.total || 0
  } catch (error) {
    toastStore.push(error.message || '加载服务商 Token 失败', 'error')
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
    providerId: record.providerId || '',
    tokenName: record.tokenName || '',
    tokenValue: '',
    tokenStatus: record.tokenStatus ?? 1,
    quotaTotal: record.quotaTotal ?? '',
    expireTime: toDateTimeLocalValue(record.expireTime),
    remark: record.remark || '',
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (dialogMode.value === 'create' && (!form.providerId || !form.tokenName || !form.tokenValue)) {
    toastStore.push('请填写服务商、Token 名称和 Token 值', 'warning')
    return
  }

  submitting.value = true
  try {
    const payload = {
      providerId: Number(form.providerId),
      tokenName: form.tokenName,
      tokenValue: form.tokenValue || undefined,
      tokenStatus: Number(form.tokenStatus || 1),
      quotaTotal: form.quotaTotal === '' ? null : Number(form.quotaTotal),
      expireTime: normalizeDateTimeInput(form.expireTime),
      remark: form.remark,
    }

    if (dialogMode.value === 'create') {
      await createProviderToken(payload)
      toastStore.push('服务商 Token 创建成功', 'success')
    } else {
      await updateProviderToken(currentId.value, {
        tokenName: payload.tokenName,
        tokenValue: payload.tokenValue,
        quotaTotal: payload.quotaTotal,
        expireTime: payload.expireTime,
        remark: payload.remark,
      })
      toastStore.push('服务商 Token 更新成功', 'success')
    }
    dialogVisible.value = false
    await loadTokens()
  } catch (error) {
    toastStore.push(error.message || '保存服务商 Token 失败', 'error')
  } finally {
    submitting.value = false
  }
}

const handleToggleStatus = async (record) => {
  try {
    await updateProviderTokenStatus(record.id, Number(record.tokenStatus) === 1 ? 0 : 1)
    toastStore.push('服务商 Token 状态已更新', 'success')
    await loadTokens()
  } catch (error) {
    toastStore.push(error.message || '更新状态失败', 'error')
  }
}

const handleDelete = async (record) => {
  if (!window.confirm(`确认删除 Token「${record.tokenName}」吗？`)) {
    return
  }
  try {
    await deleteProviderToken(record.id)
    toastStore.push('服务商 Token 已删除', 'success')
    if (records.value.length === 1 && page.page > 1) {
      page.page -= 1
    }
    await loadTokens()
  } catch (error) {
    toastStore.push(error.message || '删除服务商 Token 失败', 'error')
  }
}

const resetFilters = async () => {
  Object.assign(filters, { providerId: '', tokenStatus: '' })
  page.page = 1
  await loadTokens()
}

const changePage = async (nextPage) => {
  if (nextPage < 1 || nextPage > pageCount.value) {
    return
  }
  page.page = nextPage
  await loadTokens()
}

onMounted(async () => {
  await loadProviders()
  await loadTokens()
})
</script>

<template>
  <div class="p-6 space-y-6">
    <div class="flex flex-col gap-3 lg:flex-row lg:items-end lg:justify-between">
      <div>
        <h1 class="text-2xl font-bold text-slate-900">服务商 Token</h1>
        <p class="text-slate-500">维护上游服务商调用凭证、配额上限与过期时间。</p>
      </div>
      <button class="btn-primary" @click="openCreate">新增服务商 Token</button>
    </div>

    <div class="card">
      <div class="card-body grid gap-4 md:grid-cols-4">
        <div>
          <label class="text-sm text-slate-500">服务商</label>
          <select v-model="filters.providerId" class="input mt-2">
            <option value="">全部</option>
            <option v-for="provider in providers" :key="provider.id" :value="provider.id">{{ provider.providerName }}</option>
          </select>
        </div>
        <div>
          <label class="text-sm text-slate-500">状态</label>
          <select v-model="filters.tokenStatus" class="input mt-2">
            <option value="">全部</option>
            <option :value="1">启用</option>
            <option :value="0">停用</option>
          </select>
        </div>
        <div class="md:col-span-2 flex items-end justify-end gap-3">
          <button class="btn-primary h-11" @click="page.page = 1; loadTokens()">搜索</button>
          <button class="btn-outline h-11" @click="resetFilters">重置</button>
        </div>
      </div>
    </div>

    <div class="card">
      <div class="card-header">
        <div>
          <h2 class="card-title">服务商 Token 列表</h2>
          <p class="text-sm text-slate-400 mt-1">共 {{ page.total }} 条记录</p>
        </div>
        <button class="btn-outline" :disabled="loading" @click="loadTokens">{{ loading ? '刷新中...' : '刷新' }}</button>
      </div>
      <div class="card-body">
        <div class="table-wrap">
          <table class="table">
            <thead>
              <tr>
                <th>Token 名称</th>
                <th>服务商</th>
                <th>总配额</th>
                <th>已用配额</th>
                <th>到期时间</th>
                <th>状态</th>
                <th>更新时间</th>
                <th class="text-right">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="!records.length && !loading">
                <td colspan="8" class="empty-state">暂无服务商 Token 数据</td>
              </tr>
              <tr v-for="record in records" :key="record.id">
                <td>
                  <div class="font-medium text-slate-800">{{ record.tokenName }}</div>
                  <div class="text-xs text-slate-400 mt-1">{{ formatNullable(record.remark) }}</div>
                </td>
                <td>{{ providerNameMap[record.providerId] || `#${record.providerId}` }}</td>
                <td>{{ record.quotaTotal ?? '-' }}</td>
                <td>{{ record.quotaUsed ?? 0 }}</td>
                <td>{{ formatDateTime(record.expireTime) }}</td>
                <td>
                  <span class="badge" :class="Number(record.tokenStatus) === 1 ? 'badge-success' : 'badge-danger'">
                    {{ formatStatus(record.tokenStatus) }}
                  </span>
                </td>
                <td>{{ formatDateTime(record.updatedAt) }}</td>
                <td>
                  <div class="flex items-center justify-end gap-2">
                    <button class="btn-outline" @click="openEdit(record)">编辑</button>
                    <button class="btn-outline" @click="handleToggleStatus(record)">
                      {{ Number(record.tokenStatus) === 1 ? '停用' : '启用' }}
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
            <h3 class="text-lg font-semibold text-slate-800">{{ dialogMode === 'create' ? '新增服务商 Token' : '编辑服务商 Token' }}</h3>
            <p class="text-sm text-slate-400 mt-1">出于安全考虑，列表和详情不会返回 Token 明文。</p>
          </div>
          <button class="btn-text" @click="dialogVisible = false">关闭</button>
        </div>

        <div class="mt-6 grid gap-4 md:grid-cols-2">
          <div>
            <label class="text-sm text-slate-500">服务商</label>
            <select v-model="form.providerId" class="input mt-2" :disabled="dialogMode === 'edit'">
              <option value="">请选择服务商</option>
              <option v-for="provider in providers" :key="provider.id" :value="provider.id">{{ provider.providerName }}</option>
            </select>
          </div>
          <div>
            <label class="text-sm text-slate-500">Token 名称</label>
            <input v-model.trim="form.tokenName" class="input mt-2" placeholder="如 OpenAI 主账号" />
          </div>
          <div class="md:col-span-2">
            <label class="text-sm text-slate-500">Token 值</label>
            <input v-model.trim="form.tokenValue" class="input mt-2" :placeholder="dialogMode === 'create' ? '请输入 Token 明文' : '如需轮换再填写'" />
          </div>
          <div>
            <label class="text-sm text-slate-500">总配额</label>
            <input v-model="form.quotaTotal" type="number" class="input mt-2" placeholder="可选" />
          </div>
          <div v-if="dialogMode === 'create'">
            <label class="text-sm text-slate-500">初始状态</label>
            <select v-model.number="form.tokenStatus" class="input mt-2">
              <option :value="1">启用</option>
              <option :value="0">停用</option>
            </select>
          </div>
          <div>
            <label class="text-sm text-slate-500">到期时间</label>
            <input v-model="form.expireTime" type="datetime-local" class="input mt-2" />
          </div>
          <div class="md:col-span-2">
            <label class="text-sm text-slate-500">备注</label>
            <textarea v-model.trim="form.remark" class="input mt-2 min-h-24" placeholder="可选"></textarea>
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
