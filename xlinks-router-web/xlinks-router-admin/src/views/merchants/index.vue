<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { listMerchants, updateMerchant, updateMerchantStatus } from '@/api/admin'
import { useToastStore } from '@/stores/toast'
import { formatDateTime, formatStatus } from '@/utils/format'

const toastStore = useToastStore()

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const currentRecord = ref(null)
const records = ref([])

const filters = reactive({
  keyword: '',
  status: '',
})

const page = reactive({
  page: 1,
  pageSize: 10,
  total: 0,
})

const form = reactive({
  remark: '',
})

const pageCount = computed(() => Math.max(1, Math.ceil((page.total || 0) / page.pageSize)))
const enabledCount = computed(() => records.value.filter((item) => Number(item.status) === 1).length)

const loadMerchants = async () => {
  loading.value = true
  try {
    const data = await listMerchants({
      page: page.page,
      pageSize: page.pageSize,
      keyword: filters.keyword,
      status: filters.status,
    })
    records.value = data.records || []
    page.total = data.total || 0
  } catch (error) {
    toastStore.push(error.message || '加载商户列表失败', 'error')
  } finally {
    loading.value = false
  }
}

const resetFilters = async () => {
  Object.assign(filters, { keyword: '', status: '' })
  page.page = 1
  await loadMerchants()
}

const changePage = async (nextPage) => {
  if (nextPage < 1 || nextPage > pageCount.value) {
    return
  }
  page.page = nextPage
  await loadMerchants()
}

const openEdit = (record) => {
  currentRecord.value = record
  form.remark = record.remark || ''
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!currentRecord.value) {
    return
  }
  submitting.value = true
  try {
    await updateMerchant(currentRecord.value.id, { remark: form.remark })
    toastStore.push('商户备注已更新', 'success')
    dialogVisible.value = false
    await loadMerchants()
  } catch (error) {
    toastStore.push(error.message || '保存商户备注失败', 'error')
  } finally {
    submitting.value = false
  }
}

const handleToggleStatus = async (record) => {
  try {
    await updateMerchantStatus(record.id, Number(record.status) === 1 ? 0 : 1)
    toastStore.push('商户状态已更新', 'success')
    await loadMerchants()
  } catch (error) {
    toastStore.push(error.message || '更新商户状态失败', 'error')
  }
}

onMounted(loadMerchants)
</script>

<template>
  <div class="p-6 space-y-6">
    <div class="flex flex-col gap-3 lg:flex-row lg:items-end lg:justify-between">
      <div>
        <h1 class="text-2xl font-bold text-slate-900">商户管理</h1>
        <p class="text-slate-500">集中维护商户账号的基础资料、启用状态和运营备注。</p>
      </div>
      <div class="flex items-center gap-3">
        <div class="rounded-2xl border border-slate-200 bg-white px-4 py-3 shadow-sm">
          <div class="text-xs text-slate-400">当前页启用商户</div>
          <div class="mt-2 text-2xl font-semibold text-slate-900">{{ enabledCount }}</div>
        </div>
        <button class="btn-outline" :disabled="loading" @click="loadMerchants">{{ loading ? '刷新中...' : '刷新' }}</button>
      </div>
    </div>

    <div class="card">
      <div class="card-body grid gap-4 md:grid-cols-4">
        <div class="md:col-span-2">
          <label class="text-sm text-slate-500">商户关键字</label>
          <input v-model.trim="filters.keyword" class="input mt-2" placeholder="支持用户名、手机号、邮箱搜索" />
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
          <button class="btn-primary h-11" @click="page.page = 1; loadMerchants()">搜索</button>
          <button class="btn-outline h-11" @click="resetFilters">重置</button>
        </div>
      </div>
    </div>

    <div class="card">
      <div class="card-header">
        <div>
          <h2 class="card-title">商户列表</h2>
          <p class="text-sm text-slate-400 mt-1">共 {{ page.total }} 条记录</p>
        </div>
      </div>
      <div class="card-body">
        <div class="table-wrap">
          <table class="table min-w-[1080px]">
            <thead>
              <tr>
                <th>商户</th>
                <th>联系方式</th>
                <th>邀请码</th>
                <th>状态</th>
                <th>备注</th>
                <th>创建时间</th>
                <th class="text-right">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="!records.length && !loading">
                <td colspan="7" class="empty-state">暂无商户数据</td>
              </tr>
              <tr v-for="record in records" :key="record.id">
                <td>
                  <div class="font-medium text-slate-800">{{ record.username || `#${record.id}` }}</div>
                  <div class="text-xs text-slate-400 mt-1">商户 ID：{{ record.id }}</div>
                </td>
                <td>
                  <div>{{ record.phone || '-' }}</div>
                  <div class="text-xs text-slate-400 mt-1">{{ record.email || '-' }}</div>
                </td>
                <td>{{ record.inviteCode || '-' }}</td>
                <td>
                  <span class="badge" :class="Number(record.status) === 1 ? 'badge-success' : 'badge-danger'">
                    {{ formatStatus(record.status) }}
                  </span>
                </td>
                <td class="max-w-[240px] break-words">{{ record.remark || '-' }}</td>
                <td>{{ formatDateTime(record.createdAt) }}</td>
                <td>
                  <div class="flex items-center justify-end gap-2">
                    <button class="btn-outline" @click="openEdit(record)">编辑备注</button>
                    <button class="btn-outline" @click="handleToggleStatus(record)">
                      {{ Number(record.status) === 1 ? '停用' : '启用' }}
                    </button>
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
      <div class="modal-panel max-w-xl">
        <div class="flex items-center justify-between gap-4">
          <div>
            <h3 class="text-lg font-semibold text-slate-800">编辑商户备注</h3>
            <p class="text-sm text-slate-400 mt-1">{{ currentRecord?.username || `商户 #${currentRecord?.id || ''}` }}</p>
          </div>
          <button class="btn-text" @click="dialogVisible = false">关闭</button>
        </div>

        <div class="mt-6">
          <label class="text-sm text-slate-500">备注</label>
          <textarea v-model.trim="form.remark" class="input mt-2 min-h-28" placeholder="填写运营跟进、风险提示或客服备注"></textarea>
        </div>

        <div class="mt-6 flex justify-end gap-3">
          <button class="btn-outline" @click="dialogVisible = false">取消</button>
          <button class="btn-primary" :disabled="submitting" @click="handleSubmit">
            {{ submitting ? '提交中...' : '确认保存' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
