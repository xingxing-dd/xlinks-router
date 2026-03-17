<script setup>
import { ref, computed } from 'vue'
import { 
  TrendingUp, 
  TrendingDown, 
  Activity, 
  DollarSign, 
  Key, 
  Zap,
  CreditCard,
  Plus
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

const usageData = [
  { date: '03-03', tokens: 12000, cost: 240 },
  { date: '03-04', tokens: 15000, cost: 300 },
  { date: '03-05', tokens: 18000, cost: 360 },
  { date: '03-06', tokens: 14000, cost: 280 },
  { date: '03-07', tokens: 22000, cost: 440 },
  { date: '03-08', tokens: 25000, cost: 500 },
  { date: '03-09', tokens: 28000, cost: 560 },
]

const modelUsage = [
  { model: 'GPT-4', requests: 850 },
  { model: 'GPT-3.5', requests: 1200 },
  { model: 'Claude-3', requests: 650 },
  { model: 'Gemini', requests: 420 },
]

const paymentMethods = [
  { id: 'alipay', name: '支付宝', icon: '💳' },
  { id: 'wechat', name: '微信支付', icon: '💚' },
]

const isRechargeModalOpen = ref(false)
const usdAmount = ref('')
const selectedPayment = ref('alipay')

const calculateCnyAmount = computed(() => {
  const amount = parseFloat(usdAmount.value)
  if (isNaN(amount) || amount <= 0) return 0
  return amount * 0.2
})

const handleConfirmRecharge = () => {
  const amount = parseFloat(usdAmount.value)
  if (isNaN(amount) || amount <= 0) {
    alert('请输入有效的充值金额')
    return
  }
  alert('正在跳转到支付页面...')
  isRechargeModalOpen.value = false
  usdAmount.value = ''
}

const lineOption = ref({
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
    data: usageData.map(d => d.date),
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
      data: usageData.map(d => d.tokens),
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 10,
      itemStyle: { color: '#8b5cf6' },
      lineStyle: {
        width: 3,
        color: {
          type: 'linear',
          x: 0, y: 0, x2: 1, y2: 0,
          colorStops: [
            { offset: 0, color: '#8b5cf6' },
            { offset: 1, color: '#d946ef' }
          ]
        }
      }
    }
  ]
})

const barOption = ref({
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
    data: modelUsage.map(d => d.model),
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
      data: modelUsage.map(d => d.requests),
      type: 'bar',
      barWidth: '40%',
      itemStyle: {
        borderRadius: [12, 12, 0, 0],
        color: {
          type: 'linear',
          x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: '#8b5cf6' },
            { offset: 1, color: '#d946ef' }
          ]
        }
      }
    }
  ]
})

const activities = [
  { time: '2 分钟前', event: 'GPT-4 API 调用成功', tokens: '1,245 tokens', status: 'success' },
  { time: '15 分钟前', event: 'Claude-3 API 调用成功', tokens: '856 tokens', status: 'success' },
  { time: '1 小时前', event: '账户充值 ¥500', tokens: '', status: 'info' },
  { time: '2 小时前', event: 'GPT-3.5 API 调用失败', tokens: '重试中', status: 'error' },
  { time: '3 小时前', event: '新增 API Key', tokens: 'sk-***abc', status: 'info' },
]
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
          <span class="flex items-center text-sm text-green-600 font-medium">
            <TrendingUp class="w-4 h-4 mr-1" />
            12.5%
          </span>
        </div>
        <h3 class="text-slate-500 text-sm mb-1">
          今日请求数
        </h3>
        <p class="text-2xl font-bold text-slate-900">
          3,245
        </p>
      </div>

      <div class="bg-white rounded-2xl p-6 border border-slate-200 shadow-sm hover:shadow-lg transition-shadow">
        <div class="flex items-center justify-between mb-4">
          <div class="w-12 h-12 bg-gradient-to-br from-emerald-500 to-emerald-600 rounded-xl flex items-center justify-center shadow-lg">
            <Key class="w-6 h-6 text-white" />
          </div>
          <span class="flex items-center text-sm text-green-600 font-medium">
            <TrendingUp class="w-4 h-4 mr-1" />
            8.2%
          </span>
        </div>
        <h3 class="text-slate-500 text-sm mb-1">
          Token 消耗
        </h3>
        <p class="text-2xl font-bold text-slate-900">
          28.5K
        </p>
      </div>

      <div class="bg-white rounded-2xl p-6 border border-slate-200 shadow-sm hover:shadow-lg transition-shadow">
        <div class="flex items-center justify-between mb-4">
          <div class="w-12 h-12 bg-gradient-to-br from-purple-500 to-purple-600 rounded-xl flex items-center justify-center shadow-lg">
            <DollarSign class="w-6 h-6 text-white" />
          </div>
          <span class="flex items-center text-sm text-red-600 font-medium">
            <TrendingDown class="w-4 h-4 mr-1" />
            3.1%
          </span>
        </div>
        <h3 class="text-slate-500 text-sm mb-1">
          今日费用
        </h3>
        <p class="text-2xl font-bold text-slate-900">
          ¥56.80
        </p>
      </div>

      <div class="bg-white rounded-2xl p-6 border border-slate-200 shadow-sm hover:shadow-lg transition-shadow">
        <div class="flex items-center justify-between mb-4">
          <div class="w-12 h-12 bg-gradient-to-br from-amber-500 to-amber-600 rounded-xl flex items-center justify-center shadow-lg">
            <Zap class="w-6 h-6 text-white" />
          </div>
          <button
            @click="isRechargeModalOpen = true"
            class="flex items-center gap-1 px-3 py-1.5 bg-gradient-to-r from-indigo-500 to-purple-500 text-white text-xs font-medium rounded-lg hover:shadow-lg transition-all"
          >
            <Plus class="w-3.5 h-3.5" />
            <span>充值</span>
          </button>
        </div>
        <h3 class="text-slate-500 text-sm mb-1">
          账户余额
        </h3>
        <p class="text-2xl font-bold text-slate-900">
          ¥1,258.00
        </p>
      </div>
    </div>

    <!-- 图表区域 -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
      <div class="bg-white rounded-2xl p-6 border border-slate-200 shadow-sm hover:shadow-lg transition-shadow">
        <h2 class="text-lg font-semibold text-slate-900 mb-6">Token 使用趋势</h2>
        <div class="h-[300px]">
          <v-chart class="h-full w-full" :option="lineOption" autoresize />
        </div>
      </div>

      <div class="bg-white rounded-2xl p-6 border border-slate-200 shadow-sm hover:shadow-lg transition-shadow">
        <h2 class="text-lg font-semibold text-slate-900 mb-6">模型使用分布</h2>
        <div class="h-[300px]">
          <v-chart class="h-full w-full" :option="barOption" autoresize />
        </div>
      </div>
    </div>

    <!-- 最近活动 -->
    <div class="bg-white rounded-2xl p-6 border border-slate-200 shadow-sm hover:shadow-lg transition-shadow">
      <h2 class="text-lg font-semibold text-slate-900 mb-6">最近活动</h2>
      <div class="space-y-4">
        <div
          v-for="(activity, index) in activities"
          :key="index"
          class="flex items-center justify-between py-3 border-b border-slate-100 last:border-0"
        >
          <div class="flex items-center gap-4">
            <div
              class="w-2 h-2 rounded-full"
              :class="[
                activity.status === 'success' ? 'bg-green-500' : 
                activity.status === 'error' ? 'bg-red-500' : 
                'bg-blue-500'
              ]"
            />
            <div>
              <p class="text-slate-900 font-medium">{{ activity.event }}</p>
              <p class="text-sm text-slate-500">{{ activity.time }}</p>
            </div>
          </div>
          <span v-if="activity.tokens" class="text-sm text-slate-600 font-medium">
            {{ activity.tokens }}
          </span>
        </div>
      </div>
    </div>

    <!-- 充值弹窗 -->
    <div v-if="isRechargeModalOpen" class="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center z-50 p-4">
      <div class="bg-white rounded-3xl max-w-md w-full p-8 shadow-2xl">
        <div class="flex items-center gap-3 mb-6">
          <div class="w-12 h-12 bg-gradient-to-br from-indigo-500 to-purple-500 rounded-2xl flex items-center justify-center shadow-lg">
            <CreditCard class="w-6 h-6 text-white" />
          </div>
          <div>
            <h2 class="text-xl font-bold text-slate-900">
              账户充值
            </h2>
            <p class="text-sm text-slate-500">
              余额充值
            </p>
          </div>
        </div>

        <div class="bg-slate-50 rounded-2xl p-4 mb-6">
          <div class="mb-4">
            <label class="block text-sm font-semibold text-slate-900 mb-2">
              充值金额 (美元)
            </label>
            <div class="relative">
              <span class="absolute left-3 top-1/2 -translate-y-1/2 text-slate-500 font-medium">
                $
              </span>
              <input
                v-model="usdAmount"
                type="number"
                placeholder="请输入充值金额"
                min="0"
                step="0.01"
                class="w-full pl-8 pr-4 py-3 border border-slate-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent bg-white"
              />
            </div>
          </div>

          <div class="flex items-center justify-between mb-2">
            <span class="text-slate-600">充值金额</span>
            <span class="font-semibold text-slate-900">
              ${{ usdAmount || "0.00" }}
            </span>
          </div>
          <div class="flex items-center justify-between mb-2">
            <span class="text-slate-600 text-sm">
              兑换比例
            </span>
            <span class="text-sm text-slate-600">
              $1 = ¥0.2
            </span>
          </div>
          <div class="flex items-center justify-between pt-2 border-t border-slate-200">
            <span class="text-slate-900 font-semibold">
              支付金额
            </span>
            <span class="text-2xl font-bold text-slate-900">
              ¥{{ calculateCnyAmount.toFixed(2) }}
            </span>
          </div>
        </div>

        <div class="mb-6">
          <h3 class="text-sm font-semibold text-slate-900 mb-3">
            支付方式
          </h3>
          <div class="space-y-2">
            <label
              v-for="method in paymentMethods"
              :key="method.id"
              class="flex items-center gap-3 p-3 border-2 rounded-xl cursor-pointer transition-colors"
              :class="[
                selectedPayment === method.id
                  ? 'border-indigo-500 bg-indigo-50'
                  : 'border-slate-200 hover:border-slate-300'
              ]"
            >
              <input
                type="radio"
                name="payment"
                :value="method.id"
                v-model="selectedPayment"
                class="w-4 h-4 text-indigo-600"
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
            取消
          </button>
          <button
            @click="handleConfirmRecharge"
            class="flex-1 px-4 py-3 bg-gradient-to-r from-indigo-500 to-purple-500 text-white rounded-xl hover:shadow-lg transition-all duration-200 font-medium"
          >
            确认支付
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
