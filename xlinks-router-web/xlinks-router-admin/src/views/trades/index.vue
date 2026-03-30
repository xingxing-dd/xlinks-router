<script setup>
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { formatCurrency } from '@/utils/formatters'

const { t } = useI18n()

const keyword = ref('')
const status = ref('')

const rows = [
  { id: 'TR8001', merchant: '未来科技', amount: 32800, status: 'success', createdAt: '2026-03-24 12:30' },
  { id: 'TR8002', merchant: '星云智能', amount: 9800, status: 'pending', createdAt: '2026-03-24 09:20' },
  { id: 'TR8003', merchant: '蓝鲸数据', amount: 4800, status: 'failed', createdAt: '2026-03-23 19:10' },
]
</script>

<template>
  <div class="p-6 space-y-6">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold text-slate-900">{{ t('trades.title') }}</h1>
        <p class="text-slate-500 mt-1">查看订单、支付与结算状态（静态示例）</p>
      </div>
      <button class="btn-primary">导出报表</button>
    </div>

    <div class="card">
      <div class="card-body grid gap-4 md:grid-cols-4">
        <div>
          <input v-model.trim="keyword" class="input mt-2" placeholder="订单号/商户" />
        </div>
        <div>
          <select v-model="status" class="input mt-2">
            <option value="">全部</option>
            <option value="success">成功</option>
            <option value="pending">处理中</option>
            <option value="failed">失败</option>
          </select>
        </div>
        <div class="md:col-span-2 flex items-end justify-end gap-3">
          <button class="btn-primary h-11">{{ t('common.search') }}</button>
          <button class="btn-outline h-11">{{ t('common.reset') }}</button>
        </div>
      </div>
    </div>

    <div class="card">
      <div class="card-header">
        <h2 class="card-title">交易列表</h2>
        <span class="text-sm text-slate-400">共 {{ rows.length }} 条</span>
      </div>
      <div class="card-body">
        <table class="table">
          <thead>
            <tr>
              <th>订单号</th>
              <th>商户</th>
              <th>金额</th>
              <th>状态</th>
              <th>创建时间</th>
              <th class="text-right">{{ t('common.action') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in rows" :key="row.id">
              <td>{{ row.id }}</td>
              <td>{{ row.merchant }}</td>
              <td>{{ formatCurrency(row.amount) }}</td>
              <td>
                <span
                  class="badge"
                  :class="row.status === 'success'
                    ? 'badge-success'
                    : row.status === 'pending'
                      ? 'badge-warning'
                      : 'badge-danger'"
                >
                  {{ row.status === 'success' ? '成功' : row.status === 'pending' ? '处理中' : '失败' }}
                </span>
              </td>
              <td>{{ row.createdAt }}</td>
              <td>
                <div class="flex items-center justify-end gap-2">
                  <button class="btn-outline">查看</button>
                  <button class="btn-primary">退款</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>
