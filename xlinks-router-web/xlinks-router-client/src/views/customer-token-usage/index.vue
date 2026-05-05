<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  Activity,
  KeyRound,
  RefreshCcw,
  Search,
  TrendingDown,
  TrendingUp,
  DollarSign,
} from 'lucide-vue-next'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, BarChart } from 'echarts/charts'
import {
  GridComponent,
  TooltipComponent,
  LegendComponent,
  TitleComponent,
  DatasetComponent,
} from 'echarts/components'
import VChart from 'vue-echarts'
import { useCustomerTokenUsage } from '@/composables/useCustomerTokenUsage'
import { formatCompactNumber, formatCurrency, formatDateTime, formatNumber } from '@/utils/formatters'

const { t } = useI18n()

use([
  CanvasRenderer,
  LineChart,
  BarChart,
  GridComponent,
  TooltipComponent,
  LegendComponent,
  TitleComponent,
  DatasetComponent,
])

const {
  tokenInput,
  currentToken,
  usageData,
  modelUsage,
  dashboardStats,
  usageRecords,
  usageLoading,
  usageCurrentPage,
  usageTotal,
  usageTotalPages,
  loading,
  searched,
  hasSavedToken,
  isTokenEditing,
  hasLoaded,
  canQuery,
  maskedTokenInput,
  maskedCurrentToken,
  handleEditToken,
  handleSearch,
  handleUsagePageChange,
  handleUsageRefresh,
  formatChange,
} = useCustomerTokenUsage()

const lineOption = computed(() => ({
  tooltip: {
    trigger: 'axis',
    backgroundColor: '#fff',
    borderColor: '#e2e8f0',
    borderRadius: 12,
    boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)',
    padding: [10, 15],
  },
  grid: {
    left: '3%',
    right: '4%',
    bottom: '3%',
    top: '10%',
    containLabel: true,
  },
  xAxis: {
    type: 'category',
    data: usageData.value.map(d => d.date),
    axisLine: { lineStyle: { color: '#94a3b8' } },
    axisTick: { show: false },
  },
  yAxis: {
    type: 'value',
    axisLine: { show: false },
    splitLine: { lineStyle: { color: '#e2e8f0', type: 'dashed' } },
    axisLabel: { color: '#94a3b8' },
  },
  series: [
    {
      data: usageData.value.map(d => d.tokens),
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 10,
      itemStyle: { color: '#f97316' },
      lineStyle: {
        width: 3,
        color: {
          type: 'linear',
          x: 0, y: 0, x2: 1, y2: 0,
          colorStops: [
            { offset: 0, color: '#f97316' },
            { offset: 1, color: '#ec4899' },
          ],
        },
      },
    },
  ],
}))

const barOption = computed(() => ({
  tooltip: {
    trigger: 'axis',
    backgroundColor: '#fff',
    borderColor: '#e2e8f0',
    borderRadius: 12,
    boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)',
    padding: [10, 15],
  },
  grid: {
    left: '3%',
    right: '4%',
    bottom: '3%',
    top: '10%',
    containLabel: true,
  },
  xAxis: {
    type: 'category',
    data: modelUsage.value.map(d => d.model),
    axisLine: { lineStyle: { color: '#94a3b8' } },
    axisTick: { show: false },
  },
  yAxis: {
    type: 'value',
    axisLine: { show: false },
    splitLine: { lineStyle: { color: '#e2e8f0', type: 'dashed' } },
    axisLabel: { color: '#94a3b8' },
  },
  series: [
    {
      data: modelUsage.value.map(d => d.requests),
      type: 'bar',
      barWidth: '40%',
      itemStyle: {
        borderRadius: [12, 12, 0, 0],
        color: {
          type: 'linear',
          x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: '#f97316' },
            { offset: 1, color: '#ec4899' },
          ],
        },
      },
    },
  ],
}))

const formatUsageSource = (usageType) => {
  if (usageType === 'plan') {
    return t('dashboard.usageTable.usageSourcePlan')
  }
  if (usageType === 'balance') {
    return t('dashboard.usageTable.usageSourceBalance')
  }
  return t('dashboard.usageTable.usageSourceUnknown')
}

const formatResponseSeconds = (responseMs) => {
  const ms = Number(responseMs || 0)
  if (ms <= 0) {
    return t('dashboard.usageTable.responseAbnormal')
  }
  return `${(ms / 1000).toFixed(2)}s`
}

const getResponseTagClass = (responseMs) => {
  const ms = Number(responseMs || 0)
  if (ms <= 0) {
    return 'bg-red-100 text-red-700 border-red-200'
  }
  const seconds = ms / 1000
  if (seconds > 10) {
    return 'bg-red-100 text-red-700 border-red-200'
  }
  if (seconds >= 6) {
    return 'bg-amber-100 text-amber-700 border-amber-200'
  }
  return 'bg-emerald-100 text-emerald-700 border-emerald-200'
}

const getUsageSourceTagClass = (usageType) => {
  if (usageType === 'plan') {
    return 'bg-blue-100 text-blue-700 border-blue-200'
  }
  if (usageType === 'balance') {
    return 'bg-violet-100 text-violet-700 border-violet-200'
  }
  return 'bg-slate-100 text-slate-600 border-slate-200'
}

const getInputTokenTagClass = (inputTokens) => {
  const tokens = Number(inputTokens || 0)
  if (tokens > 200000) {
    return 'bg-red-100 text-red-700 border-red-200'
  }
  if (tokens > 150000) {
    return 'bg-amber-100 text-amber-700 border-amber-200'
  }
  return 'bg-emerald-100 text-emerald-700 border-emerald-200'
}
</script>

<template>
  <div class="min-h-screen bg-gradient-main">
    <div class="mx-auto flex w-full max-w-[90rem] flex-col px-4 py-6 md:px-6 md:py-10 xl:px-8">
      <section class="overflow-hidden rounded-[2rem] border border-white/70 bg-white/80 shadow-[0_24px_80px_rgba(15,23,42,0.12)] backdrop-blur">
        <div class="bg-gradient-hero px-6 py-8 md:px-8 md:py-10">
          <div class="flex flex-col gap-6 lg:flex-row lg:items-end lg:justify-between">
            <div class="max-w-3xl">
              <div class="inline-flex items-center gap-2 rounded-full border border-white/25 bg-white/10 px-3 py-1 text-sm text-white/90 backdrop-blur-sm">
                <KeyRound class="h-4 w-4" />
                <span>{{ t('customerTokenUsage.badge') }}</span>
              </div>
              <h1 class="mt-4 text-3xl font-bold tracking-tight text-white md:text-4xl">
                {{ t('customerTokenUsage.title') }}
              </h1>
              <p class="mt-3 max-w-2xl text-sm leading-6 text-white/85 md:text-base">
                {{ t('customerTokenUsage.subtitle') }}
              </p>
            </div>

            <form class="w-full max-w-2xl" @submit.prevent="handleSearch">
              <div class="rounded-[1.75rem] border border-white/20 bg-white/12 p-4 shadow-[0_18px_50px_rgba(15,23,42,0.15)] backdrop-blur-md">
                <label class="mb-3 block text-sm font-medium text-white/90">
                  {{ t('customerTokenUsage.formLabel') }}
                </label>
                <div class="flex flex-col gap-3 md:flex-row">
                  <div v-if="isTokenEditing" class="relative flex-1">
                    <KeyRound class="pointer-events-none absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
                    <input
                      v-model="tokenInput"
                      type="text"
                      :placeholder="t('customerTokenUsage.placeholder')"
                      class="h-12 w-full rounded-2xl border border-white/15 bg-white px-11 pr-4 text-sm text-slate-900 shadow-sm outline-none transition focus:border-white focus:ring-2 focus:ring-white/40"
                    />
                  </div>
                  <button
                    v-else
                    type="button"
                    class="flex h-12 flex-1 items-center rounded-2xl border border-white/15 bg-white/95 px-4 text-left text-sm font-medium text-slate-700 shadow-sm transition hover:border-white/40"
                    @click="handleEditToken"
                  >
                    <KeyRound class="mr-3 h-4 w-4 text-slate-400" />
                    <span class="truncate">{{ maskedTokenInput }}</span>
                  </button>
                  <button
                    type="submit"
                    :disabled="loading || !canQuery"
                    class="inline-flex h-12 items-center justify-center gap-2 rounded-2xl bg-slate-900 px-5 text-sm font-semibold text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:bg-slate-500"
                  >
                    <Search class="h-4 w-4" />
                    <span>{{ loading ? t('common.loading') : t('customerTokenUsage.search') }}</span>
                  </button>
                </div>
                <p class="mt-3 text-xs text-white/75">
                  {{ t('customerTokenUsage.helper') }}
                </p>
              </div>
            </form>
          </div>
        </div>

        <div class="border-t border-slate-200/80 bg-white px-6 py-4 md:px-8">
          <div class="flex flex-col gap-2 text-sm text-slate-500 md:flex-row md:items-center md:justify-between">
            <p>{{ t('customerTokenUsage.tip') }}</p>
            <p v-if="currentToken" class="font-medium text-slate-700">
              {{ t('customerTokenUsage.currentToken') }}: {{ maskedCurrentToken }}
            </p>
          </div>
        </div>
      </section>

      <section v-if="hasLoaded" class="mt-8">
        <div class="mb-8 grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3">
          <div class="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm transition-shadow hover:shadow-lg">
            <div class="mb-4 flex items-center justify-between">
              <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-gradient-to-br from-blue-500 to-blue-600 shadow-lg">
                <Activity class="h-6 w-6 text-white" />
              </div>
              <span class="flex items-center text-sm font-medium" :class="dashboardStats.todayRequestsChange >= 0 ? 'text-green-600' : 'text-red-600'">
                <component :is="dashboardStats.todayRequestsChange >= 0 ? TrendingUp : TrendingDown" class="mr-1 h-4 w-4" />
                {{ formatChange(dashboardStats.todayRequestsChange) }}
              </span>
            </div>
            <h3 class="mb-1 text-sm text-slate-500">{{ t('dashboard.todayRequests') }}</h3>
            <p class="text-2xl font-bold text-slate-900">{{ formatNumber(dashboardStats.todayRequests) }}</p>
          </div>

          <div class="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm transition-shadow hover:shadow-lg">
            <div class="mb-4 flex items-center justify-between">
              <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-gradient-to-br from-emerald-500 to-emerald-600 shadow-lg">
                <KeyRound class="h-6 w-6 text-white" />
              </div>
              <span class="flex items-center text-sm font-medium" :class="dashboardStats.todayTokensChange >= 0 ? 'text-green-600' : 'text-red-600'">
                <component :is="dashboardStats.todayTokensChange >= 0 ? TrendingUp : TrendingDown" class="mr-1 h-4 w-4" />
                {{ formatChange(dashboardStats.todayTokensChange) }}
              </span>
            </div>
            <h3 class="mb-1 text-sm text-slate-500">{{ t('dashboard.tokenUsage') }}</h3>
            <p class="text-2xl font-bold text-slate-900">{{ formatNumber(dashboardStats.todayTokens) }}</p>
          </div>

          <div class="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm transition-shadow hover:shadow-lg">
            <div class="mb-4 flex items-center justify-between">
              <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-gradient-icon shadow-lg">
                <DollarSign class="h-6 w-6 text-white" />
              </div>
              <span class="flex items-center text-sm font-medium" :class="dashboardStats.todayCostChange >= 0 ? 'text-green-600' : 'text-red-600'">
                <component :is="dashboardStats.todayCostChange >= 0 ? TrendingUp : TrendingDown" class="mr-1 h-4 w-4" />
                {{ formatChange(dashboardStats.todayCostChange) }}
              </span>
            </div>
            <h3 class="mb-1 text-sm text-slate-500">{{ t('dashboard.todayCost') }}</h3>
            <p class="text-2xl font-bold text-slate-900">{{ formatCurrency(dashboardStats.todayCost) }}</p>
          </div>
        </div>

        <div class="mb-8 grid grid-cols-1 gap-6 lg:grid-cols-2">
          <div class="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm transition-shadow hover:shadow-lg">
            <h2 class="mb-6 text-lg font-semibold text-slate-900">{{ t('dashboard.usageTrend') }}</h2>
            <div class="h-[300px]">
              <v-chart v-if="usageData.length" class="h-full w-full" :option="lineOption" autoresize />
              <div v-else class="flex h-full items-center justify-center text-slate-400">
                {{ loading ? t('common.loading') : t('common.noData') }}
              </div>
            </div>
          </div>

          <div class="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm transition-shadow hover:shadow-lg">
            <h2 class="mb-6 text-lg font-semibold text-slate-900">{{ t('dashboard.modelDistribution') }}</h2>
            <div class="h-[300px]">
              <v-chart v-if="modelUsage.length" class="h-full w-full" :option="barOption" autoresize />
              <div v-else class="flex h-full items-center justify-center text-slate-400">
                {{ loading ? t('common.loading') : t('common.noData') }}
              </div>
            </div>
          </div>
        </div>

        <div class="overflow-hidden rounded-3xl border-2 border-slate-200 bg-white shadow-sm">
          <div class="bg-gradient-hero p-6">
            <div class="flex items-center justify-between gap-3">
              <div class="flex items-center gap-3">
                <div class="flex h-10 w-10 items-center justify-center rounded-xl border border-white/30 bg-white/25 backdrop-blur-sm">
                  <Activity class="h-5 w-5 text-white" />
                </div>
                <h2 class="text-xl font-bold text-white">{{ t('dashboard.usageRecords') }}</h2>
              </div>
              <button
                class="inline-flex items-center gap-2 rounded-lg border border-white/30 bg-white/15 px-3 py-1.5 text-sm font-medium text-white transition-colors hover:bg-white/25 disabled:cursor-not-allowed disabled:opacity-60"
                :disabled="usageLoading || loading"
                @click="handleUsageRefresh"
              >
                <RefreshCcw class="h-4 w-4" :class="{ 'animate-spin': usageLoading || loading }" />
                <span>{{ t('common.refresh') }}</span>
              </button>
            </div>
          </div>

          <div class="p-6">
            <div v-if="usageLoading && !usageRecords.length" class="py-12 text-center text-slate-500">
              {{ t('common.loading') }}
            </div>

            <div v-else-if="!usageRecords.length" class="py-12 text-center text-slate-400">
              {{ t('common.noData') }}
            </div>

            <template v-else>
              <div class="hidden overflow-x-auto md:block">
                <table class="w-full">
                  <thead>
                    <tr class="border-b-2 border-slate-200">
                      <th class="px-4 py-3 text-left text-sm font-semibold text-slate-700">{{ t('dashboard.usageTable.time') }}</th>
                      <th class="w-[140px] px-4 py-3 text-left text-sm font-semibold text-slate-700">{{ t('dashboard.usageTable.token') }}</th>
                      <th class="w-[140px] px-4 py-3 text-left text-sm font-semibold text-slate-700">{{ t('dashboard.usageTable.model') }}</th>
                      <th class="px-4 py-3 text-left text-sm font-semibold text-slate-700">{{ t('dashboard.usageTable.inputTokens') }}</th>
                      <th class="px-4 py-3 text-left text-sm font-semibold text-slate-700">{{ t('dashboard.usageTable.cacheHitTokens') }}</th>
                      <th class="px-4 py-3 text-left text-sm font-semibold text-slate-700">{{ t('dashboard.usageTable.outputTokens') }}</th>
                      <th class="px-4 py-3 text-left text-sm font-semibold text-slate-700">{{ t('dashboard.usageTable.totalTokens') }}</th>
                      <th class="whitespace-nowrap px-4 py-3 text-left text-sm font-semibold text-slate-700">{{ t('dashboard.usageTable.responseMs') }}</th>
                      <th class="whitespace-nowrap px-4 py-3 text-left text-sm font-semibold text-slate-700">{{ t('dashboard.usageTable.usageSource') }}</th>
                      <th class="w-[120px] px-4 py-3 text-right text-sm font-semibold text-slate-700">{{ t('dashboard.usageTable.cost') }}</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr
                      v-for="(record, index) in usageRecords"
                      :key="index"
                      class="border-b border-slate-100 transition-colors hover:bg-slate-50"
                    >
                      <td class="whitespace-nowrap px-4 py-4 text-sm text-slate-600">{{ formatDateTime(record.time) }}</td>
                      <td class="px-4 py-4 text-sm font-medium text-slate-700">
                        <div class="max-w-[140px] truncate" :title="record.token">{{ record.token }}</div>
                      </td>
                      <td class="px-4 py-4">
                        <div class="max-w-[140px] truncate" :title="record.model">
                          <span class="font-medium text-slate-900">{{ record.model }}</span>
                        </div>
                      </td>
                      <td class="whitespace-nowrap px-4 py-4 text-left text-sm text-slate-600">
                        <span class="inline-flex items-center rounded-full border px-2 py-0.5 text-xs" :class="getInputTokenTagClass(record.inputTokens)">
                          {{ formatCompactNumber(record.inputTokens) }}
                        </span>
                      </td>
                      <td class="whitespace-nowrap px-4 py-4 text-left text-sm text-slate-600">{{ formatCompactNumber(record.cacheHitTokens) }}</td>
                      <td class="whitespace-nowrap px-4 py-4 text-left text-sm text-slate-600">{{ formatCompactNumber(record.outputTokens) }}</td>
                      <td class="whitespace-nowrap px-4 py-4 text-left text-sm text-slate-600">{{ formatCompactNumber(record.totalTokens) }}</td>
                      <td class="whitespace-nowrap px-4 py-4 text-left text-sm text-slate-600">
                        <span class="inline-flex items-center rounded-full border px-2 py-0.5 text-xs" :class="getResponseTagClass(record.responseMs)">
                          {{ formatResponseSeconds(record.responseMs) }}
                        </span>
                      </td>
                      <td class="whitespace-nowrap px-4 py-4 text-left text-sm text-slate-600">
                        <span class="inline-flex items-center rounded-full border px-2 py-0.5 text-xs" :class="getUsageSourceTagClass(record.usageType)">
                          {{ formatUsageSource(record.usageType) }}
                        </span>
                      </td>
                      <td class="whitespace-nowrap px-4 py-4 text-right text-sm text-slate-600">{{ formatCurrency(record.cost, 'USD', undefined, 6) }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>

              <div class="space-y-4 md:hidden">
                <div
                  v-for="(record, index) in usageRecords"
                  :key="index"
                  class="rounded-2xl border border-slate-200 bg-slate-50 p-4"
                >
                  <div class="mb-3 flex items-start justify-between gap-3">
                    <div>
                      <p class="font-semibold text-slate-900">{{ record.model }}</p>
                      <p class="text-sm text-slate-500">{{ formatDateTime(record.time) }}</p>
                      <div class="mt-2 flex flex-wrap items-center gap-2">
                        <span class="inline-flex items-center rounded-full border border-primary/15 bg-primary/10 px-2 py-0.5 text-[11px] font-medium text-primary">
                          {{ t('dashboard.usageTable.token') }}: {{ record.token }}
                        </span>
                      </div>
                    </div>
                    <div class="text-right">
                      <p class="text-xs text-slate-500">{{ t('dashboard.usageTable.cost') }}</p>
                      <p class="font-bold text-slate-900">{{ formatCurrency(record.cost, 'USD', undefined, 6) }}</p>
                    </div>
                  </div>

                  <div class="grid grid-cols-2 gap-3">
                    <div class="rounded-xl border border-slate-200 bg-white p-3">
                      <p class="mb-1 text-xs text-slate-500">{{ t('dashboard.usageTable.inputTokens') }}</p>
                      <p>
                        <span class="inline-flex items-center rounded-full border px-2 py-0.5 text-xs" :class="getInputTokenTagClass(record.inputTokens)">
                          {{ formatCompactNumber(record.inputTokens) }}
                        </span>
                      </p>
                    </div>
                    <div class="rounded-xl border border-slate-200 bg-white p-3">
                      <p class="mb-1 text-xs text-slate-500">{{ t('dashboard.usageTable.cacheHitTokens') }}</p>
                      <p class="text-sm text-slate-700">{{ formatCompactNumber(record.cacheHitTokens) }}</p>
                    </div>
                    <div class="rounded-xl border border-slate-200 bg-white p-3">
                      <p class="mb-1 text-xs text-slate-500">{{ t('dashboard.usageTable.outputTokens') }}</p>
                      <p class="text-sm text-slate-700">{{ formatCompactNumber(record.outputTokens) }}</p>
                    </div>
                    <div class="rounded-xl border border-slate-200 bg-white p-3">
                      <p class="mb-1 text-xs text-slate-500">{{ t('dashboard.usageTable.totalTokens') }}</p>
                      <p class="text-sm text-slate-700">{{ formatCompactNumber(record.totalTokens) }}</p>
                    </div>
                    <div class="rounded-xl border border-slate-200 bg-white p-3">
                      <p class="mb-1 text-xs text-slate-500">{{ t('dashboard.usageTable.responseMs') }}</p>
                      <p>
                        <span class="inline-flex items-center rounded-full border px-2 py-0.5 text-xs" :class="getResponseTagClass(record.responseMs)">
                          {{ formatResponseSeconds(record.responseMs) }}
                        </span>
                      </p>
                    </div>
                    <div class="rounded-xl border border-slate-200 bg-white p-3">
                      <p class="mb-1 text-xs text-slate-500">{{ t('dashboard.usageTable.usageSource') }}</p>
                      <p>
                        <span class="inline-flex items-center rounded-full border px-2 py-0.5 text-xs" :class="getUsageSourceTagClass(record.usageType)">
                          {{ formatUsageSource(record.usageType) }}
                        </span>
                      </p>
                    </div>
                  </div>
                </div>
              </div>

              <div class="mt-6 flex flex-col gap-3 border-slate-200 md:flex-row md:items-center md:justify-between">
                <p class="text-sm text-slate-500">
                  {{ t('dashboard.pagination.summary', { total: usageTotal, page: usageCurrentPage, pages: usageTotalPages }) }}
                </p>
                <div class="flex items-center gap-2">
                  <button
                    class="rounded-lg border border-slate-300 px-3 py-1.5 text-sm transition-colors"
                    :class="usageCurrentPage <= 1 || usageLoading ? 'cursor-not-allowed bg-slate-50 text-slate-300' : 'text-slate-700 hover:bg-slate-100'"
                    :disabled="usageCurrentPage <= 1 || usageLoading"
                    @click="handleUsagePageChange(usageCurrentPage - 1)"
                  >
                    {{ t('dashboard.pagination.prev') }}
                  </button>
                  <span class="min-w-[72px] text-center text-sm text-slate-600">
                    {{ usageCurrentPage }} / {{ usageTotalPages }}
                  </span>
                  <button
                    class="rounded-lg border border-slate-300 px-3 py-1.5 text-sm transition-colors"
                    :class="usageCurrentPage >= usageTotalPages || usageLoading ? 'cursor-not-allowed bg-slate-50 text-slate-300' : 'text-slate-700 hover:bg-slate-100'"
                    :disabled="usageCurrentPage >= usageTotalPages || usageLoading"
                    @click="handleUsagePageChange(usageCurrentPage + 1)"
                  >
                    {{ t('dashboard.pagination.next') }}
                  </button>
                </div>
              </div>
            </template>
          </div>
        </div>
      </section>

      <section v-else class="mt-8 rounded-[2rem] border border-dashed border-slate-300 bg-white/70 p-10 text-center shadow-sm backdrop-blur">
        <div class="mx-auto flex max-w-xl flex-col items-center">
          <div class="flex h-16 w-16 items-center justify-center rounded-2xl bg-slate-100 text-slate-500">
            <KeyRound class="h-8 w-8" />
          </div>
          <h2 class="mt-5 text-2xl font-bold text-slate-900">
            {{ searched ? t('customerTokenUsage.emptyTitleAfterSearch') : (hasSavedToken ? t('customerTokenUsage.emptyTitleWithSavedToken') : t('customerTokenUsage.emptyTitle')) }}
          </h2>
          <p class="mt-3 text-sm leading-6 text-slate-500">
            {{ searched ? t('customerTokenUsage.emptyDescAfterSearch') : (hasSavedToken ? t('customerTokenUsage.emptyDescWithSavedToken') : t('customerTokenUsage.emptyDesc')) }}
          </p>
        </div>
      </section>
    </div>
  </div>
</template>
