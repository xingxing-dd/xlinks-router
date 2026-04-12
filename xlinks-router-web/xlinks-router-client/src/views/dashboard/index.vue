<script setup>
import { computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { 
  TrendingUp, 
  TrendingDown, 
  Activity, 
  DollarSign, 
  Key, 
  Zap,
  CreditCard,
  Plus,
  RefreshCcw
} from 'lucide-vue-next'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, BarChart } from 'echarts/charts'
import {
  GridComponent,
  TooltipComponent,
  LegendComponent,
  TitleComponent,
  DatasetComponent
} from 'echarts/components'
import VChart from 'vue-echarts'
import { useDashboard } from '@/composables/useDashboard'
import { formatCurrency, formatNumber, formatDateTime, formatCompactNumber } from '@/utils/formatters'

const { t } = useI18n()

use([
  CanvasRenderer,
  LineChart,
  BarChart,
  GridComponent,
  TooltipComponent,
  LegendComponent,
  TitleComponent,
  DatasetComponent
])

const {
  usageData,
  modelUsage,
  dashboardStats,
  usageRecords,
  usageLoading,
  usageCurrentPage,
  usageTotal,
  usageTotalPages,
  loading,
  isRechargeModalOpen,
  usdAmount,
  selectedPayment,
  calculateCnyAmount,
  loadDashboard,
  handleUsagePageChange,
  handleUsageRefresh,
  handleConfirmRecharge,
  formatChange,
} = useDashboard()

const paymentMethods = [
  { id: 'alipay', name: t('plans.alipay'), icon: '💳' },
  { id: 'wechat', name: t('plans.wechat'), icon: '💚' },
]

const lineOption = computed(() => ({
  tooltip: {
    trigger: 'axis',
    backgroundColor: '#fff',
    borderColor: '#e2e8f0',
    borderRadius: 12,
    boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)',
    padding: [10, 15]
  },
  grid: {
    left: '3%',
    right: '4%',
    bottom: '3%',
    top: '10%',
    containLabel: true
  },
  xAxis: {
    type: 'category',
    data: usageData.value.map(d => d.date),
    axisLine: { lineStyle: { color: '#94a3b8' } },
    axisTick: { show: false }
  },
  yAxis: {
    type: 'value',
    axisLine: { show: false },
    splitLine: { lineStyle: { color: '#e2e8f0', type: 'dashed' } },
    axisLabel: { color: '#94a3b8' }
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
            { offset: 1, color: '#ec4899' }
          ]
        }
      }
    }
  ]
}))

const barOption = computed(() => ({
  tooltip: {
    trigger: 'axis',
    backgroundColor: '#fff',
    borderColor: '#e2e8f0',
    borderRadius: 12,
    boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)',
    padding: [10, 15]
  },
  grid: {
    left: '3%',
    right: '4%',
    bottom: '3%',
    top: '10%',
    containLabel: true
  },
  xAxis: {
    type: 'category',
    data: modelUsage.value.map(d => d.model),
    axisLine: { lineStyle: { color: '#94a3b8' } },
    axisTick: { show: false }
  },
  yAxis: {
    type: 'value',
    axisLine: { show: false },
    splitLine: { lineStyle: { color: '#e2e8f0', type: 'dashed' } },
    axisLabel: { color: '#94a3b8' }
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
            { offset: 1, color: '#ec4899' }
          ]
        }
      }
    }
  ]
}))

onMounted(loadDashboard)
</script>

<template>
  <div class="p-4 md:p-8 max-w-7xl mx-auto">
    <!-- 统计卡片 -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
      <div class="bg-white rounded-2xl p-6 border border-slate-200 shadow-sm hover:shadow-lg transition-shadow">
        <div class="flex items-center justify-between mb-4">
          <div class="w-12 h-12 bg-gradient-to-br from-blue-500 to-blue-600 rounded-xl flex items-center justify-center shadow-lg">
            <Activity class="w-6 h-6 text-white" />
          </div>
          <span class="flex items-center text-sm font-medium" :class="dashboardStats.todayRequestsChange >= 0 ? 'text-green-600' : 'text-red-600'">
            <component :is="dashboardStats.todayRequestsChange >= 0 ? TrendingUp : TrendingDown" class="w-4 h-4 mr-1" />
            {{ formatChange(dashboardStats.todayRequestsChange) }}
          </span>
        </div>
        <h3 class="text-slate-500 text-sm mb-1">
          {{ t('dashboard.todayRequests') }}
        </h3>
        <p class="text-2xl font-bold text-slate-900">
          {{ formatNumber(dashboardStats.todayRequests) }}
        </p>
      </div>

      <div class="bg-white rounded-2xl p-6 border border-slate-200 shadow-sm hover:shadow-lg transition-shadow">
        <div class="flex items-center justify-between mb-4">
          <div class="w-12 h-12 bg-gradient-to-br from-emerald-500 to-emerald-600 rounded-xl flex items-center justify-center shadow-lg">
            <Key class="w-6 h-6 text-white" />
          </div>
          <span class="flex items-center text-sm font-medium" :class="dashboardStats.todayTokensChange >= 0 ? 'text-green-600' : 'text-red-600'">
            <component :is="dashboardStats.todayTokensChange >= 0 ? TrendingUp : TrendingDown" class="w-4 h-4 mr-1" />
            {{ formatChange(dashboardStats.todayTokensChange) }}
          </span>
        </div>
        <h3 class="text-slate-500 text-sm mb-1">
          {{ t('dashboard.tokenUsage') }}
        </h3>
        <p class="text-2xl font-bold text-slate-900">
          {{ formatNumber(dashboardStats.todayTokens) }}
        </p>
      </div>

      <div class="bg-white rounded-2xl p-6 border border-slate-200 shadow-sm hover:shadow-lg transition-shadow">
        <div class="flex items-center justify-between mb-4">
          <div class="w-12 h-12 bg-gradient-icon rounded-xl flex items-center justify-center shadow-lg">
            <DollarSign class="w-6 h-6 text-white" />
          </div>
          <span class="flex items-center text-sm font-medium" :class="dashboardStats.todayCostChange >= 0 ? 'text-green-600' : 'text-red-600'">
            <component :is="dashboardStats.todayCostChange >= 0 ? TrendingUp : TrendingDown" class="w-4 h-4 mr-1" />
            {{ formatChange(dashboardStats.todayCostChange) }}
          </span>
        </div>
        <h3 class="text-slate-500 text-sm mb-1">
          {{ t('dashboard.todayCost') }}
        </h3>
        <p class="text-2xl font-bold text-slate-900">
          {{ formatCurrency(dashboardStats.todayCost) }}
        </p>
      </div>

      <div class="bg-white rounded-2xl p-6 border border-slate-200 shadow-sm hover:shadow-lg transition-shadow">
        <div class="flex items-center justify-between mb-4">
          <div class="w-12 h-12 bg-gradient-to-br from-amber-500 to-amber-600 rounded-xl flex items-center justify-center shadow-lg">
            <Zap class="w-6 h-6 text-white" />
          </div>
          <button
            @click="isRechargeModalOpen = true"
            class="flex items-center gap-1 px-3 py-1.5 bg-gradient-button text-white text-xs font-medium rounded-lg hover:shadow-lg hover:shadow-primary/25 transition-all"
          >
            <Plus class="w-3.5 h-3.5" />
            <span>{{ t('dashboard.recharge') }}</span>
          </button>
        </div>
        <h3 class="text-slate-500 text-sm mb-1">
          {{ t('dashboard.balance') }}
        </h3>
        <p class="text-2xl font-bold text-slate-900">
          {{ formatCurrency(dashboardStats.balance) }}
        </p>
      </div>
    </div>

    <!-- 图表区域 -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
      <div class="bg-white rounded-2xl p-6 border border-slate-200 shadow-sm hover:shadow-lg transition-shadow">
        <h2 class="text-lg font-semibold text-slate-900 mb-6">{{ t('dashboard.usageTrend') }}</h2>
        <div class="h-[300px]">
          <v-chart v-if="usageData.length" class="h-full w-full" :option="lineOption" autoresize />
          <div v-else class="h-full flex items-center justify-center text-slate-400">
            {{ loading ? t('common.loading') : t('common.noData') }}
          </div>
        </div>
      </div>

      <div class="bg-white rounded-2xl p-6 border border-slate-200 shadow-sm hover:shadow-lg transition-shadow">
        <h2 class="text-lg font-semibold text-slate-900 mb-6">{{ t('dashboard.modelDistribution') }}</h2>
        <div class="h-[300px]">
          <v-chart v-if="modelUsage.length" class="h-full w-full" :option="barOption" autoresize />
          <div v-else class="h-full flex items-center justify-center text-slate-400">
            {{ loading ? t('common.loading') : t('common.noData') }}
          </div>
        </div>
      </div>
    </div>

    <div class="bg-white rounded-3xl border-2 border-slate-200 shadow-sm overflow-hidden">
      <div class="bg-gradient-hero p-6">
        <div class="flex items-center justify-between gap-3">
          <div class="flex items-center gap-3">
            <div class="w-10 h-10 bg-white/25 rounded-xl flex items-center justify-center backdrop-blur-sm border border-white/30">
              <Activity class="w-5 h-5 text-white" />
            </div>
            <h2 class="text-xl font-bold text-white">{{ t('dashboard.usageRecords') }}</h2>
          </div>
          <button
            class="inline-flex items-center gap-2 px-3 py-1.5 text-sm font-medium text-white bg-white/15 border border-white/30 rounded-lg hover:bg-white/25 transition-colors disabled:opacity-60 disabled:cursor-not-allowed"
            :disabled="usageLoading"
            @click="handleUsageRefresh"
          >
            <RefreshCcw class="w-4 h-4" :class="{ 'animate-spin': usageLoading }" />
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
          <div class="hidden md:block overflow-x-auto">
            <table class="w-full">
              <thead>
                <tr class="border-b-2 border-slate-200">
                  <th class="text-left py-3 px-4 text-sm font-semibold text-slate-700">{{ t('dashboard.usageTable.time') }}</th>
                  <th class="text-left py-3 px-4 text-sm font-semibold text-slate-700 w-[180px]">{{ t('dashboard.usageTable.token') }}</th>
                  <!-- <th class="text-left py-3 px-4 text-sm font-semibold text-slate-700">{{ t('dashboard.usageTable.channel') }}</th> -->
                  <th class="text-left py-3 px-4 text-sm font-semibold text-slate-700 w-[140px]">{{ t('dashboard.usageTable.model') }}</th>
                  <th class="text-left py-3 px-4 text-sm font-semibold text-slate-700">{{ t('dashboard.usageTable.inputTokens') }}</th>
                  <th class="text-left py-3 px-4 text-sm font-semibold text-slate-700">{{ t('dashboard.usageTable.cacheHitTokens') }}</th>
                  <th class="text-left py-3 px-4 text-sm font-semibold text-slate-700">{{ t('dashboard.usageTable.outputTokens') }}</th>
                  <th class="text-left py-3 px-4 text-sm font-semibold text-slate-700">{{ t('dashboard.usageTable.totalTokens') }}</th>
                  <th class="text-right py-3 px-4 text-sm font-semibold text-slate-700 w-[120px]">{{ t('dashboard.usageTable.cost') }}</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="(record, index) in usageRecords"
                  :key="index"
                  class="border-b border-slate-100 hover:bg-slate-50 transition-colors"
                >
                  <td class="py-4 px-4 text-sm text-slate-600 whitespace-nowrap">{{ formatDateTime(record.time) }}</td>
                  <td class="py-4 px-4 text-sm text-slate-700 font-medium">
                    <div class="truncate max-w-[180px]" :title="record.token">{{ record.token }}</div>
                  </td>
                  <!-- <td class="py-4 px-4 text-sm text-slate-700 font-medium">
                    <div class="truncate max-w-[160px]" :title="record.channel">{{ record.channel }}</div>
                  </td> -->
                  <td class="py-4 px-4">
                    <div class="truncate max-w-[140px]" :title="record.model">
                      <span class="font-medium text-slate-900">{{ record.model }}</span>
                    </div>
                  </td>
                  <td class="py-4 px-4 text-left text-sm font-semibold text-slate-900 whitespace-nowrap">{{ formatCompactNumber(record.inputTokens) }}</td>
                  <td class="py-4 px-4 text-left text-sm font-semibold text-slate-900 whitespace-nowrap">{{ formatCompactNumber(record.cacheHitTokens) }}</td>
                  <td class="py-4 px-4 text-left text-sm font-semibold text-slate-900 whitespace-nowrap">{{ formatCompactNumber(record.outputTokens) }}</td>
                  <td class="py-4 px-4 text-left text-sm font-semibold text-slate-900 whitespace-nowrap">{{ formatCompactNumber(record.totalTokens) }}</td>
                  <td class="py-4 px-4 text-right text-sm font-semibold text-slate-900 whitespace-nowrap">{{ formatCurrency(record.cost, 'USD', undefined, 6) }}</td>
                </tr>
              </tbody>
            </table>
          </div>

          <div class="md:hidden space-y-4">
            <div
              v-for="(record, index) in usageRecords"
              :key="index"
              class="bg-slate-50 rounded-2xl p-4 border border-slate-200"
            >
              <div class="flex items-start justify-between gap-3 mb-3">
                <div>
                  <p class="font-semibold text-slate-900">{{ record.model }}</p>
                  <p class="text-sm text-slate-500">{{ formatDateTime(record.time) }}</p>
                  <div class="flex flex-wrap items-center gap-2 mt-2">
                    <span class="inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-medium bg-primary/10 text-primary border border-primary/15">
                      {{ t('dashboard.usageTable.token') }}：{{ record.token }}
                    </span>
                    <!-- <span class="inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-medium bg-slate-200/60 text-slate-700 border border-slate-200">
                      {{ t('dashboard.usageTable.channel') }}：{{ record.channel }}
                    </span> -->
                  </div>
                </div>
                <div class="text-right">
                  <p class="text-xs text-slate-500">{{ t('dashboard.usageTable.cost') }}</p>
                  <p class="font-bold text-slate-900">{{ formatCurrency(record.cost, 'USD', undefined, 6) }}</p>
                </div>
              </div>

              <div class="grid grid-cols-2 gap-3">
                <div class="bg-white rounded-xl p-3 border border-slate-200">
                  <p class="text-xs text-slate-500 mb-1">{{ t('dashboard.usageTable.inputTokens') }}</p>
                  <p class="text-sm font-semibold text-slate-900">{{ formatCompactNumber(record.inputTokens) }}</p>
                </div>
                <div class="bg-white rounded-xl p-3 border border-slate-200">
                  <p class="text-xs text-slate-500 mb-1">{{ t('dashboard.usageTable.cacheHitTokens') }}</p>
                  <p class="text-sm font-semibold text-slate-900">{{ formatCompactNumber(record.cacheHitTokens) }}</p>
                </div>
                <div class="bg-white rounded-xl p-3 border border-slate-200">
                  <p class="text-xs text-slate-500 mb-1">{{ t('dashboard.usageTable.outputTokens') }}</p>
                  <p class="text-sm font-semibold text-slate-900">{{ formatCompactNumber(record.outputTokens) }}</p>
                </div>
                <div class="bg-white rounded-xl p-3 border border-slate-200">
                  <p class="text-xs text-slate-500 mb-1">{{ t('dashboard.usageTable.totalTokens') }}</p>
                  <p class="text-sm font-semibold text-slate-900">{{ formatCompactNumber(record.totalTokens) }}</p>
                </div>
              </div>
            </div>
          </div>

          <div class="mt-6 border-slate-200 flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
            <p class="text-sm text-slate-500">
              {{ t('dashboard.pagination.summary', { total: usageTotal, page: usageCurrentPage, pages: usageTotalPages }) }}
            </p>
            <div class="flex items-center gap-2">
              <button
                class="px-3 py-1.5 rounded-lg border border-slate-300 text-sm transition-colors"
                :class="usageCurrentPage <= 1 || usageLoading ? 'text-slate-300 cursor-not-allowed bg-slate-50' : 'text-slate-700 hover:bg-slate-100'"
                :disabled="usageCurrentPage <= 1 || usageLoading"
                @click="handleUsagePageChange(usageCurrentPage - 1)"
              >
                {{ t('dashboard.pagination.prev') }}
              </button>
              <span class="text-sm text-slate-600 min-w-[72px] text-center">
                {{ usageCurrentPage }} / {{ usageTotalPages }}
              </span>
              <button
                class="px-3 py-1.5 rounded-lg border border-slate-300 text-sm transition-colors"
                :class="usageCurrentPage >= usageTotalPages || usageLoading ? 'text-slate-300 cursor-not-allowed bg-slate-50' : 'text-slate-700 hover:bg-slate-100'"
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

    <!-- 充值弹窗 -->
    <div v-if="isRechargeModalOpen" class="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center z-50 p-4">
      <div class="bg-white rounded-3xl max-w-md w-full p-8 shadow-2xl">
        <div class="flex items-center gap-3 mb-6">
          <div class="w-12 h-12 bg-gradient-icon rounded-2xl flex items-center justify-center shadow-lg">
            <CreditCard class="w-6 h-6 text-white" />
          </div>
          <div>
            <h2 class="text-xl font-bold text-slate-900">
              {{ t('dashboard.recharge') }}
            </h2>
            <p class="text-sm text-slate-500">
              {{ t('dashboard.balance') }} {{ t('dashboard.recharge') }}
            </p>
          </div>
        </div>

        <div class="bg-slate-50 rounded-2xl p-4 mb-6">
          <div class="mb-4">
            <label class="block text-sm font-semibold text-slate-900 mb-2">
              {{ t('dashboard.rechargeAmount') }} ({{ t('dashboard.usd') }})
            </label>
            <div class="relative">
              <span class="absolute left-3 top-1/2 -translate-y-1/2 text-slate-500 font-medium">
                $
              </span>
              <input
                v-model="usdAmount"
                type="number"
                :placeholder="t('dashboard.inputAmountPlaceholder')"
                min="0"
                step="0.01"
                class="w-full pl-8 pr-4 py-3 border border-slate-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-ring focus:border-transparent bg-white"
              />
            </div>
          </div>

          <div class="flex items-center justify-between mb-2">
            <span class="text-slate-600">{{ t('dashboard.rechargeAmount') }}</span>
            <span class="font-semibold text-slate-900">
              ${{ usdAmount || "0.00" }}
            </span>
          </div>
          <div class="flex items-center justify-between mb-2">
            <span class="text-slate-600 text-sm">
              {{ t('dashboard.exchangeRate') }}
            </span>
            <span class="text-sm text-slate-600">
              $1 = ¥0.2
            </span>
          </div>
          <div class="flex items-center justify-between pt-2 border-t border-slate-200">
            <span class="text-slate-900 font-semibold">
              {{ t('dashboard.payAmount') }}
            </span>
            <span class="text-2xl font-bold text-slate-900">
              ¥{{ calculateCnyAmount.toFixed(2) }}
            </span>
          </div>
        </div>

        <div class="mb-6">
          <h3 class="text-sm font-semibold text-slate-900 mb-3">
            {{ t('dashboard.paymentMethod') }}
          </h3>
          <div class="space-y-2">
            <label
              v-for="method in paymentMethods"
              :key="method.id"
              class="flex items-center gap-3 p-3 border-2 rounded-xl cursor-pointer transition-colors"
              :class="[
                selectedPayment === method.id
                  ? 'border-primary bg-primary/5'
                  : 'border-slate-200 hover:border-slate-300'
              ]"
            >
              <input
                type="radio"
                name="payment"
                :value="method.id"
                v-model="selectedPayment"
                class="w-4 h-4 text-primary"
              />
              <span class="text-2xl">{{ method.icon }}</span>
              <span class="font-medium text-slate-900">
                {{ method.name }}
              </span>
            </label>
          </div>
        </div>

        <div class="flex gap-3">
          <button
            @click="() => { isRechargeModalOpen = false; usdAmount = '' }"
            class="flex-1 px-4 py-3 border border-slate-300 text-slate-700 rounded-xl hover:bg-slate-50 transition-colors font-medium"
          >
            {{ t('common.cancel') }}
          </button>
          <button
            @click="handleConfirmRecharge"
            class="flex-1 px-4 py-3 bg-gradient-button text-white rounded-xl hover:shadow-lg hover:shadow-primary/25 transition-all duration-200 font-medium"
          >
            {{ t('common.confirm') }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
