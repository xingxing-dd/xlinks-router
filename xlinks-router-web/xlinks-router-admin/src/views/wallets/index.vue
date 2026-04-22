<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import {
  batchOpenWallets,
  getWalletDetail,
  getWalletMainFlows,
  getWalletSubFlows,
  listWallets,
  walletFreeze,
  walletManualCredit,
  walletManualDebit,
  walletPendingSettlement,
  walletSettle,
  walletUnfreeze,
  walletUpdateState,
} from '@/api/admin'
import { useToastStore } from '@/stores/toast'
import { formatDateTime, formatStatus } from '@/utils/format'

const toastStore = useToastStore()

const walletTypeOptions = [
  { label: '全部', value: '' },
  { label: '基本户', value: 'basic' },
  { label: '冻结户', value: 'frozen' },
  { label: '待结算户', value: 'pending_settlement' },
]

const operationConfigMap = {
  manualCredit: {
    title: '手工入账',
    actionText: '确认入账',
    action: walletManualCredit,
    successMessage: '手工入账成功',
  },
  manualDebit: {
    title: '手工扣减',
    actionText: '确认扣减',
    action: walletManualDebit,
    successMessage: '手工扣减成功',
  },
  freeze: {
    title: '冻结金额',
    actionText: '确认冻结',
    action: walletFreeze,
    successMessage: '冻结成功',
  },
  unfreeze: {
    title: '解冻金额',
    actionText: '确认解冻',
    action: walletUnfreeze,
    successMessage: '解冻成功',
  },
  pendingSettlement: {
    title: '待结算入账',
    actionText: '确认入账',
    action: walletPendingSettlement,
    successMessage: '待结算入账成功',
  },
  settle: {
    title: '待结算结转',
    actionText: '确认结转',
    action: walletSettle,
    successMessage: '待结算结转成功',
  },
}

const loading = ref(false)
const batchOpening = ref(false)
const detailLoading = ref(false)
const operationSubmitting = ref(false)
const detailVisible = ref(false)
const operationVisible = ref(false)
const stateUpdatingKey = ref('')
const activeDetailTab = ref('subWallets')
const currentRecord = ref(null)
const detail = ref(null)
const records = ref([])
const mainFlows = ref([])
const subFlows = ref([])
const operationType = ref('manualCredit')

const filters = reactive({
  keyword: '',
  walletStatus: '',
})

const page = reactive({
  page: 1,
  pageSize: 10,
  total: 0,
})

const mainFlowPage = reactive({
  page: 1,
  pageSize: 10,
  total: 0,
})

const subFlowPage = reactive({
  page: 1,
  pageSize: 10,
  total: 0,
})

const detailFilters = reactive({
  walletType: '',
})

const operationForm = reactive({
  amount: '',
  orderNo: '',
  remark: '',
})

const pageCount = computed(() => Math.max(1, Math.ceil((page.total || 0) / page.pageSize)))
const mainFlowPageCount = computed(() => Math.max(1, Math.ceil((mainFlowPage.total || 0) / mainFlowPage.pageSize)))
const subFlowPageCount = computed(() => Math.max(1, Math.ceil((subFlowPage.total || 0) / subFlowPage.pageSize)))
const enabledCount = computed(() => records.value.filter((item) => Number(item.status) === 1).length)
const blockedInCount = computed(() => records.value.filter((item) => Number(item.allowIn) !== 1).length)
const blockedOutCount = computed(() => records.value.filter((item) => Number(item.allowOut) !== 1).length)
const currentOperationConfig = computed(() => operationConfigMap[operationType.value] || operationConfigMap.manualCredit)

const formatMoney = (value) => {
  if (value === null || value === undefined || value === '') {
    return '0.00'
  }
  const amount = Number(value)
  return Number.isNaN(amount) ? `${value}` : amount.toFixed(2)
}

const formatBooleanFlag = (value) => (Number(value) === 1 ? '是' : '否')
const formatStopFlag = (value) => (Number(value) === 1 ? '否' : '是')

const formatWalletType = (value) => {
  const match = walletTypeOptions.find((item) => item.value === value)
  return match?.label || value || '-'
}

const formatDirection = (value) => {
  const normalized = `${value || ''}`.trim().toLowerCase()
  if (['in', 'income', 'credit'].includes(normalized)) {
    return '收入'
  }
  if (['out', 'expense', 'debit'].includes(normalized)) {
    return '支出'
  }
  return value || '-'
}

const getDirectionBadgeClass = (value) => {
  const normalized = `${value || ''}`.trim().toLowerCase()
  return ['in', 'income', 'credit'].includes(normalized) ? 'badge-success' : 'badge-warning'
}

const getWalletStateBadgeClass = (value) => (Number(value) === 1 ? 'badge-success' : 'badge-danger')
const getFlagBadgeClass = (value) => (Number(value) === 1 ? 'badge-success' : 'badge-warning')
const getStopFlagBadgeClass = (value) => (Number(value) === 1 ? 'badge-success' : 'badge-warning')

const resetOperationForm = () => {
  Object.assign(operationForm, {
    amount: '',
    orderNo: '',
    remark: '',
  })
}

const loadWallets = async () => {
  loading.value = true
  try {
    const data = await listWallets({
      page: page.page,
      pageSize: page.pageSize,
      keyword: filters.keyword,
      walletStatus: filters.walletStatus,
    })
    records.value = data.records || []
    page.total = data.total || 0
  } catch (error) {
    toastStore.push(error.message || '加载钱包列表失败', 'error')
  } finally {
    loading.value = false
  }
}

const handleBatchOpen = async () => {
  if (batchOpening.value) {
    return
  }
  if (!window.confirm('确认批量为所有未开通钱包的商户自动开通钱包吗？默认将启用钱包，并允许入账和出账。')) {
    return
  }
  batchOpening.value = true
  try {
    const data = await batchOpenWallets()
    toastStore.push(
      `批量开通完成，新增 ${data?.createdWalletCount ?? 0} 个钱包，已存在 ${data?.existingWalletCount ?? 0} 个`,
      'success',
    )
    page.page = 1
    await loadWallets()
  } catch (error) {
    toastStore.push(error.message || '批量开通钱包失败', 'error')
  } finally {
    batchOpening.value = false
  }
}

const loadWalletDetail = async (accountId) => {
  detailLoading.value = true
  try {
    const [detailData, mainFlowData, subFlowData] = await Promise.all([
      getWalletDetail(accountId),
      getWalletMainFlows(accountId, {
        page: mainFlowPage.page,
        pageSize: mainFlowPage.pageSize,
      }),
      getWalletSubFlows(accountId, {
        page: subFlowPage.page,
        pageSize: subFlowPage.pageSize,
        walletType: detailFilters.walletType,
      }),
    ])
    detail.value = detailData
    mainFlows.value = mainFlowData.records || []
    mainFlowPage.total = mainFlowData.total || 0
    subFlows.value = subFlowData.records || []
    subFlowPage.total = subFlowData.total || 0
  } catch (error) {
    toastStore.push(error.message || '加载钱包详情失败', 'error')
  } finally {
    detailLoading.value = false
  }
}

const loadMainFlows = async () => {
  if (!currentRecord.value) {
    return
  }
  try {
    const data = await getWalletMainFlows(currentRecord.value.accountId, {
      page: mainFlowPage.page,
      pageSize: mainFlowPage.pageSize,
    })
    mainFlows.value = data.records || []
    mainFlowPage.total = data.total || 0
  } catch (error) {
    toastStore.push(error.message || '加载主账户流水失败', 'error')
  }
}

const loadSubFlows = async () => {
  if (!currentRecord.value) {
    return
  }
  try {
    const data = await getWalletSubFlows(currentRecord.value.accountId, {
      page: subFlowPage.page,
      pageSize: subFlowPage.pageSize,
      walletType: detailFilters.walletType,
    })
    subFlows.value = data.records || []
    subFlowPage.total = data.total || 0
  } catch (error) {
    toastStore.push(error.message || '加载子账户流水失败', 'error')
  }
}

const refreshCurrentWallet = async (accountId = currentRecord.value?.accountId) => {
  if (!accountId) {
    return
  }
  await Promise.all([loadWallets(), detailVisible.value ? loadWalletDetail(accountId) : Promise.resolve()])
}

const resetFilters = async () => {
  Object.assign(filters, {
    keyword: '',
    walletStatus: '',
  })
  page.page = 1
  await loadWallets()
}

const changePage = async (nextPage) => {
  if (nextPage < 1 || nextPage > pageCount.value) {
    return
  }
  page.page = nextPage
  await loadWallets()
}

const changeMainFlowPage = async (nextPage) => {
  if (nextPage < 1 || nextPage > mainFlowPageCount.value) {
    return
  }
  mainFlowPage.page = nextPage
  await loadMainFlows()
}

const changeSubFlowPage = async (nextPage) => {
  if (nextPage < 1 || nextPage > subFlowPageCount.value) {
    return
  }
  subFlowPage.page = nextPage
  await loadSubFlows()
}

const openDetail = async (record) => {
  currentRecord.value = record
  detailVisible.value = true
  activeDetailTab.value = 'subWallets'
  mainFlowPage.page = 1
  subFlowPage.page = 1
  detailFilters.walletType = ''
  await loadWalletDetail(record.accountId)
}

const openOperationDialog = (record, type) => {
  currentRecord.value = record
  operationType.value = type
  resetOperationForm()
  operationVisible.value = true
}

const submitOperation = async () => {
  if (!currentRecord.value) {
    return
  }
  const amount = Number(operationForm.amount)
  if (!amount || Number.isNaN(amount) || amount <= 0) {
    toastStore.push('请输入正确的金额', 'warning')
    return
  }
  operationSubmitting.value = true
  try {
    await currentOperationConfig.value.action(currentRecord.value.accountId, {
      amount,
      orderNo: operationForm.orderNo.trim() || undefined,
      remark: operationForm.remark.trim() || undefined,
    })
    toastStore.push(currentOperationConfig.value.successMessage, 'success')
    operationVisible.value = false
    await refreshCurrentWallet(currentRecord.value.accountId)
  } catch (error) {
    toastStore.push(error.message || `${currentOperationConfig.value.title}失败`, 'error')
  } finally {
    operationSubmitting.value = false
  }
}

const handleSubFlowFilterChange = async () => {
  subFlowPage.page = 1
  await loadSubFlows()
}

const isToggleUpdating = (record, field) => stateUpdatingKey.value === `${record.accountId}-${field}`

const getStatusToggleActive = (value) => Number(value) === 1
const getStopToggleActive = (value) => Number(value) !== 1

const getToggleTrackClass = (active, tone = 'success') => {
  if (!active) {
    return 'bg-slate-200'
  }
  return tone === 'warning' ? 'bg-amber-500' : 'bg-emerald-500'
}

const getToggleKnobClass = (active) => (active ? 'translate-x-5' : 'translate-x-0')

const handleQuickToggle = async (record, field) => {
  const toggleKey = `${record.accountId}-${field}`
  if (stateUpdatingKey.value === toggleKey) {
    return
  }

  let body
  let successMessage
  if (field === 'status') {
    body = { status: Number(record.status) === 1 ? 0 : 1 }
    successMessage = '钱包状态已更新'
  } else if (field === 'allowIn') {
    body = { allowIn: Number(record.allowIn) === 1 ? 0 : 1 }
    successMessage = '止入状态已更新'
  } else if (field === 'allowOut') {
    body = { allowOut: Number(record.allowOut) === 1 ? 0 : 1 }
    successMessage = '止出状态已更新'
  } else {
    return
  }

  stateUpdatingKey.value = toggleKey
  try {
    await walletUpdateState(record.accountId, body)
    toastStore.push(successMessage, 'success')
    await refreshCurrentWallet(record.accountId)
  } catch (error) {
    toastStore.push(error.message || '状态切换失败', 'error')
  } finally {
    stateUpdatingKey.value = ''
  }
}

onMounted(loadWallets)
</script>

<template>
  <div class="p-6 space-y-6">
    <div class="flex flex-col gap-3 lg:flex-row lg:items-end lg:justify-between">
      <div>
        <h1 class="text-2xl font-bold text-slate-900">钱包管理</h1>
        <p class="text-slate-500">查看商户主钱包、子账户余额和账务流水，并支持常见的人工账务处理。</p>
      </div>
      <div class="flex flex-wrap items-center gap-3">
        <button class="btn-primary" :disabled="batchOpening" @click="handleBatchOpen">
          {{ batchOpening ? '开通中...' : '批量开通' }}
        </button>
        <button class="btn-outline" :disabled="loading" @click="loadWallets">
          {{ loading ? '刷新中...' : '刷新' }}
        </button>
      </div>
    </div>

    <div class="card">
      <div class="card-body grid gap-4 md:grid-cols-4">
        <div class="md:col-span-2">
          <label class="text-sm text-slate-500">商户关键字</label>
          <input
            v-model.trim="filters.keyword"
            class="input mt-2"
            placeholder="支持用户名、手机号、邮箱检索"
          />
        </div>
        <div>
          <label class="text-sm text-slate-500">钱包状态</label>
          <select v-model="filters.walletStatus" class="input mt-2">
            <option value="">全部</option>
            <option :value="1">启用</option>
            <option :value="0">停用</option>
          </select>
        </div>
        <div class="flex items-end justify-end gap-3">
          <button class="btn-primary h-11" @click="page.page = 1; loadWallets()">搜索</button>
          <button class="btn-outline h-11" @click="resetFilters">重置</button>
        </div>
      </div>
    </div>

    <div class="card">
      <div class="card-header">
        <div>
          <h2 class="card-title">钱包列表</h2>
          <p class="mt-1 text-sm text-slate-400">共 {{ page.total }} 条记录</p>
        </div>
      </div>
      <div class="card-body">
        <div class="table-wrap">
          <table class="table min-w-[1480px]">
            <thead>
              <tr>
                <th>商户</th>
                <th>钱包号</th>
                <th>总余额</th>
                <th>可用余额</th>
                <th>止入</th>
                <th>止出</th>
                <th>状态</th>
                <th>更新时间</th>
                <th class="text-right">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="!records.length && !loading">
                <td colspan="9" class="empty-state">暂无钱包数据</td>
              </tr>
              <tr v-for="record in records" :key="record.accountId">
                <td>
                  <div class="font-medium text-slate-800">{{ record.username || `商户 #${record.accountId}` }}</div>
                  <div class="mt-1 text-xs text-slate-400">{{ record.phone || record.email || '-' }}</div>
                </td>
                <td class="font-mono text-xs">{{ record.walletNo || '-' }}</td>
                <td class="font-semibold text-slate-900">{{ formatMoney(record.totalBalance) }}</td>
                <td class="font-semibold text-slate-900">{{ formatMoney(record.availableBalance) }}</td>
                <td>
                  <button
                    class="inline-flex items-center justify-center rounded-full border border-slate-200 px-1.5 py-1 transition hover:border-slate-300 disabled:cursor-not-allowed disabled:opacity-60"
                    :disabled="isToggleUpdating(record, 'allowIn')"
                    @click="handleQuickToggle(record, 'allowIn')"
                    :title="`止入：${formatStopFlag(record.allowIn)}`"
                  >
                    <span
                      class="relative inline-flex h-5 w-10 rounded-full transition-colors"
                      :class="getToggleTrackClass(getStopToggleActive(record.allowIn), 'warning')"
                    >
                      <span
                        class="absolute left-0.5 top-0.5 h-4 w-4 rounded-full bg-white shadow-sm transition-transform"
                        :class="getToggleKnobClass(getStopToggleActive(record.allowIn))"
                      ></span>
                    </span>
                  </button>
                </td>
                <td>
                  <button
                    class="inline-flex items-center justify-center rounded-full border border-slate-200 px-1.5 py-1 transition hover:border-slate-300 disabled:cursor-not-allowed disabled:opacity-60"
                    :disabled="isToggleUpdating(record, 'allowOut')"
                    @click="handleQuickToggle(record, 'allowOut')"
                    :title="`止出：${formatStopFlag(record.allowOut)}`"
                  >
                    <span
                      class="relative inline-flex h-5 w-10 rounded-full transition-colors"
                      :class="getToggleTrackClass(getStopToggleActive(record.allowOut), 'warning')"
                    >
                      <span
                        class="absolute left-0.5 top-0.5 h-4 w-4 rounded-full bg-white shadow-sm transition-transform"
                        :class="getToggleKnobClass(getStopToggleActive(record.allowOut))"
                      ></span>
                    </span>
                  </button>
                </td>
                <td>
                  <button
                    class="inline-flex items-center justify-center rounded-full border border-slate-200 px-1.5 py-1 transition hover:border-slate-300 disabled:cursor-not-allowed disabled:opacity-60"
                    :disabled="isToggleUpdating(record, 'status')"
                    @click="handleQuickToggle(record, 'status')"
                    :title="`状态：${formatStatus(record.status)}`"
                  >
                    <span
                      class="relative inline-flex h-5 w-10 rounded-full transition-colors"
                      :class="getToggleTrackClass(getStatusToggleActive(record.status))"
                    >
                      <span
                        class="absolute left-0.5 top-0.5 h-4 w-4 rounded-full bg-white shadow-sm transition-transform"
                        :class="getToggleKnobClass(getStatusToggleActive(record.status))"
                      ></span>
                    </span>
                  </button>
                </td>
                <td>{{ formatDateTime(record.updatedAt) }}</td>
                <td>
                  <div class="flex items-center justify-end gap-2">
                    <button class="btn-primary" @click="openDetail(record)">详情</button>
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

    <div v-if="detailVisible" class="fixed inset-0 z-50 flex items-center justify-center px-4">
      <div class="absolute inset-0 bg-slate-900/50" @click="detailVisible = false"></div>
      <div class="modal-panel max-w-7xl">
        <div class="flex items-center justify-between gap-4">
          <div>
            <h3 class="text-lg font-semibold text-slate-800">钱包详情</h3>
            <p class="mt-1 text-sm text-slate-400">
              {{ detail?.username || currentRecord?.username || `商户 #${currentRecord?.accountId || ''}` }}
            </p>
          </div>
          <button class="btn-text" @click="detailVisible = false">关闭</button>
        </div>

        <div v-if="detailLoading" class="empty-state">详情加载中...</div>

        <div v-else-if="detail" class="mt-6 space-y-6">
          <div class="grid gap-4 lg:grid-cols-4">
            <div class="rounded-2xl border border-slate-200 bg-slate-50 p-4">
              <div class="text-xs text-slate-400">钱包号</div>
              <div class="mt-2 break-all font-mono text-sm text-slate-800">{{ detail.walletNo || '-' }}</div>
            </div>
            <div class="rounded-2xl border border-slate-200 bg-slate-50 p-4">
              <div class="text-xs text-slate-400">总余额</div>
              <div class="mt-2 text-2xl font-semibold text-slate-900">{{ formatMoney(detail.totalBalance) }}</div>
            </div>
            <div class="rounded-2xl border border-slate-200 bg-slate-50 p-4">
              <div class="text-xs text-slate-400">可用余额</div>
              <div class="mt-2 text-2xl font-semibold text-slate-900">{{ formatMoney(detail.availableBalance) }}</div>
            </div>
            <div class="rounded-2xl border border-slate-200 bg-slate-50 p-4">
              <div class="text-xs text-slate-400">钱包状态</div>
              <div class="mt-2 flex flex-wrap items-center gap-2">
                <span class="badge" :class="getWalletStateBadgeClass(detail.status)">{{ formatStatus(detail.status) }}</span>
                <span class="badge" :class="getStopFlagBadgeClass(detail.allowIn)">止入: {{ formatStopFlag(detail.allowIn) }}</span>
                <span class="badge" :class="getStopFlagBadgeClass(detail.allowOut)">止出: {{ formatStopFlag(detail.allowOut) }}</span>
              </div>
            </div>
          </div>

          <div class="rounded-2xl border border-slate-200 bg-white p-4">
            <div class="grid gap-4 lg:grid-cols-4">
              <div>
                <div class="text-xs text-slate-400">用户名</div>
                <div class="mt-2 text-sm text-slate-800">{{ detail.username || '-' }}</div>
              </div>
              <div>
                <div class="text-xs text-slate-400">手机号</div>
                <div class="mt-2 text-sm text-slate-800">{{ detail.phone || '-' }}</div>
              </div>
              <div>
                <div class="text-xs text-slate-400">邮箱</div>
                <div class="mt-2 text-sm text-slate-800">{{ detail.email || '-' }}</div>
              </div>
              <div>
                <div class="text-xs text-slate-400">更新时间</div>
                <div class="mt-2 text-sm text-slate-800">{{ formatDateTime(detail.updatedAt) }}</div>
              </div>
            </div>
            <div class="mt-4">
              <div class="text-xs text-slate-400">备注</div>
              <div class="mt-2 rounded-xl bg-slate-50 px-4 py-3 text-sm text-slate-700">{{ detail.remark || '-' }}</div>
            </div>
          </div>

          <div class="rounded-2xl border border-slate-200 bg-white p-4">
            <div class="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
              <div>
                <div class="text-sm font-semibold text-slate-800">快捷操作</div>
                <p class="mt-1 text-sm text-slate-400">将常用账务动作收在详情内，列表页保持简洁。</p>
              </div>
              <div class="flex flex-wrap gap-2">
                <button class="btn-primary" @click="openOperationDialog(currentRecord, 'manualCredit')">手工入账</button>
                <button class="btn-outline" @click="openOperationDialog(currentRecord, 'manualDebit')">手工扣减</button>
                <button class="btn-outline" @click="openOperationDialog(currentRecord, 'freeze')">冻结金额</button>
                <button class="btn-outline" @click="openOperationDialog(currentRecord, 'unfreeze')">解冻金额</button>
                <button class="btn-outline" @click="openOperationDialog(currentRecord, 'pendingSettlement')">待结算入账</button>
                <button class="btn-outline" @click="openOperationDialog(currentRecord, 'settle')">待结算结转</button>
              </div>
            </div>
          </div>

          <div class="flex flex-wrap gap-2">
            <button
              class="btn-outline"
              :class="{ 'btn-primary': activeDetailTab === 'subWallets' }"
              @click="activeDetailTab = 'subWallets'"
            >
              子账户
            </button>
            <button
              class="btn-outline"
              :class="{ 'btn-primary': activeDetailTab === 'mainFlows' }"
              @click="activeDetailTab = 'mainFlows'"
            >
              主账户流水
            </button>
            <button
              class="btn-outline"
              :class="{ 'btn-primary': activeDetailTab === 'subFlows' }"
              @click="activeDetailTab = 'subFlows'"
            >
              子账户流水
            </button>
          </div>

          <div v-if="activeDetailTab === 'subWallets'" class="rounded-2xl border border-slate-200 bg-white">
            <div class="card-header">
              <div>
                <h4 class="card-title">子账户列表</h4>
                <p class="mt-1 text-sm text-slate-400">母虚子实账户结构下的实际余额分布</p>
              </div>
            </div>
            <div class="card-body">
              <div class="table-wrap">
                <table class="table min-w-full">
                  <thead>
                    <tr>
                      <th>子账户号</th>
                      <th>类型</th>
                      <th>余额</th>
                      <th>状态</th>
                      <th>备注</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-if="!detail.subWallets?.length">
                      <td colspan="5" class="empty-state">暂无子账户数据</td>
                    </tr>
                    <tr v-for="item in detail.subWallets" :key="item.walletNo">
                      <td class="font-mono text-xs">{{ item.walletNo || '-' }}</td>
                      <td>{{ formatWalletType(item.walletType) }}</td>
                      <td class="font-semibold text-slate-900">{{ formatMoney(item.balance) }}</td>
                      <td>
                        <span class="badge" :class="getWalletStateBadgeClass(item.status)">{{ formatStatus(item.status) }}</span>
                      </td>
                      <td class="max-w-[320px] break-words">{{ item.remark || '-' }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>

          <div v-if="activeDetailTab === 'mainFlows'" class="rounded-2xl border border-slate-200 bg-white">
            <div class="card-header">
              <div>
                <h4 class="card-title">主账户流水</h4>
                <p class="mt-1 text-sm text-slate-400">记录母账户总余额与可用余额变化</p>
              </div>
              <button class="btn-outline" @click="loadMainFlows">刷新</button>
            </div>
            <div class="card-body">
              <div class="table-wrap">
                <table class="table min-w-[1320px]">
                  <thead>
                    <tr>
                      <th>流水号</th>
                      <th>订单号</th>
                      <th>业务类型</th>
                      <th>方向</th>
                      <th>变动金额</th>
                      <th>总余额变动</th>
                      <th>可用余额变动</th>
                      <th>备注</th>
                      <th>时间</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-if="!mainFlows.length">
                      <td colspan="9" class="empty-state">暂无主账户流水</td>
                    </tr>
                    <tr v-for="item in mainFlows" :key="item.id">
                      <td>{{ item.id }}</td>
                      <td class="font-mono text-xs">{{ item.orderNo || '-' }}</td>
                      <td>{{ item.bizType || '-' }}</td>
                      <td>
                        <span class="badge" :class="getDirectionBadgeClass(item.direction)">{{ formatDirection(item.direction) }}</span>
                      </td>
                      <td class="font-semibold text-slate-900">{{ formatMoney(item.changeAmount) }}</td>
                      <td>{{ formatMoney(item.totalBalanceBefore) }} -> {{ formatMoney(item.totalBalanceAfter) }}</td>
                      <td>{{ formatMoney(item.availableBalanceBefore) }} -> {{ formatMoney(item.availableBalanceAfter) }}</td>
                      <td class="max-w-[260px] break-words">{{ item.remark || '-' }}</td>
                      <td>{{ formatDateTime(item.createdAt) }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>

              <div class="mt-4 flex items-center justify-between text-sm text-slate-500">
                <span>第 {{ mainFlowPage.page }} / {{ mainFlowPageCount }} 页</span>
                <div class="flex gap-2">
                  <button
                    class="btn-outline"
                    :disabled="mainFlowPage.page <= 1"
                    @click="changeMainFlowPage(mainFlowPage.page - 1)"
                  >
                    上一页
                  </button>
                  <button
                    class="btn-outline"
                    :disabled="mainFlowPage.page >= mainFlowPageCount"
                    @click="changeMainFlowPage(mainFlowPage.page + 1)"
                  >
                    下一页
                  </button>
                </div>
              </div>
            </div>
          </div>

          <div v-if="activeDetailTab === 'subFlows'" class="rounded-2xl border border-slate-200 bg-white">
            <div class="card-header">
              <div>
                <h4 class="card-title">子账户流水</h4>
                <p class="mt-1 text-sm text-slate-400">记录基础户、冻结户、待结算户的实际余额变化</p>
              </div>
              <div class="flex items-center gap-3">
                <select v-model="detailFilters.walletType" class="input h-10 w-44" @change="handleSubFlowFilterChange">
                  <option v-for="option in walletTypeOptions" :key="option.value || 'all'" :value="option.value">
                    {{ option.label }}
                  </option>
                </select>
                <button class="btn-outline" @click="loadSubFlows">刷新</button>
              </div>
            </div>
            <div class="card-body">
              <div class="table-wrap">
                <table class="table min-w-[1280px]">
                  <thead>
                    <tr>
                      <th>流水号</th>
                      <th>订单号</th>
                      <th>子账户类型</th>
                      <th>业务类型</th>
                      <th>方向</th>
                      <th>变动金额</th>
                      <th>余额变动</th>
                      <th>备注</th>
                      <th>时间</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-if="!subFlows.length">
                      <td colspan="9" class="empty-state">暂无子账户流水</td>
                    </tr>
                    <tr v-for="item in subFlows" :key="item.id">
                      <td>{{ item.id }}</td>
                      <td class="font-mono text-xs">{{ item.orderNo || '-' }}</td>
                      <td>{{ formatWalletType(item.walletType) }}</td>
                      <td>{{ item.bizType || '-' }}</td>
                      <td>
                        <span class="badge" :class="getDirectionBadgeClass(item.direction)">{{ formatDirection(item.direction) }}</span>
                      </td>
                      <td class="font-semibold text-slate-900">{{ formatMoney(item.changeAmount) }}</td>
                      <td>{{ formatMoney(item.balanceBefore) }} -> {{ formatMoney(item.balanceAfter) }}</td>
                      <td class="max-w-[260px] break-words">{{ item.remark || '-' }}</td>
                      <td>{{ formatDateTime(item.createdAt) }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>

              <div class="mt-4 flex items-center justify-between text-sm text-slate-500">
                <span>第 {{ subFlowPage.page }} / {{ subFlowPageCount }} 页</span>
                <div class="flex gap-2">
                  <button
                    class="btn-outline"
                    :disabled="subFlowPage.page <= 1"
                    @click="changeSubFlowPage(subFlowPage.page - 1)"
                  >
                    上一页
                  </button>
                  <button
                    class="btn-outline"
                    :disabled="subFlowPage.page >= subFlowPageCount"
                    @click="changeSubFlowPage(subFlowPage.page + 1)"
                  >
                    下一页
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="operationVisible" class="fixed inset-0 z-50 flex items-center justify-center px-4">
      <div class="absolute inset-0 bg-slate-900/50" @click="operationVisible = false"></div>
      <div class="modal-panel max-w-2xl">
        <div class="flex items-center justify-between gap-4">
          <div>
            <h3 class="text-lg font-semibold text-slate-800">{{ currentOperationConfig.title }}</h3>
            <p class="mt-1 text-sm text-slate-400">
              {{ currentRecord?.username || `商户 #${currentRecord?.accountId || ''}` }}
            </p>
          </div>
          <button class="btn-text" @click="operationVisible = false">关闭</button>
        </div>

        <div class="mt-6 grid gap-4">
          <div>
            <label class="text-sm text-slate-500">金额</label>
            <input
              v-model="operationForm.amount"
              type="number"
              min="0.01"
              step="0.01"
              class="input mt-2"
              placeholder="请输入金额"
            />
          </div>
          <div>
            <label class="text-sm text-slate-500">业务单号</label>
            <input
              v-model.trim="operationForm.orderNo"
              class="input mt-2"
              placeholder="可选，支持用于幂等控制"
            />
          </div>
          <div>
            <label class="text-sm text-slate-500">备注</label>
            <textarea
              v-model.trim="operationForm.remark"
              class="input mt-2 min-h-28"
              placeholder="填写本次账务处理的说明"
            ></textarea>
          </div>
        </div>

        <div class="mt-6 flex justify-end gap-3">
          <button class="btn-outline" @click="operationVisible = false">取消</button>
          <button class="btn-primary" :disabled="operationSubmitting" @click="submitOperation">
            {{ operationSubmitting ? '提交中...' : currentOperationConfig.actionText }}
          </button>
        </div>
      </div>
    </div>

  </div>
</template>
