<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import {
  createPaymentMethod,
  deletePaymentMethod,
  listPaymentMethods,
  updatePaymentMethod,
  updatePaymentMethodStatus,
} from '@/api/admin'
import { useToastStore } from '@/stores/toast'
import { formatDateTime, formatStatus } from '@/utils/format'

const toastStore = useToastStore()

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const dialogMode = ref('create')
const currentRecord = ref(null)
const records = ref([])

const methodTypeOptions = [
  { label: '支付宝', value: 'alipay' },
  { label: '微信支付', value: 'wechat' },
  { label: '地方支付', value: 'local' },
  { label: '银行卡', value: 'bank' },
  { label: '其他', value: 'other' },
]

const filters = reactive({
  keyword: '',
  methodType: '',
  status: '',
})

const page = reactive({
  page: 1,
  pageSize: 10,
  total: 0,
})

const form = reactive({
  methodCode: '',
  methodName: '',
  methodType: 'alipay',
  iconUrl: '',
  sort: 0,
  status: 1,
  configJson: '',
  remark: '',
})

const defaultConfigText = '{\n  "appId": "",\n  "merchantId": "",\n  "notifyUrl": ""\n}'

const pageCount = computed(() => Math.max(1, Math.ceil((page.total || 0) / page.pageSize)))
const enabledCount = computed(() => records.value.filter((item) => Number(item.status) === 1).length)

const resetForm = () => {
  Object.assign(form, {
    methodCode: '',
    methodName: '',
    methodType: 'alipay',
    iconUrl: '',
    sort: 0,
    status: 1,
    configJson: defaultConfigText,
    remark: '',
  })
}

const prettyJson = (value) => {
  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  } catch (error) {
    return value
  }
}

const loadPaymentMethods = async () => {
  loading.value = true
  try {
    const data = await listPaymentMethods({
      page: page.page,
      pageSize: page.pageSize,
      keyword: filters.keyword,
      methodType: filters.methodType,
      status: filters.status,
    })
    records.value = data.records || []
    page.total = data.total || 0
  } catch (error) {
    toastStore.push(error.message || '加载支付方式失败', 'error')
  } finally {
    loading.value = false
  }
}

const resetFilters = async () => {
  Object.assign(filters, { keyword: '', methodType: '', status: '' })
  page.page = 1
  await loadPaymentMethods()
}

const changePage = async (nextPage) => {
  if (nextPage < 1 || nextPage > pageCount.value) {
    return
  }
  page.page = nextPage
  await loadPaymentMethods()
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
    methodCode: record.methodCode || '',
    methodName: record.methodName || '',
    methodType: record.methodType || 'alipay',
    iconUrl: record.iconUrl || '',
    sort: Number(record.sort ?? 0),
    status: Number(record.status ?? 1),
    configJson: prettyJson(record.configJson || defaultConfigText),
    remark: record.remark || '',
  })
  dialogVisible.value = true
}

const validateJson = () => {
  try {
    JSON.parse(form.configJson)
    return true
  } catch (error) {
    toastStore.push('支付参数必须是合法 JSON', 'warning')
    return false
  }
}

const handleSubmit = async () => {
  if (!form.methodCode.trim() && dialogMode.value === 'create') {
    toastStore.push('请输入支付方式编码', 'warning')
    return
  }
  if (!form.methodName.trim()) {
    toastStore.push('请输入支付方式名称', 'warning')
    return
  }
  if (!validateJson()) {
    return
  }

  const payload = {
    methodName: form.methodName.trim(),
    methodType: form.methodType,
    iconUrl: form.iconUrl.trim(),
    sort: Number(form.sort || 0),
    status: Number(form.status || 1),
    configJson: form.configJson,
    remark: form.remark,
  }

  submitting.value = true
  try {
    if (dialogMode.value === 'create') {
      await createPaymentMethod({
        ...payload,
        methodCode: form.methodCode.trim(),
      })
      toastStore.push('支付方式创建成功', 'success')
    } else {
      await updatePaymentMethod(currentRecord.value.id, payload)
      toastStore.push('支付方式更新成功', 'success')
    }
    dialogVisible.value = false
    await loadPaymentMethods()
  } catch (error) {
    toastStore.push(error.message || '保存支付方式失败', 'error')
  } finally {
    submitting.value = false
  }
}

const handleToggleStatus = async (record) => {
  try {
    await updatePaymentMethodStatus(record.id, Number(record.status) === 1 ? 0 : 1)
    toastStore.push('支付方式状态已更新', 'success')
    await loadPaymentMethods()
  } catch (error) {
    toastStore.push(error.message || '更新支付方式状态失败', 'error')
  }
}

const handleDelete = async (record) => {
  if (!window.confirm(`确认删除支付方式“${record.methodName}”吗？`)) {
    return
  }
  try {
    await deletePaymentMethod(record.id)
    toastStore.push('支付方式已删除', 'success')
    if (records.value.length === 1 && page.page > 1) {
      page.page -= 1
    }
    await loadPaymentMethods()
  } catch (error) {
    toastStore.push(error.message || '删除支付方式失败', 'error')
  }
}

onMounted(loadPaymentMethods)
</script>

<template>
  <div class="p-6 space-y-6">
    <div class="flex flex-col gap-3 lg:flex-row lg:items-end lg:justify-between">
      <div>
        <h1 class="text-2xl font-bold text-slate-900">支付方式管理</h1>
        <p class="text-slate-500">维护支付宝、微信、地方支付等渠道配置，支持启停控制和参数管理。</p>
      </div>
      <div class="flex items-center gap-3">
        <div class="rounded-2xl border border-slate-200 bg-white px-4 py-3 shadow-sm">
          <div class="text-xs text-slate-400">当前页启用方式</div>
          <div class="mt-2 text-2xl font-semibold text-slate-900">{{ enabledCount }}</div>
        </div>
        <button class="btn-primary" @click="openCreate">新增支付方式</button>
      </div>
    </div>

    <div class="card">
      <div class="card-body grid gap-4 md:grid-cols-4">
        <div>
          <label class="text-sm text-slate-500">关键字</label>
          <input v-model.trim="filters.keyword" class="input mt-2" placeholder="搜索编码或名称" />
        </div>
        <div>
          <label class="text-sm text-slate-500">类型</label>
          <select v-model="filters.methodType" class="input mt-2">
            <option value="">全部</option>
            <option v-for="option in methodTypeOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
          </select>
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
          <button class="btn-primary h-11" @click="page.page = 1; loadPaymentMethods()">搜索</button>
          <button class="btn-outline h-11" @click="resetFilters">重置</button>
        </div>
      </div>
    </div>

    <div class="card">
      <div class="card-header">
        <div>
          <h2 class="card-title">支付方式列表</h2>
          <p class="text-sm text-slate-400 mt-1">共 {{ page.total }} 条记录</p>
        </div>
        <button class="btn-outline" :disabled="loading" @click="loadPaymentMethods">{{ loading ? '刷新中...' : '刷新' }}</button>
      </div>
      <div class="card-body">
        <div class="table-wrap">
          <table class="table min-w-[1180px]">
            <thead>
              <tr>
                <th>方式信息</th>
                <th>类型</th>
                <th>状态</th>
                <th>排序</th>
                <th>配置摘要</th>
                <th>备注</th>
                <th>更新时间</th>
                <th class="text-right">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="!records.length && !loading">
                <td colspan="8" class="empty-state">暂无支付方式数据</td>
              </tr>
              <tr v-for="record in records" :key="record.id">
                <td>
                  <div class="font-medium text-slate-800">{{ record.methodName }}</div>
                  <div class="text-xs text-slate-400 mt-1">{{ record.methodCode }}</div>
                </td>
                <td>{{ methodTypeOptions.find((item) => item.value === record.methodType)?.label || record.methodType || '-' }}</td>
                <td>
                  <span class="badge" :class="Number(record.status) === 1 ? 'badge-success' : 'badge-danger'">
                    {{ formatStatus(record.status) }}
                  </span>
                </td>
                <td>{{ record.sort ?? 0 }}</td>
                <td class="max-w-[280px]">
                  <pre class="text-xs text-slate-600 whitespace-pre-wrap break-all">{{ prettyJson(record.configJson || '{}') }}</pre>
                </td>
                <td class="max-w-[200px] break-words">{{ record.remark || '-' }}</td>
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
      <div class="modal-panel max-w-3xl">
        <div class="flex items-center justify-between gap-4">
          <div>
            <h3 class="text-lg font-semibold text-slate-800">{{ dialogMode === 'create' ? '新增支付方式' : '编辑支付方式' }}</h3>
            <p class="text-sm text-slate-400 mt-1">支付参数采用 JSON 结构保存，方便对接不同渠道。</p>
          </div>
          <button class="btn-text" @click="dialogVisible = false">关闭</button>
        </div>

        <div class="mt-6 grid gap-4 md:grid-cols-2">
          <div>
            <label class="text-sm text-slate-500">方式编码</label>
            <input v-model.trim="form.methodCode" class="input mt-2" :disabled="dialogMode === 'edit'" placeholder="如：alipay_official" />
          </div>
          <div>
            <label class="text-sm text-slate-500">方式名称</label>
            <input v-model.trim="form.methodName" class="input mt-2" placeholder="如：支付宝官方收款" />
          </div>
          <div>
            <label class="text-sm text-slate-500">类型</label>
            <select v-model="form.methodType" class="input mt-2">
              <option v-for="option in methodTypeOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
            </select>
          </div>
          <div>
            <label class="text-sm text-slate-500">图标地址</label>
            <input v-model.trim="form.iconUrl" class="input mt-2" placeholder="可选，用于前端展示图标" />
          </div>
          <div>
            <label class="text-sm text-slate-500">排序</label>
            <input v-model.number="form.sort" type="number" class="input mt-2" placeholder="数字越小越靠前" />
          </div>
          <div>
            <label class="text-sm text-slate-500">状态</label>
            <select v-model.number="form.status" class="input mt-2">
              <option :value="1">启用</option>
              <option :value="0">停用</option>
            </select>
          </div>
          <div class="md:col-span-2">
            <label class="text-sm text-slate-500">支付参数 JSON</label>
            <textarea v-model="form.configJson" class="input mt-2 min-h-52 font-mono text-sm" placeholder="请输入合法 JSON"></textarea>
          </div>
          <div class="md:col-span-2">
            <label class="text-sm text-slate-500">备注</label>
            <textarea v-model.trim="form.remark" class="input mt-2 min-h-24" placeholder="填写渠道说明、调试状态或运营备注"></textarea>
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
