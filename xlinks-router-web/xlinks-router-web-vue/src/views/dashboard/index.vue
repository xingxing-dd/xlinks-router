<script setup>
import { ref } from 'vue'
import { 
  TrendingUp, 
  TrendingDown, 
  Activity, 
  DollarSign, 
  Key, 
  Zap
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
      <div class="bg-white rounded-2xl p-6 border border-slate-200 shadow-sm hover:shadow-lg transition-shadow group">
        <div class="flex items-center justify-between mb-4">
          <div class="w-12 h-12 bg-gradient-to-br from-blue-500 to-cyan-500 rounded-xl flex items-center justify-center shadow-lg group-hover:scale-110 transition-transform">
            <Activity class="w-6 h-6 text-white" />
          </div>
          <span class="flex items-center text-sm text-green-600 font-medium">
            <TrendingUp class="w-4 h-4 mr-1" />
            12.5%
          </span>
        </div>
        <h3 class="text-slate-500 text-sm mb-1">今日请求数</h3>
        <p class="text-2xl font-bold text-slate-900">3,245</p>
      </div>

      <div class="bg-white rounded-2xl p-6 border border-slate-200 shadow-sm hover:shadow-lg transition-shadow group">
        <div class="flex items-center justify-between mb-4">
          <div class="w-12 h-12 bg-gradient-to-br from-green-500 to-emerald-500 rounded-xl flex items-center justify-center shadow-lg group-hover:scale-110 transition-transform">
            <Key class="w-6 h-6 text-white" />
          </div>
          <span class="flex items-center text-sm text-green-600 font-medium">
            <TrendingUp class="w-4 h-4 mr-1" />
            8.2%
          </span>
        </div>
        <h3 class="text-slate-500 text-sm mb-1">Token 消耗</h3>
        <p class="text-2xl font-bold text-slate-900">28.5K</p>
      </div>

      <div class="bg-white rounded-2xl p-6 border border-slate-200 shadow-sm hover:shadow-lg transition-shadow group">
        <div class="flex items-center justify-between mb-4">
          <div class="w-12 h-12 bg-gradient-to-br from-violet-500 to-purple-500 rounded-xl flex items-center justify-center shadow-lg group-hover:scale-110 transition-transform">
            <DollarSign class="w-6 h-6 text-white" />
          </div>
          <span class="flex items-center text-sm text-red-600 font-medium">
            <TrendingDown class="w-4 h-4 mr-1" />
            3.1%
          </span>
        </div>
        <h3 class="text-slate-500 text-sm mb-1">今日费用</h3>
        <p class="text-2xl font-bold text-slate-900">¥56.80</p>
      </div>

      <div class="bg-white rounded-2xl p-6 border border-slate-200 shadow-sm hover:shadow-lg transition-shadow group">
        <div class="flex items-center justify-between mb-4">
          <div class="w-12 h-12 bg-gradient-to-br from-orange-500 to-amber-500 rounded-xl flex items-center justify-center shadow-lg group-hover:scale-110 transition-transform">
            <Zap class="w-6 h-6 text-white" />
          </div>
          <span class="text-sm text-slate-500">剩余</span>
        </div>
        <h3 class="text-slate-500 text-sm mb-1">账户余额</h3>
        <p class="text-2xl font-bold text-slate-900">¥1,258.00</p>
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
  </div>
</template>
