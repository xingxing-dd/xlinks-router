<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { createProvider, deleteProvider, listProviders, updateProvider, updateProviderStatus } from '@/api/admin'
import { useToastStore } from '@/stores/toast'
import { formatDateTime, formatStatus } from '@/utils/format'

const toastStore = useToastStore()
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const dialogMode = ref('create')
const currentId = ref(null)

const defaultProtocolOptions = [
  { value: 'chat/completions', label: 'Chat Completions' },
  { value: 'responses', label: 'Responses' },
  { value: 'messages', label: 'Anthropic Messages' },
  { value: '*', label: 'All Protocols (*)' },
]
const protocolOptions = ref([...defaultProtocolOptions])

const filters = reactive({
  providerCode: '',
  providerName: '',
  status: '',
})

const page = reactive({ page: 1, pageSize: 10, total: 0 })
const records = ref([])
const protocolDropdownOpen = ref(false)
const protocolPickerRef = ref(null)

const form = reactive({
  providerCode: '',
  providerName: '',
  supportedProtocols: ['chat/completions', 'responses'],
  priority: 0,
  baseUrl: '',
  providerLogo: '',
  providerWebsite: '',
  status: 1,
  remark: '',
})

const pageCount = computed(() => Math.max(1, Math.ceil((page.total || 0) / page.pageSize)))
const selectedProtocolText = computed(() => {
  const selected = normalizeProtocols(form.supportedProtocols)
  if (!selected.length) return '请选择支持协议'
  const labelMap = Object.fromEntries(protocolOptions.value.map((item) => [item.value, item.label]))
  return selected.map((item) => labelMap[item] || item).join(', ')
})

const selectedProtocolItems = computed(() => {
  const selected = normalizeProtocols(form.supportedProtocols)
  const labelMap = Object.fromEntries(protocolOptions.value.map((item) => [item.value, item.label]))
  return selected.map((value) => ({ value, label: labelMap[value] || value }))
})

const parseProtocols = (raw) => {
  if (!raw || !String(raw).trim()) return []
  return String(raw)
    .split(/[,;，]/)
    .map((item) => item.trim())
    .filter(Boolean)
}

const normalizeProtocols = (items) => {
  const unique = Array.from(new Set((items || []).map((item) => String(item).trim()).filter(Boolean)))
  if (unique.includes('*')) {
    return ['*']
  }
  return unique
}

const stringifyProtocols = (items) => normalizeProtocols(items).join(',')

const ensureProtocolOptions = (items) => {
  const values = new Set(protocolOptions.value.map((item) => item.value))
  for (const protocol of items || []) {
    if (!values.has(protocol)) {
      protocolOptions.value.push({ value: protocol, label: protocol })
      values.add(protocol)
    }
  }
}

const toggleProtocolSelection = (value) => {
  const set = new Set(form.supportedProtocols || [])
  if (value === '*') {
    if (set.has('*')) {
      set.delete('*')
    } else {
      set.clear()
      set.add('*')
    }
  } else {
    if (set.has(value)) {
      set.delete(value)
    } else {
      set.add(value)
    }
    set.delete('*')
  }
  form.supportedProtocols = Array.from(set)
}

const isProtocolSelected = (value) => {
  return (form.supportedProtocols || []).includes(value)
}

const clearProtocolSelection = () => {
  form.supportedProtocols = []
}

const handleDocumentClick = (event) => {
  if (!protocolDropdownOpen.value) return
  const root = protocolPickerRef.value
  if (!root) return
  if (!root.contains(event.target)) {
    protocolDropdownOpen.value = false
  }
}

const formatSupportedProtocols = (raw) => {
  const protocols = parseProtocols(raw)
  if (!protocols.length) return '-'
  const labelMap = Object.fromEntries(protocolOptions.value.map((item) => [item.value, item.label]))
  return protocols.map((item) => labelMap[item] || item).join(', ')
}

const resetForm = () => {
  Object.assign(form, {
    providerCode: '',
    providerName: '',
    supportedProtocols: ['chat/completions', 'responses'],
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
  protocolDropdownOpen.value = false
  dialogVisible.value = true
}

const openEdit = (record) => {
  const protocols = normalizeProtocols(parseProtocols(record.supportedProtocols))
  ensureProtocolOptions(protocols)
  dialogMode.value = 'edit'
  currentId.value = record.id
  Object.assign(form, {
    providerCode: record.providerCode || '',
    providerName: record.providerName || '',
    supportedProtocols: protocols,
    priority: record.priority ?? 0,
    baseUrl: record.baseUrl || '',
    providerLogo: record.providerLogo || '',
    providerWebsite: record.providerWebsite || '',
    status: record.status ?? 1,
    remark: record.remark || '',
  })
  protocolDropdownOpen.value = false
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!form.providerName || !form.baseUrl || (dialogMode.value === 'create' && !form.providerCode)) {
    toastStore.push('请完整填写服务商编码、名称和 Base URL', 'warning')
    return
  }
  const supportedProtocols = stringifyProtocols(form.supportedProtocols)
  submitting.value = true
  try {
    if (dialogMode.value === 'create') {
      await createProvider({
        ...form,
        supportedProtocols,
        priority: Number(form.priority || 0),
        status: Number(form.status || 1),
      })
      toastStore.push('服务商创建成功', 'success')
    } else {
      await updateProvider(currentId.value, {
        providerName: form.providerName,
        supportedProtocols,
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
  if (!window.confirm(`确认删除服务商「${record.providerName}」吗？`)) return
  try {
    await deleteProvider(record.id)
    toastStore.push('服务商已删除', 'success')
    if (records.value.length === 1 && page.page > 1) page.page -= 1
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
  if (nextPage < 1 || nextPage > pageCount.value) return
  page.page = nextPage
  await loadProviders()
}

onMounted(loadProviders)
onMounted(() => {
  document.addEventListener('click', handleDocumentClick)
})
onBeforeUnmount(() => {
  document.removeEventListener('click', handleDocumentClick)
})
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
                <th>支持协议</th>
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
                </td>
                <td>{{ formatSupportedProtocols(record.supportedProtocols) }}</td>
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
            <p class="text-sm text-slate-400 mt-1">创建时可维护协议能力与启用状态，编辑时可调整基础信息。</p>
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
          <div class="md:col-span-2">
            <label class="text-sm text-slate-500">Base URL</label>
            <input v-model.trim="form.baseUrl" class="input mt-2" placeholder="https://api.openai.com/v1" />
          </div>
          <div class="md:col-span-2">
            <label class="text-sm text-slate-500">支持协议（多选）</label>
            <div ref="protocolPickerRef" class="relative mt-2">
              <button
                type="button"
                class="input w-full min-h-11 text-left flex items-center justify-between gap-3"
                @click.stop="protocolDropdownOpen = !protocolDropdownOpen"
              >
                <div class="flex min-w-0 flex-1 flex-wrap items-center gap-1.5">
                  <template v-if="selectedProtocolItems.length">
                    <span
                      v-for="item in selectedProtocolItems"
                      :key="item.value"
                      class="inline-flex max-w-full items-center rounded-md border border-slate-200 bg-slate-50 px-2 py-0.5 text-xs text-slate-700"
                    >
                      <span class="truncate">{{ item.label }}</span>
                    </span>
                  </template>
                  <span v-else class="text-slate-400">{{ selectedProtocolText }}</span>
                </div>
                <span class="text-slate-400 text-xs">{{ protocolDropdownOpen ? '^' : 'v' }}</span>
              </button>
              <div
                v-if="protocolDropdownOpen"
                class="absolute z-20 mt-1 w-full rounded-xl border border-slate-200 bg-white shadow-xl p-2 max-h-60 overflow-auto"
              >
                <div class="flex items-center justify-between border-b border-slate-100 px-2 pb-2 text-xs text-slate-500">
                  <span>已选 {{ selectedProtocolItems.length }} 项</span>
                  <button type="button" class="btn-text text-xs" @click="clearProtocolSelection">清空</button>
                </div>
                <div class="max-h-44 overflow-auto pt-2">
                  <label
                    v-for="item in protocolOptions"
                    :key="item.value"
                    class="flex items-center gap-2 px-2 py-2 rounded-lg hover:bg-slate-50 cursor-pointer"
                  >
                    <input
                      type="checkbox"
                      class="h-4 w-4"
                      :checked="isProtocolSelected(item.value)"
                      @change="toggleProtocolSelection(item.value)"
                    />
                    <span class="text-sm text-slate-700">{{ item.label }}</span>
                  </label>
                </div>
                <div class="mt-2 flex justify-end border-t border-slate-100 pt-2">
                  <button type="button" class="btn-outline !px-3 !py-1.5 text-xs" @click="protocolDropdownOpen = false">完成</button>
                </div>
              </div>
            </div>
            <p class="mt-2 text-xs text-slate-400">可直接勾选多项。选择 `All Protocols (*)` 时会覆盖其他选择。</p>
          </div>
          <div>
            <label class="text-sm text-slate-500">Logo URL</label>
            <input v-model.trim="form.providerLogo" class="input mt-2" placeholder="可选" />
          </div>
          <div>
            <label class="text-sm text-slate-500">官网地址</label>
            <input v-model.trim="form.providerWebsite" class="input mt-2" placeholder="可选" />
          </div>
          <div>
            <label class="text-sm text-slate-500">路由优先级</label>
            <input v-model.number="form.priority" type="number" class="input mt-2" placeholder="100" />
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
