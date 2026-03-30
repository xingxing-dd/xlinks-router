<script setup>
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

const keyword = ref('')
const status = ref('')

const rows = [
  { id: 'M4001', name: 'GPT-4', provider: 'OpenAI', status: 'active', price: '¥0.06/1K', updatedAt: '2026-03-23 18:00' },
  { id: 'M4002', name: 'Claude 3 Haiku', provider: 'Anthropic', status: 'active', price: '¥0.02/1K', updatedAt: '2026-03-22 14:30' },
  { id: 'M4003', name: 'Llama 3', provider: 'Azure', status: 'inactive', price: '¥0.01/1K', updatedAt: '2026-03-21 08:50' },
]
</script>

<template>
  <div class="p-6 space-y-6">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold text-slate-900">{{ t('models.title') }}</h1>
        <p class="text-slate-500 mt-1">配置模型接入、计费与可见性（静态示例）</p>
      </div>
      <button class="btn-primary">新增模型</button>
    </div>

    <div class="card">
      <div class="card-body grid gap-4 md:grid-cols-4">
        <div>
          <input v-model.trim="keyword" class="input mt-2" placeholder="模型名称/编码" />
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
        <h2 class="card-title">模型列表</h2>
        <span class="text-sm text-slate-400">共 {{ rows.length }} 条</span>
      </div>
      <div class="card-body">
        <table class="table">
          <thead>
            <tr>
              <th>模型 ID</th>
              <th>模型名称</th>
              <th>服务商</th>
              <th>价格</th>
              <th>更新时间</th>
              <th>状态</th>
              <th class="text-right">{{ t('common.action') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in rows" :key="row.id">
              <td>{{ row.id }}</td>
              <td>{{ row.name }}</td>
              <td>{{ row.provider }}</td>
              <td>{{ row.price }}</td>
              <td>{{ row.updatedAt }}</td>
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
