<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { getUsageAccountSummary, getUsageModelSummary, listUsageRecords } from '@/api/admin'
import { useToastStore } from '@/stores/toast'
import { formatDateTime } from '@/utils/format'

const toastStore = useToastStore()

const activeTab = ref('flow')
const loading = ref(false)
const records = ref([])

const filters = reactive({
  accountKeyword: '',
  modelCode: '',
  providerCode: '',
  usageType: '',
  requestId: '',
  responseStatus: '',
  startAt: '',
  endAt: '',
})

const page = reactive({
  page: 1,
  pageSize: 20,
  total: 0,
})

const pageCount = computed(() => Math.max(1, Math.ceil((page.total || 0) / page.pageSize)))

const buildQuery = () => ({
  page: Number(page.page),
  pageSize: Number(page.pageSize),
  accountKeyword: filters.accountKeyword,
  modelCode: filters.modelCode,
  providerCode: filters.providerCode,
  usageType: filters.usageType,
  requestId: activeTab.value === 'flow' ? filters.requestId : undefined,
  responseStatus: activeTab.value === 'flow' ? filters.responseStatus : undefined,
  startAt: normalizeDateTime(filters.startAt),
  endAt: normalizeDateTime(filters.endAt),
})

const loadRecords = async () => {
  loading.value = true
  try {
    const query = buildQuery()
    let data
    if (activeTab.value === 'flow') {
      data = await listUsageRecords(query)
    } else if (activeTab.value === 'account') {
      data = await getUsageAccountSummary(query)
    } else {
      data = await getUsageModelSummary(query)
    }
    records.value = data.records || []
    page.total = data.total || 0
  } catch (error) {
    toastStore.push(error.message || 'Failed to load usage records', 'error')
  } finally {
    loading.value = false
  }
}

const resetFilters = async () => {
  Object.assign(filters, {
    accountKeyword: '',
    modelCode: '',
    providerCode: '',
    usageType: '',
    requestId: '',
    responseStatus: '',
    startAt: '',
    endAt: '',
  })
  page.page = 1
  await loadRecords()
}

const changeTab = async (tab) => {
  if (activeTab.value === tab) {
    return
  }
  activeTab.value = tab
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

const normalizeDateTime = (value) => {
  if (!value) {
    return undefined
  }
  return value.length === 16 ? `${value.replace('T', ' ')}:00` : value.replace('T', ' ')
}

const formatNumber = (value) => {
  if (value === null || value === undefined) {
    return '0'
  }
  return `${value}`
}

const formatCost = (value) => {
  if (value === null || value === undefined) {
    return '0.000000'
  }
  return Number(value).toFixed(6)
}

const formatLatency = (value) => {
  if (value === null || value === undefined) {
    return '0'
  }
  return Number(value).toFixed(2)
}

onMounted(loadRecords)
</script>

<template>
  <div class="p-6 space-y-6">
    <div class="flex flex-col gap-3 lg:flex-row lg:items-end lg:justify-between">
      <div>
        <h1 class="text-2xl font-bold text-slate-900">Token使用记录</h1>
        <p class="text-slate-500">支持查询流水、按账户汇总、按模型汇总（含平均响应时间）。</p>
      </div>
      <div class="flex gap-2">
        <button class="btn-outline" :class="{ 'btn-primary': activeTab === 'flow' }" @click="changeTab('flow')">流水</button>
        <button class="btn-outline" :class="{ 'btn-primary': activeTab === 'account' }" @click="changeTab('account')">账户汇总</button>
        <button class="btn-outline" :class="{ 'btn-primary': activeTab === 'model' }" @click="changeTab('model')">模型汇总</button>
      </div>
    </div>

    <div class="card">
      <div class="card-body grid gap-4 md:grid-cols-4 lg:grid-cols-6">
        <div>
          <label class="text-sm text-slate-500">账户</label>
          <input v-model.trim="filters.accountKeyword" class="input mt-2" placeholder="用户名/手机号/邮箱" />
        </div>
        <div>
          <label class="text-sm text-slate-500">模型编码</label>
          <input v-model.trim="filters.modelCode" class="input mt-2" placeholder="例如 gpt-4o-mini" />
        </div>
        <div>
          <label class="text-sm text-slate-500">服务商编码</label>
          <input v-model.trim="filters.providerCode" class="input mt-2" placeholder="例如 openai" />
        </div>
        <div>
          <label class="text-sm text-slate-500">使用类型</label>
          <select v-model="filters.usageType" class="input mt-2">
            <option value="">全部</option>
            <option value="balance">balance</option>
            <option value="plan">plan</option>
          </select>
        </div>
        <div v-if="activeTab === 'flow'">
          <label class="text-sm text-slate-500">请求ID</label>
          <input v-model.trim="filters.requestId" class="input mt-2" placeholder="request_id" />
        </div>
        <div v-if="activeTab === 'flow'">
          <label class="text-sm text-slate-500">响应状态</label>
          <input v-model.trim="filters.responseStatus" type="number" class="input mt-2" placeholder="200" />
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
          <h2 class="card-title">记录列表</h2>
          <p class="text-sm text-slate-400 mt-1">共 {{ page.total }} 条记录</p>
        </div>
        <button class="btn-outline" :disabled="loading" @click="loadRecords">{{ loading ? '刷新中...' : '刷新' }}</button>
      </div>
      <div class="card-body">
        <div class="table-wrap">
          <table v-if="activeTab === 'flow'" class="table min-w-[1680px]">
            <thead>
              <tr>
                <th>请求ID</th>
                <th>账户</th>
                <th>模型</th>
                <th>服务商</th>
                <th>类型</th>
                <th>状态</th>
                <th>输入</th>
                <th>缓存命中</th>
                <th>输出</th>
                <th>总Token</th>
                <th>总费用</th>
                <th>延迟(ms)</th>
                <th>时间</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="!records.length && !loading">
                <td colspan="13" class="empty-state">暂无数据</td>
              </tr>
              <tr v-for="item in records" :key="item.id">
                <td class="font-mono text-xs break-all">{{ item.requestId || '-' }}</td>
                <td>
                  <div class="font-medium text-slate-800">{{ item.accountName || item.accountId }}</div>
                  <div class="text-xs text-slate-400 mt-1">{{ item.accountEmail || item.accountPhone || '-' }}</div>
                </td>
                <td>
                  <div>{{ item.modelName || '-' }}</div>
                  <div class="text-xs text-slate-400 mt-1">{{ item.modelCode || '-' }}</div>
                </td>
                <td>{{ item.providerCode || '-' }}</td>
                <td>{{ item.usageType || '-' }}</td>
                <td>{{ item.responseStatus ?? '-' }}</td>
                <td>{{ formatNumber(item.promptTokens) }}</td>
                <td>{{ formatNumber(item.cacheHitTokens) }}</td>
                <td>{{ formatNumber(item.completionTokens) }}</td>
                <td>{{ formatNumber(item.totalTokens) }}</td>
                <td>{{ formatCost(item.totalCost) }}</td>
                <td>{{ item.latencyMs ?? 0 }}</td>
                <td>{{ formatDateTime(item.createdAt) }}</td>
              </tr>
            </tbody>
          </table>

          <table v-else-if="activeTab === 'account'" class="table min-w-[1200px]">
            <thead>
              <tr>
                <th>账户</th>
                <th>请求数</th>
                <th>输入</th>
                <th>缓存命中</th>
                <th>输出</th>
                <th>总Token</th>
                <th>总费用</th>
                <th>平均响应时间(ms)</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="!records.length && !loading">
                <td colspan="8" class="empty-state">暂无数据</td>
              </tr>
              <tr v-for="item in records" :key="item.accountId">
                <td>
                  <div class="font-medium text-slate-800">{{ item.accountName || item.accountId }}</div>
                  <div class="text-xs text-slate-400 mt-1">{{ item.accountEmail || item.accountPhone || '-' }}</div>
                </td>
                <td>{{ formatNumber(item.requestCount) }}</td>
                <td>{{ formatNumber(item.promptTokens) }}</td>
                <td>{{ formatNumber(item.cacheHitTokens) }}</td>
                <td>{{ formatNumber(item.completionTokens) }}</td>
                <td>{{ formatNumber(item.totalTokens) }}</td>
                <td>{{ formatCost(item.totalCost) }}</td>
                <td>{{ formatLatency(item.avgLatencyMs) }}</td>
              </tr>
            </tbody>
          </table>

          <table v-else class="table min-w-[1200px]">
            <thead>
              <tr>
                <th>模型</th>
                <th>请求数</th>
                <th>输入</th>
                <th>缓存命中</th>
                <th>输出</th>
                <th>总Token</th>
                <th>总费用</th>
                <th>平均响应时间(ms)</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="!records.length && !loading">
                <td colspan="8" class="empty-state">暂无数据</td>
              </tr>
              <tr v-for="item in records" :key="`${item.modelId}-${item.modelCode}`">
                <td>
                  <div class="font-medium text-slate-800">{{ item.modelName || '-' }}</div>
                  <div class="text-xs text-slate-400 mt-1">{{ item.modelCode || '-' }}</div>
                </td>
                <td>{{ formatNumber(item.requestCount) }}</td>
                <td>{{ formatNumber(item.promptTokens) }}</td>
                <td>{{ formatNumber(item.cacheHitTokens) }}</td>
                <td>{{ formatNumber(item.completionTokens) }}</td>
                <td>{{ formatNumber(item.totalTokens) }}</td>
                <td>{{ formatCost(item.totalCost) }}</td>
                <td>{{ formatLatency(item.avgLatencyMs) }}</td>
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
