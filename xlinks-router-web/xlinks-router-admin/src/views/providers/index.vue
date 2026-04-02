<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { createProvider, deleteProvider, listProviders, updateProvider, updateProviderStatus } from '@/api/admin'
import { useToastStore } from '@/stores/toast'
import { formatDateTime, formatNullable, formatStatus } from '@/utils/format'

const toastStore = useToastStore()
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const dialogMode = ref('create')
const currentId = ref(null)
const filters = reactive({
  providerCode: '',
  providerName: '',
  status: '',
})
const page = reactive({ page: 1, pageSize: 10, total: 0 })
const records = ref([])
const form = reactive({
  providerCode: '',
  providerName: '',
  providerType: 'openai-compatible',
  supportedProtocols: 'chat/completions,responses',
  priority: 0,
  baseUrl: '',
  providerLogo: '',
  providerWebsite: '',
  status: 1,
  remark: '',
})

const pageCount = computed(() => Math.max(1, Math.ceil((page.total || 0) / page.pageSize)))

const resetForm = () => {
  Object.assign(form, {
    providerCode: '',
    providerName: '',
    providerType: 'openai-compatible',
    supportedProtocols: 'chat/completions,responses',
    priority: 0,
    baseUrl: '',
    providerLogo: '',
    providerWebsite: '',
    status: 1,
    remark: '',
  })
}

const loadProviders = async () => {
  loading.value = true
  try {
    const data = await listProviders({
      page: page.page,
      pageSize: page.pageSize,
      providerCode: filters.providerCode,
      providerName: filters.providerName,
      status: filters.status,
    })
    records.value = data.records || []
    page.total = data.total || 0
  } catch (error) {
    toastStore.push(error.message || '加载服务商失败', 'error')
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
    providerCode: record.providerCode || '',
    providerName: record.providerName || '',
    providerType: record.providerType || 'openai-compatible',
    supportedProtocols: record.supportedProtocols || '',
    priority: record.priority ?? 0,
    baseUrl: record.baseUrl || '',
    providerLogo: record.providerLogo || '',
    providerWebsite: record.providerWebsite || '',
    status: record.status ?? 1,
    remark: record.remark || '',
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!form.providerName || !form.baseUrl || (dialogMode.value === 'create' && !form.providerCode)) {
    toastStore.push('请完整填写服务商编码、名称与 Base URL', 'warning')
    return
  }

  submitting.value = true
  try {
    if (dialogMode.value === 'create') {
      await createProvider({ ...form, priority: Number(form.priority || 0), status: Number(form.status || 1) })
      toastStore.push('服务商创建成功', 'success')
    } else {
      await updateProvider(currentId.value, {
        providerName: form.providerName,
        supportedProtocols: form.supportedProtocols,
        priority: Number(form.priority || 0),
        baseUrl: form.baseUrl,
        providerLogo: form.providerLogo,
        providerWebsite: form.providerWebsite,
        remark: form.remark,
      })
      toastStore.push('服务商更新成功', 'success')
    }
    dialogVisible.value = false
    await loadProviders()
  } catch (error) {
    toastStore.push(error.message || '保存服务商失败', 'error')
  } finally {
    submitting.value = false
  }
}

const handleToggleStatus = async (record) => {
  try {
    await updateProviderStatus(record.id, Number(record.status) === 1 ? 0 : 1)
    toastStore.push('服务商状态已更新', 'success')
    await loadProviders()
  } catch (error) {
    toastStore.push(error.message || '更新状态失败', 'error')
  }
}

const handleDelete = async (record) => {
  if (!window.confirm(`确认删除服务商「${record.providerName}」吗？`)) {
    return
  }
  try {
    await deleteProvider(record.id)
    toastStore.push('服务商已删除', 'success')
    if (records.value.length === 1 && page.page > 1) {
      page.page -= 1
    }
    await loadProviders()
  } catch (error) {
    toastStore.push(error.message || '删除服务商失败', 'error')
  }
}

const resetFilters = async () => {
  Object.assign(filters, { providerCode: '', providerName: '', status: '' })
  page.page = 1
  await loadProviders()
}

const changePage = async (nextPage) => {
  if (nextPage < 1 || nextPage > pageCount.value) {
    return
  }
  page.page = nextPage
  await loadProviders()
}

onMounted(loadProviders)
</script>

<template>
  <div class="p-6 space-y-6">
    <div class="flex flex-col gap-3 lg:flex-row lg:items-end lg:justify-between">
      <div>
        <h1 class="text-2xl font-bold text-slate-900">服务商管理</h1>
        <p class="text-slate-500">维护上游服务商基础信息、协议能力和路由优先级。</p>
      </div>
      <button class="btn-primary" @click="openCreate">新增服务商</button>
    </div>

    <div class="card">
      <div class="card-body grid gap-4 md:grid-cols-4">
        <div>
          <label class="text-sm text-slate-500">服务商编码</label>
          <input v-model.trim="filters.providerCode" class="input mt-2" placeholder="如 openai" />
        </div>
        <div>
          <label class="text-sm text-slate-500">服务商名称</label>
          <input v-model.trim="filters.providerName" class="input mt-2" placeholder="如 OpenAI" />
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
          <button class="btn-primary h-11" @click="page.page = 1; loadProviders()">搜索</button>
          <button class="btn-outline h-11" @click="resetFilters">重置</button>
        </div>
      </div>
    </div>

    <div class="card">
      <div class="card-header">
        <div>
          <h2 class="card-title">服务商列表</h2>
          <p class="text-sm text-slate-400 mt-1">共 {{ page.total }} 条记录</p>
        </div>
        <button class="btn-outline" :disabled="loading" @click="loadProviders">{{ loading ? '刷新中...' : '刷新' }}</button>
      </div>
      <div class="card-body">
        <div class="table-wrap">
          <table class="table">
            <thead>
              <tr>
                <th>编码</th>
                <th>名称</th>
                <th>协议</th>
                <th>优先级</th>
                <th>Base URL</th>
                <th>状态</th>
                <th>更新时间</th>
                <th class="text-right">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="!records.length && !loading">
                <td colspan="8" class="empty-state">暂无服务商数据</td>
              </tr>
              <tr v-for="record in records" :key="record.id">
                <td>{{ record.providerCode }}</td>
                <td>
                  <div class="font-medium text-slate-800">{{ record.providerName }}</div>
                  <div class="text-xs text-slate-400 mt-1">{{ record.providerType || '-' }}</div>
                </td>
                <td>{{ formatNullable(record.supportedProtocols) }}</td>
                <td>{{ record.priority ?? 0 }}</td>
                <td class="max-w-[280px] break-all">{{ record.baseUrl }}</td>
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
      <div class="absolute inset-0 bg-slate-900/50" @click="dialogVisible = false"></div>
      <div class="modal-panel">
        <div class="flex items-center justify-between gap-4">
          <div>
            <h3 class="text-lg font-semibold text-slate-800">{{ dialogMode === 'create' ? '新增服务商' : '编辑服务商' }}</h3>
            <p class="text-sm text-slate-400 mt-1">创建时可维护协议类型与启用状态，编辑时仅修改基础信息。</p>
          </div>
          <button class="btn-text" @click="dialogVisible = false">关闭</button>
        </div>

        <div class="mt-6 grid gap-4 md:grid-cols-2">
          <div>
            <label class="text-sm text-slate-500">服务商编码</label>
            <input v-model.trim="form.providerCode" class="input mt-2" :disabled="dialogMode === 'edit'" placeholder="openai" />
          </div>
          <div>
            <label class="text-sm text-slate-500">服务商名称</label>
            <input v-model.trim="form.providerName" class="input mt-2" placeholder="OpenAI" />
          </div>
          <div>
            <label class="text-sm text-slate-500">协议类型</label>
            <input v-model.trim="form.providerType" class="input mt-2" :disabled="dialogMode === 'edit'" placeholder="openai-compatible" />
          </div>
          <div>
            <label class="text-sm text-slate-500">路由优先级</label>
            <input v-model.number="form.priority" type="number" class="input mt-2" placeholder="100" />
          </div>
          <div class="md:col-span-2">
            <label class="text-sm text-slate-500">支持协议</label>
            <input v-model.trim="form.supportedProtocols" class="input mt-2" placeholder="chat/completions,responses" />
          </div>
          <div class="md:col-span-2">
            <label class="text-sm text-slate-500">Base URL</label>
            <input v-model.trim="form.baseUrl" class="input mt-2" placeholder="https://api.openai.com/v1" />
          </div>
          <div>
            <label class="text-sm text-slate-500">Logo URL</label>
            <input v-model.trim="form.providerLogo" class="input mt-2" placeholder="可选" />
          </div>
          <div>
            <label class="text-sm text-slate-500">官网地址</label>
            <input v-model.trim="form.providerWebsite" class="input mt-2" placeholder="可选" />
          </div>
          <div v-if="dialogMode === 'create'">
            <label class="text-sm text-slate-500">初始状态</label>
            <select v-model.number="form.status" class="input mt-2">
              <option :value="1">启用</option>
              <option :value="0">停用</option>
            </select>
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
