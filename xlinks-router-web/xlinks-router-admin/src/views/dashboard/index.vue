<script setup>
import { onMounted, ref } from 'vue'
import { Boxes, Cloud, KeyRound, KeySquare, AlarmClock, Shield, Link2 } from 'lucide-vue-next'
import { getDashboardOverview } from '@/api/admin'
import { useToastStore } from '@/stores/toast'

const toastStore = useToastStore()
const loading = ref(false)
const overview = ref({
  merchantCount: 0,
  activeMerchantCount: 0,
  providerCount: 0,
  activeProviderCount: 0,
  modelCount: 0,
  providerModelCount: 0,
  providerTokenCount: 0,
  customerTokenCount: 0,
  expiringTokenCount: 0,
})

const cards = [
  { key: 'merchantCount', label: '商户总数', icon: Shield },
  { key: 'activeMerchantCount', label: '启用商户', icon: Shield },
  { key: 'providerCount', label: '服务商总数', icon: Cloud },
  { key: 'activeProviderCount', label: '启用服务商', icon: Cloud },
  { key: 'modelCount', label: '标准模型', icon: Boxes },
  { key: 'providerTokenCount', label: '服务商 Token', icon: KeySquare },
  { key: 'customerTokenCount', label: '客户 Token', icon: KeyRound },
  { key: 'providerModelCount', label: '模型映射', icon: Link2 },
  { key: 'expiringTokenCount', label: '7 天内到期 Token', icon: AlarmClock },
]

const loadOverview = async () => {
  loading.value = true
  try {
    overview.value = await getDashboardOverview()
  } catch (error) {
    toastStore.push(error.message || '加载运营总览失败', 'error')
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
        <p class="text-slate-500">聚合商户、资源、订阅与 Token 风险数据，帮助运营人员快速判断平台状态。</p>
      </div>
      <button class="btn-outline" :disabled="loading" @click="loadOverview">
        {{ loading ? '刷新中...' : '刷新数据' }}
      </button>
    </div>

    <div class="grid gap-4 md:grid-cols-2 xl:grid-cols-5">
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
              <p class="font-medium text-slate-800">优先核查即将到期的 Token</p>
              <p class="mt-1">当前有 {{ overview.expiringTokenCount }} 个 Token 会在 7 天内到期，建议提前续期或切换备用凭证。</p>
            </div>
            <span class="badge" :class="overview.expiringTokenCount > 0 ? 'badge-warning' : 'badge-success'">
              {{ overview.expiringTokenCount > 0 ? '需关注' : '正常' }}
            </span>
          </div>
          <div class="flex items-start justify-between gap-4 rounded-2xl bg-slate-50 px-4 py-4">
            <div>
              <p class="font-medium text-slate-800">持续关注商户活跃与资源供给</p>
              <p class="mt-1">当前启用商户 {{ overview.activeMerchantCount }} 个，启用服务商 {{ overview.activeProviderCount }} 个，可用于评估资源供需是否匹配。</p>
            </div>
            <span class="badge badge-success">稳定</span>
          </div>
          <div class="flex items-start justify-between gap-4 rounded-2xl bg-slate-50 px-4 py-4">
            <div>
              <p class="font-medium text-slate-800">检查模型标准化覆盖度</p>
              <p class="mt-1">已维护 {{ overview.modelCount }} 个标准模型，并完成 {{ overview.providerModelCount }} 条服务商映射，建议保持模型与映射同步更新。</p>
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
          <p>1. 商户管理：维护商户账户状态与运营备注。</p>
          <p>2. 资源管理：维护服务商、Token、模型与模型映射。</p>
          <p>3. 套餐运营：维护套餐、订阅记录和激活码资产。</p>
          <p>4. 支付管理：维护支付方式配置与支付链接投放入口。</p>
        </div>
      </div>
    </div>
  </div>
</template>
