<script setup>
import { onMounted, ref } from 'vue'
import { Boxes, Cloud, KeyRound, KeySquare, Link2, AlarmClock } from 'lucide-vue-next'
import { getDashboardOverview } from '@/api/admin'
import { useToastStore } from '@/stores/toast'

const toastStore = useToastStore()
const loading = ref(false)
const overview = ref({
  providerCount: 0,
  activeProviderCount: 0,
  endpointCount: 0,
  modelCount: 0,
  providerModelCount: 0,
  providerTokenCount: 0,
  customerTokenCount: 0,
  expiringTokenCount: 0,
})

const cards = [
  { key: 'providerCount', label: '服务商总数', icon: Cloud },
  { key: 'endpointCount', label: '标准端点', icon: Link2 },
  { key: 'modelCount', label: '标准模型', icon: Boxes },
  { key: 'providerModelCount', label: '模型映射', icon: Link2 },
  { key: 'providerTokenCount', label: '服务商 Token', icon: KeySquare },
  { key: 'customerTokenCount', label: '客户 Token', icon: KeyRound },
  { key: 'activeProviderCount', label: '启用服务商', icon: Cloud },
  { key: 'expiringTokenCount', label: '7 天内到期 Token', icon: AlarmClock },
]

const loadOverview = async () => {
  loading.value = true
  try {
    overview.value = await getDashboardOverview()
  } catch (error) {
    toastStore.push(error.message || '加载运营概览失败', 'error')
  } finally {
    loading.value = false
  }
}

onMounted(loadOverview)
</script>

<template>
  <div class="p-6 space-y-6">
    <div class="flex flex-col gap-2 md:flex-row md:items-end md:justify-between">
      <div>
        <h1 class="text-2xl font-bold text-slate-900">运营总览</h1>
        <p class="text-slate-500">聚焦路由平台配置资产与 Token 风险的关键指标。</p>
      </div>
      <button class="btn-outline" :disabled="loading" @click="loadOverview">
        {{ loading ? '刷新中...' : '刷新数据' }}
      </button>
    </div>

    <div class="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
      <div v-for="card in cards" :key="card.key" class="card p-5 flex items-center justify-between gap-4">
        <div>
          <p class="text-sm text-slate-500">{{ card.label }}</p>
          <p class="text-3xl font-semibold text-slate-800 mt-2">{{ overview[card.key] ?? 0 }}</p>
        </div>
        <div class="w-12 h-12 bg-primary/10 rounded-2xl flex items-center justify-center shrink-0">
          <component :is="card.icon" class="w-6 h-6 text-primary" />
        </div>
      </div>
    </div>

    <div class="grid gap-6 xl:grid-cols-3">
      <div class="card xl:col-span-2">
        <div class="card-header">
          <h2 class="card-title">运营建议</h2>
        </div>
        <div class="card-body space-y-4 text-sm text-slate-600">
          <div class="flex items-start justify-between gap-4 rounded-2xl bg-slate-50 px-4 py-4">
            <div>
              <p class="font-medium text-slate-800">优先核查即将到期 Token</p>
              <p class="mt-1">当前有 {{ overview.expiringTokenCount }} 个 Token 在 7 天内到期，建议提前替换或续期。</p>
            </div>
            <span class="badge" :class="overview.expiringTokenCount > 0 ? 'badge-warning' : 'badge-success'">
              {{ overview.expiringTokenCount > 0 ? '需关注' : '正常' }}
            </span>
          </div>
          <div class="flex items-start justify-between gap-4 rounded-2xl bg-slate-50 px-4 py-4">
            <div>
              <p class="font-medium text-slate-800">核对标准模型与映射覆盖度</p>
              <p class="mt-1">已维护 {{ overview.modelCount }} 个标准模型，对应 {{ overview.providerModelCount }} 条服务商映射。</p>
            </div>
            <span class="badge badge-success">可用</span>
          </div>
        </div>
      </div>

      <div class="card">
        <div class="card-header">
          <h2 class="card-title">核心范围</h2>
        </div>
        <div class="card-body space-y-3 text-sm text-slate-600">
          <p>1. 服务商管理：维护上游平台基础信息与路由优先级。</p>
          <p>2. 服务商 Token：维护上游凭证与到期时间。</p>
          <p>3. 模型资源中心：维护端点、标准模型和服务商映射。</p>
          <p>4. 客户 Token：管理客户可调用模型与使用资格。</p>
        </div>
      </div>
    </div>
  </div>
</template>
