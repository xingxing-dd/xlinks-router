<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import {
  createCustomerToken,
  deleteCustomerToken,
  listCustomerTokens,
  updateCustomerToken,
  updateCustomerTokenStatus,
} from '@/api/admin'
import { useToastStore } from '@/stores/toast'
import { formatDateTime, formatStatus, normalizeDateTimeInput, summarizeJsonArray, toDateTimeLocalValue } from '@/utils/format'

const toastStore = useToastStore()
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const generatedToken = ref('')
const dialogMode = ref('create')
const currentId = ref(null)
const filters = reactive({ customerName: '', status: '' })
const page = reactive({ page: 1, pageSize: 10, total: 0 })
const records = ref([])
const form = reactive({
  customerName: '',
  tokenName: '',
  status: 1,
  expireTime: '',
  allowedModels: '[]',
  remark: '',
})

const pageCount = computed(() => Math.max(1, Math.ceil((page.total || 0) / page.pageSize)))
const formatTokenCount = (value) => Number(value || 0).toLocaleString('en-US')
const formatQuotaAmount = (value) => `$${Number(value || 0).toFixed(6)}`

const resetForm = () => {
  Object.assign(form, {
    customerName: '',
    tokenName: '',
    status: 1,
    expireTime: '',
    allowedModels: '[]',
    remark: '',
  })
}

const loadTokens = async () => {
  loading.value = true
  try {
    const data = await listCustomerTokens({
      page: page.page,
      pageSize: page.pageSize,
      customerName: filters.customerName,
      status: filters.status,
    })
    records.value = data.records || []
    page.total = data.total || 0
  } catch (error) {
    toastStore.push(error.message || '加载客户 Token 失败', 'error')
  } finally {
    loading.value = false
  }
}

const openCreate = () => {
  dialogMode.value = 'create'
  currentId.value = null
  generatedToken.value = ''
  resetForm()
  dialogVisible.value = true
}

const openEdit = (record) => {
  dialogMode.value = 'edit'
  currentId.value = record.id
  generatedToken.value = ''
  Object.assign(form, {
    customerName: record.customerName || '',
    tokenName: record.tokenName || '',
    status: record.status ?? 1,
    expireTime: toDateTimeLocalValue(record.expireTime),
    allowedModels: record.allowedModels || '[]',
    remark: record.remark || '',
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!form.customerName || !form.tokenName) {
    toastStore.push('请填写客户标识和 Token 名称', 'warning')
    return
  }

  submitting.value = true
  try {
    const payload = {
      customerName: form.customerName,
      tokenName: form.tokenName,
      status: Number(form.status || 1),
      expireTime: normalizeDateTimeInput(form.expireTime),
      allowedModels: form.allowedModels || '[]',
      remark: form.remark,
    }

    if (dialogMode.value === 'create') {
      const created = await createCustomerToken(payload)
      generatedToken.value = created.tokenValue || ''
      toastStore.push('客户 Token 创建成功，请立即保存生成的 Token', 'success')
      resetForm()
    } else {
      await updateCustomerToken(currentId.value, payload)
      toastStore.push('客户 Token 更新成功', 'success')
      dialogVisible.value = false
      await loadTokens()
    }
  } catch (error) {
    toastStore.push(error.message || '保存客户 Token 失败', 'error')
  } finally {
    submitting.value = false
  }
}

const closeDialog = async () => {
  dialogVisible.value = false
  generatedToken.value = ''
  await loadTokens()
}

const handleToggleStatus = async (record) => {
  try {
    await updateCustomerTokenStatus(record.id, Number(record.status) === 1 ? 0 : 1)
    toastStore.push('客户 Token 状态已更新', 'success')
    await loadTokens()
  } catch (error) {
    toastStore.push(error.message || '更新状态失败', 'error')
  }
}

const handleDelete = async (record) => {
  if (!window.confirm(`确认删除客户 Token「${record.tokenName}」吗？`)) {
    return
  }
  try {
    await deleteCustomerToken(record.id)
    toastStore.push('客户 Token 已删除', 'success')
    if (records.value.length === 1 && page.page > 1) {
      page.page -= 1
    }
    await loadTokens()
  } catch (error) {
    toastStore.push(error.message || '删除客户 Token 失败', 'error')
  }
}

const resetFilters = async () => {
  Object.assign(filters, { customerName: '', status: '' })
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

onMounted(loadTokens)
</script>

<template>
  <div class="p-6 space-y-6">
    <div class="flex flex-col gap-3 lg:flex-row lg:items-end lg:justify-between">
      <div>
        <h1 class="text-2xl font-bold text-slate-900">客户 Token</h1>
        <p class="text-slate-500">为客户账户签发调用凭证，并维护允许访问的模型列表。</p>
      </div>
      <button class="btn-primary" @click="openCreate">新增客户 Token</button>
    </div>

    <div class="card">
      <div class="card-body grid gap-4 md:grid-cols-4">
        <div>
          <label class="text-sm text-slate-500">客户标识</label>
          <input v-model.trim="filters.customerName" class="input mt-2" placeholder="用户名 / 手机 / 邮箱" />
        </div>
        <div>
          <label class="text-sm text-slate-500">状态</label>
          <select v-model="filters.status" class="input mt-2">
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
          <h2 class="card-title">客户 Token 列表</h2>
          <p class="text-sm text-slate-400 mt-1">共 {{ page.total }} 条记录</p>
        </div>
        <button class="btn-outline" :disabled="loading" @click="loadTokens">{{ loading ? '刷新中...' : '刷新' }}</button>
      </div>
      <div class="card-body">
        <div class="table-wrap">
          <table class="table">
            <thead>
              <tr>
                <th>客户标识</th>
                <th>Token 名称</th>
                <th>允许模型</th>
                <th>今日消耗</th>
                <th>累计消耗</th>
                <th>到期时间</th>
                <th>状态</th>
                <th>更新时间</th>
                <th class="text-right">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="!records.length && !loading">
                <td colspan="9" class="empty-state">暂无客户 Token 数据</td>
              </tr>
              <tr v-for="record in records" :key="record.id">
                <td>{{ record.customerName }}</td>
                <td>
                  <div class="font-medium text-slate-800">{{ record.tokenName }}</div>
                  <div class="text-xs text-slate-400 mt-1">{{ record.remark || '-' }}</div>
                </td>
                <td class="max-w-[280px] break-all">{{ summarizeJsonArray(record.allowedModels) }}</td>
                <td>
                  <div class="font-mono text-sm text-slate-700">{{ formatQuotaAmount(record.usedQuota) }}</div>
                  <div v-if="Number(record.todayUsedTokens) > 0" class="text-xs text-slate-400 mt-1">
                    Token: {{ formatTokenCount(record.todayUsedTokens) }}
                  </div>
                </td>
                <td>
                  <div class="font-mono text-sm text-slate-700">{{ formatQuotaAmount(record.totalUsedQuota) }}</div>
                  <div v-if="Number(record.totalUsedTokens) > 0" class="text-xs text-slate-400 mt-1">
                    Token: {{ formatTokenCount(record.totalUsedTokens) }}
                  </div>
                </td>
                <td>{{ formatDateTime(record.expireTime) }}</td>
                <td>
                  <span class="badge" :class="Number(record.status) === 1 ? 'badge-success' : 'badge-danger'">
                    {{ formatStatus(record.status) }}
                  </span>
                </td>
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
      <div class="absolute inset-0 bg-slate-900/50" @click="closeDialog"></div>
      <div class="modal-panel max-w-3xl">
        <div class="flex items-center justify-between gap-4">
          <div>
            <h3 class="text-lg font-semibold text-slate-800">{{ dialogMode === 'create' ? '新增客户 Token' : '编辑客户 Token' }}</h3>
            <p class="text-sm text-slate-400 mt-1">客户标识支持用户名、手机号或邮箱，后端会自动解析为客户账号。</p>
          </div>
          <button class="btn-text" @click="closeDialog">关闭</button>
        </div>

        <div v-if="generatedToken" class="mt-5 rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-4">
          <p class="text-sm font-medium text-emerald-700">Token 已生成，仅本次展示，请立即保存：</p>
          <div class="mt-2 break-all rounded-xl bg-white px-4 py-3 text-sm text-slate-700 border border-emerald-100">{{ generatedToken }}</div>
          <div class="mt-4 flex justify-end">
            <button class="btn-primary" @click="closeDialog">我已保存</button>
          </div>
        </div>

        <div v-else class="mt-6 grid gap-4 md:grid-cols-2">
          <div>
            <label class="text-sm text-slate-500">客户标识</label>
            <input v-model.trim="form.customerName" class="input mt-2" placeholder="用户名 / 手机 / 邮箱" />
          </div>
          <div>
            <label class="text-sm text-slate-500">Token 名称</label>
            <input v-model.trim="form.tokenName" class="input mt-2" placeholder="如生产环境 Token" />
          </div>
          <div>
            <label class="text-sm text-slate-500">状态</label>
            <select v-model.number="form.status" class="input mt-2">
              <option :value="1">启用</option>
              <option :value="0">停用</option>
            </select>
          </div>
          <div>
            <label class="text-sm text-slate-500">到期时间</label>
            <input v-model="form.expireTime" type="datetime-local" class="input mt-2" />
          </div>
          <div class="md:col-span-2">
            <label class="text-sm text-slate-500">允许模型</label>
            <textarea v-model.trim="form.allowedModels" class="input mt-2 min-h-24" placeholder='例如 ["gpt-5.4","deepseek-chat"]'></textarea>
          </div>
          <div class="md:col-span-2">
            <label class="text-sm text-slate-500">备注</label>
            <textarea v-model.trim="form.remark" class="input mt-2 min-h-24" placeholder="可选"></textarea>
          </div>
        </div>

        <div v-if="!generatedToken" class="mt-6 flex justify-end gap-3">
          <button class="btn-outline" @click="closeDialog">取消</button>
          <button class="btn-primary" :disabled="submitting" @click="handleSubmit">
            {{ submitting ? '提交中...' : dialogMode === 'create' ? '确认新增' : '确认保存' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
