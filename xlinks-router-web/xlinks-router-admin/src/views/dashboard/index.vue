<script setup>
import { useI18n } from 'vue-i18n'
import { TrendingUp, Users, CreditCard, KeySquare } from 'lucide-vue-next'

const { t } = useI18n()

const stats = [
  { label: '活跃商户', value: '128', icon: Users },
  { label: '服务商数量', value: '24', icon: TrendingUp },
  { label: '有效 Token', value: '2,340', icon: KeySquare },
  { label: '本月交易额', value: '¥1,245,000', icon: CreditCard },
]

const activities = [
  { title: '商户「未来科技」完成实名认证', time: '10分钟前' },
  { title: '服务商 OpenAI 新增模型配额', time: '2小时前' },
  { title: 'Token sk-****9a 刷新完成', time: '4小时前' },
]
</script>

<template>
  <div class="p-6 space-y-6">
    <div>
      <h1 class="text-2xl font-bold text-slate-900">{{ t('dashboard.title') }}</h1>
      <p class="text-slate-500 mt-1">全局运营状态一览（静态示例）</p>
    </div>

    <div class="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
      <div v-for="item in stats" :key="item.label" class="card p-5 flex items-center justify-between">
        <div>
          <p class="text-sm text-slate-500">{{ item.label }}</p>
          <p class="text-2xl font-semibold text-slate-800 mt-2">{{ item.value }}</p>
        </div>
        <div class="w-12 h-12 bg-primary/10 rounded-2xl flex items-center justify-center">
          <component :is="item.icon" class="w-6 h-6 text-primary" />
        </div>
      </div>
    </div>

    <div class="grid gap-6 lg:grid-cols-2">
      <div class="card">
        <div class="card-header">
          <h2 class="card-title">最新动态</h2>
          <button class="btn-outline">查看全部</button>
        </div>
        <div class="card-body space-y-4">
          <div v-for="activity in activities" :key="activity.title" class="flex items-center justify-between">
            <div>
              <p class="text-sm font-medium text-slate-700">{{ activity.title }}</p>
              <p class="text-xs text-slate-400 mt-1">{{ activity.time }}</p>
            </div>
            <span class="badge badge-success">已处理</span>
          </div>
        </div>
      </div>
      <div class="card">
        <div class="card-header">
          <h2 class="card-title">风险提醒</h2>
          <button class="btn-outline">查看详情</button>
        </div>
        <div class="card-body space-y-4">
          <div class="flex items-start justify-between">
            <div>
              <p class="text-sm font-medium text-slate-700">本周交易异常波动</p>
              <p class="text-xs text-slate-400 mt-1">建议复核服务商计费配置</p>
            </div>
            <span class="badge badge-warning">需关注</span>
          </div>
          <div class="flex items-start justify-between">
            <div>
              <p class="text-sm font-medium text-slate-700">2 个 Token 即将过期</p>
              <p class="text-xs text-slate-400 mt-1">提醒商户及时续费或停用</p>
            </div>
            <span class="badge badge-danger">紧急</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
