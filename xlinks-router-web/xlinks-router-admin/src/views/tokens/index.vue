<script setup>
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

const keyword = ref('')
const status = ref('')

const rows = [
  { id: 'T3001', name: '主站 Token', merchant: '未来科技', status: 'active', usage: 12450, lastUsed: '2026-03-24 14:20' },
  { id: 'T3002', name: '风控 Token', merchant: '星云智能', status: 'active', usage: 7820, lastUsed: '2026-03-23 09:10' },
  { id: 'T3003', name: '测试 Token', merchant: '蓝鲸数据', status: 'inactive', usage: 320, lastUsed: '2026-03-18 16:45' },
]
</script>

<template>
  <div class="p-6 space-y-6">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold text-slate-900">{{ t('tokens.title') }}</h1>
        <p class="text-slate-500 mt-1">查询 Token 配额、使用量与状态（静态示例）</p>
      </div>
      <button class="btn-primary">新增 Token</button>
    </div>

    <div class="card">
      <div class="card-body grid gap-4 md:grid-cols-4">
        <div>
          <input v-model.trim="keyword" class="input mt-2" placeholder="Token/商户名称" />
        </div>
        <div>
          <select v-model="status" class="input mt-2">
            <option value="">全部</option>
            <option value="active">启用</option>
            <option value="inactive">停用</option>
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
        <h2 class="card-title">Token 列表</h2>
        <span class="text-sm text-slate-400">共 {{ rows.length }} 条</span>
      </div>
      <div class="card-body">
        <table class="table">
          <thead>
            <tr>
              <th>Token ID</th>
              <th>名称</th>
              <th>商户</th>
              <th>累计请求</th>
              <th>最近使用</th>
              <th>状态</th>
              <th class="text-right">{{ t('common.action') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in rows" :key="row.id">
              <td>{{ row.id }}</td>
              <td>{{ row.name }}</td>
              <td>{{ row.merchant }}</td>
              <td>{{ row.usage.toLocaleString() }}</td>
              <td>{{ row.lastUsed }}</td>
              <td>
                <span class="badge" :class="row.status === 'active' ? 'badge-success' : 'badge-danger'">
                  {{ row.status === 'active' ? '启用' : '停用' }}
                </span>
              </td>
              <td>
                <div class="flex items-center justify-end gap-2">
                  <button class="btn-outline">查看</button>
                  <button class="btn-primary">停用</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>
