<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { listActivationCodes, listPlans } from '@/api/admin'
import { useToastStore } from '@/stores/toast'
import { formatDateTime } from '@/utils/format'

const toastStore = useToastStore()

const loading = ref(false)
const records = ref([])
const planOptions = ref([])

const filters = reactive({
  planId: '',
  activationCode: '',
  usedAccount: '',
  subscriptionId: '',
  orderId: '',
})

const page = reactive({
  page: 1,
  pageSize: 10,
  total: 0,
})

const pageCount = computed(() => Math.max(1, Math.ceil((page.total || 0) / page.pageSize)))

const summary = computed(() => ({
  pageTotal: records.value.length,
  withOrderCount: records.value.filter((item) => item.orderId).length,
  uniquePlans: new Set(records.value.map((item) => item.planId)).size,
}))

const loadPlans = async () => {
  try {
    const data = await listPlans({ page: 1, pageSize: 200 })
    planOptions.value = data.records || []
  } catch (error) {
    toastStore.push(error.message || '加载套餐选项失败', 'error')
  }
}

const loadRecords = async () => {
  loading.value = true
  try {
    const data = await listActivationCodes({
      page: page.page,
      pageSize: page.pageSize,
      status: 2,
      planId: filters.planId,
      activationCode: filters.activationCode,
      usedAccount: filters.usedAccount,
      subscriptionId: filters.subscriptionId,
      orderId: filters.orderId,
    })
    records.value = data.records || []
    page.total = data.total || 0
  } catch (error) {
    toastStore.push(error.message || '加载激活使用记录失败', 'error')
  } finally {
    loading.value = false
  }
}

const resetFilters = async () => {
  Object.assign(filters, {
    planId: '',
    activationCode: '',
    usedAccount: '',
    subscriptionId: '',
    orderId: '',
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

const copyText = async (text) => {
  try {
    await navigator.clipboard.writeText(text)
    toastStore.push('内容已复制', 'success')
  } catch (error) {
    toastStore.push('当前环境不支持自动复制', 'warning')
  }
}

onMounted(async () => {
  await Promise.all([loadPlans(), loadRecords()])
})
</script>

<template>
  <div class="p-6 space-y-6">
    <div class="flex flex-col gap-3 lg:flex-row lg:items-end lg:justify-between">
      <div>
        <h1 class="text-2xl font-bold text-slate-900">激活码使用记录</h1>
        <p class="text-slate-500">聚焦已使用激活码，追踪兑换账号、订阅记录与订单号关联。</p>
      </div>
      <div class="grid gap-3 sm:grid-cols-3">
        <div class="rounded-2xl border border-slate-200 bg-white px-4 py-3 shadow-sm">
          <div class="text-xs text-slate-400">当前页记录数</div>
          <div class="mt-2 text-2xl font-semibold text-slate-900">{{ summary.pageTotal }}</div>
        </div>
        <div class="rounded-2xl border border-slate-200 bg-white px-4 py-3 shadow-sm">
          <div class="text-xs text-slate-400">关联订单数</div>
          <div class="mt-2 text-2xl font-semibold text-slate-900">{{ summary.withOrderCount }}</div>
        </div>
        <div class="rounded-2xl border border-slate-200 bg-white px-4 py-3 shadow-sm">
          <div class="text-xs text-slate-400">涉及套餐数</div>
          <div class="mt-2 text-2xl font-semibold text-slate-900">{{ summary.uniquePlans }}</div>
        </div>
      </div>
    </div>

    <div class="card">
      <div class="card-body grid gap-4 md:grid-cols-6">
        <div>
          <label class="text-sm text-slate-500">套餐</label>
          <select v-model="filters.planId" class="input mt-2">
            <option value="">全部</option>
            <option v-for="plan in planOptions" :key="plan.id" :value="plan.id">{{ plan.planName }}</option>
          </select>
        </div>
        <div>
          <label class="text-sm text-slate-500">激活码</label>
          <input v-model.trim="filters.activationCode" class="input mt-2" placeholder="支持模糊搜索" />
        </div>
        <div>
          <label class="text-sm text-slate-500">使用账号</label>
          <input v-model.trim="filters.usedAccount" class="input mt-2" placeholder="用户名 / 手机号 / 邮箱" />
        </div>
        <div>
          <label class="text-sm text-slate-500">订阅 ID</label>
          <input v-model.trim="filters.subscriptionId" class="input mt-2" placeholder="精确匹配" />
        </div>
        <div>
          <label class="text-sm text-slate-500">订单号</label>
          <input v-model.trim="filters.orderId" class="input mt-2" placeholder="支持模糊搜索" />
        </div>
        <div class="flex items-end justify-end gap-3">
          <button class="btn-primary h-11" @click="page.page = 1; loadRecords()">搜索</button>
          <button class="btn-outline h-11" @click="resetFilters">重置</button>
        </div>
      </div>
    </div>

    <div class="card">
      <div class="card-header">
        <div>
          <h2 class="card-title">使用记录列表</h2>
          <p class="text-sm text-slate-400 mt-1">共 {{ page.total }} 条记录</p>
        </div>
        <button class="btn-outline" :disabled="loading" @click="loadRecords">{{ loading ? '刷新中...' : '刷新' }}</button>
      </div>
      <div class="card-body">
        <div class="table-wrap">
          <table class="table min-w-[1320px]">
            <thead>
              <tr>
                <th>激活码</th>
                <th>套餐</th>
                <th>使用账号</th>
                <th>使用时间</th>
                <th>订阅 ID</th>
                <th>订单号</th>
                <th>备注</th>
                <th>更新时间</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="!records.length && !loading">
                <td colspan="8" class="empty-state">暂无激活码使用记录</td>
              </tr>
              <tr v-for="record in records" :key="record.id">
                <td>
                  <div class="font-mono text-sm text-slate-800 break-all">{{ record.activationCode }}</div>
                  <button class="btn-text mt-2" @click="copyText(record.activationCode)">复制</button>
                </td>
                <td>
                  <div class="font-medium text-slate-800">{{ record.planName || '-' }}</div>
                  <div class="text-xs text-slate-400 mt-1">套餐 ID: {{ record.planId || '-' }}</div>
                </td>
                <td>{{ record.usedAccount || '-' }}</td>
                <td>{{ formatDateTime(record.usedAt) }}</td>
                <td>{{ record.subscriptionId || '-' }}</td>
                <td class="font-mono text-xs text-slate-600 break-all">{{ record.orderId || '-' }}</td>
                <td class="max-w-[220px] break-words">{{ record.remark || '-' }}</td>
                <td>{{ formatDateTime(record.updatedAt) }}</td>
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
  </div>
</template>
