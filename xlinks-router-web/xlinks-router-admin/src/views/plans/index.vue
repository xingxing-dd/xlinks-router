<script setup>
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

const keyword = ref('')
const status = ref('')

const rows = [
  { id: 'PL5001', name: '企业标准版', price: '¥9,800/月', quota: '500万 tokens', status: 'active' },
  { id: 'PL5002', name: '企业高级版', price: '¥18,800/月', quota: '1200万 tokens', status: 'active' },
  { id: 'PL5003', name: '试用体验包', price: '¥0', quota: '10万 tokens', status: 'inactive' },
]
</script>

<template>
  <div class="p-6 space-y-6">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold text-slate-900">{{ t('plans.title') }}</h1>
        <p class="text-slate-500 mt-1">配置套餐权益、计费与上下架（静态示例）</p>
      </div>
      <button class="btn-primary">新增套餐</button>
    </div>

    <div class="card">
      <div class="card-body grid gap-4 md:grid-cols-4">
        <div>
          <input v-model.trim="keyword" class="input mt-2" placeholder="套餐名称/ID" />
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
        <h2 class="card-title">套餐列表</h2>
        <span class="text-sm text-slate-400">共 {{ rows.length }} 条</span>
      </div>
      <div class="card-body">
        <table class="table">
          <thead>
            <tr>
              <th>套餐 ID</th>
              <th>套餐名称</th>
              <th>价格</th>
              <th>额度</th>
              <th>状态</th>
              <th class="text-right">{{ t('common.action') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in rows" :key="row.id">
              <td>{{ row.id }}</td>
              <td>{{ row.name }}</td>
              <td>{{ row.price }}</td>
              <td>{{ row.quota }}</td>
              <td>
                <span class="badge" :class="row.status === 'active' ? 'badge-success' : 'badge-danger'">
                  {{ row.status === 'active' ? '启用' : '停用' }}
                </span>
              </td>
              <td>
                <div class="flex items-center justify-end gap-2">
                  <button class="btn-outline">查看</button>
                  <button class="btn-primary">编辑</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>
