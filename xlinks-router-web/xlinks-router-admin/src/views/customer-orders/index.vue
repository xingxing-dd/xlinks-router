<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { listCustomerOrders } from '@/api/admin'
import { useToastStore } from '@/stores/toast'
import { formatDateTime } from '@/utils/format'

const toastStore = useToastStore()

const orderTypeOptions = [
  { label: '全部', value: '' },
  { label: '充值', value: 'recharge' },
  { label: '提现', value: 'withdraw' },
  { label: '套餐购买', value: 'subscription_purchase' },
]

const statusOptions = [
  { label: '全部', value: '' },
  { label: '待支付', value: 0 },
  { label: '已支付', value: 1 },
  { label: '失败', value: 2 },
  { label: '已关闭', value: 3 },
  { label: '已退款', value: 4 },
]

const getTodayStartDateTime = () => {
  const now = new Date()
  now.setHours(0, 0, 0, 0)
  const offset = now.getTimezoneOffset()
  const localDate = new Date(now.getTime() - offset * 60 * 1000)
  return localDate.toISOString().slice(0, 16)
}

const loading = ref(false)
const records = ref([])

const filters = reactive({
  orderKeyword: '',
  accountKeyword: '',
  orderType: '',
  paymentChannel: '',
  status: '',
  startAt: getTodayStartDateTime(),
  endAt: '',
})

const page = reactive({
  page: 1,
  pageSize: 20,
  total: 0,
})

const pageCount = computed(() => Math.max(1, Math.ceil((page.total || 0) / page.pageSize)))

const normalizeDateTime = (value) => {
  if (!value) {
    return undefined
  }
  return value.length === 16 ? `${value.replace('T', ' ')}:00` : value.replace('T', ' ')
}

const buildQuery = () => ({
  page: Number(page.page),
  pageSize: Number(page.pageSize),
  orderKeyword: filters.orderKeyword,
  accountKeyword: filters.accountKeyword,
  orderType: filters.orderType,
  paymentChannel: filters.paymentChannel,
  status: filters.status === '' ? undefined : Number(filters.status),
  startAt: normalizeDateTime(filters.startAt),
  endAt: normalizeDateTime(filters.endAt),
})

const loadRecords = async () => {
  loading.value = true
  try {
    const data = await listCustomerOrders(buildQuery())
    records.value = data.records || []
    page.total = data.total || 0
  } catch (error) {
    toastStore.push(error.message || '加载订单失败', 'error')
  } finally {
    loading.value = false
  }
}

const resetFilters = async () => {
  Object.assign(filters, {
    orderKeyword: '',
    accountKeyword: '',
    orderType: '',
    paymentChannel: '',
    status: '',
    startAt: getTodayStartDateTime(),
    endAt: '',
  })
  page.page = 1
  await loadRecords()
}

const changePage = async (nextPage) => {
  if (nextPage < 1 || nextPage > pageCount.value) {
    return
  }
  page.page = nextPage
  await loadRecords()
}

const changePageSize = async () => {
  page.page = 1
  await loadRecords()
}

const formatMoney = (value) => {
  if (value === null || value === undefined || value === '') {
    return '0.00'
  }
  const amount = Number(value)
  return Number.isNaN(amount) ? `${value}` : amount.toFixed(2)
}

const formatOrderType = (value) => {
  if (value === 'recharge') {
    return '充值'
  }
  if (value === 'withdraw') {
    return '提现'
  }
  if (value === 'subscription_purchase') {
    return '套餐购买'
  }
  return value || '-'
}

const formatOrderStatus = (value) => {
  if (Number(value) === 0) {
    return '待支付'
  }
  if (Number(value) === 1) {
    return '已支付'
  }
  if (Number(value) === 2) {
    return '失败'
  }
  if (Number(value) === 3) {
    return '已关闭'
  }
  if (Number(value) === 4) {
    return '已退款'
  }
  return value ?? '-'
}

const getStatusBadgeClass = (value) => {
  if (Number(value) === 1) {
    return 'badge-success'
  }
  if (Number(value) === 0) {
    return 'badge-warning'
  }
  return 'badge-danger'
}

const formatAccount = (record) => record.accountName || record.accountEmail || record.accountPhone || `用户 #${record.accountId}`

const formatOrderInfo = (value) => {
  if (!value) {
    return '-'
  }
  const normalized = `${value}`.replace(/\s+/g, ' ').trim()
  return normalized.length > 120 ? `${normalized.slice(0, 120)}...` : normalized
}

onMounted(loadRecords)
</script>

<template>
  <div class="p-6 space-y-6">
    <div class="flex flex-col gap-3 lg:flex-row lg:items-end lg:justify-between">
      <div>
        <h1 class="text-2xl font-bold text-slate-900">用户订单</h1>
        <p class="text-slate-500">查询 customer_orders 表中的订单数据，支持按订单、用户、类型、渠道、状态和时间筛选。</p>
      </div>
      <button class="btn-outline" :disabled="loading" @click="loadRecords">
        {{ loading ? '刷新中...' : '刷新' }}
      </button>
    </div>

    <div class="card">
      <div class="card-body grid gap-4 md:grid-cols-3 lg:grid-cols-6">
        <div>
          <label class="text-sm text-slate-500">订单关键词</label>
          <input
            v-model.trim="filters.orderKeyword"
            class="input mt-2"
            placeholder="订单号 / 渠道单号 / 标题"
          />
        </div>
        <div>
          <label class="text-sm text-slate-500">用户关键词</label>
          <input
            v-model.trim="filters.accountKeyword"
            class="input mt-2"
            placeholder="用户名 / 手机 / 邮箱"
          />
        </div>
        <div>
          <label class="text-sm text-slate-500">订单类型</label>
          <select v-model="filters.orderType" class="input mt-2">
            <option v-for="option in orderTypeOptions" :key="option.value || 'all'" :value="option.value">
              {{ option.label }}
            </option>
          </select>
        </div>
        <div>
          <label class="text-sm text-slate-500">支付渠道</label>
          <input
            v-model.trim="filters.paymentChannel"
            class="input mt-2"
            placeholder="例如 alipay"
          />
        </div>
        <div>
          <label class="text-sm text-slate-500">订单状态</label>
          <select v-model="filters.status" class="input mt-2">
            <option v-for="option in statusOptions" :key="`${option.value}`" :value="option.value">
              {{ option.label }}
            </option>
          </select>
        </div>
        <div>
          <label class="text-sm text-slate-500">开始时间</label>
          <input v-model="filters.startAt" type="datetime-local" class="input mt-2" />
        </div>
        <div>
          <label class="text-sm text-slate-500">结束时间</label>
          <input v-model="filters.endAt" type="datetime-local" class="input mt-2" />
        </div>
        <div class="flex items-end justify-end gap-3 md:col-span-2">
          <button class="btn-primary h-11" @click="page.page = 1; loadRecords()">查询</button>
          <button class="btn-outline h-11" @click="resetFilters">重置</button>
        </div>
      </div>
    </div>

    <div class="card">
      <div class="card-header">
        <div>
          <h2 class="card-title">订单列表</h2>
          <p class="mt-1 text-sm text-slate-400">共 {{ page.total }} 条记录</p>
        </div>
      </div>
      <div class="card-body">
        <div class="table-wrap">
          <table class="table min-w-[1700px]">
            <thead>
              <tr>
                <th>订单号</th>
                <th>渠道单号</th>
                <th>用户</th>
                <th>订单类型</th>
                <th>订单标题</th>
                <th>支付渠道</th>
                <th>金额</th>
                <th>状态</th>
                <th>订单信息</th>
                <th>创建时间</th>
                <th>完成时间</th>
                <th>过期时间</th>
                <th>备注</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="!records.length && !loading">
                <td colspan="13" class="empty-state">暂无订单数据</td>
              </tr>
              <tr v-for="item in records" :key="item.id">
                <td>
                  <div class="font-mono text-xs text-slate-800">{{ item.orderNo || '-' }}</div>
                  <div class="mt-1 text-xs text-slate-400">ID {{ item.id }}</div>
                </td>
                <td class="font-mono text-xs">{{ item.refNo || '-' }}</td>
                <td>
                  <div class="font-medium text-slate-800">{{ formatAccount(item) }}</div>
                  <div class="mt-1 text-xs text-slate-400">{{ item.accountEmail || item.accountPhone || '-' }}</div>
                </td>
                <td>{{ formatOrderType(item.orderType) }}</td>
                <td>{{ item.orderTitle || '-' }}</td>
                <td>{{ item.paymentChannel || '-' }}</td>
                <td class="font-semibold text-slate-900">{{ formatMoney(item.totalAmount) }}</td>
                <td>
                  <span class="badge" :class="getStatusBadgeClass(item.status)">{{ formatOrderStatus(item.status) }}</span>
                </td>
                <td>
                  <div class="max-w-[280px] whitespace-pre-wrap break-all text-xs text-slate-600">
                    {{ formatOrderInfo(item.orderInfo) }}
                  </div>
                </td>
                <td>{{ formatDateTime(item.createdAt) }}</td>
                <td>{{ formatDateTime(item.completeAt) }}</td>
                <td>{{ formatDateTime(item.expiredAt) }}</td>
                <td>
                  <div class="max-w-[220px] whitespace-pre-wrap break-words text-xs text-slate-600">
                    {{ item.remark || '-' }}
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="mt-4 flex items-center justify-between text-sm text-slate-500">
          <div class="flex items-center gap-3">
            <span>第 {{ page.page }} / {{ pageCount }} 页</span>
            <label class="flex items-center gap-2">
              <span>每页</span>
              <select v-model.number="page.pageSize" class="input h-9 w-24" @change="changePageSize">
                <option :value="20">20</option>
                <option :value="50">50</option>
                <option :value="100">100</option>
              </select>
            </label>
          </div>
          <div class="flex gap-2">
            <button class="btn-outline" :disabled="page.page <= 1" @click="changePage(page.page - 1)">上一页</button>
            <button class="btn-outline" :disabled="page.page >= pageCount" @click="changePage(page.page + 1)">下一页</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
