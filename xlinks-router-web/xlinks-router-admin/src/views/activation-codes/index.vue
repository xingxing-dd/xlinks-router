<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import {
  deleteActivationCode,
  generateActivationCodes,
  listActivationCodes,
  listPlans,
  updateActivationCode,
  updateActivationCodeStatus,
} from '@/api/admin'
import { useToastStore } from '@/stores/toast'
import { formatDateTime } from '@/utils/format'

const STATUS_TEXT = {
  0: '停用',
  1: '启用',
  2: '已使用',
}

const toastStore = useToastStore()

const loading = ref(false)
const generating = ref(false)
const updating = ref(false)
const generateDialogVisible = ref(false)
const editDialogVisible = ref(false)
const records = ref([])
const planOptions = ref([])
const currentRecord = ref(null)
const generatedResult = ref(null)

const filters = reactive({
  planId: '',
  status: '',
  activationCode: '',
  usedAccount: '',
})

const page = reactive({
  page: 1,
  pageSize: 10,
  total: 0,
})

const generateForm = reactive({
  planId: '',
  quantity: 10,
  codeLength: 12,
  prefix: '',
  remark: '',
})

const editForm = reactive({
  planId: '',
  orderId: '',
  remark: '',
})

const pageCount = computed(() => Math.max(1, Math.ceil((page.total || 0) / page.pageSize)))

const loadPlans = async () => {
  try {
    const data = await listPlans({ page: 1, pageSize: 200 })
    planOptions.value = data.records || []
  } catch (error) {
    toastStore.push(error.message || '加载套餐选项失败', 'error')
  }
}

const loadActivationCodes = async () => {
  loading.value = true
  try {
    const data = await listActivationCodes({
      page: page.page,
      pageSize: page.pageSize,
      planId: filters.planId,
      status: filters.status,
      activationCode: filters.activationCode,
      usedAccount: filters.usedAccount,
    })
    records.value = data.records || []
    page.total = data.total || 0
  } catch (error) {
    toastStore.push(error.message || '加载激活码列表失败', 'error')
  } finally {
    loading.value = false
  }
}

const resetGenerateForm = () => {
  Object.assign(generateForm, {
    planId: '',
    quantity: 10,
    codeLength: 12,
    prefix: '',
    remark: '',
  })
}

const openGenerateDialog = () => {
  resetGenerateForm()
  generateDialogVisible.value = true
}

const openEditDialog = (record) => {
  currentRecord.value = record
  Object.assign(editForm, {
    planId: record.planId || '',
    orderId: record.orderId || '',
    remark: record.remark || '',
  })
  editDialogVisible.value = true
}

const handleGenerate = async () => {
  if (!generateForm.planId) {
    toastStore.push('请选择套餐', 'warning')
    return
  }
  if (Number(generateForm.quantity) < 1 || Number(generateForm.quantity) > 500) {
    toastStore.push('生成数量需在 1 到 500 之间', 'warning')
    return
  }

  generating.value = true
  try {
    const data = await generateActivationCodes({
      planId: Number(generateForm.planId),
      quantity: Number(generateForm.quantity),
      codeLength: Number(generateForm.codeLength),
      prefix: generateForm.prefix || null,
      remark: generateForm.remark,
    })
    generatedResult.value = data
    toastStore.push(`已生成 ${data.generatedCount} 个激活码`, 'success')
    generateDialogVisible.value = false
    await loadActivationCodes()
  } catch (error) {
    toastStore.push(error.message || '生成激活码失败', 'error')
  } finally {
    generating.value = false
  }
}

const handleUpdate = async () => {
  if (!currentRecord.value) {
    return
  }

  updating.value = true
  try {
    await updateActivationCode(currentRecord.value.id, {
      planId: Number(currentRecord.value.status) === 2 ? undefined : Number(editForm.planId),
      orderId: Number(currentRecord.value.status) === 2 ? undefined : editForm.orderId,
      remark: editForm.remark,
    })
    toastStore.push('激活码更新成功', 'success')
    editDialogVisible.value = false
    currentRecord.value = null
    await loadActivationCodes()
  } catch (error) {
    toastStore.push(error.message || '更新激活码失败', 'error')
  } finally {
    updating.value = false
  }
}

const handleToggleStatus = async (record) => {
  if (Number(record.status) === 2) {
    toastStore.push('已使用激活码不可变更状态', 'warning')
    return
  }
  try {
    await updateActivationCodeStatus(record.id, Number(record.status) === 1 ? 0 : 1)
    toastStore.push('激活码状态已更新', 'success')
    await loadActivationCodes()
  } catch (error) {
    toastStore.push(error.message || '更新激活码状态失败', 'error')
  }
}

const handleDelete = async (record) => {
  if (Number(record.status) === 2) {
    toastStore.push('已使用激活码不可删除', 'warning')
    return
  }
  if (!window.confirm(`确认删除激活码「${record.activationCode}」吗？`)) {
    return
  }
  try {
    await deleteActivationCode(record.id)
    toastStore.push('激活码已删除', 'success')
    if (records.value.length === 1 && page.page > 1) {
      page.page -= 1
    }
    await loadActivationCodes()
  } catch (error) {
    toastStore.push(error.message || '删除激活码失败', 'error')
  }
}

const copyText = async (text) => {
  try {
    await navigator.clipboard.writeText(text)
    toastStore.push('内容已复制', 'success')
  } catch (error) {
    toastStore.push('当前环境不支持自动复制', 'warning')
  }
}

const maskText = (value, head = 6, tail = 4) => {
  if (!value) {
    return '-'
  }
  const text = String(value)
  if (text.length <= head + tail + 3) {
    return text
  }
  return `${text.slice(0, head)}...${text.slice(-tail)}`
}

const copyGeneratedCodes = () => {
  if (!generatedResult.value?.codes?.length) {
    return
  }
  copyText(generatedResult.value.codes.join('\n'))
}

const resetFilters = async () => {
  Object.assign(filters, { planId: '', status: '', activationCode: '', usedAccount: '' })
  page.page = 1
  await loadActivationCodes()
}

const changePage = async (nextPage) => {
  if (nextPage < 1 || nextPage > pageCount.value) {
    return
  }
  page.page = nextPage
  await loadActivationCodes()
}

onMounted(async () => {
  await Promise.all([loadPlans(), loadActivationCodes()])
})
</script>

<template>
  <div class="p-6 space-y-6">
    <div class="flex flex-col gap-3 lg:flex-row lg:items-end lg:justify-between">
      <div>
        <h1 class="text-2xl font-bold text-slate-900">激活码管理</h1>
        <p class="text-slate-500">批量生成套餐激活码，并维护激活、分配和使用记录。</p>
      </div>
      <button class="btn-primary" @click="openGenerateDialog">批量生成</button>
    </div>

    <div v-if="generatedResult?.codes?.length" class="card">
      <div class="card-header">
        <div>
          <h2 class="card-title">最近生成结果</h2>
          <p class="text-sm text-slate-400 mt-1">已为「{{ generatedResult.planName }}」生成 {{ generatedResult.generatedCount }} 个激活码</p>
        </div>
        <button class="btn-outline" @click="copyGeneratedCodes">复制全部</button>
      </div>
      <div class="card-body">
        <div class="grid gap-3 md:grid-cols-3">
          <div v-for="code in generatedResult.codes" :key="code" class="rounded-xl border border-slate-200 bg-slate-50 px-4 py-3">
            <div class="font-mono text-sm text-slate-700 break-all">{{ code }}</div>
          </div>
        </div>
      </div>
    </div>

    <div class="card">
      <div class="card-body grid gap-4 md:grid-cols-5">
        <div>
          <label class="text-sm text-slate-500">套餐</label>
          <select v-model="filters.planId" class="input mt-2">
            <option value="">全部</option>
            <option v-for="plan in planOptions" :key="plan.id" :value="plan.id">{{ plan.planName }}</option>
          </select>
        </div>
        <div>
          <label class="text-sm text-slate-500">状态</label>
          <select v-model="filters.status" class="input mt-2">
            <option value="">全部</option>
            <option :value="1">启用</option>
            <option :value="0">停用</option>
            <option :value="2">已使用</option>
          </select>
        </div>
        <div>
          <label class="text-sm text-slate-500">激活码</label>
          <input v-model.trim="filters.activationCode" class="input mt-2" placeholder="请输入激活码" />
        </div>
        <div>
          <label class="text-sm text-slate-500">使用账号</label>
          <input v-model.trim="filters.usedAccount" class="input mt-2" placeholder="用户名 / 手机号 / 邮箱" />
        </div>
        <div class="flex items-end justify-end gap-3">
          <button class="btn-primary h-11" @click="page.page = 1; loadActivationCodes()">搜索</button>
          <button class="btn-outline h-11" @click="resetFilters">重置</button>
        </div>
      </div>
    </div>

    <div class="card">
      <div class="card-header">
        <div>
          <h2 class="card-title">激活码列表</h2>
          <p class="text-sm text-slate-400 mt-1">共 {{ page.total }} 条记录</p>
        </div>
        <button class="btn-outline" :disabled="loading" @click="loadActivationCodes">{{ loading ? '刷新中...' : '刷新' }}</button>
      </div>
      <div class="card-body">
        <div class="table-wrap">
          <table class="table min-w-[1180px]">
            <thead>
              <tr>
                <th>激活码</th>
                <th>套餐</th>
                <th>状态</th>
                <th>使用账号</th>
                <th>使用时间</th>
                <th>订阅 ID</th>
                <th>订单号</th>
                <th>备注</th>
                <th>更新时间</th>
                <th class="text-right">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="!records.length && !loading">
                <td colspan="10" class="empty-state">暂无激活码数据</td>
              </tr>
              <tr v-for="record in records" :key="record.id">
                <td>
                  <button
                    class="copy-chip max-w-[220px]"
                    :title="`点击复制完整激活码\n${record.activationCode}`"
                    @click="copyText(record.activationCode)"
                  >
                    <span class="font-mono">{{ maskText(record.activationCode, 6, 4) }}</span>
                    <span class="copy-chip-hint">复制</span>
                  </button>
                </td>
                <td>
                  <div class="font-medium text-slate-800">{{ record.planName || '-' }}</div>
                  <div class="text-xs text-slate-400 mt-1">套餐 ID: {{ record.planId || '-' }}</div>
                </td>
                <td>
                  <span
                    class="badge"
                    :class="Number(record.status) === 1 ? 'badge-success' : Number(record.status) === 0 ? 'badge-warning' : 'badge-danger'"
                  >
                    {{ STATUS_TEXT[Number(record.status)] || '-' }}
                  </span>
                </td>
                <td>{{ record.usedAccount || '-' }}</td>
                <td>{{ formatDateTime(record.usedAt) }}</td>
                <td>{{ record.subscriptionId || '-' }}</td>
                <td>
                  <template v-if="record.orderId">
                    <button
                      class="copy-chip max-w-[220px]"
                      :title="`点击复制完整订单号\n${record.orderId}`"
                      @click="copyText(record.orderId)"
                    >
                      <span class="font-mono">{{ maskText(record.orderId, 8, 6) }}</span>
                      <span class="copy-chip-hint">复制</span>
                    </button>
                  </template>
                  <span v-else>-</span>
                </td>
                <td class="max-w-[180px] break-words">{{ record.remark || '-' }}</td>
                <td>{{ formatDateTime(record.updatedAt) }}</td>
                <td>
                  <div class="flex items-center justify-end gap-2">
                    <button class="btn-outline" @click="openEditDialog(record)">编辑</button>
                    <button class="btn-outline" :disabled="Number(record.status) === 2" @click="handleToggleStatus(record)">
                      {{ Number(record.status) === 1 ? '停用' : '启用' }}
                    </button>
                    <button class="btn-danger" :disabled="Number(record.status) === 2" @click="handleDelete(record)">删除</button>
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

    <div v-if="generateDialogVisible" class="fixed inset-0 z-50 flex items-center justify-center px-4">
      <div class="absolute inset-0 bg-slate-900/50" @click="generateDialogVisible = false"></div>
      <div class="modal-panel max-w-2xl">
        <div class="flex items-center justify-between gap-4">
          <div>
            <h3 class="text-lg font-semibold text-slate-800">批量生成激活码</h3>
            <p class="text-sm text-slate-400 mt-1">支持按套餐批量生成激活码，可追加前缀方便活动区分。</p>
          </div>
          <button class="btn-text" @click="generateDialogVisible = false">关闭</button>
        </div>

        <div class="mt-6 grid gap-4 md:grid-cols-2">
          <div>
            <label class="text-sm text-slate-500">套餐</label>
            <select v-model.number="generateForm.planId" class="input mt-2">
              <option value="">请选择</option>
              <option v-for="plan in planOptions" :key="plan.id" :value="plan.id">{{ plan.planName }}</option>
            </select>
          </div>
          <div>
            <label class="text-sm text-slate-500">生成数量</label>
            <input v-model.number="generateForm.quantity" type="number" min="1" max="500" class="input mt-2" />
          </div>
          <div>
            <label class="text-sm text-slate-500">编码长度</label>
            <input v-model.number="generateForm.codeLength" type="number" min="6" max="24" class="input mt-2" />
          </div>
          <div>
            <label class="text-sm text-slate-500">编码前缀</label>
            <input v-model.trim="generateForm.prefix" class="input mt-2" placeholder="例如 APRIL" />
          </div>
          <div class="md:col-span-2">
            <label class="text-sm text-slate-500">备注</label>
            <textarea v-model.trim="generateForm.remark" class="input mt-2 min-h-24" placeholder="例如 4 月活动批次"></textarea>
          </div>
        </div>

        <div class="mt-6 flex justify-end gap-3">
          <button class="btn-outline" @click="generateDialogVisible = false">取消</button>
          <button class="btn-primary" :disabled="generating" @click="handleGenerate">
            {{ generating ? '生成中...' : '确认生成' }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="editDialogVisible" class="fixed inset-0 z-50 flex items-center justify-center px-4">
      <div class="absolute inset-0 bg-slate-900/50" @click="editDialogVisible = false"></div>
      <div class="modal-panel max-w-2xl">
        <div class="flex items-center justify-between gap-4">
          <div>
            <h3 class="text-lg font-semibold text-slate-800">编辑激活码</h3>
            <p class="text-sm text-slate-400 mt-1">已使用激活码仅允许补充备注信息。</p>
          </div>
          <button class="btn-text" @click="editDialogVisible = false">关闭</button>
        </div>

        <div class="mt-6 grid gap-4 md:grid-cols-2">
          <div class="md:col-span-2 rounded-xl bg-slate-50 border border-slate-200 px-4 py-3">
            <div class="text-xs text-slate-400">激活码</div>
            <div class="font-mono text-sm text-slate-700 mt-1 break-all">{{ currentRecord?.activationCode }}</div>
          </div>
          <div>
            <label class="text-sm text-slate-500">套餐</label>
            <select v-model.number="editForm.planId" class="input mt-2" :disabled="Number(currentRecord?.status) === 2">
              <option value="">请选择</option>
              <option v-for="plan in planOptions" :key="plan.id" :value="plan.id">{{ plan.planName }}</option>
            </select>
          </div>
          <div>
            <label class="text-sm text-slate-500">订单号</label>
            <input v-model.trim="editForm.orderId" class="input mt-2" :disabled="Number(currentRecord?.status) === 2" placeholder="选填" />
          </div>
          <div class="md:col-span-2">
            <label class="text-sm text-slate-500">备注</label>
            <textarea v-model.trim="editForm.remark" class="input mt-2 min-h-24" placeholder="用于补充使用说明"></textarea>
          </div>
        </div>

        <div class="mt-6 flex justify-end gap-3">
          <button class="btn-outline" @click="editDialogVisible = false">取消</button>
          <button class="btn-primary" :disabled="updating" @click="handleUpdate">
            {{ updating ? '提交中...' : '确认保存' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
