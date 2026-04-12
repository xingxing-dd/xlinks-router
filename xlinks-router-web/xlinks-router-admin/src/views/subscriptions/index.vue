<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { createSubscription, listPlans, listSubscriptions } from '@/api/admin'
import { useToastStore } from '@/stores/toast'
import { formatDateTime, formatStatus } from '@/utils/format'

const toastStore = useToastStore()

const loading = ref(false)
const grantDialogVisible = ref(false)
const grantSubmitting = ref(false)
const records = ref([])
const planOptions = ref([])

const filters = reactive({
  accountKeyword: '',
  planId: '',
  status: '',
  source: '',
})

const page = reactive({
  page: 1,
  pageSize: 10,
  total: 0,
})

const grantForm = reactive({
  accountId: '',
  planId: '',
})

const SOURCE_OPTIONS = [
  { label: '激活码兑换', value: 'activation_code' },
  { label: '后台发放', value: 'admin' },
  { label: '直接购买', value: 'purchase' },
  { label: '赠送', value: 'grant' },
]

const pageCount = computed(() => Math.max(1, Math.ceil((page.total || 0) / page.pageSize)))

const summary = computed(() => {
  const totalDaily = records.value.reduce((sum, item) => sum + Number(item.dailyQuota || 0), 0)
  const totalRemaining = records.value.reduce((sum, item) => sum + Number(item.totalRemainingQuota || 0), 0)
  const activeCount = records.value.filter((item) => Number(item.status) === 1).length
  return {
    activeCount,
    totalDaily: totalDaily.toFixed(2),
    totalRemaining: totalRemaining.toFixed(2),
  }
})

const loadPlans = async () => {
  try {
    const data = await listPlans({ page: 1, pageSize: 200 })
    planOptions.value = data.records || []
  } catch (error) {
    toastStore.push(error.message || '加载套餐选项失败', 'error')
  }
}

const loadSubscriptions = async () => {
  loading.value = true
  try {
    const data = await listSubscriptions({
      page: page.page,
      pageSize: page.pageSize,
      accountKeyword: filters.accountKeyword,
      planId: filters.planId,
      status: filters.status,
      source: filters.source,
    })
    records.value = data.records || []
    page.total = data.total || 0
  } catch (error) {
    toastStore.push(error.message || '加载订阅记录失败', 'error')
  } finally {
    loading.value = false
  }
}

const resetFilters = async () => {
  Object.assign(filters, {
    accountKeyword: '',
    planId: '',
    status: '',
    source: '',
  })
  page.page = 1
  await loadSubscriptions()
}

const changePage = async (nextPage) => {
  if (nextPage < 1 || nextPage > pageCount.value) {
    return
  }
  page.page = nextPage
  await loadSubscriptions()
}

const resolveSourceLabel = (value) => {
  const match = SOURCE_OPTIONS.find((item) => item.value === value)
  return match ? match.label : (value || '-')
}

const openGrantDialog = () => {
  grantForm.accountId = ''
  grantForm.planId = ''
  grantDialogVisible.value = true
}

const closeGrantDialog = () => {
  if (grantSubmitting.value) {
    return
  }
  grantDialogVisible.value = false
}

const handleGrantSubscription = async () => {
  const accountId = Number(grantForm.accountId)
  const planId = Number(grantForm.planId)
  if (!accountId || accountId <= 0) {
    toastStore.push('请填写有效的商户 ID', 'warning')
    return
  }
  if (!planId || planId <= 0) {
    toastStore.push('请选择套餐', 'warning')
    return
  }

  grantSubmitting.value = true
  try {
    await createSubscription({
      accountId,
      planId,
    })
    toastStore.push('后台发放订阅成功', 'success')
    grantDialogVisible.value = false
    page.page = 1
    await loadSubscriptions()
  } catch (error) {
    toastStore.push(error.message || '后台发放订阅失败', 'error')
  } finally {
    grantSubmitting.value = false
  }
}

onMounted(async () => {
  await Promise.all([loadPlans(), loadSubscriptions()])
})
</script>

<template>
  <div class="p-6 space-y-6">
    <div class="flex flex-col gap-3 lg:flex-row lg:items-end lg:justify-between">
      <div>
        <h1 class="text-2xl font-bold text-slate-900">套餐订阅记录</h1>
        <p class="text-slate-500">按客户、套餐、来源追踪订阅生效情况与额度消耗。</p>
      </div>
      <div class="grid gap-3 sm:grid-cols-3">
        <div class="rounded-2xl border border-slate-200 bg-white px-4 py-3 shadow-sm">
          <div class="text-xs text-slate-400">当前页生效订阅</div>
          <div class="mt-2 text-2xl font-semibold text-slate-900">{{ summary.activeCount }}</div>
        </div>
        <div class="rounded-2xl border border-slate-200 bg-white px-4 py-3 shadow-sm">
          <div class="text-xs text-slate-400">当前页每日额度</div>
          <div class="mt-2 text-2xl font-semibold text-slate-900">${{ summary.totalDaily }}</div>
        </div>
        <div class="rounded-2xl border border-slate-200 bg-white px-4 py-3 shadow-sm">
          <div class="text-xs text-slate-400">当前页总剩余额度</div>
          <div class="mt-2 text-2xl font-semibold text-slate-900">${{ summary.totalRemaining }}</div>
        </div>
      </div>
    </div>

    <div class="card">
      <div class="card-body grid gap-4 md:grid-cols-5">
        <div>
          <label class="text-sm text-slate-500">客户账号</label>
          <input v-model.trim="filters.accountKeyword" class="input mt-2" placeholder="用户名 / 手机号 / 邮箱" />
        </div>
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
          </select>
        </div>
        <div>
          <label class="text-sm text-slate-500">来源</label>
          <select v-model="filters.source" class="input mt-2">
            <option value="">全部</option>
            <option v-for="option in SOURCE_OPTIONS" :key="option.value" :value="option.value">{{ option.label }}</option>
          </select>
        </div>
        <div class="flex items-end justify-end gap-3">
          <button class="btn-primary h-11" @click="page.page = 1; loadSubscriptions()">搜索</button>
          <button class="btn-outline h-11" @click="resetFilters">重置</button>
        </div>
      </div>
    </div>

    <div class="card">
      <div class="card-header">
        <div>
          <h2 class="card-title">订阅列表</h2>
          <p class="text-sm text-slate-400 mt-1">共 {{ page.total }} 条记录</p>
        </div>
        <div class="flex items-center gap-2">
          <button class="btn-primary" @click="openGrantDialog">后台发放订阅</button>
          <button class="btn-outline" :disabled="loading" @click="loadSubscriptions">{{ loading ? '刷新中...' : '刷新' }}</button>
        </div>
      </div>
      <div class="card-body">
        <div class="table-wrap">
          <table class="table min-w-[1320px]">
            <thead>
              <tr>
                <th>客户</th>
                <th>套餐</th>
                <th>来源</th>
                <th>状态</th>
                <th>每日额度</th>
                <th>日剩余额度</th>
                <th>总额度</th>
                <th>总剩余额度</th>
                <th>到期时间</th>
                <th>更新时间</th>
                <th>备注</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="!records.length && !loading">
                <td colspan="11" class="empty-state">暂无订阅记录</td>
              </tr>
              <tr v-for="record in records" :key="record.id">
                <td>
                  <div class="font-medium text-slate-800">{{ record.accountName || '-' }}</div>
                  <div class="text-xs text-slate-400 mt-1">ID: {{ record.accountId || '-' }}</div>
                  <div class="text-xs text-slate-400 mt-1">{{ record.accountPhone || record.accountEmail || '-' }}</div>
                </td>
                <td>
                  <div class="font-medium text-slate-800">{{ record.planName }}</div>
                  <div class="text-xs text-slate-400 mt-1">套餐 ID: {{ record.planId }}</div>
                </td>
                <td>
                  <span class="badge badge-warning">{{ resolveSourceLabel(record.source) }}</span>
                </td>
                <td>
                  <span class="badge" :class="Number(record.status) === 1 ? 'badge-success' : 'badge-danger'">
                    {{ formatStatus(record.status) }}
                  </span>
                </td>
                <td>${{ Number(record.dailyQuota || 0).toFixed(2) }}</td>
                <td>${{ Number(record.dailyRemainingQuota || 0).toFixed(2) }}</td>
                <td>${{ Number(record.totalQuota || 0).toFixed(2) }}</td>
                <td>${{ Number(record.totalRemainingQuota || 0).toFixed(2) }}</td>
                <td>{{ formatDateTime(record.planExpireTime) }}</td>
                <td>{{ formatDateTime(record.updatedAt) }}</td>
                <td class="max-w-[240px] break-words">{{ record.remark || '-' }}</td>
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

    <div v-if="grantDialogVisible" class="fixed inset-0 z-50 flex items-center justify-center px-4">
      <div class="absolute inset-0 bg-slate-900/50" @click="closeGrantDialog"></div>
      <div class="modal-panel max-w-xl">
        <div class="flex items-center justify-between gap-4">
          <div>
            <h3 class="text-lg font-semibold text-slate-800">后台发放订阅</h3>
            <p class="text-sm text-slate-400 mt-1">为指定商户直接创建订阅记录</p>
          </div>
          <button class="btn-text" @click="closeGrantDialog">关闭</button>
        </div>

        <div class="mt-6 grid gap-4 md:grid-cols-2">
          <div>
            <label class="text-sm text-slate-500">商户 ID</label>
            <input v-model.trim="grantForm.accountId" class="input mt-2" placeholder="请输入商户 ID" />
          </div>
          <div>
            <label class="text-sm text-slate-500">套餐</label>
            <select v-model="grantForm.planId" class="input mt-2">
              <option value="">请选择套餐</option>
              <option v-for="plan in planOptions" :key="plan.id" :value="plan.id">{{ plan.planName }}</option>
            </select>
          </div>
        </div>

        <div class="mt-6 rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-600">
          备注将自动写入：后台发放的订阅
        </div>

        <div class="mt-6 flex justify-end gap-3">
          <button class="btn-outline" @click="closeGrantDialog">取消</button>
          <button class="btn-primary" :disabled="grantSubmitting" @click="handleGrantSubscription">
            {{ grantSubmitting ? '提交中...' : '确认发放' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
